# Task 13: 数据备份 API - 完成报告

## 变更摘要

### 新增文件
- `src/main/java/com/huaxing/service/BackupService.java` — 备份服务

### 修改文件
- `src/main/java/com/huaxing/controller/SettingController.java` — 调整类级路径并添加备份接口

## BackupService 功能

| 方法 | 说明 |
|------|------|
| `backup()` | 执行 mysqldump 命令生成 `backup_yyyyMMdd_HHmmss.sql`，返回 `{fileName, size, time}` |
| `listBackups()` | 列出备份目录下所有 `.sql` 文件，按时间倒序 |
| `download(name)` | 返回文件 `Resource`，含路径穿越检查（校验 `..`、`/`、`\`，并验证 `normalize()` 后仍在备份目录下） |
| `cleanOldBackups()` | 删除 `keep-days` 天前修改的备份文件，`backup()` 执行后自动触发 |

### 关键实现细节
- 数据库连接参数从 `DataSource` 实时解析 JDBC URL（支持 `jdbc:mysql://host:port/dbname` 格式），无需额外配置
- mysqldump 通过 `ProcessBuilder` 执行，错误流被读取用于异常信息
- 备份目录在初始化时自动创建，路径自动转换为绝对路径
- 文件名校验：不允许 `/`、`\`、`..`，必须以 `.sql` 结尾

## API 接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/setting/backup` | 执行备份 | ROLE_BOSS |
| GET | `/api/setting/backup/list` | 备份列表 | ROLE_BOSS |
| GET | `/api/setting/backup/download/{name}` | 下载备份文件 | ROLE_BOSS |

## SettingController 调整
- 类级 `@RequestMapping` 从 `/api/setting/users` 调整为 `/api/setting`
- 原有用户管理接口路径增加 `/users` 前缀，对外路径不变
- 通过构造器注入 `BackupService`

## 配置
`application.yml` 和 `application-h2.yml` 中已有配置:
```yaml
app:
  backup:
    path: ./backup
    keep-days: 7
```

## 编译
- `mvn compile` 通过，41 个源文件编译成功

## 提交
- Commit: `a22ade6` — `feat: 实现数据备份 API (Task 13)`
