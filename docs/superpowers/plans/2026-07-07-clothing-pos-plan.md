# 华兴服装店库存管理与收银系统 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个本地部署的服装店库存管理与收银系统，支持扫码收银、库存管理、小票打印。

**Architecture:** Spring Boot 3.x 单体后端 + Vue 3 SPA 前端，前后端分离开发，最终前端静态文件打包进 Spring Boot `resources/static`，打成一个 fat jar 部署。MySQL 8.0 本地数据库。

**Tech Stack:** Java 17, Spring Boot 3.x, Spring Security + JWT, JPA/Hibernate, MySQL 8.0, Vue 3, Vue Router, Pinia, Element Plus, Axios, JsBarcode, ESC/POS 指令打印

## Global Constraints

- Java 17+，Spring Boot 3.x
- MySQL 8.0，InnoDB 引擎，字符集 utf8mb4
- 密码使用 BCrypt 加密
- 所有金额字段使用 DECIMAL(10,2)
- 包路径：`com.huaxing`
- 前端构建产物输出到：`src/main/resources/static/`
- API 前缀：`/api/`
- 条码规则：自生成条码 `HUAXING + YYYYMMDD + 三位序号`

---

## 实施顺序

```
Phase 1: 后端基础
  Task 1  → Task 2 → Task 3 → Task 4 → Task 5 → Task 6
  (项目初始化 → 数据库 → 实体 → Repository → Security → Auth API)

Phase 2: 前端基础
  Task 7 → Task 8
  (Vue 初始化 → 登录流程)

Phase 3: 核心业务 API
  Task 9 → Task 10 → Task 11
  (商品 API → 库存 API → 订单 API)

Phase 4: 系统管理
  Task 12 → Task 13
  (用户管理 → 备份)

Phase 5: 打印
  Task 14
  (小票打印)

Phase 6: 前端页面
  Task 15 → Task 16 → Task 17 → Task 18 → Task 19
  (商品页 → 收银台 → 库存/订单/设置 → 标签打印 → 导航布局)

Phase 7: 部署
  Task 20
  (构建 + 启动脚本 + 文档)
```

共 **20 个任务**，每个任务独立可测试，逐步构建完整系统。

---

### Task 1: 初始化 Spring Boot 项目

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/huaxing/ClothingPosApplication.java`
- Create: `src/main/resources/application.yml`

**Interfaces:**
- Produces: Spring Boot 主应用类，Maven 项目配置

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    <groupId>com.huaxing</groupId>
    <artifactId>clothing-pos</artifactId>
    <version>1.0.0</version>
    <name>clothing-pos</name>
    <description>华兴服装店库存管理与收银系统</description>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建主应用类**

```java
package com.huaxing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClothingPosApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClothingPosApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/clothing_pos?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

app:
  jwt:
    secret: huaxing-clothing-pos-jwt-secret-key-2026
    expiration-ms: 86400000
  store:
    name: 华兴服装店
  backup:
    path: ./backup
    keep-days: 7
  printer:
    enabled: true
```

- [ ] **Step 4: 编译验证**

```bash
cd /Users/luohuai/IdeaProjects/claude_demo/ai_project
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/huaxing/ClothingPosApplication.java src/main/resources/application.yml
git commit -m "feat: initialize Spring Boot project with dependencies"
```
### Task 2: 创建数据库初始化脚本

**Files:**
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`

**Interfaces:**
- Produces: 数据库表结构 + 初始数据（默认管理员账号 admin/admin123）

- [ ] **Step 1: 创建 schema.sql（建表语句）**

