# 图片存储/查询服务 - 技术调研文档

**版本**: v1.0  
**创建日期**: 2026-02-10  
**状态**: 调研中  
**项目代号**: ImgVault

---

## 1. 调研背景

### 1.1 业务目标

构建一个高可用、高性能的**图片存储与查询服务**（ImgVault），支持图片的上传、存储、处理、检索和分发。该服务将作为独立的基础服务，为业务系统提供统一的图片管理能力。

### 1.2 调研范围

| 维度 | 调研内容 |
|------|---------|
| 存储引擎 | MinIO、FastDFS、Ceph、SeaweedFS、AWS S3、阿里云 OSS |
| 图片处理 | 缩略图生成、水印、格式转换、压缩 |
| 上传方案 | 秒传、分片上传、断点续传 |
| 安全防护 | 文件校验、恶意文件检测、访问控制 |
| 数据库设计 | 元数据存储、标签系统、索引策略 |
| 图片检索 | 哈希去重、感知哈希、相似图搜索 |
| 分发加速 | CDN 集成、缓存策略、预签名 URL |
| Java 生态 | 相关库和框架选型 |

---

## 2. 存储引擎对比分析

### 2.1 主要候选方案

#### 方案 A：MinIO（推荐）

| 特性 | 说明 |
|------|------|
| **类型** | 对象存储（S3 兼容） |
| **协议** | 完全兼容 AWS S3 API |
| **数据保护** | 纠删码（Erasure Coding），比三副本节省约 40% 空间 |
| **性能** | 单机最高 183GB/s 读取吞吐（NVMe） |
| **部署** | Docker / Kubernetes 原生支持，开箱即用 |
| **运维成本** | 低，故障率低 |
| **社区** | GitHub 48k+ stars，社区活跃 |
| **管理界面** | 内置 GUI 管理控制台 |
| **License** | AGPL v3 / 商业许可 |

**Spring Boot 集成**:

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.5</version>
</dependency>
```

```yaml
# application.yml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: imgvault
```

**核心优势**:
- S3 API 完全兼容，未来可无缝迁移至 AWS S3 或阿里云 OSS
- 纠删码存储相比三副本方案节省 40% 存储成本
- 支持版本控制、生命周期管理、事件通知
- Docker 单命令部署，适合开发和生产环境
- 支持预签名 URL，可直接由客户端上传/下载

#### 方案 B：FastDFS

| 特性 | 说明 |
|------|------|
| **类型** | 分布式文件存储 |
| **协议** | 自定义 HTTP 协议 |
| **数据保护** | 组内副本冗余 |
| **部署** | Tracker + Storage 架构，需手动部署 |
| **运维成本** | 中等偏高 |
| **社区** | 活跃度一般，文档较少 |
| **适用场景** | 传统互联网应用（电商图片、用户头像） |

**Spring Boot 集成**:

```xml
<dependency>
    <groupId>com.github.tobato</groupId>
    <artifactId>fastdfs-client</artifactId>
    <version>1.27.2</version>
