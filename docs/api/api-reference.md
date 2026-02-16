# ImgVault API 接口文档

**版本**: v2.1.0  
**基础路径**: `https://www.meczyc6.info/imgvault/api/v1`  
**Swagger UI**: `https://www.meczyc6.info/imgvault/swagger-ui.html`  
**OpenAPI JSON**: `https://www.meczyc6.info/imgvault/v3/api-docs`  
**内部路径**: `http://localhost:8080`（仅限服务器本地访问）

## 目录

- [通用说明](#通用说明)
- [图片管理](#图片管理)
  - [上传图片](#上传图片)
  - [批量上传](#批量上传)
  - [按 ID 查询详情](#按-id-查询详情)
  - [按 UUID 查询详情](#按-uuid-查询详情)
  - [分页查询列表](#分页查询列表)
  - [获取下载 URL](#获取下载-url)
  - [下载图片(302)](#下载图片302)
  - [软删除图片](#软删除图片)
  - [永久删除图片](#永久删除图片)
- [高级上传](#高级上传)
  - [秒传检测](#秒传检测)
  - [获取预签名上传 URL](#获取预签名上传-url)
  - [预签名上传确认](#预签名上传确认)
  - [初始化分片上传](#初始化分片上传)
  - [上传分片](#上传分片)
  - [查询上传进度](#查询上传进度)
- [图片处理](#图片处理)
  - [图片处理(302)](#图片处理302)
  - [获取图片处理 URL](#获取图片处理-url)
- [标签管理](#标签管理)
  - [创建标签](#创建标签)
  - [获取标签详情](#获取标签详情)
  - [获取所有标签](#获取所有标签)
  - [更新标签](#更新标签)
  - [删除标签](#删除标签)
  - [为图片添加标签](#为图片添加标签)
  - [为图片批量添加标签](#为图片批量添加标签)
  - [移除图片标签](#移除图片标签)
  - [获取图片的所有标签](#获取图片的所有标签)
  - [按标签检索图片](#按标签检索图片)
- [相册管理](#相册管理)
  - [创建相册](#创建相册)
  - [获取相册详情](#获取相册详情)
  - [相册列表](#相册列表)
  - [更新相册](#更新相册)
  - [删除相册](#删除相册)
  - [添加图片到相册](#添加图片到相册)
  - [从相册移除图片](#从相册移除图片)
  - [设置相册封面](#设置相册封面)
  - [获取相册中的图片](#获取相册中的图片)
- [管理后台](#管理后台)
  - [管理后台图片列表](#管理后台图片列表)
  - [操作日志列表](#操作日志列表)
  - [查询目标操作日志](#查询目标操作日志)
  - [回收站](#回收站)
  - [恢复图片](#恢复图片)
  - [系统统计](#系统统计)
  - [批量删除](#批量删除)
  - [批量打标签](#批量打标签)
- [系统接口](#系统接口)
  - [健康检查](#健康检查)

---

## 通用说明

### 统一响应格式

所有接口返回统一的 JSON 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1739500000000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 状态码: 200=成功, 400=参数错误, 404=未找到, 409=冲突, 500=服务器错误 |
| `message` | string | 响应消息 |
| `data` | object | 响应数据 |
| `timestamp` | long | 时间戳(毫秒) |

### 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

### 支持的图片格式

| 格式 | MIME 类型 | 扩展名 |
|------|-----------|--------|
| JPEG | image/jpeg | .jpg, .jpeg |
| PNG | image/png | .png |
| GIF | image/gif | .gif |
| WebP | image/webp | .webp |
| BMP | image/bmp | .bmp |

### 安全校验

- 所有上传接口强制执行 **Magic Bytes** 文件头校验
- 不依赖客户端声称的 MIME 类型或扩展名

---

## 图片管理

### 上传图片

单张图片上传，支持 JPEG/PNG/GIF/WebP/BMP 格式，最大 50MB。

```
POST /api/v1/images/upload
Content-Type: multipart/form-data
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file | 是 | 图片文件 |

**响应示例**:

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": 1,
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "originalName": "sunset.jpg",
    "format": "jpeg",
    "mimeType": "image/jpeg",
    "fileSize": 2048576,
    "width": 1920,
    "height": 1080,
    "storagePath": "originals/2026/02/14/a1b2c3d4.jpg",
    "downloadUrl": "http://minio:9000/imgvault/originals/..."
  }
}
```

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/v1/images/upload \
  -F "file=@/path/to/photo.jpg"
```

---

### 批量上传

支持同时上传多张图片。

```
POST /api/v1/images/batch-upload
Content-Type: multipart/form-data
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `files` | file[] | 是 | 图片文件列表 |

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/v1/images/batch-upload \
  -F "files=@photo1.jpg" \
  -F "files=@photo2.png"
```

---

### 按 ID 查询详情

```
GET /api/v1/images/{id}
```

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 图片 ID |

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "originalName": "sunset.jpg",
    "format": "jpeg",
    "mimeType": "image/jpeg",
    "fileSize": 2048576,
    "width": 1920,
    "height": 1080,
    "status": "active",
    "storagePath": "originals/2026/02/14/a1b2c3d4.jpg",
    "createdAt": "2026-02-14T10:30:00",
    "tags": ["风景", "日落"]
  }
}
```

---

### 按 UUID 查询详情

```
GET /api/v1/images/uuid/{uuid}
```

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `uuid` | string | 图片 UUID |

---

### 分页查询列表

支持按格式/时间/大小排序筛选。

```
GET /api/v1/images
```

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 20 | 每页大小 |
| `format` | string | - | 图片格式筛选: jpeg/png/gif/webp/bmp |
| `keyword` | string | - | 关键词搜索(原始文件名) |
| `sortBy` | string | created_at | 排序字段 |
| `sortOrder` | string | desc | 排序方式: asc/desc |

**curl 示例**:

```bash
curl "http://localhost:8080/api/v1/images?page=1&size=10&format=jpeg"
```

---

### 获取下载 URL

返回预签名下载 URL，有效期 1 小时。

```
GET /api/v1/images/{id}/download-url
```

**响应示例**:

```json
{
  "code": 200,
  "data": "http://minio:9000/imgvault/originals/2026/02/14/a1b2c3d4.jpg?X-Amz-Algorithm=..."
}
```

---

### 下载图片(302)

返回 302 重定向到 MinIO 预签名 URL。

```
GET /api/v1/images/{id}/download
```

**响应**: `302 Found`, `Location: http://minio:9000/imgvault/...`

---

### 软删除图片

标记为已删除，可恢复。

```
DELETE /api/v1/images/{id}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 永久删除图片

物理删除，同时删除 MinIO 文件，不可恢复。

```
DELETE /api/v1/images/{id}/permanent
```

---

## 高级上传

### 秒传检测

通过文件哈希检测是否已存在，实现秒传。

```
POST /api/v1/images/instant-upload
Content-Type: application/json
```

**请求体**:

```json
{
  "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "md5": "d41d8cd98f00b204e9800998ecf8427e",
  "fileSize": 2048576,
  "originalName": "photo.jpg"
}
```

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "exists": true,
    "imageId": 1,
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }
}
```

---

### 获取预签名上传 URL

客户端直传 MinIO，返回预签名 PUT URL。

```
POST /api/v1/images/presigned-upload
Content-Type: application/json
```

**请求体**:

```json
{
  "originalName": "photo.jpg",
  "mimeType": "image/jpeg",
  "fileSize": 2048576
}
```

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "uploadUrl": "http://minio:9000/imgvault/originals/...?X-Amz-Algorithm=...",
    "storagePath": "originals/2026/02/14/uuid.jpg",
    "expiresAt": "2026-02-14T11:30:00"
  }
}
```

---

### 预签名上传确认

客户端直传完成后，调用此接口创建图片记录。

```
POST /api/v1/images/presigned-upload/confirm
Content-Type: application/json
```

**请求体**:

```json
{
  "storagePath": "originals/2026/02/14/uuid.jpg",
  "originalName": "photo.jpg",
  "mimeType": "image/jpeg",
  "fileSize": 2048576
}
```

---

### 初始化分片上传

返回上传任务 ID 和分片信息。

```
POST /api/v1/images/chunk-upload/init
Content-Type: application/json
```

**请求体**:

```json
{
  "originalName": "large-photo.jpg",
  "mimeType": "image/jpeg",
  "fileSize": 104857600,
  "chunkSize": 5242880
}
```

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "uploadId": "task-uuid",
    "totalChunks": 20,
    "chunkSize": 5242880,
    "uploadedChunks": []
  }
}
```

---

### 上传分片

上传单个分片，支持断点续传。

```
POST /api/v1/images/chunk-upload/{uploadId}/{chunkNumber}
Content-Type: multipart/form-data
```

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `uploadId` | string | 上传任务 ID |
| `chunkNumber` | int | 分片编号(从 1 开始) |

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `chunk` | file | 是 | 分片数据 |

---

### 查询上传进度

返回已上传分片列表，用于断点续传。

```
GET /api/v1/images/chunk-upload/{uploadId}/progress
```

---

## 图片处理

通过 imgproxy 实现实时图片处理（缩放/格式转换/压缩/智能裁剪）。

### 图片处理(302)

302 重定向到签名后的 imgproxy URL。

```
GET /api/v1/images/{id}/process
```

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `width` | int | - | 目标宽度 |
| `height` | int | - | 目标高度 |
| `format` | string | - | 目标格式: jpeg/png/webp/gif |
| `quality` | int | - | 质量 (1-100) |
| `resizeType` | string | fit | 缩放方式: fit/fill/auto |

**curl 示例**:

```bash
# 生成 300x300 WebP 缩略图
curl -L "https://www.meczyc6.info/imgvault/api/v1/images/1/process?width=300&height=300&format=webp&quality=80"
```

---

### 获取图片处理 URL

返回签名后的 imgproxy URL，不重定向。

```
GET /api/v1/images/{id}/process-url
```

参数同上。

---

## 标签管理

### 创建标签

```
POST /api/v1/tags
Content-Type: application/json
```

**请求体**:

```json
{
  "name": "风景"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "name": "风景",
    "imageCount": 0,
    "createdAt": "2026-02-14T10:00:00"
  }
}
```

---

### 获取标签详情

```
GET /api/v1/tags/{id}
```

---

### 获取所有标签

```
GET /api/v1/tags
```

**响应示例**:

```json
{
  "code": 200,
  "data": [
    {"id": 1, "name": "风景", "imageCount": 10},
    {"id": 2, "name": "人物", "imageCount": 5}
  ]
}
```

---

### 更新标签

```
PUT /api/v1/tags/{id}
Content-Type: application/json
```

**请求体**:

```json
{
  "name": "山水风景"
}
```

---

### 删除标签

```
DELETE /api/v1/tags/{id}
```

---

### 为图片添加标签

```
POST /api/v1/tags/images/{imageId}/tags/{tagId}
```

---

### 为图片批量添加标签

通过标签名称批量添加，自动创建不存在的标签。

```
POST /api/v1/tags/images/{imageId}/tags
Content-Type: application/json
```

**请求体**:

```json
["风景", "日落", "海边"]
```

---

### 移除图片标签

```
DELETE /api/v1/tags/images/{imageId}/tags/{tagId}
```

---

### 获取图片的所有标签

```
GET /api/v1/tags/images/{imageId}/tags
```

---

### 按标签检索图片

返回关联此标签的图片 ID 列表。

```
GET /api/v1/tags/{tagId}/images
```

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 20 | 每页大小 |

---

## 相册管理

### 创建相册

```
POST /api/v1/albums
Content-Type: application/json
```

**请求体**:

```json
{
  "name": "旅行照片",
  "description": "2026年春节旅行"
}
```

---

### 获取相册详情

```
GET /api/v1/albums/{id}
```

---

### 相册列表

```
GET /api/v1/albums
```

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 20 | 每页大小 |

---

### 更新相册

```
PUT /api/v1/albums/{id}
Content-Type: application/json
```

**请求体**:

```json
{
  "name": "新名称",
  "description": "新描述"
}
```

---

### 删除相册

删除相册并移除所有图片关联（不删除图片本身）。

```
DELETE /api/v1/albums/{id}
```

---

### 添加图片到相册

```
POST /api/v1/albums/{albumId}/images/{imageId}
```

---

### 从相册移除图片

```
DELETE /api/v1/albums/{albumId}/images/{imageId}
```

---

### 设置相册封面

```
PUT /api/v1/albums/{albumId}/cover/{imageId}
```

---

### 获取相册中的图片

```
GET /api/v1/albums/{albumId}/images
```

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 20 | 每页大小 |

---

## 管理后台

### 管理后台图片列表

支持所有状态的图片浏览。

```
GET /api/v1/admin/images
```

参数同 [分页查询列表](#分页查询列表)。

---

### 操作日志列表

```
GET /api/v1/admin/logs
```

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `limit` | int | 50 | 返回记录数 |

---

### 查询目标操作日志

```
GET /api/v1/admin/logs/{targetType}/{targetId}
```

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `targetType` | string | 目标类型: image/tag/album |
| `targetId` | long | 目标 ID |

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 20 | 每页大小 |

---

### 回收站

查看已软删除的图片。

```
GET /api/v1/admin/trash
```

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 20 | 每页大小 |

---

### 恢复图片

从回收站恢复软删除的图片。

```
POST /api/v1/admin/trash/{id}/restore
```

---

### 系统统计

图片数量/存储空间/任务统计。

```
GET /api/v1/admin/stats
```

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "asyncTasks": {
      "pending": 0,
      "running": 0,
      "completed": 5,
      "failed": 0
    },
    "totalLogs": 42
  }
}
```

---

### 批量删除

```
POST /api/v1/admin/batch-delete
Content-Type: application/json
```

**请求体**:

```json
[1, 2, 3, 5]
```

---

### 批量打标签

```
POST /api/v1/admin/batch-tag
Content-Type: application/json
```

**请求体**:

```json
{
  "imageIds": [1, 2, 3],
  "tagNames": ["风景", "自然"]
}
```

---

## 系统接口

### 健康检查

```
GET /actuator/health
```

**响应示例**:

```json
{
  "status": "UP"
}
```

---

**文档版本**: 2.1.0  
**创建时间**: 2026-02-14  
**维护者**: ImgVault Team  
**最后更新**: 2026-02-14
