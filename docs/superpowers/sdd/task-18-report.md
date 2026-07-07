# Task 18 Report: 前端系统设置页面

## 完成内容

创建 `/frontend/src/views/setting/SettingView.vue`，实现系统设置页面，包含三个 Tab：

### 1. 用户管理 (Tab 1)
- 用户列表表格（ID/用户名/显示名称/角色/启用状态/创建时间）
- 新增/编辑弹窗，字段：`username`、`password`、`displayName`、`role`（BOSS/CLERK）
- 编辑模式密码留空则不修改
- 删除用户（带确认弹窗）
- 接口：`GET/POST/PUT/DELETE /api/setting/users/*`

### 2. 分类管理 (Tab 2)
- 分类树形展示（el-tree），将扁平列表转为树形结构
- 新增/编辑弹窗，字段：名称、上级分类（tree-select）、排序号
- 编辑时自动排除自身及子分类作为父级
- 接口：`GET/POST/PUT /api/setting/categories/*`

### 3. 备份管理 (Tab 3)
- "一键备份"按钮，调用 `POST /api/setting/backup`
- 备份历史列表（文件名/大小/时间）
- 每行"下载"按钮，使用 fetch + blob 方式携带 Authorization header
- 接口：`GET /api/setting/backup/list`、`POST /api/setting/backup`、`GET /api/setting/backup/download/{name}`

### 技术实现
- 直接使用 `request.ts`（已封装好的 axios 实例）调用 API
- 沿用项目已有的 Element Plus 组件模式（el-table/el-dialog/el-form/el-tree/el-tree-select）
- 类型定义直接写在 vue 文件中（UserDTO/CategoryDTO/BackupDTO/TreeNode）
- `npm run build` 验证通过