</dependency>
```

**劣势**:
- 架构相对老旧，社区活跃度下降
- 不兼容 S3 API，迁移成本高
- 文档资料匮乏

#### 方案 C：Ceph

| 特性 | 说明 |
|------|------|
| **类型** | 统一存储（对象/块/文件） |
| **协议** | S3/Swift/POSIX |
| **数据保护** | 纠删码 + 副本 |
| **部署** | 复杂，需要专业运维团队 |
| **运维成本** | 高 |
| **社区** | 活跃，Red Hat 主导 |
| **适用场景** | 大规模混合存储需求 |

**劣势**:
- 学习曲线陡峭，配置复杂
- 运维成本高，需要专业团队
- 对于单纯的图片存储场景可能过于重量级

#### 方案 D：SeaweedFS

| 特性 | 说明 |
|------|------|
| **类型** | 分布式对象存储 |
| **协议** | REST API / S3 兼容 |
| **特点** | 基于 Facebook Haystack 论文，针对小文件优化 |
| **部署** | 较简单 |
| **社区** | GitHub 23k+ stars，活跃 |

**适用场景**: 海量小文件存储

#### 方案 E：云厂商 OSS（AWS S3 / 阿里云 OSS）

| 特性 | 说明 |
|------|------|
| **类型** | 托管式对象存储 |
| **优势** | 免运维、高可用、弹性扩展 |
| **劣势** | 长期成本高、数据主权、网络依赖 |
| **适用场景** | 云原生应用、快速上线 |

### 2.2 综合对比矩阵

| 维度 | MinIO | FastDFS | Ceph | SeaweedFS | 云厂商 OSS |
|------|-------|---------|------|-----------|-----------|
| S3 兼容性 | 完全兼容 | 不兼容 | 兼容 | 部分兼容 | 原生/兼容 |
| 部署难度 | 低 | 中 | 高 | 低 | 无需部署 |
| 运维成本 | 低 | 中 | 高 | 低 | 无 |
| 性能 | 极高 | 高 | 高 | 高 | 依赖网络 |
| 扩展性 | PB 级 | PB 级 | EB 级 | PB 级 | 无限 |
| 社区活跃 | 极高 | 一般 | 高 | 高 | N/A |
| 迁移成本 | 低 | 高 | 中 | 中 | 低 |
| 适合团队 | 小/中/大 | 小/中 | 大 | 小/中 | 任意 |

### 2.3 选型建议

| 场景 | 推荐方案 | 理由 |
|------|---------|------|
| 个人/小团队、快速搭建 | **MinIO** | 开箱即用，S3 兼容 |
| 传统企业内网部署 | FastDFS 或 MinIO | 成熟稳定 |
| 大规模混合存储 | Ceph | 功能全面 |
| 海量小文件优化 | SeaweedFS | 小文件性能极好 |
| 云原生、预算充足 | 云厂商 OSS | 免运维 |
| **ImgVault 推荐** | **MinIO** | S3 兼容 + 低运维 + 高性能 + 易迁移 |

---

## 3. 图片上传场景分析

### 3.1 普通上传

- 适用于小于 5MB 的图片文件
- 直接通过 HTTP POST 上传到服务端
- 服务端校验后存入对象存储

### 3.2 分片上传（Multipart Upload）

**适用场景**: 大于 5MB 的文件

**流程**:
1. 前端计算文件 MD5 / SHA-256 哈希值
2. 将文件按指定大小（5-20MB）分割成多个分片
3. 每个分片携带分片序号并行上传
4. 后端接收并暂存所有分片
5. 所有分片上传完成后，触发合并操作
6. 合并后验证完整性（哈希比对）

**MinIO 原生支持**: MinIO S3 API 原生支持 Multipart Upload，无需手动管理分片合并。

### 3.3 断点续传

**实现原理**:
- 服务端记录每个上传任务的分片状态（Redis）
- 网络中断后，客户端查询已上传的分片列表
- 仅重传失败/缺失的分片
- 所有分片到齐后自动合并

**数据结构设计**:
```
Redis Key: upload:{uploadId}:chunks
Value: BitSet (每个位代表一个分片的上传状态)
TTL: 24h
```

### 3.4 秒传

**实现原理**:
1. 上传前，客户端计算文件 MD5 / SHA-256
2. 发送哈希值到服务端校验
3. 服务端查询数据库：
   - 若文件已存在 → 直接返回文件 URL（秒传成功）
   - 若文件不存在 → 正常上传流程

**关键表设计**:
```sql
-- 文件指纹表
CREATE TABLE file_fingerprint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_hash VARCHAR(64) NOT NULL UNIQUE COMMENT 'SHA-256 哈希',
    file_md5 VARCHAR(32) NOT NULL COMMENT 'MD5 哈希',
    storage_path VARCHAR(512) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    ref_count INT DEFAULT 1 COMMENT '引用计数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_hash (file_hash),
    INDEX idx_file_md5 (file_md5)
);
```

### 3.5 客户端直传（Presigned URL）

**流程**:
1. 客户端向服务端请求预签名上传 URL
2. 服务端生成 MinIO Presigned PUT URL（含过期时间）
3. 客户端直接上传文件至 MinIO，绕过服务端
4. MinIO 通过 Webhook 通知服务端上传完成
5. 服务端更新元数据

**优势**: 减轻服务端带宽压力，适合高并发场景

---

## 4. 图片处理方案

### 4.1 Java 图片处理库对比

#### Thumbnailator（推荐）

| 特性 | 说明 |
|------|------|
| GitHub Stars | 5.4k+ |
| 功能 | 缩略图、水印、旋转、裁剪、压缩 |
| 支持格式 | JPEG, PNG, GIF, BMP, WBMP |
| API 风格 | 流式 API，代码简洁 |
| 扩展性 | 可集成 TwelveMonkeys 支持更多格式 |

```java
// 生成缩略图 + 水印
Thumbnails.of("original.jpg")
    .size(800, 600)
    .watermark(Positions.BOTTOM_RIGHT, watermarkImage, 0.5f)
    .outputQuality(0.85)
    .toFile("thumbnail.jpg");
