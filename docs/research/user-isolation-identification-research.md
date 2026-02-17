# 无登录场景下用户隔离/识别技术调研

**版本**: v1.0  
**创建日期**: 2026-02-16  
**状态**: 已完成  
**项目代号**: ImgVault

---

## 1. 调研背景

### 1.1 业务目标

在**无登录场景**下实现用户隔离与识别，支持以下能力：

- 匿名用户的数据隔离（如相册、上传记录）
- 跨会话的轻量级用户识别
- 设备/浏览器维度的唯一标识
- 隐私合规、实现简单、可靠性可接受

### 1.2 调研范围

| 维度 | 调研内容 |
|------|---------|
| 浏览器指纹 | FingerprintJS 使用方式、可靠性、隐私合规 |
| 设备标识 | MAC 地址、localStorage、sessionStorage、Cookie |
| 轻量级方案 | Token-based、UUID 客户端生成 |
| 商业对比 | FingerprintJS 开源版 vs Pro 版 |

---

## 2. 方案一：浏览器指纹 (Browser Fingerprint)

### 2.1 实现原理

通过收集浏览器/设备的多维属性（屏幕分辨率、时区、字体、Canvas、WebGL、User-Agent 等），生成哈希后的唯一标识符。FingerprintJS 是主流开源实现，2025 年 v5.0 已恢复 MIT 许可。

**核心流程**：

```
浏览器属性采集 → 信号组合 → 哈希算法 → Visitor ID
```

### 2.2 FingerprintJS 使用方式

```javascript
// 安装
npm install @fingerprintjs/fingerprintjs

// 使用
import FingerprintJS from '@fingerprintjs/fingerprintjs';

const fpPromise = FingerprintJS.load();
const fp = await fpPromise;
const result = await fp.get();
console.log(result.visitorId);  // 唯一标识符
```

### 2.3 优缺点

| 优点 | 缺点 |
|------|------|
| 无需用户操作，静默识别 | 同型号设备（如两台 iPhone 15）可能产生相同指纹 |
| 隐私模式/清除数据后仍可生成 | 浏览器/系统更新会导致指纹变化 |
| 不依赖 Cookie，跨域友好 | 易被伪造和逆向 |
| 开源版 MIT 许可，无额外成本 | 准确率有限，约半数回访设备可能无法识别 |

### 2.4 可靠性

| 维度 | 评估 |
|------|------|
| **唯一性** | 中等。同型号设备易碰撞，不同设备区分度较好 |
| **持久性** | 低~中。浏览器更新、时区/语言变更会改变指纹 |
| **稳定性** | 开源版约数周，Pro 版可达数月~数年 |

### 2.5 隐私合规

| 法规/场景 | 要求 |
|-----------|------|
| **GDPR** | 欺诈防护/安全可适用「合法利益」，无需明确同意；用于归因/个性化通常需用户同意 |
| **UK ICO (2025)** | 指纹识别合规门槛高，需透明、可同意、公平处理，用户难以像 Cookie 那样简单同意 |
| **实践建议** | 若用于安全/防欺诈，可基于合法利益；若用于追踪/个性化，需在同意后触发 |

### 2.6 实现复杂度

- **复杂度**：低
- **集成时间**：约 10 分钟
- **依赖**：仅前端，无服务端改造

---

## 3. 方案二：设备标识方案

### 3.1 MAC 地址

| 项目 | 说明 |
|------|------|
| **能否获取** | ❌ **不能**。标准 Web/JavaScript 无法获取 MAC 地址 |
| **原因** | 浏览器出于隐私和安全限制，不暴露底层网络信息 |
| **历史** | 仅 IE + ActiveX 曾可查询 WMI，现已不可用 |
| **替代** | 使用 Network Information API 仅能获取连接类型（WiFi/蜂窝/以太网），无 MAC |

**结论**：Web 应用无法依赖 MAC 地址做设备识别。

