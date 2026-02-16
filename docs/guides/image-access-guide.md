# ImgVault 图片访问指南 - 多尺寸与多格式

**版本**: v2.1.0  
**适用范围**: 前端开发者、业务系统对接方  
**最后更新**: 2026-02-16

## 概述

ImgVault 通过 **imgproxy** 实现实时图片处理，支持多种尺寸、多种格式的图片获取。所有图片 URL 均通过 HTTPS 域名代理访问，无需关心内部存储细节。

**基础域名**: `https://www.meczyc6.info`

## 1. API 返回的 URL 结构

上传或查询图片时，API 会返回完整的图片 URL：

```json
{
  "downloadUrl": "https://www.meczyc6.info/imgvault/storage/originals/2026/02/16/xxx.png",
  "thumbnails": {
    "small":  "https://www.meczyc6.info/imgvault/imgproxy/{签名}/rs:fit:150:150:0/plain/s3://imgvault/originals/...",
    "medium": "https://www.meczyc6.info/imgvault/imgproxy/{签名}/rs:fit:800:600:0/plain/s3://imgvault/originals/...",
    "large":  "https://www.meczyc6.info/imgvault/imgproxy/{签名}/rs:fit:1920:1080:0/plain/s3://imgvault/originals/..."
  },
  "variants": {
    "webp": "https://www.meczyc6.info/imgvault/imgproxy/{签名}/q:85/plain/s3://...@webp",
    "avif": "https://www.meczyc6.info/imgvault/imgproxy/{签名}/q:80/plain/s3://...@avif"
  }
}
```

| 字段 | 说明 | 用途 |
|------|------|------|
| `downloadUrl` | 原图直接访问 URL | 下载原图、需要最高质量时使用 |
| `thumbnails.small` | 150×150 缩略图 | 列表缩略图、头像 |
| `thumbnails.medium` | 800×600 缩略图 | 卡片展示、中等预览 |
| `thumbnails.large` | 1920×1080 缩略图 | 全屏查看、大图预览 |
| `variants.webp` | WebP 格式 (质量 85) | 现代浏览器使用，体积更小 |
| `variants.avif` | AVIF 格式 (质量 80) | 最新浏览器支持，体积最小 |

## 2. 多尺寸获取

### 2.1 直接使用 API 返回的缩略图 URL

最简单的方式是直接使用 API 返回的 `thumbnails` 字段：

```bash
# 查询图片详情（包含所有尺寸的 URL）
curl "https://www.meczyc6.info/imgvault/api/v1/images/6"
```

响应中直接包含 `thumbnails.small`、`thumbnails.medium`、`thumbnails.large` 三种预生成的缩略图 URL，可以直接在 `<img>` 标签中使用。

### 2.2 通过图片处理 API 获取自定义尺寸

如果需要 API 预设之外的尺寸，使用图片处理接口：

```bash
# 生成 400×300 的缩略图（302 重定向到处理后的图片）
curl -L "https://www.meczyc6.info/imgvault/api/v1/images/{id}/process?width=400&height=300"

# 获取处理后的 URL（不重定向，返回 JSON）
curl "https://www.meczyc6.info/imgvault/api/v1/images/{id}/process-url?width=400&height=300"
```

**支持的参数**:

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `width` | int | 目标宽度 (px) | 400 |
| `height` | int | 目标高度 (px) | 300 |
| `quality` | int | 输出质量 (1-100) | 80 |
| `format` | string | 输出格式 | jpeg, png, webp, avif |
| `smartCrop` | boolean | 智能裁剪（内容感知） | true |

### 2.3 常见尺寸对照表

| 场景 | 推荐尺寸 | 字段/参数 |
|------|----------|-----------|
| 头像/图标 | 150×150 | `thumbnails.small` |
| 列表卡片 | 400×300 | `process?width=400&height=300` |
| 中等预览 | 800×600 | `thumbnails.medium` |
| 文章配图 | 1200×800 | `process?width=1200&height=800` |
| 全屏查看 | 1920×1080 | `thumbnails.large` |
| 原图 | 原始尺寸 | `downloadUrl` |