```

```xml
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>
```

#### TwelveMonkeys ImageIO

| 特性 | 说明 |
|------|------|
| 定位 | Java ImageIO 扩展插件 |
| 额外格式 | SVG, ICNS, IFF, PCX, PICT, JPEG Lossless, TIFF, WebP |
| 用途 | 扩展 Thumbnailator/ImageIO 的格式支持 |

```xml
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-jpeg</artifactId>
    <version>3.10.1</version>
</dependency>
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-webp</artifactId>
    <version>3.10.1</version>
</dependency>
```

#### imgscalr

| 特性 | 说明 |
|------|------|
| 定位 | 轻量级图片缩放库 |
| 特点 | 零依赖、高质量缩放算法 |
| API | `Scalr.resize(image, Method.ULTRA_QUALITY, 800, 600)` |

### 4.2 处理功能矩阵

| 功能 | Thumbnailator | TwelveMonkeys | imgscalr | 自研 |
|------|---------------|---------------|----------|------|
| 缩略图生成 | 原生支持 | 格式扩展 | 原生支持 | 需开发 |
| 水印添加 | 原生支持 | N/A | 不支持 | 需开发 |
| 格式转换 | 基础格式 | 丰富格式 | 不支持 | 复杂 |
| 图片压缩 | 原生支持 | N/A | 不支持 | 需开发 |
| 图片裁剪 | 原生支持 | N/A | 原生支持 | 需开发 |
| WebP 支持 | 需扩展 | 原生支持 | 不支持 | 复杂 |

### 4.3 推荐组合

**Thumbnailator + TwelveMonkeys** = 功能全面的图片处理方案
- Thumbnailator 提供核心处理能力
- TwelveMonkeys 扩展格式支持（WebP、TIFF 等）

---

## 5. 图片检索与去重

### 5.1 精确去重（哈希匹配）

| 算法 | 说明 | 碰撞概率 |
|------|------|---------|
| MD5 | 128 位，速度快 | 理论可碰撞 |
| SHA-256 | 256 位，安全性高 | 极低 |
| 双重哈希 | MD5 + SHA-256 | 接近零 |

**推荐方案**: SHA-256 作为主键 + MD5 作为快速索引

### 5.2 感知哈希（相似图检测）

**JImageHash 库**:
- GitHub 444+ stars，MIT 许可
- 支持 pHash、dHash、aHash 等多种感知哈希算法
- 可用于相似图片检测和去重

```java
// 计算感知哈希
HashingAlgorithm hasher = new PerceptiveHash(32);
Hash hash1 = hasher.hash(image1);
Hash hash2 = hasher.hash(image2);
double similarity = hash1.normalizedHammingDistance(hash2);
// similarity < 0.1 表示高度相似
```

```xml
<dependency>
    <groupId>dev.brachtendorf</groupId>
    <artifactId>JImageHash</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 5.3 EXIF 元数据提取

**metadata-extractor 库**:
- 1465+ commits，72 位贡献者，成熟稳定
- 支持 EXIF、IPTC、XMP 元数据
- 支持 JPEG, TIFF, PNG, GIF, BMP 及 RAW 格式

```java
Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
if (directory != null) {
    Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
    String camera = directory.getString(ExifSubIFDDirectory.TAG_LENS_MODEL);
}
```

```xml
<dependency>
    <groupId>com.drewnoakes</groupId>
    <artifactId>metadata-extractor</artifactId>
    <version>2.19.0</version>
</dependency>
```

---

## 6. 安全防护

### 6.1 上传安全检查清单

