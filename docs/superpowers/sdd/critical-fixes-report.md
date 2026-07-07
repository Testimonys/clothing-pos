# 华兴服装店 POS 系统 - Critical 问题修复报告

**日期**: 2026-07-07
**分支**: master
**提交**: 580ce06 - "fix: resolve 8 critical issues from final review"

---

## Critical-1: 库存扣减无乐观锁保护

**风险**: 并发订单扣减同一 SKU 库存时，多个事务可能读取相同库存值后各自扣减，导致库存超卖（lost update 问题）。

**修复方案**: 在 `ProductSku` 实体添加 JPA `@Version` 乐观锁字段，在 `schema.sql` 添加对应数据库列。

**变更文件**:

1. `src/main/java/com/huaxing/entity/ProductSku.java` (第 38-40 行)
   - 新增字段:
   ```java
   @Version
   @Column(name = "version")
   private Long version = 0L;
   ```

2. `src/main/resources/schema.sql` (第 49 行)
   - product_sku 表新增列:
   ```sql
   version BIGINT DEFAULT 0,
   ```

**效果**: JPA 在每次 UPDATE 时自动检查 `WHERE version = ?` 并递增版本号。若两个事务并发更新同一行，先提交者成功，后提交者因版本不匹配抛出 `OptimisticLockException`，由 Spring 事务回滚，杜绝超卖。

---

## Critical-2: CORS 配置过于宽松

**风险**: `setAllowedOriginPatterns(List.of("*"))` 允许任意域名跨域访问 API，攻击者可通过恶意网站发起 CSRF 攻击。

**修复方案**: 区分开发环境和生产环境。开发环境（H2 profile）保留宽松配置便于调试；其他环境限制为本地和局域网来源。

**变更文件**:

`src/main/java/com/huaxing/config/CorsConfig.java`
- 注入 `Environment` 检测当前激活的 profile
- 包含 `"h2"` profile 时使用 `"*"` pattern
- 其他环境使用限制列表:
  ```java
  List.of("http://localhost:*", "http://127.0.0.1:*", "http://192.168.*.*:*")
  ```

---

## Critical-3: mysqldump 密码暴露于进程列表

**风险**: 使用 `-p"密码"` 参数传递数据库密码，任意系统用户执行 `ps aux | grep mysqldump` 即可看到明文密码。

**修复方案**: 改用 MySQL `--defaults-extra-file` 方式。将凭据写入权限为 600 的临时配置文件，mysqldump 从该文件读取，执行后立即删除。

**变更文件**:

`src/main/java/com/huaxing/service/BackupService.java`
- 在 `backup()` 方法中创建临时配置文件:
  ```java
  Path tmpConfig = Files.createTempFile("mysqldump_", ".cnf");
  // POSIX 系统设置权限为仅 owner 可读写 (600)
  Set<PosixFilePermission> perms = new HashSet<>();
  perms.add(PosixFilePermission.OWNER_READ);
  perms.add(PosixFilePermission.OWNER_WRITE);
  Files.setPosixFilePermissions(tmpConfig, perms);
  // 写入 [client] 段
  Files.writeString(tmpConfig, "[client]\nuser=...\npassword=...\n...");
  ```
- 修改 ProcessBuilder 命令，用 `--defaults-extra-file=临时文件` 替代 `-h -P -u -p` 参数
- `finally` 块中异常安全地删除临时文件

**效果**: `ps aux` 输出中仅显示 `--defaults-extra-file=/tmp/mysqldump_xxxxx.cnf`，密码仅存在于 600 权限的临时文件中，mysqldump 退出后立即删除。

---

## Critical-4: 条码生成并发重复风险

**风险**: `generateBarcode` 方法中查询最大条码和插入新条码之间存在 race condition。两个并发请求可能查询到相同的 maxBarcode，然后生成相同的条码，导致数据库唯一约束冲突或条码重复。

**修复方案**: 将条码序号生成逻辑提取为 `synchronized` 方法，确保同一 Controller 实例上的条码生成是串行化的。

**变更文件**:

`src/main/java/com/huaxing/controller/ProductController.java`
- 新增 `private synchronized String doGenerateBarcode(String prefix, ProductSkuRepository repo)` 方法
  - 查询 maxBarcode
  - 解析序号并 +1
  - 序号超限 (>999) 抛出 `IllegalStateException`
  - 返回完整条码字符串