```sql
-- 商品分类
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT NULL,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品主表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NULL,
    name VARCHAR(200) NOT NULL,
    image_url VARCHAR(500) NULL,
    cost_price DECIMAL(10,2) DEFAULT 0.00,
    sell_price DECIMAL(10,2) DEFAULT 0.00,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SKU 规格
CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    color VARCHAR(50) DEFAULT '',
    size VARCHAR(50) DEFAULT '',
    barcode VARCHAR(100) NULL UNIQUE,
    stock_qty INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 库存流水
CREATE TABLE IF NOT EXISTS stock_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL DEFAULT '',
    sku_spec VARCHAR(100) NOT NULL DEFAULT '',
    type VARCHAR(20) NOT NULL COMMENT 'INBOUND/OUTBOUND',
    qty INT NOT NULL,
    before_qty INT NOT NULL,
    after_qty INT NOT NULL,
    operator_id BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sku_id) REFERENCES product_sku(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 销售订单
CREATE TABLE IF NOT EXISTS sys_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    receive_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    change_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    pay_method VARCHAR(20) NOT NULL COMMENT 'CASH/WECHAT/ALIPAY',
    cashier_id BIGINT NULL,
    member_id BIGINT NULL,
    create_time DATETIME(0) DEFAULT CURRENT_TIMESTAMP(0),
    FOREIGN KEY (cashier_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单明细
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL DEFAULT '',
    sku_spec VARCHAR(100) NOT NULL DEFAULT '',
    unit_price DECIMAL(10,2) NOT NULL,
    qty INT NOT NULL,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    sub_total DECIMAL(10,2) NOT NULL,
    barcode VARCHAR(100) DEFAULT '',
    FOREIGN KEY (order_id) REFERENCES sys_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'BOSS/CLERK',
    enabled TINYINT(1) DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 创建 data.sql（初始数据）**

```sql
-- 默认管理员: admin / admin123 (BCrypt)
INSERT IGNORE INTO sys_user (username, password, display_name, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '老板', 'BOSS');

-- 默认商品分类
INSERT IGNORE INTO category (id, name, parent_id, sort_order) VALUES
(1, '上衣', NULL, 1),
(2, '裤装', NULL, 2),
(3, '裙装', NULL, 3),
(4, '外套', NULL, 4),
(5, 'T恤', 1, 1),
(6, '衬衫', 1, 2),
(7, '牛仔裤', 2, 1),
(8, '休闲裤', 2, 2);
```

- [ ] **Step 3: 创建 MySQL 数据库**

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS clothing_pos DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/schema.sql src/main/resources/data.sql
git commit -m "feat: add database schema and seed data"
```

---

### Task 3: 创建 JPA 实体类

**Files:**
- Create: `src/main/java/com/huaxing/entity/Category.java`
- Create: `src/main/java/com/huaxing/entity/Product.java`
- Create: `src/main/java/com/huaxing/entity/ProductSku.java`
- Create: `src/main/java/com/huaxing/entity/StockRecord.java`
- Create: `src/main/java/com/huaxing/entity/Order.java`
- Create: `src/main/java/com/huaxing/entity/OrderItem.java`
- Create: `src/main/java/com/huaxing/entity/SysUser.java`
- Create: `src/main/java/com/huaxing/enums/PayMethod.java`
- Create: `src/main/java/com/huaxing/enums/StockType.java`
- Create: `src/main/java/com/huaxing/enums/UserRole.java`

**Interfaces:**
- Produces: 7 个 JPA Entity + 3 个枚举类，与数据库表结构一一对应

- [ ] **Step 1: 创建枚举类 (PayMethod, StockType, UserRole)**

```java
// PayMethod.java
package com.huaxing.enums;
public enum PayMethod { CASH, WECHAT, ALIPAY }

// StockType.java
package com.huaxing.enums;
public enum StockType { INBOUND, OUTBOUND }

// UserRole.java
package com.huaxing.enums;
public enum UserRole { BOSS, CLERK }
```

- [ ] **Step 2: 创建 Entity 类**

使用 `@Entity`, `@Table`, `@Data`, `@Builder`, Lombok 注解。关键关系：
- Product `@OneToMany` → ProductSku (mappedBy="productId", cascade=ALL, orphanRemoval=true)
- Order `@OneToMany` → OrderItem (mappedBy="orderId", cascade=ALL, orphanRemoval=true)