| 检查项 | 说明 | 优先级 |
|--------|------|--------|
| 文件类型白名单 | 仅允许 JPEG/PNG/GIF/WebP/BMP | P0 |
| Magic Bytes 校验 | 验证文件头魔数，防止伪装 | P0 |
| 文件大小限制 | 单文件最大 20MB | P0 |
| 文件名消毒 | 使用系统生成的文件名替代原始名 | P0 |
| Content-Type 二次验证 | 不信任客户端 Content-Type | P0 |
| 图片解码验证 | 尝试解码确认是合法图片 | P1 |
| SVG 禁止/消毒 | SVG 可嵌入 JS，存在 XSS 风险 | P1 |
| EXIF 清理 | 移除 GPS 等敏感元数据 | P1 |
| 病毒扫描 | 集成 ClamAV 等扫描引擎 | P2 |
| 图片炸弹检测 | 检测像素炸弹（如 1x1 压缩到 100000x100000） | P2 |

### 6.2 Magic Bytes 校验表

| 格式 | Magic Bytes (Hex) |
|------|-------------------|
| JPEG | `FF D8 FF` |
| PNG | `89 50 4E 47 0D 0A 1A 0A` |
| GIF | `47 49 46 38` |
| BMP | `42 4D` |
| WebP | `52 49 46 46 ... 57 45 42 50` |

### 6.3 访问控制

| 策略 | 说明 |
|------|------|
| 预签名 URL | 限时访问，防止盗链 |
| Referer 校验 | 限制来源域名 |
| Token 鉴权 | 对敏感图片使用 Token 访问 |
| 水印保护 | 对付费内容添加隐式/显式水印 |
| IP 限流 | 防止恶意爬取 |

---

## 7. 有损/无损上传与质量保真

> 详细的多语言对比和深度分析请参阅：`image-service-language-comparison-research.md` 第 5 章

### 7.1 核心问题：代际损失（Generation Loss）

JPEG 等有损格式在每次重新编码时都会引入额外的质量损失。在图片服务中，以下操作会触发重编码：

| 操作 | 是否触发重编码 | 质量影响 |
|------|--------------|---------|
| 原图直存（字节流写入） | 否 | **零损失** |
| 生成缩略图 | 是（解码 → 缩放 → 编码） | 有损失 |
| 添加水印 | 是（解码 → 叠加 → 编码） | 有损失 |
| 格式转换 | 是（解码 → 转码） | 可能有损失 |
| EXIF 提取 | 否（仅读取元数据） | **零损失** |
| 哈希计算 | 否（仅读取字节流） | **零损失** |

### 7.2 图片格式压缩特性

| 格式 | 压缩类型 | 压缩比 | 适用场景 | 重编码损失 |
|------|---------|--------|---------|----------|
| JPEG | 有损 | 10:1~20:1 | 照片 | 每次重编码都损失 |
| PNG | 无损 | 2:1~5:1 | 图标/截图/透明图 | 无 |
| GIF | 无损(256色) | 2:1~5:1 | 动图 | 无 |
| WebP | 有损+无损 | 比JPEG小25-34% | 通用 | 有损模式有 |
| AVIF | 有损+无损 | 比JPEG小~50% | 高质量照片 | 有损模式有 |
| JPEG XL | 有损+无损 | 比JPEG小~50% | 通用 | **支持JPEG无损重压缩** |
| TIFF | 无损 | 1:1~5:1 | 专业/印刷 | 无 |

### 7.3 原图保真策略（推荐）

**核心原则：原图永远不被修改或重编码，处理结果作为衍生物单独存储。**

```
上传流程:
  客户端 → 安全校验（Magic Bytes, 不解码整图）
         → SHA-256 哈希计算（读取字节流, 不解码）
         → 原始字节流直写 MinIO (零重编码)
         → 存储后哈希比对确认字节级一致
         → 异步提取 EXIF（仅读取, 不修改文件）

处理流程:
  所有衍生图均从原图派生, 绝不从衍生图再派生
  originals/{uuid}.jpg → [解码] → [处理] → [编码] → thumbnails/{uuid}_s.jpg
```

### 7.4 不同场景的质量策略

| 场景 | 策略 | 推荐质量 |
|------|------|---------|
| 原图存储 | **无损直存** | 原样保留 |
| 缩略图（展示用） | 有损可接受 | JPEG 85 / WebP 80 |
| 大图预览 | 轻微有损 | JPEG 92 / WebP 90 |
| 水印图 | 有损可接受 | JPEG 85 |
| 下载/导出 | **返回原图** | 原样返回 |
| 格式转换(WebP) | 有损可接受 | WebP 85 |
| 专业/印刷 | **无损** | PNG / TIFF |

