# ImgVault 项目上下文

## 项目信息

- **项目名称**: ImgVault (图片存储/查询服务)
- **版本**: v2.0.0
- **定位**: 统一图片存储、处理与查询的基础服务

## 技术栈

### 后端框架
- Java 8+ / Spring Boot 2.7.x
- Spring Cloud 2021.0.5 (可选)
- DDD 领域驱动设计分层架构

### 存储与中间件
- MinIO 8.5.x (对象存储，S3 兼容)
- MySQL 8.0 (元数据持久化)
- Redis 6.x (缓存 + 分片上传状态)
- RocketMQ 4.9.x (异步图片处理消息)

### 图片处理
- Thumbnailator 0.4.20 (缩略图/水印/裁剪/压缩)
- TwelveMonkeys ImageIO 3.10.x (WebP/TIFF 等格式扩展)
- metadata-extractor 2.19.x (EXIF 元数据提取)
- JImageHash 1.0.0 (感知哈希去重)

### ORM 与工具
- MyBatis 3.5.x (ORM)
- MapStruct 1.5.2 (对象映射)
- Lombok 1.18.20 (简化代码)
- FastJson 1.2.83 (JSON 序列化)
- SpringDoc OpenAPI 1.7.x (API 文档)

## 核心功能

1. **图片上传**: 普通上传 / 预签名直传 / 分片上传 / 断点续传 / 秒传
2. **图片存储**: MinIO 对象存储，按日期分目录，支持多桶管理
3. **图片处理**: 缩略图生成 / 水印添加 / 格式转换 / 图片压缩
4. **图片查询**: 按 ID/UUID/标签/日期/格式/相册多维检索
5. **安全防护**: Magic Bytes 校验 / 类型白名单 / 大小限制 / EXIF 隐私清理
6. **图片去重**: SHA-256 精确去重 + 感知哈希相似图检测
7. **元数据管理**: EXIF 自动提取 / 标签管理 / 相册管理
8. **管理后台**: 图片浏览/搜索/审核/统计

## 常用命令

```bash
# === 开发环境启动 ===

# 启动基础设施 (MinIO + MySQL + Redis)
docker-compose up -d

# 启动 API 服务
mvn spring-boot:run -pl imgvault-api -Dspring-boot.run.profiles=dev

# 启动管理后台
mvn spring-boot:run -pl imgvault-admin -Dspring-boot.run.profiles=dev

# 启动异步任务服务
mvn spring-boot:run -pl imgvault-task -Dspring-boot.run.profiles=dev

# === 构建与测试 ===

# 构建项目 (跳过测试)
mvn clean package -DskipTests

# 运行测试
mvn test

# 运行单元测试 (特定模块)
mvn test -pl imgvault-domain

# 运行集成测试
mvn verify -pl imgvault-api

# === MinIO 管理 ===

# MinIO 控制台
open http://localhost:9001

# MinIO CLI 操作 (需安装 mc)
mc alias set imgvault http://localhost:9000 minioadmin minioadmin
mc ls imgvault/
mc mb imgvault/imgvault-dev

# === 数据库 ===

# 执行 SQL 脚本
mysql -u root -p imgvault < sql/init.sql

# === Docker ===

# 重建并启动
docker-compose down && docker-compose up -d --build

# 查看日志
docker-compose logs -f minio
```

## 关键文件路径

### 后端模块
- API 服务: `imgvault-api/src/main/java/`
- 管理后台: `imgvault-admin/src/main/java/`
- 应用层: `imgvault-app/src/main/java/`
- 领域层: `imgvault-domain/src/main/java/`
- 基础设施: `imgvault-infrastructure/src/main/java/`
- 公共模块: `imgvault-common/src/main/java/`
- 任务服务: `imgvault-task/src/main/java/`

### 配置文件
- API 配置: `imgvault-api/src/main/resources/`
- Admin 配置: `imgvault-admin/src/main/resources/`
- Task 配置: `imgvault-task/src/main/resources/`

### 脚本与文档
- SQL 脚本: `sql/`
- 项目文档: `docs/`
- 调研文档: `docs/research/`
- 需求文档: `docs/requirements/`
- 架构文档: `docs/architecture/`

## MCP 工具使用

- **Context7**: 查询技术文档 (Spring Boot, MinIO, MyBatis, Redis, Thumbnailator)
- **GitHub**: Git 操作、PR 管理、开源项目调研
- **Sequential Thinking**: 复杂架构设计和问题分析
- **Browser**: 前端页面测试、API 文档验证

## 数据库信息

### 核心表
| 表名 | 说明 |
|------|------|
| `img_image` | 图片主表（元数据、存储路径、状态） |
| `img_metadata` | EXIF 元数据表（相机、GPS、拍摄参数） |
| `img_tag` | 标签表 |
| `img_image_tag` | 图片-标签关联表 |
| `img_album` | 相册表 |
| `img_album_image` | 相册-图片关联表 |
| `img_thumbnail` | 缩略图/衍生图表 |
| `img_upload_task` | 分片上传任务表 |
| `file_fingerprint` | 文件指纹表（秒传用） |

## MinIO 存储结构

```
imgvault/                    # Bucket
├── originals/              # 原图 (按日期: 2026/02/10/{uuid}.jpg)
├── thumbnails/             # 缩略图 (small/medium/large)
├── watermarked/            # 水印图
├── temp/                   # 分片上传临时存储
└── avatars/                # 头像 (业务扩展)
```

## 开发规范

- 文档必须放在 `docs/` 目录
- 后端遵循 DDD 分层架构
- API 使用 RESTful 风格
- 使用 MapStruct 进行对象映射
- 图片文件名使用 UUID，不暴露原始文件名
- 对外使用 UUID，内部使用 BIGINT 自增 ID
- 图片处理任务使用消息队列异步执行

## 环境配置

### 本地开发环境
- application-dev.yml
- MinIO: localhost:9000 (控制台: localhost:9001)
- MySQL: localhost:3306/imgvault
- Redis: localhost:6379

### 测试环境
- application-test.yml

### 预发环境
- application-pre.yml

### 生产环境
- application-prod.yml

## 服务端口

| 服务 | HTTP 端口 | 管理端口 |
|------|----------|---------|
| API | 8080 | 8899 |
| Admin | 8082 | 8890 |
| Task | 8084 | 8892 |

## 依赖版本速查

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.x | Web 框架 |
| MinIO SDK | 8.5.5 | 对象存储客户端 |
| Thumbnailator | 0.4.20 | 图片处理 |
| TwelveMonkeys | 3.10.1 | 图片格式扩展 |
| metadata-extractor | 2.19.0 | EXIF 提取 |
| JImageHash | 1.0.0 | 感知哈希 |
| MyBatis | 3.5.x | ORM |
| MapStruct | 1.5.2 | 对象映射 |
| SpringDoc | 1.7.x | API 文档 |
