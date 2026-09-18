# 腾讯云 COS 上传接入说明

日期：2026-09-17。参考 `WeChatBusinessCard` 项目 `PublicController.test` 的 COS SDK 流式上传方式，使用用户指定依赖 `com.qcloud:cos_api-bundle:5.6.244`。本轮未配置真实密钥，也未发起腾讯云上传。

## 接口

两个接口都要求现有 JWT 登录认证，multipart 字段名为 `file`：

| 接口 | 用途 | 限制 | COS 对象目录 |
|---|---|---|---|
| `POST /api/upload/image` | 商品图片 | JPG/JPEG/PNG/GIF，最大 5MB | `<前缀>/images/yyyyMMdd/UUID.ext` |
| `POST /api/upload/file` | 通用文件 | 最大 10MB | `<前缀>/files/yyyyMMdd/UUID.ext` |

成功响应保留前端已使用的 `url`，同时返回 `key`、原始 `filename` 和 `size`：

```json
{
  "url": "https://examplebucket-1250000000.cos.ap-shanghai.myqcloud.com/clothing-pos/images/20260917/uuid.png",
  "key": "clothing-pos/images/20260917/uuid.png",
  "filename": "coat.png",
  "size": 12345
}
```

对象名不直接使用用户文件名，以免覆盖同名文件或将路径字符写入对象键。图片同时校验 MIME 类型和扩展名；通用文件使用下载响应头。上传流设置实际 Content-Length，COSClient 在应用内复用，并在 Spring 容器关闭时释放。

商品导入预览的 XLS/XLSX 只在请求期间解析，不作为持久文件保存，因此不写入 COS。历史 `/uploads/**` URL 继续只读兼容；所有新持久上传均走 COS。

## 配置

配置位于 `application.yml` 的 `cos` 节点，Docker 可复制 `.env.example` 为 `.env`。必填项：

- `COS_SECRET_ID`
- `COS_SECRET_KEY`
- `COS_BUCKET_NAME`，格式包含 APPID，例如 `examplebucket-1250000000`
- `COS_REGION`，例如 `ap-shanghai`

可选 `COS_PUBLIC_URL` 用于 CDN 或自定义域名，必须是 HTTPS；留空后自动生成存储桶默认域名。`COS_KEY_PREFIX` 默认 `clothing-pos`。

密钥未配置时应用仍可启动，上传接口返回 503 和明确提示。密钥只在后端环境变量或本地配置中提供，不进入浏览器。商品图片需要返回 URL 可读取，因此还需配置存储桶公开读取或 CDN 访问策略；写入密钥至少需要目标对象前缀的 `cos:PutObject` 权限。

## 验证边界

- Maven 离线编译及 22 项后端测试通过；新增用例覆盖空文件、图片扩展名/MIME 不一致、未配置密钥时图片与通用文件的明确失败，以及占位配置下完整 Spring 应用启动，测试不构造远程客户端。
- 前端生产构建验证图片错误提示和既有响应契约。
- 本轮不持有 SecretId/SecretKey，未验证实际桶名、地域、权限、跨域规则或公网访问。

配置完成后，可登录系统从商品编辑页上传一张小于 5MB 的图片；或调用通用文件接口：

```bash
# luohuai codeX generate: replace token and local file before manually validating the configured COS bucket.
curl -X POST http://localhost:8080/api/upload/file \
  -H "Authorization: Bearer YOUR_LOGIN_TOKEN" \
  -F "file=@/absolute/path/to/file.pdf"
```
