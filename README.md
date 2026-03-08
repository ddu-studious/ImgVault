# ImgVault - 统一图片存储与查询服务

轻量化图片管理平台，基于 Spring Boot + SQLite + MinIO + imgproxy 构建，支持多格式上传、智能缩略图、多尺寸下载、访客隔离等能力。

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 8 |
| 框架 | Spring Boot 2.7.18 |
| 构建 | Maven |
| 数据库 | SQLite (WAL 模式) |
| ORM | MyBatis |
| 缓存 | Caffeine |
| 对象存储 | MinIO |
| 图片处理 | imgproxy (Go + libvips) |
| API 文档 | SpringDoc OpenAPI |

## 核心功能

- **图片上传** - 支持拖拽上传、批量上传、秒传去重、分片上传
- **多格式支持** - JPEG / PNG / GIF / WebP / BMP，最大 50MB
- **多尺寸处理** - imgproxy 实时生成缩略图 (150×150 / 800×600 / 1920×1080)
- **格式转换** - 支持 WebP / AVIF 格式转换、质量压缩
- **智能裁剪** - 基于内容感知的智能裁剪
- **水印** - 支持图片水印叠加
- **瀑布流浏览** - 前端瀑布流 + Lightbox 走马灯浏览
- **多规格下载** - 自定义尺寸、格式、质量的下载
- **标签管理** - 图片标签和相册管理
- **访客隔离** - 基于 visitor_id 的数据隔离
- **管理后台** - 独立 Admin 端，支持统计、图片管理、回收站、日志

## 项目结构

```
ImgVault/
├── imgvault-common/         # 公共模块 (DTO、枚举、工具)
├── imgvault-domain/         # 领域层 (实体、仓储接口)
├── imgvault-infrastructure/ # 基础设施 (MyBatis、MinIO、imgproxy)
├── imgvault-app/            # 应用服务 (业务编排)
├── imgvault-api/            # API 服务 (REST 控制器, 端口 8080)
├── imgvault-admin/          # 管理后台 (独立应用, 端口 8082)
├── imgvault-task/           # 异步任务
├── frontend/                # 前端 (原生 HTML/CSS/JS)
│   ├── index.html           # 用户端
│   ├── app.js               # 用户端逻辑
│   ├── style.css            # 样式
│   └── admin/               # 管理后台前端
├── sql/                     # 数据库脚本
├── scripts/                 # 辅助脚本
├── docs/                    # 文档
├── docker-compose.yml       # MinIO + imgproxy
├── start.sh                 # 一键启动
└── stop.sh                  # 一键停止
```

## 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 8+ | Java 运行环境 |
| Maven | 3.6+ | Java 构建工具 |
| Docker | 20+ | 容器运行时 (用于 MinIO + imgproxy) |
| Docker Compose | v2+ | 容器编排 |

### 一键启动

```bash
chmod +x start.sh
./start.sh
```

启动脚本会自动完成：
1. 检查 Java / Maven / Docker 环境
2. 自动安装 Docker（Linux，如未安装）
3. 创建数据目录 `data/`
4. 生成 imgproxy 签名密钥
5. 启动 MinIO + imgproxy (Docker)
6. 等待 MinIO 就绪，创建 Bucket
7. Maven 编译打包
8. 启动 Java 服务

### 启动后访问

| 服务 | 地址 | 说明 |
|------|------|------|
| 用户端 | http://localhost:8080 | 图片浏览上传 |
| Swagger | http://localhost:8080/swagger-ui.html | API 文档 |
| Health | http://localhost:8080/actuator/health | 健康检查 |
| imgproxy | http://localhost:8081 | 图片处理代理 |
| MinIO Console | http://localhost:9001 | 对象存储管理 (admin/minioadmin) |

### 停止服务

```bash
./stop.sh
```

## 配置说明

主配置文件：`imgvault-api/src/main/resources/application.yml`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | API 端口 |
| `minio.endpoint` | http://localhost:9000 | MinIO 地址 |
| `minio.bucket-name` | imgvault | 存储桶名称 |
| `imgproxy.base-url` | http://localhost:8081 | imgproxy 地址 |
| `admin.password` | imgvault-admin | 管理后台密码 |
| `spring.servlet.multipart.max-file-size` | 50MB | 最大上传文件大小 |

## API 概览

详细 API 文档见 `docs/api/api-reference.md`

```bash
# 上传图片
curl -X POST http://localhost:8080/imgvault/api/v1/images/upload -F "file=@photo.jpg"

# 获取图片列表
curl http://localhost:8080/imgvault/api/v1/images?page=1&size=20

# 获取图片详情
curl http://localhost:8080/imgvault/api/v1/images/{id}

# 图片处理 (缩放+格式转换)
curl "http://localhost:8080/imgvault/api/v1/images/{id}/process?width=800&format=webp"
```

## License

MIT