### 7.5 跨语言方案说明

本文基于 Java 生态调研。关于 Go（imgproxy + libvips）、Rust、Python、C/C++ 等语言在图片服务场景的详细对比，包含 7 维度评分体系和 5 套架构方案推荐，请参阅：

**→ `docs/research/image-service-language-comparison-research.md`**

---

## 8. 数据库设计方案

### 7.1 核心表设计

```sql
-- 图片主表
CREATE TABLE img_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    image_uuid VARCHAR(36) NOT NULL UNIQUE COMMENT '图片唯一标识',
    file_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 哈希',
    file_md5 VARCHAR(32) NOT NULL COMMENT 'MD5 哈希',
    original_name VARCHAR(255) COMMENT '原始文件名',
    storage_path VARCHAR(512) NOT NULL COMMENT '存储路径',
    bucket_name VARCHAR(64) NOT NULL COMMENT '存储桶名称',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    width INT COMMENT '图片宽度(px)',
    height INT COMMENT '图片高度(px)',
    format VARCHAR(16) NOT NULL COMMENT '图片格式(jpeg/png/gif/webp)',
    mime_type VARCHAR(64) NOT NULL COMMENT 'MIME 类型',
    color_space VARCHAR(16) COMMENT '色彩空间(RGB/CMYK)',
    has_alpha TINYINT DEFAULT 0 COMMENT '是否有透明通道',
    uploader_id BIGINT COMMENT '上传者ID',
    upload_source VARCHAR(32) COMMENT '上传来源(web/api/admin)',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-删除 1-正常 2-审核中',
    access_level TINYINT DEFAULT 0 COMMENT '访问级别: 0-公开 1-私有 2-受限',
    view_count BIGINT DEFAULT 0 COMMENT '访问次数',
    description TEXT COMMENT '图片描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME COMMENT '软删除时间',
    INDEX idx_file_hash (file_hash),
    INDEX idx_file_md5 (file_md5),
    INDEX idx_uploader (uploader_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_format (format)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片主表';

-- 图片 EXIF 元数据表
CREATE TABLE img_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    image_id BIGINT NOT NULL COMMENT '关联图片ID',
    camera_make VARCHAR(64) COMMENT '相机品牌',
    camera_model VARCHAR(64) COMMENT '相机型号',
    lens_model VARCHAR(128) COMMENT '镜头型号',
    focal_length VARCHAR(16) COMMENT '焦距',
    aperture VARCHAR(16) COMMENT '光圈',
    shutter_speed VARCHAR(16) COMMENT '快门速度',
    iso INT COMMENT 'ISO 感光度',
    taken_at DATETIME COMMENT '拍摄时间',
    gps_latitude DECIMAL(10,8) COMMENT 'GPS 纬度',
    gps_longitude DECIMAL(11,8) COMMENT 'GPS 经度',
    orientation INT COMMENT '拍摄方向',
    raw_exif JSON COMMENT '完整 EXIF 数据(JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_image_id (image_id),
    INDEX idx_taken_at (taken_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片 EXIF 元数据表';

-- 标签表
CREATE TABLE img_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE COMMENT '标签名',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 图片-标签关联表
CREATE TABLE img_image_tag (
    image_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (image_id, tag_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片标签关联表';

-- 图片分组/相册表
CREATE TABLE img_album (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL COMMENT '相册名称',
    description TEXT COMMENT '相册描述',
    cover_image_id BIGINT COMMENT '封面图片ID',
    owner_id BIGINT NOT NULL COMMENT '所有者ID',
    image_count INT DEFAULT 0 COMMENT '图片数量',
    access_level TINYINT DEFAULT 0 COMMENT '访问级别',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册表';

-- 图片-相册关联表
CREATE TABLE img_album_image (
    album_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (album_id, image_id),
    INDEX idx_image_id (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册图片关联表';

-- 缩略图/衍生图表
CREATE TABLE img_thumbnail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_image_id BIGINT NOT NULL COMMENT '原图ID',
    thumbnail_type VARCHAR(32) NOT NULL COMMENT '类型: small/medium/large/custom',
    width INT NOT NULL COMMENT '宽度',
    height INT NOT NULL COMMENT '高度',
    storage_path VARCHAR(512) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    format VARCHAR(16) NOT NULL COMMENT '格式',
    quality INT COMMENT '压缩质量(1-100)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source (source_image_id),
    UNIQUE KEY uk_source_type (source_image_id, thumbnail_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缩略图表';

-- 上传任务表（分片上传）
CREATE TABLE img_upload_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    upload_id VARCHAR(64) NOT NULL UNIQUE COMMENT '上传任务ID',
    file_hash VARCHAR(64) COMMENT '文件哈希',
    file_name VARCHAR(255) COMMENT '文件名',
    file_size BIGINT NOT NULL COMMENT '文件总大小',
    chunk_size INT NOT NULL COMMENT '分片大小',
    total_chunks INT NOT NULL COMMENT '总分片数',
    uploaded_chunks INT DEFAULT 0 COMMENT '已上传分片数',
    status TINYINT DEFAULT 0 COMMENT '0-上传中 1-合并中 2-已完成 3-已失败 4-已过期',
    uploader_id BIGINT COMMENT '上传者',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_upload_id (upload_id),
    INDEX idx_status (status),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传任务表';
```