## 3. 多格式获取

### 3.1 直接使用 API 返回的格式变体

API 返回的 `variants` 字段包含预生成的格式变体：

```html
<!-- 使用 <picture> 元素实现渐进增强 -->
<picture>
  <source srcset="{variants.avif}" type="image/avif">
  <source srcset="{variants.webp}" type="image/webp">
  <img src="{downloadUrl}" alt="图片描述">
</picture>
```

### 3.2 通过处理 API 指定格式

```bash
# 转换为 WebP 格式，质量 85
curl -L "https://www.meczyc6.info/imgvault/api/v1/images/{id}/process?format=webp&quality=85"

# 转换为 AVIF 格式，质量 70
curl -L "https://www.meczyc6.info/imgvault/api/v1/images/{id}/process?format=avif&quality=70"

# 转换为 JPEG 格式，压缩到质量 60
curl -L "https://www.meczyc6.info/imgvault/api/v1/images/{id}/process?format=jpeg&quality=60"
```

### 3.3 同时指定尺寸和格式

```bash
# 生成 800x600 的 WebP 缩略图，质量 80
curl -L "https://www.meczyc6.info/imgvault/api/v1/images/{id}/process?width=800&height=600&format=webp&quality=80"

# 生成 300x300 智能裁剪的 AVIF 缩略图
curl -L "https://www.meczyc6.info/imgvault/api/v1/images/{id}/process?width=300&height=300&format=avif&quality=75&smartCrop=true"
```

### 3.4 格式对比

| 格式 | 浏览器支持 | 体积 (相对 JPEG) | 质量 | 推荐场景 |
|------|-----------|------------------|------|----------|
| JPEG | 所有 | 100% (基准) | 良 | 通用兼容 |
| PNG | 所有 | 150-300% | 无损 | 需要透明通道 |
| WebP | Chrome/Edge/Firefox/Safari 14+ | 25-35% | 优 | 现代浏览器首选 |
| AVIF | Chrome 85+/Firefox 93+/Safari 16+ | 15-25% | 优 | 极致压缩 |
| GIF | 所有 | - | - | 动图 |

## 4. 前端最佳实践

### 4.1 响应式图片

```html
<!-- 根据屏幕宽度自动选择合适尺寸 -->
<img
  srcset="{thumbnails.small} 150w,
          {thumbnails.medium} 800w,
          {thumbnails.large} 1920w"
  sizes="(max-width: 600px) 150px,
         (max-width: 1200px) 800px,
         1920px"
  src="{thumbnails.medium}"
  alt="图片描述"
  loading="lazy"
>
```

### 4.2 渐进增强（格式 + 尺寸）

```html
<picture>
  <!-- AVIF: 最小体积 -->
  <source
    srcset="{processUrl}?width=800&height=600&format=avif&quality=75"
    type="image/avif">
  <!-- WebP: 广泛支持 -->
  <source
    srcset="{processUrl}?width=800&height=600&format=webp&quality=80"
    type="image/webp">
  <!-- JPEG: 兜底 -->
  <img
    src="{thumbnails.medium}"
    alt="图片描述"
    loading="lazy">
</picture>
```

### 4.3 JavaScript 动态加载

```javascript
// 获取图片详情
const res = await fetch('/imgvault/api/v1/images/6');
const { data } = await res.json();

// 使用缩略图
const thumbnail = data.thumbnails.medium;  // 800×600
const fullImage = data.downloadUrl;         // 原图

// 使用现代格式
const webpUrl = data.variants.webp;
const avifUrl = data.variants.avif;

// 自定义尺寸: 通过 process API
const customUrl = `/imgvault/api/v1/images/${data.id}/process?width=400&height=400&format=webp&quality=80`;
```

## 5. 完整示例

### 5.1 curl 示例：获取各种尺寸和格式

