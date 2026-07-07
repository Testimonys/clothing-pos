# Task 15 完成报告：前端商品管理页面（含图片上传）

## 状态：已完成

### 创建/修改的文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/api/product.ts` | 新建 | 商品 API 封装层 |
| `frontend/src/views/product/ProductView.vue` | 重写 | 完整商品管理页面 |

### API 封装 (product.ts)

封装了 11 个 API 函数，涵盖所有后端提供的商品相关端点：

| 函数 | HTTP 方法 | 端点 |
|------|-----------|------|
| `listProducts` | GET | `/api/product` |
| `getProduct` | GET | `/api/product/{id}` |
| `createProduct` | POST | `/api/product` |
| `updateProduct` | PUT | `/api/product/{id}` |
| `deleteProduct` | DELETE | `/api/product/{id}` |
| `queryByBarcode` | GET | `/api/product/barcode/{code}` |
| `addSku` | POST | `/api/product/{productId}/sku` |
| `updateSku` | PUT | `/api/product/{productId}/sku/{skuId}` |
| `deleteSku` | DELETE | `/api/product/{productId}/sku/{skuId}` |
| `generateBarcode` | POST | `/api/product/{productId}/sku/{skuId}/barcode` |
| `listCategories` | GET | `/api/setting/categories` |

### ProductView.vue 功能

1. **搜索栏**：关键字模糊搜索 + 分类下拉筛选 + 重置按钮 + 新增按钮
2. **商品表格**：
   - 列：ID、图片缩略图(50x50, 支持点击预览)、名称、分类、进价、售价、规格标签(chip 展示)、操作(编辑/删除)
   - 规格标签使用 `el-tag` 展示 color/size 组合
   - 删除操作使用 `el-popconfirm` 二次确认
3. **分页**：支持切换每页条数(10/20/50)和页码跳转
4. **新增弹窗**：
   - 商品名称、分类、进价、售价表单字段
   - 图片上传：使用 `el-upload` 组件，POST 到 `/api/upload/image`，Authorization header 通过 `authStore.token` 注入，支持上传预览
   - SKU 动态表单：可增删行，每行包含颜色、尺码、条码、初始库存
5. **编辑弹窗**：
   - 加载商品详情(含现有 SKU)填充表单
   - 已有 SKU 可修改(updateSku)、删除(deleteSku)
   - 可新增 SKU 行(addSku)
   - 已有 SKU 支持一键生成条码(generateBarcode)

### 技术要点

- 图片上传 header 使用 `computed(() => ({ Authorization: 'Bearer ' + authStore.token }))`，因为 `el-upload` 不走 axios 拦截器
- SKU 表单使用 `_key` 作为 v-for 的唯一 key（时间戳+随机数），`_isNew` 标记区分新增/已有规格
- TypeScript 类型安全：定义了 `SkuFormItem` 接口扩展 `ProductSkuDTO`，增加前端专用属性
- 表单校验：商品名称和售价为必填项

### 构建验证

```
npm run build → vue-tsc --noEmit ✓ + vite build ✓
```

无 TypeScript 错误，编译输出正常。

### 提交信息

```
commit 9e51d36
feat: 前端商品管理页面（含图片上传）
```
