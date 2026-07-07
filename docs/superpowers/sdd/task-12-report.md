# Task 12 Report: 用户管理 API

## 完成内容

### 新增文件

1. **`CreateUserRequest.java`** — 创建/更新用户请求 DTO
   - 字段：`username`, `password`, `displayName`, `role`（UserRole 枚举）

2. **`UserDTO.java`** — 用户响应 DTO
   - 字段：`id`, `username`, `displayName`, `role`, `enabled`, `createTime`

3. **`SettingController.java`** — 用户管理 Controller
   - `GET    /api/setting/users`          — 查询所有用户
   - `POST   /api/setting/users`          — 新增用户（BCrypt 加密密码）
   - `PUT    /api/setting/users/{id}`     — 编辑用户（密码为空则不更新密码）
   - `DELETE /api/setting/users/{id}`     — 删除用户

### 关键设计决策

- **路由路径**: `/api/setting/users`，与已有 CategoryController 的 `/api/setting/categories` 不冲突
- **密码加密**: 使用 `PasswordEncoder` (BCryptPasswordEncoder) 加密密码
- **权限控制**: SecurityConfig 已配置 `.requestMatchers("/api/setting/**").hasRole("BOSS")`，无需额外注解
- **角色枚举**: 复用已有的 `UserRole` 枚举（BOSS / CLERK）
- **代码风格**: 遵循现有 CategoryController 的构造器注入 + toDTO 模式

### 编译结果

- `mvn compile` — BUILD SUCCESS

### Commit

- Commit `0b048d6` — `feat: 用户管理 API - SettingController`
- 3 files changed, 129 insertions(+)
