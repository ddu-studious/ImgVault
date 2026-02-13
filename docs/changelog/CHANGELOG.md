# 变更日志

## [2.1.0] - 2026-02-14

### Phase 2: imgproxy 集成 (2026-02-14)

#### 新增
- imgproxy URL 签名服务（HMAC-SHA256，参考官方 Java 示例）
- 图片查询返回 imgproxy 缩略图 URL（small/medium/large 三种规格）
- 图片查询返回格式变体 URL（WebP/AVIF 自动生成）
- 图片处理参数化 API（`GET /api/v1/images/{id}/process`）
- 支持缩放、格式转换、水印、压缩、智能裁剪
- imgproxy Docker Compose 配置（S3/MinIO 后端、URL 签名、内存优化）
- ImgproxyConfig 配置属性类
- ImgproxyService 服务（签名、URL 生成）
- ImageProcessRequest DTO

#### 改进
- MinIO 初始化容错（服务不可用时不阻塞启动，支持延迟重连）
- SQLite 初始化脚本解析器改进（正确处理 BEGIN...END 触发器块）
- ImageDetailDTO 增加 thumbnails 和 variants 字段

---

### Phase 1: 轻量化 MVP (2026-02-14)

### 新增
- Spring Boot 2.7.18 + SQLite + Caffeine + MinIO 基础框架搭建
- Maven 多模块项目结构（7 个模块全部编译通过）
- 图片上传 API（单张/批量）：原始字节流直写 MinIO，零重编码
- Magic Bytes + 类型白名单安全校验
- 图片查询 API（按 ID/UUID/分页列表）
- 图片下载 API（预签名 URL，302 重定向）
- 图片删除 API（软删除 + 物理删除）
- 自动提取图片尺寸、格式、MIME 类型元数据
- Caffeine 本地缓存（替代 Redis）
- SQLite WAL 模式数据库（替代 MySQL）
- Spring Async 异步任务配置（替代 RocketMQ）
- MinIO 健康检查 + SQLite 健康检查
- SpringDoc OpenAPI 文档
- 全局异常处理
- Docker Compose 配置（MinIO + imgproxy）
- 一键启动脚本 start.sh

### 架构变更
- 数据库：MySQL 8.0 → **SQLite WAL 模式**
- 缓存：Redis 6.x → **Caffeine 本地缓存**
- 消息队列：RocketMQ 4.9.x → **Spring Async Event + SQLite 任务表**
- 图片处理：Thumbnailator → **imgproxy (Go + libvips)**（Phase 2 集成）
- 部署：4 个容器 → **2 个容器** (MinIO + imgproxy)
- 内存：~3GB → **~500MB**

---

## [2.0.0] - 2026-02-10

### 规划
- 图片存储查询服务需求文档
- MySQL + Redis + RocketMQ 技术选型
- DDD 架构设计

---

**最后更新**: 2026-02-14
