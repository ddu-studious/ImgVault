# 访客隔离（Visitor Isolation）技术方案

## 概述

在无登录体系下，通过浏览器本地生成的匿名访客 ID 实现图片数据隔离，使不同设备/浏览器的用户只能看到自己上传的图片。

## 核心设计

### 架构图

```
┌───────────────────────────────────────────────────────┐
│  浏览器 A                                              │
│  ┌─────────────────────────┐                           │
│  │ localStorage             │                           │
│  │ imgvault_visitor_id:     │                           │
│  │   "a1b2c3d4-..."        │                           │
│  └──────────┬──────────────┘                           │
│             │ X-Visitor-Id: a1b2c3d4-...               │
│             ▼                                          │
│  ┌──────────────────────┐                              │
│  │ GET /api/v1/images   │                              │
│  │ POST /api/v1/upload  │                              │
│  └──────────┬───────────┘                              │
└─────────────┼─────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────┐
│  Spring Boot API                             │
│  ┌────────────────────────────────────────┐  │
│  │ ImageController                         │  │
│  │  - 提取 X-Visitor-Id Header            │  │
│  │  - 传递给 ImageAppService              │  │
│  └────────────────┬───────────────────────┘  │
│                   │                           │
│  ┌────────────────▼───────────────────────┐  │
│  │ ImageAppService                         │  │
│  │  - upload: 存储 visitor_id             │  │
│  │  - query:  按 visitor_id 过滤          │  │
│  └────────────────┬───────────────────────┘  │
│                   │                           │
│  ┌────────────────▼───────────────────────┐  │
│  │ SQLite (img_image)                      │  │
│  │  visitor_id TEXT 列                     │  │
│  │  idx_image_visitor 索引                 │  │
│  └────────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### 核心组件

| 组件 | 职责 | 变更 |
|------|------|------|
| **前端 app.js** | 生成/存储/发送 Visitor ID | 新增 `getVisitorId()` 函数，所有请求携带 `X-Visitor-Id` Header |
| **ImageController** | 提取 Header 中的 Visitor ID | 新增 `@RequestHeader` 参数 |
| **ImageAppService** | 业务编排 | upload 时存储 visitorId，query 时按 visitorId 过滤 |
| **ImageMapper** | SQL 查询 | findPage/count 增加 visitor_id 条件 |
| **img_image 表** | 数据存储 | 新增 `visitor_id TEXT` 列 + 索引 |
| **AdminController** | 管理后台 | 不传 visitorId，可查看所有图片 |

## 数据流转

### 上传流程

```
1. 前端: getVisitorId() → localStorage 读取/生成 UUID
2. 前端: POST /api/v1/images/upload
         Header: X-Visitor-Id: {uuid}
3. Controller: 提取 X-Visitor-Id header
4. AppService: entity.setVisitorId(visitorId)
5. Mapper: INSERT INTO img_image (..., visitor_id) VALUES (..., #{visitorId})
```

### 查询流程

```
1. 前端: getVisitorId() → localStorage 读取 UUID
2. 前端: GET /api/v1/images?page=1&size=24
         Header: X-Visitor-Id: {uuid}
3. Controller: 提取 X-Visitor-Id → request.setVisitorId(visitorId)
4. AppService: 传递 visitorId 到 repository.findPage()
5. Mapper: SELECT ... WHERE status=1 AND visitor_id=#{visitorId}
6. 结果: 只返回该访客上传的图片
```

### 管理端流程

```
1. 管理端: GET /api/v1/admin/images
         Header: Authorization: Bearer {token}
         (无 X-Visitor-Id)
2. AdminController: 不设置 visitorId
3. AppService: visitorId 为 null → 不过滤
4. Mapper: SELECT ... WHERE status=#{status} (不含 visitor_id 条件)
5. 结果: 返回所有图片
```

## 数据库变更

### 新增列

```sql
ALTER TABLE img_image ADD COLUMN visitor_id TEXT;
CREATE INDEX idx_image_visitor ON img_image(visitor_id);
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `visitor_id` | TEXT | 访客唯一标识（UUID 格式），可为空 |

- 新上传的图片会带上 visitor_id
- 历史数据 visitor_id 为 NULL，管理端可见，用户端不可见（除非无 visitor_id 过滤）

## 前端实现

### Visitor ID 生成与存储

```javascript
const VISITOR_ID_KEY = 'imgvault_visitor_id';

function getVisitorId() {
    try {
        let id = localStorage.getItem(VISITOR_ID_KEY);
        if (!id) {
            id = crypto.randomUUID();
            localStorage.setItem(VISITOR_ID_KEY, id);
        }
        return id;
    } catch (e) {
        // 隐私模式降级到 sessionStorage
        let id = sessionStorage.getItem(VISITOR_ID_KEY);
        if (!id) {
            id = crypto.randomUUID();
            sessionStorage.setItem(VISITOR_ID_KEY, id);
        }
        return id;
    }
}
```

### Header 传递

```javascript
async function api(path, opts = {}) {
    const headers = {
        'Content-Type': 'application/json',
        'X-Visitor-Id': getVisitorId(),
        ...opts.headers,
    };
    // ...
}
```

## 安全考虑

1. **Visitor ID 伪造**: 用户可以修改 localStorage 中的值，但只会影响自己看到的数据范围
2. **UUID 碰撞**: UUID v4 碰撞概率极低（约 2^-122），可忽略
3. **隐私**: 不采集设备指纹，仅使用随机 UUID，隐私友好
4. **数据清除**: 用户清除浏览器数据后，会生成新的 Visitor ID，之前的图片对其不可见（但管理端仍可见）

## 注意事项

1. **管理端不受影响**: AdminController 不传递 visitorId，始终看到所有图片
2. **历史数据兼容**: 已有图片的 visitor_id 为 NULL，只在管理端可见
3. **跨设备不共享**: 同一用户在不同设备/浏览器上有不同的 Visitor ID
4. **未来扩展**: 引入用户系统后，可将 visitor_id 关联到 user_id，实现数据迁移

---

**文档版本**: 1.0
**创建时间**: 2026-02-16
**维护者**: ImgVault Team