每个实体包含 `@PrePersist` 方法设置 `createTime`，Product 额外有 `@PreUpdate` 设置 `updateTime`。

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/huaxing/entity/ src/main/java/com/huaxing/enums/
git commit -m "feat: add JPA entity classes and enums"
```

---

### Task 4: 创建 Repository 层

**Files:**
- Create: `src/main/java/com/huaxing/repository/CategoryRepository.java`
- Create: `src/main/java/com/huaxing/repository/ProductRepository.java`
- Create: `src/main/java/com/huaxing/repository/ProductSkuRepository.java`
- Create: `src/main/java/com/huaxing/repository/StockRecordRepository.java`
- Create: `src/main/java/com/huaxing/repository/OrderRepository.java`
- Create: `src/main/java/com/huaxing/repository/OrderItemRepository.java`
- Create: `src/main/java/com/huaxing/repository/SysUserRepository.java`

**Interfaces:**
- Produces: 7 个 Spring Data JPA Repository

关键查询方法：
- `ProductSkuRepository.findByBarcode(String)` → Optional<ProductSku>
- `ProductSkuRepository.existsByBarcode(String)` → boolean
- `ProductRepository.search(keyword, categoryId, Pageable)` → Page<Product> (JPQL 模糊搜索)
- `OrderRepository.search(startTime, endTime, payMethod, Pageable)` → Page<Order>
- `OrderRepository.maxIdToday(LocalDateTime todayStart)` → Long
- `SysUserRepository.findByUsername(String)` → Optional<SysUser>
- `SysUserRepository.existsByUsername(String)` → boolean

- [ ] **Commit**

```bash
git add src/main/java/com/huaxing/repository/
git commit -m "feat: add JPA repository interfaces"
```

---

### Task 5: Spring Security + JWT 配置

**Files:**
- Create: `src/main/java/com/huaxing/config/SecurityConfig.java`
- Create: `src/main/java/com/huaxing/config/JwtUtil.java`
- Create: `src/main/java/com/huaxing/config/JwtAuthFilter.java`
- Create: `src/main/java/com/huaxing/config/CorsConfig.java`

**Interfaces:**
- `JwtUtil.generateToken(username)` → String
- `JwtUtil.parseUsername(token)` → String
- `JwtUtil.validateToken(token)` → boolean
- Security 过滤器链：放行 `POST /api/auth/login`，其他需要 Bearer Token
- `/api/setting/**` 需要 ROLE_BOSS

- [ ] **Step 1: 创建 JwtUtil (jjwt 0.12.x API)**

```java
@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
        return Jwts.builder().subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key).compact();
    }

    public String parseUsername(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateToken(String token) {
        try { parseUsername(token); return true; }
        catch (JwtException e) { return false; }
    }
}
```

- [ ] **Step 2: 创建 JwtAuthFilter**

OncePerRequestFilter，从 Authorization header 提取 Bearer token，验证并设置 SecurityContext。

- [ ] **Step 3: 创建 SecurityConfig**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/setting/**").hasRole("BOSS")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }
}
```

- [ ] **Step 4: 创建 CorsConfig** — 允许所有来源，支持开发时的跨域请求

- [ ] **Step 5: Commit**

---

### Task 6: 认证 API（登录/登出/当前用户）

**Files:**
- Create: `src/main/java/com/huaxing/dto/LoginRequest.java`
- Create: `src/main/java/com/huaxing/dto/LoginResponse.java`
- Create: `src/main/java/com/huaxing/dto/UserInfo.java`
- Create: `src/main/java/com/huaxing/controller/AuthController.java`

**Interfaces:**
- `POST /api/auth/login` body: `{username, password}` → `{token, displayName, role}`
- `POST /api/auth/logout` → `{message: "ok"}`
- `GET /api/auth/current-user` → `{id, username, displayName, role}` (需认证)

- [ ] **Step 1: 创建 DTO** — LoginRequest (username, password, @NotBlank), LoginResponse (token, displayName, role), UserInfo (id, username, displayName, role)

- [ ] **Step 2: 创建 AuthController**

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // login: 查用户 → BCrypt 验证 → 生成 JWT → 返回 token + 用户信息
    // logout: 返回 ok (无状态 JWT，客户端删除 token 即可)
    // currentUser: 从 @AuthenticationPrincipal SysUser 获取当前用户信息
}
```

- [ ] **Step 3: 测试** — `curl POST /api/auth/login` 用 admin/admin123 验证返回 token

- [ ] **Step 4: Commit**

---

### Task 7: 初始化 Vue 3 前端项目

**Files:**
- Create: `frontend/package.json`, `vite.config.ts`, `tsconfig.json`, `tsconfig.node.json`
- Create: `frontend/index.html`, `frontend/src/main.ts`, `frontend/src/App.vue`, `frontend/src/env.d.ts`

**Interfaces:**
- Produces: Vite + Vue 3 + Element Plus 项目骨架
- `npm run dev` → localhost:5173，API proxy → localhost:8080
- `npm run build` → 输出到 `../src/main/resources/static/`

- [ ] **Step 1: 创建 package.json** — 依赖: vue 3.4, vue-router 4, pinia 2, element-plus 2.4, axios, jsbarcode, @element-plus/icons-vue

- [ ] **Step 2: 创建 vite.config.ts** — proxy `/api` → `localhost:8080`，alias `@` → `src`，build outDir → `../src/main/resources/static/`

- [ ] **Step 3: 创建入口文件** — main.ts (createApp + pinia + router + ElementPlus 中文)，App.vue (`<router-view />`)

- [ ] **Step 4: npm install && npm run build 验证**

- [ ] **Step 5: Commit**

---

### Task 8: 前端路由 + 登录页面 + Token 管理

**Files:**
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/api/request.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/layout/MainLayout.vue` (占位)

**Interfaces:**
- 前端登录流程闭环：LoginView → authStore.loginAction → API → 存 token → router.push('/pos')
- Axios 拦截器自动附带 Authorization header，401 时跳转登录页
- 路由守卫：未登录 → /login，已登录跳过 /login

- [ ] **Step 1: 创建 request.ts** — axios 实例，baseURL='/api'，请求拦截器加 token，响应拦截器处理 401

- [ ] **Step 2: 创建 auth store** — loginAction / fetchUser / logoutAction / isLoggedIn / isBoss

- [ ] **Step 3: 创建路由** — /login, /pos, /product, /stock, /orders, /setting，beforeEach 守卫

- [ ] **Step 4: 创建 LoginView.vue** — 居中卡片，用户名+密码+登录按钮，调用 authStore

- [ ] **Step 5: 启动前后端验证登录流程**

- [ ] **Step 6: Commit**

---

### Task 9: 商品分类 + 商品 CRUD API + 条码查询 + 图片上传

**Files:**
- Create: `src/main/java/com/huaxing/dto/CategoryDTO.java`
- Create: `src/main/java/com/huaxing/dto/ProductDTO.java`
- Create: `src/main/java/com/huaxing/dto/ProductSkuDTO.java`
- Create: `src/main/java/com/huaxing/controller/CategoryController.java`
- Create: `src/main/java/com/huaxing/controller/ProductController.java`
- Create: `src/main/java/com/huaxing/controller/UploadController.java`
- Create: `src/main/java/com/huaxing/config/WebMvcConfig.java`

**Interfaces:**
- 分类 CRUD：`GET/POST/PUT /api/setting/categories`
- 商品 CRUD：`GET/POST/PUT/DELETE /api/product` + `/{id}`
- 条码查询：`GET /api/product/barcode/{code}` → `{found, skuId, productName, sellPrice, ...}`
- SKU 子资源：`POST/PUT/DELETE /api/product/{productId}/sku[/{skuId}]`
- 条码生成：`POST /api/product/{productId}/sku/{skuId}/barcode` → `{barcode}`
- 图片上传：`POST /api/upload/image` (multipart/form-data) → `{url}`

- [ ] **Step 1: 创建 UploadController**

```java
@RestController
@RequestMapping("/api/upload")
public class UploadController {
    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        // 校验文件类型 (jpg/png/gif)
        // 生成文件名: yyyyMMdd_HHmmss_随机6位.扩展名
        // 保存到 uploadPath 目录
        // 返回 {url: "/uploads/filename.jpg"}
    }
}
```

- [ ] **Step 2: 创建 WebMvcConfig** — 添加静态资源映射 `/uploads/**` → `file:./uploads/`

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
```

- [ ] **Step 3: 创建 ProductController**

核心方法：
- `GET /` — 分页搜索（keyword + categoryId），返回 Page<Product>
- `GET /{id}` — 商品详情（自动包含 skus 列表）
- `POST /` — 新建商品 + SKU（事务，先存 product 再存 skus）
- `PUT /{id}` — 更新商品基本信息
- `DELETE /{id}` — 删除商品及关联 SKU
- `GET /barcode/{code}` — 扫码查询，返回 `{found, skuId, productId, productName, color, size, sellPrice, stockQty, skuSpec}`
- SKU CRUD: addSku / updateSku / deleteSku
- `POST /{productId}/sku/{skuId}/barcode` — 生成条码 `HUAXING + yyyyMMdd + 三位序号`

- [ ] **Step 4: 更新 application.yml** 添加 upload path 配置

```yaml
app:
  upload:
    path: ./uploads
```

- [ ] **Step 5: 测试** — curl 测试创建商品 + 扫码查询 + 图片上传

- [ ] **Step 6: Commit**

---

### Task 10: 库存管理 API（查询 + 入库 + 流水）

**Files:**
- Create: `src/main/java/com/huaxing/dto/InboundRequest.java`
- Create: `src/main/java/com/huaxing/controller/StockController.java`

**Interfaces:**
- `GET /api/stock` — 库存查询（分页）
- `GET /api/stock/{skuId}/flow` — SKU 库存流水
- `POST /api/stock/inbound` — 入库（事务：更新 stockQty + 写 stock_record）

- [ ] **Step 1: 创建 InboundRequest** — items: List<{skuId, qty}>

- [ ] **Step 2: 创建 StockController**

入库核心逻辑（事务）：
```
for each item:
    sku = findById(skuId)
    beforeQty = sku.stockQty
    afterQty = beforeQty + item.qty
    sku.stockQty = afterQty  → save
    stockRecord(INBOUND, productName, skuSpec, qty, beforeQty, afterQty, operatorId) → save
```

- [ ] **Step 3: 测试入库 + 流水查询**

- [ ] **Step 4: Commit**

---

### Task 11: 订单 API（创建 + 列表 + 详情 + 补打）

**Files:**
- Create: `src/main/java/com/huaxing/dto/CreateOrderRequest.java`
- Create: `src/main/java/com/huaxing/controller/OrderController.java`

**Interfaces:**
- `POST /api/order` — 创建订单（事务：校验库存 → 扣库存 → 写流水 → 存订单+明细 → 触发打印）
- `GET /api/order` — 订单列表（按时间/支付方式筛选，分页，按时间倒序）
- `GET /api/order/{id}` — 订单详情（含 items）
- `POST /api/order/{id}/print` — 补打小票

- [ ] **Step 1: 创建 CreateOrderRequest** — items + totalAmount + discount + payAmount + receiveAmount + changeAmount + payMethod + printReceipt(boolean)

- [ ] **Step 2: 创建 OrderController**

创建订单核心逻辑（事务）：
```
1. 校验所有 SKU 库存是否充足
2. 生成单号 POS + yyyyMMdd + 4位序号
3. 保存 Order
4. for each item:
    扣库存 → 写 OUTBOUND 流水 → 保存 OrderItem（冗余 productName + skuSpec）
5. 如果 printReceipt=true，调用 EscPosPrinter.printOrder()
```

- [ ] **Step 3: Commit**

---

### Task 12: 用户管理 API（老板权限）

**Files:**
- Create: `src/main/java/com/huaxing/dto/CreateUserRequest.java`
- Create: `src/main/java/com/huaxing/controller/SettingController.java`

**Interfaces:**
- `GET /api/setting/users` — 用户列表
- `POST /api/setting/users` — 新增用户（BCrypt 加密密码）
- `PUT /api/setting/users/{id}` — 编辑用户
- `DELETE /api/setting/users/{id}` — 删除用户

以上接口仅 ROLE_BOSS 可访问（SecurityConfig 已配置）。

- [ ] **Step 1: 创建 SettingController（用户管理部分）**

- [ ] **Step 2: Commit**

---

### Task 13: 数据备份 API

**Files:**
- Create: `src/main/java/com/huaxing/service/BackupService.java`
- Modify: `src/main/java/com/huaxing/controller/SettingController.java` — 增加备份接口

**Interfaces:**
- `POST /api/setting/backup` — 执行 mysqldump → 返回 `{fileName, size, time}`
- `GET /api/setting/backup/list` — 备份文件列表
- `GET /api/setting/backup/download/{name}` — 下载备份文件

- [ ] **Step 1: 创建 BackupService**

```java
@Service
public class BackupService {
    // backup(): 执行 mysqldump 命令，文件命名 backup_yyyyMMdd_HHmmss.sql
    // listBackups(): 列出 backup 目录下 .sql 文件，按时间倒序
    // download(name): 返回文件 Resource（路径穿越检查）
    // cleanOldBackups(): 删除 keep-days 天前的备份
}
```

- [ ] **Step 2: 在 SettingController 中注入 BackupService 并添加备份接口**

- [ ] **Step 3: Commit**

---

### Task 14: ESC/POS 小票打印服务

**Files:**
- Create: `src/main/java/com/huaxing/printer/EscPosPrinter.java`

**Interfaces:**
- `EscPosPrinter.printOrder(Order order, List<OrderItem> items)` — 生成 ESC/POS 指令并发送到 USB 打印机
- 配置项 `app.printer.enabled` 控制是否启用打印

- [ ] **Step 1: 实现 ESC/POS 指令生成**

指令序列：初始化 → 居中 → 店名 → 左对齐 → 单号/时间 → 分隔线 → 商品明细表 → 分隔线 → 合计/折扣/实付/支付方式 → 谢谢惠顾 → 切纸

- [ ] **Step 2: USB 端口写入**

macOS: `/dev/cu.usbprinter`，Windows: 可配置端口名
降级方案：写入 `receipt_debug.txt` 文件

- [ ] **Step 3: 在 OrderController.create() 中集成打印**

注入 `EscPosPrinter`（`@Autowired(required = false)`），创建订单后根据 `printReceipt` 参数决定是否打印

- [ ] **Step 4: Commit**

---

### Task 15: 前端商品管理页面（含图片上传）

**Files:**
- Create: `frontend/src/api/product.ts`
- Create: `frontend/src/views/ProductView.vue`

- [ ] **Step 1: 创建 product.ts API 封装** — listProducts, getProduct, createProduct, updateProduct, deleteProduct, queryByBarcode, addSku, updateSku, deleteSku, generateBarcode

- [ ] **Step 2: 创建 ProductView.vue**

页面结构：
- 搜索栏：关键字搜索 + 分类筛选
- 商品表格：ID、图片缩略图(50x50)、名称、进价、售价、规格标签(chip)、操作按钮(编辑/删除)
- 分页组件
- 新增/编辑弹窗：
  - 商品名称、进价、售价
  - **图片上传**：使用 `el-upload` 组件，上传到 `/api/upload/image`，获取返回 URL 填入 `imageUrl`，支持预览
  - SKU 动态表单：颜色 + 尺码 + 条码 + 初始库存，可增删行

```vue
<!-- 图片上传组件示例 -->
<el-form-item label="商品图片">
  <el-upload
    :action="'/api/upload/image'"
    :headers="{Authorization: 'Bearer ' + authStore.token}"
    :on-success="(res) => form.imageUrl = res.url"
    :before-upload="checkImageType"
    :file-list="imageList"
    list-type="picture-card"
    :limit="1">
    <el-icon><Plus /></el-icon>
  </el-upload>
</el-form-item>
```

- [ ] **Step 3: npm run build 验证编译通过**

- [ ] **Step 4: Commit**

---

### Task 16: 前端收银台页面 (POS)

**Files:**
- Create: `frontend/src/views/PosView.vue`

**核心交互:**
- 扫码/搜索输入框始终自动聚焦
- 扫码 → 查 `/api/product/barcode/{code}` → 加入交易明细
- 已存在条码 → 数量 +1
- 明细列表：商品名、规格、单价、数量(可调)、单品折扣、小计、删除按钮
- 底部：合计 → 整单折扣 → 应收金额 → 「结账 F12」按钮
- 结账弹窗：应收(只读)、实收(可编辑)、找零(自动)、支付方式(现金/微信/支付宝)、打印小票勾选框(默认勾选)
- 确认收款 → POST `/api/order` → 成功弹窗(显示单号) → "开始下一单"

- [ ] **Step 1: 创建 PosView.vue** — 完整的收银页面 + 结账弹窗

- [ ] **Step 2: 绑定 F12 快捷键触发结账**

- [ ] **Step 3: Commit**

---

### Task 17: 前端库存管理 + 销售记录页面

**Files:**
- Create: `frontend/src/api/stock.ts`
- Create: `frontend/src/api/order.ts`
- Create: `frontend/src/views/StockView.vue`
- Create: `frontend/src/views/OrderView.vue`

- [ ] **Step 1: StockView.vue**

功能：库存查询表格(商品名/规格/条码/库存数) + 入库按钮 → 入库弹窗(扫码添加明细 / 手动添加 + 确认入库) + 点击 SKU 查看库存流水

- [ ] **Step 2: OrderView.vue**

功能：订单列表(时间范围筛选 + 支付方式筛选 + 分页) + 点击查看订单详情弹窗(商品明细 + 金额信息) + 补打小票按钮

- [ ] **Step 3: Commit**

---

### Task 18: 前端系统设置页面

**Files:**
- Create: `frontend/src/views/SettingView.vue`

三个 Tab 页：
1. **用户管理** — 用户表格 + 新增/编辑弹窗(username/password/displayName/role)，仅老板可见
2. **分类管理** — 分类树 + 新增/编辑
3. **备份管理** — 一键备份按钮 + 备份历史列表(下载按钮)

- [ ] **Step 1: 创建 SettingView.vue**

- [ ] **Step 2: Commit**

---

### Task 19: 前端条码标签打印 + 导航布局完善

**Files:**
- Create: `frontend/src/components/BarcodeLabel.vue`
- Modify: `frontend/src/views/layout/MainLayout.vue` — 更新为侧边栏布局
- Modify: `frontend/src/router/index.ts` — 完善路由守卫（BOSS 权限检查）

- [ ] **Step 1: 创建 BarcodeLabel.vue** — 用 JsBarcode 生成 Code128 SVG → 调用 `window.print()` 打印标签

- [ ] **Step 2: 更新 MainLayout.vue** — 侧边栏(el-aside) + el-menu，菜单项：收银台/商品管理/库存管理/销售记录/系统设置(仅老板)，el-main 区域放 `<router-view />`

- [ ] **Step 3: 完善路由守卫** — to.meta.bossOnly 且非老板 → redirect /pos

- [ ] **Step 4: Commit**

---

### Task 20: 构建配置 + 启动脚本 + 部署文档

**Files:**
- Create: `start.sh` (macOS/Linux)
- Create: `start.bat` (Windows)
- Create: `DEPLOY.md`

- [ ] **Step 1: 创建启动脚本**

macOS/Linux: `java -Xms256m -Xmx512m -jar clothing-pos.jar`
Windows: 同上 + pause

- [ ] **Step 2: 创建 DEPLOY.md** — 环境要求(JDK 17 + MySQL 8.0) + 安装步骤 + 访问方式 + 扫码枪/打印机配置 + 备份说明 + 默认密码修改提醒

- [ ] **Step 3: 构建生产版本并验证**

```bash
cd frontend && npm run build    # 前端构建到 ../src/main/resources/static/
cd .. && mvn clean package -DskipTests   # 后端打包
ls -la target/clothing-pos-1.0.0.jar     # 验证 jar 存在
java -jar target/clothing-pos-1.0.0.jar  # 启动验证
```

- [ ] **Step 4: Commit**

---

## 实施顺序

```
Phase 1: 后端基础      Task 1 → 2 → 3 → 4 → 5 → 6
Phase 2: 前端基础      Task 7 → 8
Phase 3: 核心业务 API  Task 9 → 10 → 11
Phase 4: 系统管理      Task 12 → 13
Phase 5: 打印          Task 14
Phase 6: 前端页面      Task 15 → 16 → 17 → 18 → 19
Phase 7: 部署          Task 20
```

共 **20 个任务**，每个任务独立可测试，逐步构建完整系统。