### 7.2 主键策略

| 方案 | 优势 | 劣势 |
|------|------|------|
| AUTO_INCREMENT | 简单高效，B+Tree 友好 | 分库分表困难 |
| UUID | 全局唯一，分布式友好 | 索引效率低 |
| Snowflake | 有序 + 全局唯一 | 需要额外部署 |
| **推荐: BIGINT 自增 + UUID 对外暴露** | 内部高效 + 外部安全 | - |

---

## 9. 分发与缓存策略

### 9.1 多级缓存架构

```
客户端缓存 (浏览器 Cache)
    ↓ miss
CDN 边缘节点缓存
    ↓ miss
Nginx 反向代理缓存 (本地磁盘/内存)
    ↓ miss
Redis 元数据缓存
    ↓ miss
MinIO 对象存储 (源站)
```

### 9.2 CDN 策略

| 策略 | 说明 |
|------|------|
| URL 哈希 | 通过 URL 哈希实现 Cache Busting |
| 长 TTL | 图片不变，设置较长 TTL（7-30 天） |
| 预热机制 | 热门图片提前推送至边缘节点 |
| 回源优化 | 合并回源请求，减轻源站压力 |

### 9.3 冷热分离

| 分层 | 存储介质 | 访问频率 | 成本 |
|------|---------|---------|------|
| 热数据 | SSD + Redis 缓存 | 高频（7 天内） | 高 |
| 温数据 | HDD + CDN | 中频（30 天内） | 中 |
| 冷数据 | 低频存储 / 归档 | 低频（30 天以上） | 低 |

MinIO 支持生命周期策略（Lifecycle Policy），可自动实现数据分层。

---

## 10. 高可用与容灾

### 10.1 架构要点

| 组件 | 高可用方案 |
|------|-----------|
| 服务层 | 多实例 + 负载均衡 |
| 存储层 | MinIO 纠删码 + 多节点部署 |
| 数据库 | MySQL 主从 + 读写分离 |
| 缓存层 | Redis Cluster / Sentinel |
| 消息队列 | 异步处理图片任务 |

### 10.2 参考指标

| 指标 | 目标 |
|------|------|
| 可用性 | 99.99%（4 个 9） |
| 上传延迟 | P99 < 3s（5MB 图片） |
| 下载延迟 | P99 < 200ms（CDN 命中） |
| 数据持久性 | 99.999999999%（11 个 9） |

---

## 11. 系统设计高频面试场景

以下是图片存储服务在系统设计面试中的常见问题，也是实际开发需要考虑的场景：

### 11.1 热点文件问题

**场景**: 百万并发下载同一张热门图片

**解决方案**:
- CDN 多节点缓存
- 本地缓存 + L1/L2 缓存
- 限流 + 排队机制
- 预热热门内容

### 11.2 元数据分片热点

**场景**: 如何设计元数据表的分片策略避免热点？

**方案**:
- 以 image_id 为分片键，均匀分布
- 避免以时间为分片键（写入热点）
- 热点数据使用 Redis 缓存