### 3.2 localStorage 方案

| 项目 | 说明 |
|------|------|
| **实现原理** | 首次访问生成 UUID，写入 `localStorage`，后续访问读取 |
| **持久性** | 高，除非用户主动清除或隐私模式 |
| **跨域** | 同源策略，仅当前域名可读 |
| **优点** | 实现简单、无 Cookie 限制、用户可清除 |
| **缺点** | 清除存储即丢失；隐私模式下可能不可用 |

```javascript
function getOrCreateAnonymousId() {
  const key = 'imgvault_anonymous_id';
  let id = localStorage.getItem(key);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(key, id);
  }
  return id;
}
```

### 3.3 sessionStorage 方案

| 项目 | 说明 |
|------|------|
| **实现原理** | 与 localStorage 类似，但仅存活于当前标签页会话 |
| **持久性** | 低，关闭标签页即失效 |
| **适用场景** | 单次会话内的临时隔离，不适合跨会话识别 |

### 3.4 Cookie 方案

| 项目 | 说明 |
|------|------|
| **实现原理** | 服务端或前端设置 Cookie，每次请求自动携带 |
| **持久性** | 可配置（Session / 长期） |
| **优点** | 服务端可读、可设 HttpOnly、适合鉴权 |
| **缺点** | 受第三方 Cookie 限制、易被拦截、需考虑 SameSite/跨域 |

```javascript
// 设置长期 Cookie
document.cookie = `anonymous_id=${uuid}; path=/; max-age=31536000; SameSite=Lax`;
```

### 3.5 存储方案对比

| 方案 | 持久性 | 服务端可读 | 隐私模式 | 实现复杂度 |
|------|--------|------------|----------|------------|
| localStorage | 高 | 否 | 可能不可用 | 低 |
| sessionStorage | 低 | 否 | 可能不可用 | 低 |
| Cookie | 可配置 | 是 | 可能不可用 | 低 |

---

## 4. 方案三：轻量级用户隔离方案

### 4.1 Token-based 匿名用户方案

| 项目 | 说明 |
|------|------|
| **实现原理** | 首次访问由服务端签发匿名 JWT，客户端存储并随请求携带 |
| **特点** | 服务端可控、可设置过期、可迁移到正式账号 |
| **适用** | 需要服务端参与、有会话管理的场景 |

### 4.2 UUID 客户端生成方案

| 项目 | 说明 |
|------|------|
| **实现原理** | 客户端用 `crypto.randomUUID()` 生成 UUID，存入 localStorage |
| **特点** | 纯前端、无服务端状态、实现极简 |
| **持久性** | 依赖 localStorage，清除即失效 |
| **最佳实践** | 匿名用户用 UUID；登录后切换为真实用户 ID |

```javascript
// 推荐实现
const ANON_ID_KEY = 'imgvault_anon_id';
export function getAnonymousId() {
  let id = localStorage.getItem(ANON_ID_KEY);
  if (!id) {
    id = crypto.randomUUID();
    try {
      localStorage.setItem(ANON_ID_KEY, id);
    } catch (e) {
      // 隐私模式可能失败，使用 sessionStorage 降级
      id = sessionStorage.getItem(ANON_ID_KEY) || id;
      sessionStorage.setItem(ANON_ID_KEY, id);
    }
  }
  return id;
}
```

### 4.3 方案对比

| 方案 | 实现复杂度 | 持久性 | 服务端依赖 | 隐私友好 |
|------|------------|--------|------------|----------|
| Token-based | 中 | 可配置 | 是 | 中 |
| UUID + localStorage | 低 | 高（未清除时） | 否 | 高 |

---

## 5. FingerprintJS 开源版 vs 商业版对比

### 5.1 核心差异

