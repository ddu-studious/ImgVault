/**
 * ImgVault Admin - 管理后台
 */
const BASE = '/imgvault/api/v1';
const PAGE_SIZE = 20;

function proxyImageUrl(url) {
    if (!url) return '';
    const c = url.split('?')[0];
    if (c.includes('/imgvault/storage/') || c.includes('/imgvault/imgproxy/')) return c;
    const m1 = c.match(/https?:\/\/[^/]+:8081\/(.+)/);
    if (m1) return '/imgvault/imgproxy/' + m1[1];
    const m2 = c.match(/https?:\/\/[^/]+\/imgvault\/(.+)/);
    if (m2) return '/imgvault/storage/' + m2[1];
    return url;
}

// ==================== State ====================
const state = { section: 'dashboard', imgPage: 1, trashPage: 1, selected: new Set() };

// ==================== Token ====================
function getToken() { return localStorage.getItem('imgvault-admin-token'); }
function setToken(t) { localStorage.setItem('imgvault-admin-token', t); }
function clearToken() { localStorage.removeItem('imgvault-admin-token'); }

// ==================== API ====================
async function api(path, opts = {}) {
    const tk = getToken();
    const h = { ...opts.headers };
    if (tk) h['Authorization'] = 'Bearer ' + tk;
    if (opts.body && typeof opts.body === 'string') h['Content-Type'] = 'application/json';
    try {
        const r = await fetch(BASE + path, { ...opts, headers: h });
        if (r.status === 401 && !path.includes('/admin/login')) { clearToken(); showLogin(); toast('登录已过期', 'error'); return null; }
        return await r.json();
    } catch (e) { toast('网络请求失败', 'error'); return null; }
}

// ==================== Theme ====================
function initTheme() {
    const s = localStorage.getItem('imgvault-theme') || 'system';
    applyTheme(s);
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if ((localStorage.getItem('imgvault-theme') || 'system') === 'system') applyTheme('system');
    });
}
function applyTheme(m) {
    const dark = m === 'system' ? window.matchMedia('(prefers-color-scheme: dark)').matches : m === 'dark';
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    const b = document.getElementById('themeToggle');
    if (b) { b.textContent = { light:'☀️', dark:'🌙', system:'💻' }[m] || '💻'; b.title = { light:'浅色模式', dark:'深色模式', system:'跟随系统' }[m] || '跟随系统'; }
}
function toggleTheme() {
    const ms = ['light','dark','system'], c = localStorage.getItem('imgvault-theme') || 'system';
    const n = ms[(ms.indexOf(c)+1)%ms.length];
    localStorage.setItem('imgvault-theme', n); applyTheme(n);
}

// ==================== Auth ====================
function showLogin() { document.getElementById('loginPage').style.display='flex'; document.getElementById('adminApp').style.display='none'; }
function showAdmin() { document.getElementById('loginPage').style.display='none'; document.getElementById('adminApp').style.display=''; navigate('dashboard'); }
function logout() { clearToken(); showLogin(); toast('已退出', 'info'); }