- 简化 `generateBarcode` 方法体，调用 `doGenerateBarcode`，捕获异常返回 400 错误

**效果**: 与 `OrderController.generateOrderNo()` 同样的并发保护机制。

---

## Critical-5: Lombok 版本号配置

**原始问题**: pom.xml 中定义了 `<lombok.version>1.18.46</lombok.version>`，但 Lombok 依赖本身未指定版本（由 Spring Boot 3.2.0 parent 管理为 1.18.30），导致依赖版本与注解处理器版本不一致。同时，开发环境的 JDK 26 需要 Lombok 1.18.46（支持 class file major version 70）。

**修复方案**: 保留属性配置，并同时在 Lombok 依赖和注解处理器路径中使用该版本，确保全项目 Lombok 版本一致。

**变更文件**:

`pom.xml`
- 保留 `<lombok.version>1.18.46</lombok.version>` 属性
- Lombok 依赖新增 `<version>${lombok.version}</version>`，覆盖 Spring Boot parent 的 1.18.30
- 注解处理器路径使用 `<version>${lombok.version}</version>`（已存在，保持不变）

---

## Critical-6: 缺少 CLERK 角色种子数据

**风险**: 系统仅预置 BOSS 角色管理员账户，缺少 CLERK（店员）角色的初始数据，无法在开发/测试环境直接以店员身份登录。

**修复方案**: 在 MySQL 和 H2 两份种子数据文件中添加店员用户（user / user123）。

**变更文件**:

1. `src/main/resources/data.sql`
   ```sql
   -- 默认店员: user / user123 (BCrypt)
   INSERT IGNORE INTO sys_user (username, password, display_name, role) VALUES
   ('user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '店员', 'CLERK');
   ```

2. `src/main/resources/data-h2.sql`
   ```sql
   -- 默认店员: user / user123 (BCrypt)
   INSERT INTO sys_user (id, username, password, display_name, role, enabled, create_time) VALUES
   (2, 'user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '店员', 'CLERK', TRUE, NOW());
   ```

---

## Critical-7: H2 模式缺少 defer-datasource-initialization

**风险**: Spring Boot 3.x 中 JPA `ddl-auto: create` 与 `sql.init` 的执行顺序不确定。如果 `data-h2.sql` 在 Hibernate DDL 之前执行，INSERT 会因表不存在而失败。

**修复方案**: 添加 `spring.jpa.defer-datasource-initialization: true`，确保 Hibernate 先完成 DDL，再执行 `data-h2.sql` 种子数据插入。

**变更文件**:

`src/main/resources/application-h2.yml`
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
    defer-datasource-initialization: true    # 新增
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

---

## Critical-8: 登录页硬编码管理员凭据

**风险**: 登录页面明文展示 `默认管理员：admin / admin123`，攻击者获取页面即可得知管理员凭据。

**修复方案**: 删除登录页面中的凭据提示 HTML 元素和对应的 CSS 样式。

**变更文件**:

`frontend/src/views/LoginView.vue`
- 删除 `<div class="login-hint"><span>默认管理员：admin / admin123</span></div>` (模板第 45-47 行)
- 删除 `.login-hint` CSS 样式 (第 113-118 行)

---

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn compile` (后端) | 通过 |
| `npm run build` (前端) | 通过 |
| Git commit | 580ce06 |

## 修改文件汇总

| 文件 | 修复项 |
|------|--------|
| `pom.xml` | Critical-5 |
| `src/main/java/com/huaxing/entity/ProductSku.java` | Critical-1 |
| `src/main/java/com/huaxing/config/CorsConfig.java` | Critical-2 |
| `src/main/java/com/huaxing/service/BackupService.java` | Critical-3 |
| `src/main/java/com/huaxing/controller/ProductController.java` | Critical-4 |
| `src/main/resources/schema.sql` | Critical-1 |
| `src/main/resources/data.sql` | Critical-6 |
| `src/main/resources/data-h2.sql` | Critical-6 |
| `src/main/resources/application-h2.yml` | Critical-7 |
| `frontend/src/views/LoginView.vue` | Critical-8 |
