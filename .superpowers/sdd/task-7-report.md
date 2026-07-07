### Task 7: 初始化 Vue 3 前端项目 - 完成

**完成内容：**
- [x] Step 1: 创建 `frontend/package.json`
- [x] Step 2: 创建 `frontend/vite.config.ts`
- [x] Step 3: 创建入口文件 (`main.ts`, `App.vue`, `env.d.ts`)
- [x] Step 4: `npm install && npm run build` 验证成功
- [x] Step 5: Commit

**文件清单：**
- `frontend/package.json` - 依赖: vue 3.4, vue-router 4, pinia 2, element-plus 2.4, axios, jsbarcode, @element-plus/icons-vue
- `frontend/vite.config.ts` - proxy /api → localhost:8080, alias @ → src, build outDir → ../src/main/resources/static/
- `frontend/tsconfig.json` - TypeScript 配置
- `frontend/tsconfig.node.json` - Node TypeScript 配置
- `frontend/index.html` - 入口 HTML
- `frontend/src/env.d.ts` - 类型声明
- `frontend/src/main.ts` - 应用入口 (Pinia + Router + ElementPlus 中文)
- `frontend/src/App.vue` - 根组件 (<router-view />)
- `frontend/src/router/index.ts` - 路由配置
- `frontend/src/views/Home.vue` - 首页组件

**构建输出：** `../src/main/resources/static/` (index.html + assets/)

**验证结果：**
- `npm install` - 新增 99 个包
- `npm run build` - 成功构建, 输出至 src/main/resources/static/
