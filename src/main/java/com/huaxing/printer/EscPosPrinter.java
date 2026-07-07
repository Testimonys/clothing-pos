package com.huaxing.printer;

import com.huaxing.entity.Order;
import com.huaxing.entity.OrderItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ESC/POS 热敏小票打印机服务
 *
 * 生成 ESC/POS 指令序列并通过 USB 端口发送到打印机。
 * 如果 USB 写入失败，自动降级输出到 receipt_debug.txt 文件。
 *
 * 由配置项 app.printer.enabled 控制是否启用（@ConditionalOnProperty）。
 */
@Component
@ConditionalOnProperty(name = "app.printer.enabled", havingValue = "true", matchIfMissing = false)
public class EscPosPrinter {

    private static final Logger log = LoggerFactory.getLogger(EscPosPrinter.class);
    private static final int LINE_WIDTH = 32;

    @Value("${app.printer.port:}")
    private String printerPort;

    @Value("${app.store.name:华兴服装店}")
    private String storeName;

    private Charset gbkCharset;

    @PostConstruct
    public void init() {
        try {
            gbkCharset = Charset.forName("GBK");
        } catch (Exception e) {
            gbkCharset = Charset.defaultCharset();
            log.warn("GBK charset not available, fallback to default: {}", gbkCharset);
        }
        log.info("ESC/POS Printer initialized, storeName={}, port='{}'", storeName,
                printerPort != null && !printerPort.isEmpty() ? printerPort : "(auto-detect)");
    }

    // ==================== Public API ====================

    /**
     * 打印订单小票
     *
     * @param order 订单实体
     * @param items 订单明细列表
     */
    public void printOrder(Order order, List<OrderItem> items) {
        if (order == null) {
            log.warn("printOrder: order is null, skipping");
            return;
        }
        if (items == null || items.isEmpty()) {
            log.warn("printOrder: items is null/empty for orderNo={}, printing header only",
                    order.getOrderNo());
        }

        byte[] receiptData = buildReceipt(order, items != null ? items : List.of());

        if (!writeToPort(receiptData)) {
            writeToFile(receiptData, order);
        }
    }

    // ==================== ESC/POS 指令构建 ====================

    /**
     * 构建完整的 ESC/POS 指令序列
     *
     * 序列：初始化 → 居中店名 → 单号/时间 → 分隔线 → 商品明细表 → 分隔线
     *       → 合计/折扣/实付 → 支付方式 → 谢谢惠顾 → 切纸
     */
    private byte[] buildReceipt(Order order, List<OrderItem> items) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(512);

        try {
            // === 1. 初始化打印机 ===
            bos.write(ESC_INIT);                     // ESC @

            // === 2. 居中打印店名 ===
            bos.write(ESC_ALIGN_CENTER);             // ESC a 1
            bos.write(encodeLine(centerText(storeName, LINE_WIDTH)));
            bos.write(LF);

            // === 3. 左对齐打印单号和日期时间 ===
            bos.write(ESC_ALIGN_LEFT);               // ESC a 0

            String orderNoLine = "单号: " + order.getOrderNo();
            String timeLine = "时间: " + formatDateTime(order.getCreateTime());
            bos.write(encodeLine(orderNoLine));
            bos.write(encodeLine(timeLine));

            // === 4. 分隔线 ===
            bos.write(encodeLine(SEPARATOR));

            // === 5. 商品列表表头 ===
            bos.write(encodeLine(formatItemHeader()));

            // === 6. 分隔线 ===
            bos.write(encodeLine(SEPARATOR));

            // === 7. 商品明细 ===
            for (OrderItem item : items) {
                bos.write(encodeLine(formatItemRow(item)));
            }

            // === 8. 分隔线 ===
            bos.write(encodeLine(SEPARATOR));

            // === 9. 合计、折扣、实付 ===
            bos.write(encodeLine(formatAmountLine("合计:", order.getTotalAmount())));
            if (order.getDiscount() != null
                    && order.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                bos.write(encodeLine(formatAmountLine("折扣:", order.getDiscount().negate())));
            }
            bos.write(encodeLine(SEPARATOR));
            bos.write(encodeLine(formatAmountLine("实付:", order.getPayAmount())));

            // === 10. 支付方式 ===
            String payMethodText = payMethodDisplay(order);
            bos.write(encodeLine("支付方式: " + payMethodText));

            // === 11. 分隔线 ===
            bos.write(encodeLine(SEPARATOR));

            // === 12. 谢谢惠顾（居中） ===
            bos.write(ESC_ALIGN_CENTER);
            bos.write(encodeLine(centerText("谢谢惠顾！", LINE_WIDTH)));
            bos.write(encodeLine(centerText("欢迎下次光临", LINE_WIDTH)));

            // === 13. 走纸 + 切纸 ===
            bos.write(ESC_ALIGN_LEFT);               // reset alignment
            bos.write(LF);
            bos.write(LF);
            bos.write(LF);
            bos.write(GS_CUT);                       // GS V 0

        } catch (IOException e) {
            log.error("构建小票数据异常", e);
        }

