# 华兴服装店 POS 系统部署文档

## 1. 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK  | 17 或更高版本 |
| MySQL | 8.0 或更高版本 |
| Node.js | 18 或更高版本（仅构建时需要） |
| npm | 9 或更高版本（仅构建时需要） |

---

## 2. 数据库配置

### 2.1 创建数据库

登录 MySQL 并执行以下命令：

```sql
CREATE DATABASE IF NOT EXISTS clothing_pos
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2.2 创建数据库用户（可选）

```sql
CREATE USER 'clothing'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON clothing_pos.* TO 'clothing'@'localhost';
FLUSH PRIVILEGES;
```

---

## 3. 配置文件

编辑 `src/main/resources/application.yml`（或打包后在与 jar 同级的 `config/application.yml` 中覆盖）。

### 关键配置项

```yaml
server:
  port: 8080                   # 服务端口，可按需修改

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/clothing_pos?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root              # 数据库用户名
    password: root              # 数据库密码

app:
  jwt:
    secret: huaxing-clothing-pos-jwt-secret-key-2026   # JWT 密钥，生产环境请修改
    expiration-ms: 86400000                            # Token 过期时间（毫秒）
  store:
    name: 华兴服装店                                      # 店铺名称
  backup:
    path: ./backup                                      # 备份文件存放路径
    keep-days: 7                                        # 备份保留天数
  printer:
    enabled: true                                       # 是否启用打印机
    port: /dev/cu.usbprinter                             # 打印机串口（Windows 下为 COM1、COM2 等）
  upload:
    path: ./uploads                                     # 上传文件存放路径
```

---

## 4. 构建项目

### 4.1 一键构建

```bash
# 在项目根目录执行
cd frontend && npm run build && cd .. && mvn clean package -DskipTests
```

### 4.2 分步构建

**前端构建：**
```bash
cd frontend
npm install       # 安装前端依赖
npm run build     # 构建前端，输出到 src/main/resources/static/
cd ..
```

**后端打包：**
```bash
mvn clean package -DskipTests
```

构建完成后，可在 `target/` 目录找到 `clothing-pos-1.0.0.jar`。

---

## 5. 启动服务

### 5.1 macOS / Linux

```bash
chmod +x start.sh
./start.sh
```

或直接：

```bash
java -Xms256m -Xmx512m -jar target/clothing-pos-1.0.0.jar
```

### 5.2 Windows

双击 `start.bat`，或在命令行中执行：

```bat
start.bat
```

或直接：

```bat
java -Xms256m -Xmx512m -jar clothing-pos-1.0.0.jar
```

### 5.3 后台启动（Linux / macOS）

```bash
nohup java -Xms256m -Xmx512m -jar clothing-pos-1.0.0.jar > app.log 2>&1 &
```

### 5.4 指定外部配置文件

```bash
java -jar clothing-pos-1.0.0.jar --spring.config.location=file:./config/application.yml
```

---

## 6. 访问系统

启动成功后，打开浏览器访问：

```
http://localhost:8080
```

### 默认登录账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin  | admin123 | 管理员 |
| user   | user123  | 普通用户 |

> **重要：** 首次登录后请立即修改默认密码！

---

## 7. 扫码枪配置

系统支持条形码扫描枪，连接方式为 USB 键盘模拟模式（即插即用）。

### 配置步骤

1. 将扫码枪通过 USB 连接至电脑
2. 扫码枪默认为键盘模拟模式，无需额外驱动
3. 在 POS 收银页面的商品输入框中，扫描商品条形码即可自动识别
4. 如需调整扫码枪参数，请参考扫码枪说明书

### 注意事项

- 确保输入法为英文模式，避免扫码内容异常
- 如果扫码后带有多余换行符，可在系统设置中调整输入灵敏度

---

## 8. 打印机配置

### 8.1 小票打印机（USB / 串口）

1. 将打印机连接到电脑
2. 确认打印机端口号：
   - **Windows**：在设备管理器中查看端口（如 COM1、COM2）
   - **Linux/macOS**：使用 `ls /dev/tty.*` 或 `ls /dev/cu.*` 查看
3. 修改 `application.yml` 中的 `app.printer.port` 为实际端口

### 8.2 热敏打印机（网络）

如果使用网络热敏打印机，可配置 IP 地址和端口（需自行扩展代码支持）。

### 8.3 测试打印

登录系统后，在"系统设置 -> 打印机设置"中可进行测试打印。

---

## 9. 数据备份

### 9.1 自动备份

系统每日自动备份数据库，备份文件存放在 `app.backup.path` 配置的目录中（默认 `./backup`），保留天数由 `app.backup.keep-days` 控制。

### 9.2 手动备份

**MySQL 命令行备份：**
```bash
mysqldump -u root -p clothing_pos > backup_$(date +%Y%m%d_%H%M%S).sql
```

**恢复备份：**
```bash
mysql -u root -p clothing_pos < backup_file.sql
```

---

## 10. 安全提醒

1. **修改默认密码**：首次登录后立即修改 admin 和 user 的默认密码
2. **修改 JWT 密钥**：生产环境中请修改 `app.jwt.secret` 为一个复杂的随机字符串
3. **数据库密码**：使用强密码，避免使用 root/root 等默认凭据
4. **防火墙**：确保 8080 端口仅对可信网络开放
5. **HTTPS**：生产环境建议配置 SSL/TLS 证书启用 HTTPS

---

## 11. 常见问题

**Q: 启动报错 "Unable to find a field 'driver-class-name'"**
A: 检查 MySQL 连接驱动是否在 pom.xml 中正确引入。

**Q: 前端页面空白**
A: 确认前端已构建成功，`src/main/resources/static/` 目录下存在 `index.html`。

**Q: 数据库连接失败**
A: 确认 MySQL 服务已启动，数据库 `clothing_pos` 已创建，用户名密码配置正确。

**Q: 端口被占用**
A: 修改 `application.yml` 中的 `server.port` 为其他端口（如 8081）。

---

## 12. 目录结构说明

```
clothing-pos/
├── start.sh                  # macOS/Linux 启动脚本
├── start.bat                 # Windows 启动脚本
├── DEPLOY.md                 # 部署文档（本文件）
├── pom.xml                   # Maven 构建配置
├── frontend/                 # 前端源码
│   ├── package.json
│   └── src/
├── src/
│   ├── main/
│   │   ├── java/            # Java 后端源码
│   │   └── resources/
│   │       ├── application.yml       # 主配置
│   │       ├── application-h2.yml    # H2 开发配置
│   │       ├── schema.sql            # 数据库表结构
│   │       ├── data.sql              # 初始数据
│   │       └── static/               # 前端构建产物
│   └── test/                # 测试代码
├── target/                  # 构建输出目录
│   └── clothing-pos-1.0.0.jar
├── backup/                  # 备份文件目录
└── uploads/                 # 上传文件目录
```