```bash
# === 原图 ===
curl -O "https://www.meczyc6.info/imgvault/storage/originals/2026/02/16/xxx.png"

# === 通过 API 获取缩略图 ===
# 小缩略图 (150x150)
curl -L -o thumb_small.avif \
  "https://www.meczyc6.info/imgvault/api/v1/images/6/process?width=150&height=150"

# 中缩略图 (800x600)
curl -L -o thumb_medium.avif \
  "https://www.meczyc6.info/imgvault/api/v1/images/6/process?width=800&height=600"

# 大缩略图 (1920x1080)
curl -L -o thumb_large.avif \
  "https://www.meczyc6.info/imgvault/api/v1/images/6/process?width=1920&height=1080"

# === 格式转换 ===
# 转 WebP
curl -L -o image.webp \
  "https://www.meczyc6.info/imgvault/api/v1/images/6/process?format=webp&quality=85"

# 转 JPEG (压缩)
curl -L -o image.jpg \
  "https://www.meczyc6.info/imgvault/api/v1/images/6/process?format=jpeg&quality=75"

# 转 AVIF (极致压缩)
curl -L -o image.avif \
  "https://www.meczyc6.info/imgvault/api/v1/images/6/process?format=avif&quality=70"

# === 尺寸 + 格式组合 ===
# 400x300 WebP 缩略图
curl -L -o card.webp \
  "https://www.meczyc6.info/imgvault/api/v1/images/6/process?width=400&height=300&format=webp&quality=80"

# 300x300 智能裁剪 AVIF
curl -L -o avatar.avif \
  "https://www.meczyc6.info/imgvault/api/v1/images/6/process?width=300&height=300&format=avif&smartCrop=true"
```

### 5.2 直接使用 imgproxy URL

API 返回的 `thumbnails` 和 `variants` URL 可以直接在浏览器中打开或嵌入到 HTML 中：

```bash
# 直接访问缩略图 (使用 API 返回的完整 URL)
curl -o medium.avif "https://www.meczyc6.info/imgvault/imgproxy/{签名}/rs:fit:800:600:0/plain/s3://imgvault/originals/2026/02/16/xxx.png"

# 直接访问 WebP 变体
curl -o image.webp "https://www.meczyc6.info/imgvault/imgproxy/{签名}/q:85/plain/s3://imgvault/originals/2026/02/16/xxx.png@webp"
```

> **注意**: imgproxy URL 中的签名是基于密钥生成的，不能自行拼接。请始终使用 API 返回的完整 URL。

## 6. API 速查表

| 需求 | API | 说明 |
|------|-----|------|
| 查看图片所有 URL | `GET /api/v1/images/{id}` | 返回 downloadUrl + thumbnails + variants |
| 自定义尺寸 | `GET /api/v1/images/{id}/process?width=W&height=H` | 302 重定向到处理后图片 |
| 自定义格式 | `GET /api/v1/images/{id}/process?format=webp` | 302 重定向到格式转换后图片 |
| 尺寸+格式 | `GET /api/v1/images/{id}/process?width=W&height=H&format=F&quality=Q` | 组合处理 |
| 获取处理 URL | `GET /api/v1/images/{id}/process-url?...` | 返回 URL 字符串，不重定向 |
| 原图下载 | `GET /api/v1/images/{id}/download` | 302 重定向到原图 |
| 分页列表 | `GET /api/v1/images?page=1&size=20` | 批量获取，每条记录都包含各尺寸 URL |

## 7. 注意事项

1. **imgproxy URL 不可自行拼接** —— URL 中包含 HMAC 签名，修改任何参数会导致签名验证失败（403）
2. **缓存策略** —— imgproxy 处理后的图片默认缓存 7 天（nginx `expires 7d`），原图也缓存 7 天
3. **首次请求较慢** —— imgproxy 首次处理某个尺寸/格式的图片时需要从 MinIO 下载并处理，后续请求会更快
4. **智能裁剪** —— `smartCrop=true` 使用内容感知算法裁剪，适合头像等需要正方形裁剪的场景
5. **支持的源格式** —— JPEG、PNG、GIF、WebP、BMP、TIFF（输入格式不限于输出格式）
6. **最大源图分辨率** —— 5000 万像素（`IMGPROXY_MAX_SRC_RESOLUTION=50`）

---

**文档版本**: 1.0  
**创建时间**: 2026-02-16  
**维护者**: ImgVault Team