async function handleLogin() {
    const pwd = document.getElementById('loginPassword').value;
    if (!pwd) { toast('请输入密码', 'error'); return; }
    const btn = document.getElementById('loginBtn');
    btn.disabled = true; btn.textContent = '登录中...';
    try {
        const r = await fetch(BASE + '/admin/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({password:pwd}) });
        const res = await r.json();
        if (res.code === 200 && res.data?.token) { setToken(res.data.token); showAdmin(); toast('登录成功','success'); }
        else { toast(res.message||'密码错误','error'); document.getElementById('loginPassword').value=''; document.getElementById('loginPassword').focus(); }
    } catch(e) { toast('网络错误','error'); }
    finally { btn.disabled=false; btn.textContent='登录'; }
}

// ==================== Router ====================
function navigate(s) {
    state.section = s;
    document.querySelectorAll('.nav-item').forEach(el => el.classList.toggle('active', el.dataset.s === s));
    const C = document.getElementById('mainContent');
    C.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
    switch(s) {
        case 'dashboard': loadDashboard(C); break;
        case 'images': loadImages(C); break;
        case 'trash': loadTrash(C); break;
        case 'tags': loadTags(C); break;
        case 'albums': loadAlbums(C); break;
        case 'logs': loadLogs(C); break;
        case 'system': loadSystem(C); break;
    }
}

// ==================== Dashboard ====================
async function loadDashboard(C) {
    const res = await api('/admin/stats');
    if (!res || res.code !== 200) { C.innerHTML = '<div class="empty"><div class="empty-icon">😕</div><div class="empty-text">加载失败</div></div>'; return; }
    const s = res.data;
    const fmtHtml = Object.entries(s.formatDistribution||{}).map(([k,v]) => {
        const pct = s.totalImages ? Math.round(v/s.totalImages*100) : 0;
        return `<div class="fmt-row"><span class="fmt-label">${k.toUpperCase()}</span><div class="fmt-bar"><div class="fmt-bar-fill" style="width:${pct}%"></div></div><span class="fmt-count">${v}</span></div>`;
    }).join('') || '<div style="color:var(--muted);font-size:12px">暂无数据</div>';
    const tasks = s.asyncTasks || {};
    C.innerHTML = `
        <div class="page-hdr"><h1>Dashboard</h1><div class="page-hdr-actions"><button class="btn btn-outline btn-sm" onclick="navigate('dashboard')">🔄 刷新</button></div></div>
        <div class="stats-grid">
            <div class="stat-card"><div class="stat-icon">🖼</div><div class="stat-value">${s.totalImages||0}</div><div class="stat-label">图片总数</div></div>
            <div class="stat-card"><div class="stat-icon">💾</div><div class="stat-value">${fmtSize(s.totalStorage||0)}</div><div class="stat-label">存储空间</div></div>
            <div class="stat-card"><div class="stat-icon">📤</div><div class="stat-value">${s.todayUploads||0}</div><div class="stat-label">今日上传</div></div>
            <div class="stat-card"><div class="stat-icon">🗑</div><div class="stat-value">${s.deletedImages||0}</div><div class="stat-label">已删除</div></div>
        </div>
        <div class="section-grid">
            <div class="section-card"><h3>格式分布</h3>${fmtHtml}</div>
            <div class="section-card"><h3>异步任务</h3>
                <div class="detail-grid">
                    <div class="lbl">待处理</div><div class="val">${tasks.pending||0}</div>
                    <div class="lbl">处理中</div><div class="val">${tasks.processing||0}</div>
                    <div class="lbl">已完成</div><div class="val">${tasks.success||0}</div>
                    <div class="lbl">失败</div><div class="val">${tasks.failed||0}</div>
                </div>
            </div>
        </div>`;
}

// ==================== Images ====================
async function loadImages(C, page) {
    page = page || state.imgPage || 1;
    state.imgPage = page; state.selected.clear();
    const res = await api(`/admin/images?page=${page}&size=${PAGE_SIZE}&status=1`);
    if (!res || res.code !== 200) { C.innerHTML = '<div class="empty"><div class="empty-icon">😕</div><div class="empty-text">加载失败</div></div>'; return; }
    const d = res.data;
    C.innerHTML = `
        <div class="page-hdr"><h1>图片管理</h1><div class="page-hdr-actions">
            <button class="btn btn-danger btn-sm" onclick="batchDeleteImages()">🗑 批量删除</button>
        </div></div>
        <div class="table-wrap"><table>
            <thead><tr><th style="width:30px"><input type="checkbox" onchange="toggleAllImg(this.checked)"></th><th></th><th>文件名</th><th>格式</th><th>大小</th><th>尺寸</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>${(d.records||[]).map(img => imgRow(img)).join('')}</tbody>
        </table></div>
        <div class="pager">${renderPager(d.total, d.pages, page, 'imgPage')}</div>`;
}
function imgRow(img) {
    const thumb = proxyImageUrl((img.thumbnails?.medium)||img.downloadUrl);
    return `<tr>
        <td><input type="checkbox" value="${img.id}" onchange="toggleSel(${img.id},this.checked)"></td>
        <td>${thumb?`<img class="thumb" src="${thumb}" alt="">`:'<div class="thumb" style="display:flex;align-items:center;justify-content:center;color:var(--muted)">🖼</div>'}</td>
        <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${img.originalName||''}">${img.originalName||'-'}</td>
        <td><span class="status status-normal">${(img.format||'?').toUpperCase()}</span></td>
        <td>${fmtSize(img.fileSize)}</td>
        <td style="white-space:nowrap">${img.width&&img.height?img.width+'×'+img.height:'-'}</td>
        <td style="white-space:nowrap;color:var(--muted)">${img.createdAt||'-'}</td>
        <td><div class="action-btns"><button class="action-btn" onclick="viewImage(${img.id})">查看</button><button class="action-btn danger" onclick="deleteImg(${img.id})">删除</button></div></td>
    </tr>`;
}
function toggleSel(id, checked) { checked ? state.selected.add(id) : state.selected.delete(id); }
function toggleAllImg(checked) { document.querySelectorAll('tbody input[type=checkbox]').forEach(cb => { cb.checked=checked; toggleSel(+cb.value, checked); }); }

async function deleteImg(id) { if (!confirm('确定删除此图片？')) return; await api(`/images/${id}`,{method:'DELETE'}); toast('已删除','success'); loadImages(document.getElementById('mainContent')); }
async function batchDeleteImages() {
    if (state.selected.size===0) { toast('请先选择图片','error'); return; }
    if (!confirm(`确定删除 ${state.selected.size} 张图片？`)) return;
    await api('/admin/batch-delete',{method:'POST',body:JSON.stringify([...state.selected])});
    toast('批量删除完成','success'); state.selected.clear(); loadImages(document.getElementById('mainContent'));
}
async function viewImage(id) {
    const res = await api(`/images/${id}`);
    if (!res || res.code!==200) { toast('加载失败','error'); return; }
    const img = res.data, url = proxyImageUrl(img.downloadUrl);
    let tagHtml = '';
    try { const tr = await api(`/tags/images/${id}/tags`); if(tr?.code===200) tagHtml = (tr.data||[]).map(t=>`<span class="status status-normal">${t.name}</span>`).join(' '); } catch(e){}
    document.getElementById('modalTitle').textContent = '图片详情';
    document.getElementById('modalBody').innerHTML = `
        ${url?`<img class="detail-img" src="${url}">`:''}
        <div class="detail-grid">
            <div class="lbl">文件名</div><div class="val">${img.originalName||'-'}</div>
            <div class="lbl">格式</div><div class="val">${(img.format||'-').toUpperCase()}</div>
            <div class="lbl">尺寸</div><div class="val">${img.width&&img.height?img.width+'×'+img.height:'-'}</div>
            <div class="lbl">大小</div><div class="val">${fmtSize(img.fileSize)}</div>
            <div class="lbl">UUID</div><div class="val">${img.imageUuid||'-'}</div>
            <div class="lbl">创建时间</div><div class="val">${img.createdAt||'-'}</div>
            <div class="lbl">标签</div><div class="val">${tagHtml||'<span style="color:var(--muted)">无</span>'}</div>
        </div>`;
    openModal();
}

// ==================== Trash ====================
async function loadTrash(C, page) {
    page = page || state.trashPage || 1; state.trashPage = page;
    const res = await api(`/admin/trash?page=${page}&size=${PAGE_SIZE}`);
    if (!res || res.code !== 200) { C.innerHTML = '<div class="empty"><div class="empty-icon">😕</div><div class="empty-text">加载失败</div></div>'; return; }
    const d = res.data;
    C.innerHTML = `
        <div class="page-hdr"><h1>回收站</h1></div>
        ${(d.records||[]).length===0 ? '<div class="empty"><div class="empty-icon">🗑</div><div class="empty-text">回收站为空</div></div>' : `
        <div class="table-wrap"><table>
            <thead><tr><th></th><th>文件名</th><th>格式</th><th>大小</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>${(d.records||[]).map(img => {
                const thumb = proxyImageUrl((img.thumbnails?.medium)||img.downloadUrl);
                return `<tr>
                    <td>${thumb?`<img class="thumb" src="${thumb}">`:'🖼'}</td>
                    <td>${img.originalName||'-'}</td>
                    <td>${(img.format||'?').toUpperCase()}</td>
                    <td>${fmtSize(img.fileSize)}</td>
                    <td style="color:var(--muted)">${img.createdAt||'-'}</td>
                    <td><div class="action-btns"><button class="action-btn" onclick="restoreImg(${img.id})">恢复</button><button class="action-btn danger" onclick="permDeleteImg(${img.id})">永久删除</button></div></td>
                </tr>`; }).join('')}</tbody>
        </table></div>
        <div class="pager">${renderPager(d.total, d.pages, page, 'trashPage')}</div>`}`;
}
async function restoreImg(id) { await api(`/admin/trash/${id}/restore`,{method:'POST'}); toast('已恢复','success'); loadTrash(document.getElementById('mainContent')); }
async function permDeleteImg(id) { if(!confirm('永久删除后不可恢复，确定？'))return; await api(`/images/${id}/permanent`,{method:'DELETE'}); toast('已永久删除','success'); loadTrash(document.getElementById('mainContent')); }

// ==================== Tags ====================
async function loadTags(C) {
    const res = await api('/tags');
    if (!res || res.code !== 200) { C.innerHTML = '<div class="empty"><div class="empty-icon">😕</div><div class="empty-text">加载失败</div></div>'; return; }
    const tags = res.data || [];
    C.innerHTML = `
        <div class="page-hdr"><h1>标签管理</h1><div class="page-hdr-actions"><button class="btn btn-primary btn-sm" onclick="createTag()">+ 新建标签</button></div></div>
        ${tags.length===0?'<div class="empty"><div class="empty-icon">🏷</div><div class="empty-text">暂无标签</div></div>':`
        <div class="table-wrap"><table>
            <thead><tr><th>ID</th><th>名称</th><th>图片数</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>${tags.map(t=>`<tr>
                <td>${t.id}</td><td><strong>${t.name}</strong></td><td>${t.imageCount||0}</td><td style="color:var(--muted)">${t.createdAt||'-'}</td>
                <td><div class="action-btns"><button class="action-btn" onclick="editTag(${t.id},'${t.name}')">编辑</button><button class="action-btn danger" onclick="deleteTag(${t.id},'${t.name}')">删除</button></div></td>
            </tr>`).join('')}</tbody>
        </table></div>`}`;
}
async function createTag() { const n=prompt('输入标签名称:'); if(!n)return; const r=await api('/tags',{method:'POST',body:JSON.stringify({name:n.trim()})}); if(r?.code===200){toast('标签已创建','success');loadTags(document.getElementById('mainContent'));}else toast(r?.message||'创建失败','error'); }
async function editTag(id,old) { const n=prompt('修改标签名称:',old); if(!n||n===old)return; const r=await api(`/tags/${id}`,{method:'PUT',body:JSON.stringify({name:n.trim()})}); if(r?.code===200){toast('已更新','success');loadTags(document.getElementById('mainContent'));}else toast(r?.message||'更新失败','error'); }
async function deleteTag(id,name) { if(!confirm(`删除标签"${name}"？`))return; const r=await api(`/tags/${id}`,{method:'DELETE'}); if(r?.code===200){toast('已删除','success');loadTags(document.getElementById('mainContent'));}else toast(r?.message||'删除失败','error'); }

// ==================== Albums ====================
async function loadAlbums(C) {
    const res = await api('/albums?page=1&size=100');
    if (!res || res.code !== 200) { C.innerHTML = '<div class="empty"><div class="empty-icon">😕</div><div class="empty-text">加载失败</div></div>'; return; }
    const albums = res.data?.records || [];
    C.innerHTML = `
        <div class="page-hdr"><h1>相册管理</h1><div class="page-hdr-actions"><button class="btn btn-primary btn-sm" onclick="createAlbum()">+ 新建相册</button></div></div>
        ${albums.length===0?'<div class="empty"><div class="empty-icon">📁</div><div class="empty-text">暂无相册</div></div>':`
        <div class="table-wrap"><table>
            <thead><tr><th>ID</th><th>名称</th><th>描述</th><th>图片数</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>${albums.map(a=>`<tr>
                <td>${a.id}</td><td><strong>${a.name}</strong></td><td style="color:var(--muted);max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${a.description||'-'}</td><td>${a.imageCount||0}</td><td style="color:var(--muted)">${a.createdAt||'-'}</td>
                <td><div class="action-btns"><button class="action-btn" onclick="editAlbum(${a.id},'${a.name}','${(a.description||'').replace(/'/g,"\\'")}')">编辑</button><button class="action-btn danger" onclick="deleteAlbum(${a.id},'${a.name}')">删除</button></div></td>
            </tr>`).join('')}</tbody>
        </table></div>`}`;
}
async function createAlbum() { const n=prompt('相册名称:'); if(!n)return; const d=prompt('相册描述（可选）:')||''; const r=await api('/albums',{method:'POST',body:JSON.stringify({name:n.trim(),description:d})}); if(r?.code===200){toast('相册已创建','success');loadAlbums(document.getElementById('mainContent'));}else toast(r?.message||'创建失败','error'); }
async function editAlbum(id,oldName,oldDesc) { const n=prompt('修改相册名称:',oldName); if(!n)return; const d=prompt('修改描述:',oldDesc)||''; const r=await api(`/albums/${id}`,{method:'PUT',body:JSON.stringify({name:n.trim(),description:d})}); if(r?.code===200){toast('已更新','success');loadAlbums(document.getElementById('mainContent'));}else toast(r?.message||'更新失败','error'); }
async function deleteAlbum(id,name) { if(!confirm(`删除相册"${name}"？`))return; const r=await api(`/albums/${id}`,{method:'DELETE'}); if(r?.code===200){toast('已删除','success');loadAlbums(document.getElementById('mainContent'));}else toast(r?.message||'删除失败','error'); }

// ==================== Logs ====================
async function loadLogs(C) {
    const res = await api('/admin/logs?limit=50');
    if (!res || res.code !== 200) { C.innerHTML = '<div class="empty"><div class="empty-icon">😕</div><div class="empty-text">加载失败</div></div>'; return; }
    const logs = res.data || [];
    C.innerHTML = `
        <div class="page-hdr"><h1>操作日志</h1><div class="page-hdr-actions"><button class="btn btn-outline btn-sm" onclick="loadLogs(document.getElementById('mainContent'))">🔄 刷新</button></div></div>
        ${logs.length===0?'<div class="empty"><div class="empty-icon">📋</div><div class="empty-text">暂无日志</div></div>':`
        <div class="table-wrap"><table>
            <thead><tr><th>时间</th><th>操作</th><th>目标类型</th><th>目标ID</th><th>详情</th></tr></thead>
            <tbody>${logs.map(l=>`<tr>
                <td style="white-space:nowrap;color:var(--muted)">${l.createdAt||'-'}</td>
                <td><strong>${l.operationType||'-'}</strong></td>
                <td>${l.targetType||'-'}</td>
                <td>${l.targetId||'-'}</td>
                <td style="max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--muted)">${l.details||'-'}</td>
            </tr>`).join('')}</tbody>
        </table></div>`}`;
}

// ==================== System ====================
async function loadSystem(C) {
    let healthHtml = '<span style="color:var(--muted)">获取中...</span>';
    try {
        const r = await fetch('/imgvault/actuator/health');
        const h = await r.json();
        const st = h.status === 'UP';
        healthHtml = `<span class="status ${st?'status-normal':'status-deleted'}">${h.status}</span>`;
        if (h.components) {
            healthHtml += '<div style="margin-top:12px">';
            for (const [k,v] of Object.entries(h.components)) {
                const up = v.status === 'UP';
                healthHtml += `<div style="display:flex;justify-content:space-between;padding:4px 0;font-size:12px"><span>${k}</span><span class="status ${up?'status-normal':'status-deleted'}">${v.status}</span></div>`;
            }
            healthHtml += '</div>';
        }
    } catch(e) { healthHtml = '<span class="status status-deleted">不可达</span>'; }

    let cacheHtml = '';
    try {
        const r = await fetch('/imgvault/actuator/caches');
        const c = await r.json();
        if (c.cacheManagers) {
            for (const [mgr, data] of Object.entries(c.cacheManagers)) {
                for (const [name] of Object.entries(data.caches||{})) {
                    cacheHtml += `<div style="display:flex;justify-content:space-between;padding:4px 0;font-size:12px"><span>${name}</span><span class="status status-normal">Active</span></div>`;
                }
            }
        }
    } catch(e) { cacheHtml = '<span style="color:var(--muted);font-size:12px">不可用</span>'; }

    C.innerHTML = `
        <div class="page-hdr"><h1>系统监控</h1><div class="page-hdr-actions"><button class="btn btn-outline btn-sm" onclick="loadSystem(document.getElementById('mainContent'))">🔄 刷新</button></div></div>
        <div class="section-grid">
            <div class="section-card"><h3>健康状态</h3>${healthHtml}</div>
            <div class="section-card"><h3>缓存</h3>${cacheHtml||'<span style="color:var(--muted);font-size:12px">无缓存</span>'}</div>
        </div>
        <div class="section-grid">
            <div class="section-card"><h3>服务信息</h3>
                <div class="detail-grid">
                    <div class="lbl">服务</div><div class="val">ImgVault API</div>
                    <div class="lbl">版本</div><div class="val">v2.1.0</div>
                    <div class="lbl">端口</div><div class="val">8080</div>
                </div>
            </div>
        </div>`;
}

// ==================== Pagination Helper ====================
function renderPager(total, pages, current, stateKey) {
    if (pages <= 1) return '';
    let h = `<button class="pg-btn" onclick="gotoPage('${stateKey}',${current-1})" ${current<=1?'disabled':''}>‹</button>`;
    const s = Math.max(1, current-2), e = Math.min(pages, current+2);
    if (s>1) h+=`<button class="pg-btn" onclick="gotoPage('${stateKey}',1)">1</button>`;
    if (s>2) h+=`<span class="pg-info">...</span>`;
    for (let i=s;i<=e;i++) h+=`<button class="pg-btn ${i===current?'active':''}" onclick="gotoPage('${stateKey}',${i})">${i}</button>`;
    if (e<pages-1) h+=`<span class="pg-info">...</span>`;
    if (e<pages) h+=`<button class="pg-btn" onclick="gotoPage('${stateKey}',${pages})">${pages}</button>`;
    h += `<button class="pg-btn" onclick="gotoPage('${stateKey}',${current+1})" ${current>=pages?'disabled':''}>›</button>`;
    h += `<span class="pg-info">${current}/${pages} · 共${total}条</span>`;
    return h;
}
function gotoPage(key, page) {
    const C = document.getElementById('mainContent');
    if (key === 'imgPage') { state.imgPage = page; loadImages(C, page); }
    else if (key === 'trashPage') { state.trashPage = page; loadTrash(C, page); }
}

// ==================== Modal ====================
function openModal() { document.getElementById('modal').classList.add('active'); }
function closeModal() { document.getElementById('modal').classList.remove('active'); }

// ==================== Utils ====================
function fmtSize(b) { if(!b)return'0 B'; if(b<1024)return b+' B'; if(b<1048576)return(b/1024).toFixed(1)+' KB'; if(b<1073741824)return(b/1048576).toFixed(1)+' MB'; return(b/1073741824).toFixed(2)+' GB'; }
function toast(msg,type='info') {
    const c=document.getElementById('toastContainer'); if(!c)return;
    const el=document.createElement('div'); el.className=`toast ${type}`;
    el.innerHTML=`<span>${{success:'✅',error:'❌',info:'ℹ️'}[type]||'ℹ️'}</span> ${msg}`;
    c.appendChild(el); setTimeout(()=>{el.style.opacity='0';setTimeout(()=>el.remove(),300)},3000);
}

// ==================== Init ====================
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    const tk = getToken();
    if (tk) {
        api('/admin/stats').then(r => { if (r && r.code === 200) showAdmin(); else showLogin(); });
    } else { showLogin(); }
});