| 维度 | FingerprintJS 开源版 | Fingerprint Pro |
|------|----------------------|-----------------|
| **许可** | MIT (v5.0+) | 商业 SaaS |
| **架构** | 纯客户端 | 客户端 + 服务端 + ML |
| **准确率** | 低~中，约半数回访可能无法识别 | 高，宣称 99.5%+ |
| **稳定性** | 数周 | 数月~数年 |
| **识别方式** | 确定性（Cookie）+ 概率性（屏幕、时区等） | 100+ 信号 + 机器学习 + 模糊匹配 |
| **隐私模式** | 支持 | 支持 |
| **同型号设备** | 难以区分 | 可区分 |
| **防篡改/反欺诈** | 无 | 有（Smart Signals） |

### 5.2 适用场景

| 场景 | 推荐 |
|------|------|
| PoC、内部工具、小规模应用 | 开源版 |
| 防欺诈、高准确率、大规模生产 | Pro |
| 成本敏感、合规要求高 | 开源版 + UUID 混合 |

---

## 6. 综合对比与推荐

### 6.1 方案对比总表

| 方案 | 唯一性 | 持久性 | 实现复杂度 | 隐私合规 | 成本 |
|------|--------|--------|------------|----------|------|
| 浏览器指纹 (FingerprintJS) | 中 | 低~中 | 低 | 需注意 | 免费 |
| UUID + localStorage | 高（单设备） | 高 | 极低 | 友好 | 免费 |
| UUID + Cookie | 高（单设备） | 高 | 低 | 需注意 | 免费 |
| Token-based 匿名 | 高 | 可配置 | 中 | 中 | 免费 |
| Fingerprint Pro | 高 | 高 | 低 | 需注意 | 付费 |

### 6.2 推荐方案

#### 主推：UUID + localStorage（分层降级）

**理由**：

1. **实现简单**：纯前端，无服务端改造，集成快
2. **可靠性好**：单设备内唯一性高，持久性依赖用户是否清除
3. **隐私友好**：用户可清除，不涉及指纹，合规压力小
4. **成本为零**：无第三方依赖和费用

**实现建议**：

```
首次访问 → 生成 UUID → 写入 localStorage
         → 若失败（隐私模式）→ 降级到 sessionStorage
后续访问 → 读取已有 ID
登录后   → 可关联匿名 ID 与真实用户，实现数据迁移
```

#### 备选：UUID + FingerprintJS 混合

适用于需要更高识别率、且能接受指纹合规风险的场景：

- 以 UUID 为主标识
- FingerprintJS 作为辅助，用于检测同一设备多浏览器/多清除场景
- 需在隐私政策中说明指纹使用目的，必要时获取同意

#### 不推荐

- **MAC 地址**：Web 无法获取
- **纯 sessionStorage**：无法跨会话识别
- **纯 FingerprintJS 开源版**：准确率有限，不适合作为唯一依赖

---

## 7. ImgVault 落地建议

### 7.1 推荐实现

```javascript
// 前端：匿名用户 ID 获取
const STORAGE_KEY = 'imgvault_visitor_id';

export function getVisitorId() {
  try {
    let id = localStorage.getItem(STORAGE_KEY);
    if (!id) {
      id = crypto.randomUUID();
      localStorage.setItem(STORAGE_KEY, id);
    }
    return id;
  } catch {
    let id = sessionStorage.getItem(STORAGE_KEY);
    if (!id) {
      id = crypto.randomUUID();
      sessionStorage.setItem(STORAGE_KEY, id);
    }
    return id;
  }
}
```

### 7.2 API 传递方式

- **Header**：`X-Visitor-Id: {uuid}`
- **Query**：`?visitorId={uuid}`（不推荐，易泄露）
- **Cookie**：由服务端 Set-Cookie，后续自动携带

### 7.3 与登录的衔接

- 匿名上传/相册与 `visitorId` 关联
- 用户登录后，将匿名数据迁移到 `userId`，并可选清理 `visitorId` 关联

---

**文档版本**: 1.0  
**创建时间**: 2026-02-16  
**维护者**: ImgVault Team