        return bos.toByteArray();
    }

    // ==================== USB 端口写入 ====================

    /**
     * 将指令数据写入 USB 打印机端口
     *
     * @return true=写入成功, false=失败（将触发降级到文件）
     */
    private boolean writeToPort(byte[] data) {
        String port = resolvePort();
        if (port == null || port.isEmpty()) {
            log.warn("无法自动检测打印机端口，将降级到文件输出");
            return false;
        }

        try (OutputStream out = new FileOutputStream(port)) {
            out.write(data);
            out.flush();
            log.info("小票已发送到打印机端口: {}", port);
            return true;
        } catch (IOException e) {
            log.warn("写入打印机端口失败 ({}): {}，将降级到文件输出", port, e.getMessage());
            return false;
        }
    }

    /**
     * 降级方案：将小票数据写入 receipt_debug.txt 文件
     */
    private void writeToFile(byte[] data, Order order) {
        String filePath = "receipt_debug.txt";
        try (FileOutputStream fos = new FileOutputStream(filePath, true)) {
            String header = "\n===== 小票 " + order.getOrderNo()
                    + " @ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + " =====\n";
            fos.write(header.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fos.write(data);
            fos.flush();
            log.info("小票已输出到文件: {}", filePath);
        } catch (IOException e) {
            log.error("写入小票文件失败: {}", e.getMessage());
        }
    }

    /**
     * 解析打印机端口：优先使用配置值，否则根据操作系统猜测
     */
    private String resolvePort() {
        if (printerPort != null && !printerPort.isEmpty()) {
            return printerPort;
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return "/dev/cu.usbprinter";
        }
        if (os.contains("linux")) {
            return "/dev/usb/lp0";
        }
        // Windows 需要用户显式配置端口名，如 "COM3"
        return null;
    }

    // ==================== ESC/POS 常量 ====================

    /** 初始化打印机 */
    private static final byte[] ESC_INIT       = new byte[]{0x1B, 0x40};
    /** 居中对齐 */
    private static final byte[] ESC_ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01};
    /** 左对齐 */
    private static final byte[] ESC_ALIGN_LEFT   = new byte[]{0x1B, 0x61, 0x00};
    /** 全切纸 */
    private static final byte[] GS_CUT         = new byte[]{0x1D, 0x56, 0x00};
    /** 换行 */
    private static final byte[] LF             = new byte[]{0x0A};
    /** 分隔线字符串 */
    private static final String SEPARATOR      = "--------------------------------";

    // ==================== 格式辅助方法 ====================

    /**
     * 用 GBK 编码将文本转为字节数组并追加换行
     */
    private byte[] encodeLine(String text) {
        return (text + "\n").getBytes(gbkCharset);
    }

    /**
     * 使文本居中（用空格补齐两侧）
     */
    private static String centerText(String text, int width) {
        if (text == null) text = "";
        // 对于含中文的文本，使用字节长度近似计算
        int visualLen = visualWidth(text);
        if (visualLen >= width) {
            return text;
        }
        int padding = (width - visualLen) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    /**
     * 估算字符串的视觉宽度（中文算2，英文算1）
     */
    private static int visualWidth(String s) {
        if (s == null) return 0;
        int w = 0;
        for (char c : s.toCharArray()) {
            w += (c > 0x7E) ? 2 : 1;
        }
        return w;
    }

    /**
     * 商品列表表头：品名(左) 数量(右) 小计(右)
     */
    private static String formatItemHeader() {
        return String.format("%-16s %4s %8s", "品名", "数量", "小计");
    }

    /**
     * 商品明细行：品名+规格 / 数量 / 小计
     */
    private static String formatItemRow(OrderItem item) {
        // 构建商品名称 (名称 + 规格)
        StringBuilder nameBuilder = new StringBuilder(item.getProductName() != null ? item.getProductName() : "");
        if (item.getSkuSpec() != null && !item.getSkuSpec().isEmpty()) {
            nameBuilder.append(" ").append(item.getSkuSpec());
        }
        String name = nameBuilder.toString();

        // 品名最大宽度：LINE_WIDTH(32) - 空格(1) - 数量区(5) - 金额区(9) = 17
        int maxNameWidth = 17;
        int nv = visualWidth(name);
        if (nv > maxNameWidth) {
            // 截断并加 "…"
            name = truncateByVisualWidth(name, maxNameWidth - 2) + "…";
        }

        return String.format("%-17s %5d %8.2f", name, item.getQty(), item.getSubTotal());
    }

    /**
     * 按视觉宽度截断字符串
     */
    private static String truncateByVisualWidth(String s, int maxWidth) {
        if (visualWidth(s) <= maxWidth) return s;
        StringBuilder sb = new StringBuilder();
        int w = 0;
        for (char c : s.toCharArray()) {
            int cw = (c > 0x7E) ? 2 : 1;
            if (w + cw > maxWidth) break;
            sb.append(c);
            w += cw;
        }
        return sb.toString();
    }

    /**
     * 格式化标签 + 金额行，金额右对齐
     */
    private static String formatAmountLine(String label, java.math.BigDecimal amount) {
        String amountStr = String.format("%.2f", amount);
        int labelWidth = LINE_WIDTH - amountStr.length() - 1;
        if (labelWidth < 0) labelWidth = 0;
        return String.format("%-" + labelWidth + "s %s", label, amountStr);
    }

    /**
     * 支付方式的中文显示
     */
    private static String payMethodDisplay(Order order) {
        if (order.getPayMethod() == null) {
            return "";
        }
        return switch (order.getPayMethod()) {
            case CASH   -> "现金";
            case WECHAT -> "微信";
            case ALIPAY -> "支付宝";
        };
    }

    /**
     * 格式化日期时间（精确到秒）
     */
    private static String formatDateTime(LocalDateTime dt) {
        if (dt == null) {
            dt = LocalDateTime.now();
        }
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