### 11.3 大规模图片去重

**场景**: PB 级别存储下如何去重？

**方案**:
- L1: MD5/SHA-256 精确匹配
- L2: 感知哈希近似匹配
- 引用计数 + 延迟删除

### 11.4 成本与性能的平衡

**场景**: 如何在存储成本和访问性能间取得平衡？

**方案**:
- 冷热分离 + 生命周期管理
- 图片压缩 + WebP 转换（节省 30-50% 空间）
- 按需生成缩略图 vs 预生成

### 11.5 图片审核

**场景**: 如何防止违规图片上传？

**方案**:
- 上传后异步审核
- 集成 AI 内容审核服务
- 先审后发 vs 先发后审（根据业务选择）

### 11.6 跨地域容灾

**场景**: 如何实现多地域数据同步和容灾？

**方案**:
- QQ 相册实践：两阶段模型（数据落地 + 索引同步）
- 就近写入 + 异步复制
- 跨 IDC 超时率监控 + 动态调整
- MinIO 支持跨区域复制（Site Replication）

---

## 12. 技术选型总结

### 12.1 推荐技术栈

| 组件 | 推荐方案 | 备选方案 |
|------|---------|---------|
| **存储引擎** | MinIO (S3 兼容) | 阿里云 OSS / AWS S3 |
| **Web 框架** | Spring Boot 2.7.x | Spring Boot 3.x |
| **数据库** | MySQL 8.0 | PostgreSQL |
| **缓存** | Redis Cluster | Caffeine + Redis |
| **图片处理** | Thumbnailator + TwelveMonkeys | ImageMagick (外部命令) |
| **元数据提取** | metadata-extractor | - |
| **图片去重** | JImageHash + SHA-256 | pHash 自研 |
| **消息队列** | RocketMQ / RabbitMQ | Kafka |
| **对象映射** | MapStruct | ModelMapper |
| **API 文档** | SpringDoc (OpenAPI 3) | Swagger 2 |
| **任务调度** | XXL-Job | Spring Task |

### 12.2 三套方案对比

#### 方案一：轻量级（推荐起步）

```
Spring Boot + MinIO + MySQL + Redis + Thumbnailator
```
- 适合: 个人/小团队，快速验证
- 成本: 低
- 复杂度: 低
- 扩展性: 中

#### 方案二：中等规模

```
Spring Boot + MinIO Cluster + MySQL 主从 + Redis Cluster + RocketMQ + CDN
```
- 适合: 中等团队，百万级图片
- 成本: 中
- 复杂度: 中
- 扩展性: 高

#### 方案三：企业级

```
Spring Cloud + MinIO/Ceph + MySQL 分库分表 + Redis Cluster + RocketMQ + CDN + AI 审核
```
- 适合: 大团队，亿级图片
- 成本: 高
- 复杂度: 高
- 扩展性: 极高

---

## 13. 参考资料

### 行业实践
- [QQ 相册后台存储架构重构与跨 IDC 容灾实践](https://blog.csdn.net/Tencent_TEG/article/details/78865013)
- [Uber Eats 图片去重与存储方案](https://www.uber.com/blog/deduping-and-storing-images-at-uber-eats/)
- [Instagram 系统设计](https://grokkingthesystemdesign.com/guides/instagram-system-design/)
- [Flickr 系统设计](https://livebook.manning.com/book/acing-the-system-design-interview/chapter-12)

### 技术文档
- [MinIO 官方文档](https://min.io/docs/minio/linux/index.html)
- [Thumbnailator Wiki](https://github.com/coobird/thumbnailator/wiki)
- [JImageHash GitHub](https://github.com/KilianB/JImageHash)
- [metadata-extractor](https://github.com/drewnoakes/metadata-extractor)
- [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)

### Spring Boot 集成
- [Spring Boot 整合 MinIO 实现全场景文件管理](https://blog.csdn.net/Minoz_wl/article/details/148429445)
- [Spring Boot 3 整合 MinIO 实现分布式文件存储](https://developer.aliyun.com/article/1655776)
- [MinIO 分片上传 + 秒传 + 断点续传 Demo](https://github.com/zazhiii/minio-upload-demo)

---

**文档版本**: 1.0  
**创建时间**: 2026-02-10  
**维护者**: ImgVault Team
