# MCP 命令快捷方式

## 常用 MCP 工具

```json
{
  "cursor.commands": {
    "doc": "@Context7",
    "git": "@github",
    "thinking": "@sequential-thinking",
    "browser": "@browser"
  }
}
```

## 使用示例

### 查询技术文档 (@Context7)

```
# Spring Boot / Java 相关
@doc Spring Boot 文件上传配置
@doc Spring Boot Multipart 配置参数
@doc Spring Boot Actuator 健康检查
@doc Spring Boot 异步任务配置
@doc MyBatis 动态 SQL
@doc MyBatis 批量插入

# MinIO / 对象存储
@doc MinIO Java SDK 上传文件
@doc MinIO Presigned URL 生成
@doc MinIO Bucket 生命周期策略
@doc MinIO 纠删码配置
@doc MinIO 事件通知 Webhook

# Redis
@doc Spring Data Redis 操作
@doc Redis 分布式锁
@doc Redis BitSet 操作
@doc Redis 缓存淘汰策略

# 图片处理
@doc Thumbnailator 生成缩略图
@doc Thumbnailator 水印添加
@doc TwelveMonkeys WebP 支持
@doc metadata-extractor EXIF 提取

# RocketMQ
@doc RocketMQ Spring Boot 集成
@doc RocketMQ 消息消费
@doc RocketMQ 延迟消息

# 其他
@doc MapStruct 对象映射
@doc SpringDoc OpenAPI 3 配置
@doc Docker Compose MinIO 部署
```

### Git 操作 (@github)

```
# 提交代码
@git 提交代码并推送

# 分支管理
@git 创建功能分支 feature-image-upload
@git 创建功能分支 feature-thumbnail
@git 合并到主分支

# PR 管理
@git 创建 PR
@git 查看 PR 状态

# 查看历史
@git 查看最近提交
@git 查看文件变更

# 开源调研
@git 搜索 Spring Boot MinIO 图片上传项目
@git 查看 MinIO Java SDK 仓库最新 release
```

### 复杂问题分析 (@sequential-thinking)

```
# 架构设计
@thinking 设计图片上传流程方案
@thinking 设计分片上传与断点续传方案
@thinking 设计图片去重策略
@thinking 设计 MinIO 存储路径规划

# 性能优化
@thinking 分析图片上传接口性能瓶颈
@thinking 设计多级缓存架构
@thinking 分析图片处理任务并发策略

# 问题排查
@thinking 分析大文件上传超时的可能原因
@thinking 分析图片处理 OOM 的解决方案
@thinking 排查 MinIO 连接池耗尽问题

# 方案对比
@thinking 对比 MinIO vs Ceph vs FastDFS 选型
@thinking 对比同步处理 vs 异步处理缩略图
@thinking 对比预签名直传 vs 服务端中转上传
```

### 浏览器操作 (@browser)

```
# MinIO 控制台
@browser 打开 MinIO 控制台 http://localhost:9001

# API 文档
@browser 检查 Swagger/OpenAPI 文档 http://localhost:8080/swagger-ui.html

# 功能验证
@browser 测试图片上传接口
@browser 验证缩略图生成效果

# 技术调研
@browser 搜索 MinIO Java SDK 最新文档
@browser 搜索图片存储服务最佳实践
@browser 搜索 Spring Boot 大文件上传方案
```

## 自动触发场景

AI 会根据上下文自动选择合适的 MCP 工具：

| 场景 | 自动使用工具 |
|-----|-------------|
| 提到"文档"、"怎么用"、"API"、"SDK" | Context7 |
| 提到"提交"、"推送"、"分支"、"PR" | GitHub |
| 复杂问题、多步骤分析、架构设计 | Sequential Thinking |
| 需要浏览器测试验证、搜索调研 | Browser |

## 项目特定查询示例

### MinIO 操作
```
@doc MinIO Java SDK PutObject
@doc MinIO Multipart Upload
@doc MinIO Presigned GET/PUT URL
@doc MinIO Bucket Policy
@doc MinIO Object Lifecycle
@doc MinIO Server-Side Encryption
```

### Spring Boot 配置
```
@doc Spring Boot @ConfigurationProperties 使用
@doc Spring Boot MultipartFile 配置
@doc Spring Boot 异常处理 @ControllerAdvice
@doc Spring Boot 文件上传大小限制
@doc Spring Boot Redis 序列化配置
```

### MyBatis
```
@doc MyBatis 动态 SQL 标签
@doc MyBatis 批量操作
@doc MyBatis 结果映射
@doc MyBatis 分页查询
```

### 图片处理库
```
@doc Thumbnailator resize 缩放
@doc Thumbnailator watermark 水印
@doc Thumbnailator 输出质量控制
@doc TwelveMonkeys ImageIO WebP 读写
@doc metadata-extractor EXIF 读取
@doc JImageHash 感知哈希计算
```

## 组合使用示例

### 开发新功能

```
# 1. 先查文档了解技术细节
@doc MinIO Multipart Upload Java SDK

# 2. 分析实现方案
@thinking 设计分片上传与断点续传方案

# 3. 提交代码
@git 提交分片上传功能代码

# 4. 测试验证
@browser 验证分片上传功能
```

### 技术调研

```
# 1. 搜索开源方案
@git 搜索 Java 图片存储服务开源项目

# 2. 浏览技术文档
@browser 搜索 MinIO 最佳实践 2026

# 3. 查阅官方文档
@doc MinIO 高可用部署方案

# 4. 深入分析
@thinking 对比各种存储方案的优缺点
```

### 问题排查

```
# 1. 分析问题
@thinking 分析图片上传偶发失败的可能原因

# 2. 查阅文档
@doc MinIO 连接池配置
@doc Spring Boot 文件上传超时设置

# 3. 修复并提交
@git 提交 Bug 修复代码
```

### 性能优化

```
# 1. 分析瓶颈
@thinking 分析图片服务 P99 延迟过高的原因

# 2. 查阅优化方案
@doc Redis 缓存策略
@doc MinIO 并发上传优化

# 3. 搜索业界实践
@browser 搜索大规模图片服务性能优化实践

# 4. 实施优化
@git 提交性能优化代码
```

## 快捷命令总结

| 命令 | 用途 | 示例 |
|-----|------|-----|
| `@doc` | 查询技术文档 | `@doc MinIO Presigned URL` |
| `@git` | Git 操作/GitHub 调研 | `@git 提交代码` |
| `@thinking` | 复杂问题分析 | `@thinking 设计上传方案` |
| `@browser` | 浏览器操作/搜索 | `@browser 打开 MinIO 控制台` |

---

**最后更新**: 2026-02-10
