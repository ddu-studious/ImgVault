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
    const img = res.data;
    const originalUrl = proxyImageUrl(img.downloadUrl);
    const thumbSmall = img.thumbnails?.small ? proxyImageUrl(img.thumbnails.small) : null;
    const thumbMedium = img.thumbnails?.medium ? proxyImageUrl(img.thumbnails.medium) : null;
    const thumbLarge = img.thumbnails?.large ? proxyImageUrl(img.thumbnails.large) : null;

    let tagHtml = '';
    try { const tr = await api(`/tags/images/${id}/tags`); if(tr?.code===200) tagHtml = (tr.data||[]).map(t=>`<span class="status status-normal">${t.name}</span>`).join(' '); } catch(e){}

    const sizePresets = buildAdminSizePresets(img.width, img.height);
    const formatOptions = buildAdminFormatOptions(img.format);

    document.getElementById('modalTitle').textContent = '图片详情';
    document.getElementById('modalBody').innerHTML = `
        <div class="admin-detail-layout">
            <div class="admin-detail-preview">
                <div class="admin-preview-main" id="adminPreviewMain">
                    ${originalUrl ? `<img id="adminPreviewImg" class="admin-preview-img" src="${thumbLarge || thumbMedium || originalUrl}" alt="${img.originalName||''}" onclick="adminToggleZoom(this)">` : '<div style="padding:40px;color:var(--muted);text-align:center">无法加载图片</div>'}
                </div>
                <div class="admin-preview-sizes">
                    <span class="admin-size-label">查看尺寸:</span>
                    ${thumbSmall ? `<button class="admin-size-btn" onclick="adminSwitchPreview('${thumbSmall}', this)" title="小图 150×150">S</button>` : ''}
                    ${thumbMedium ? `<button class="admin-size-btn" onclick="adminSwitchPreview('${thumbMedium}', this)" title="中图 800×600">M</button>` : ''}
                    ${thumbLarge ? `<button class="admin-size-btn active" onclick="adminSwitchPreview('${thumbLarge}', this)" title="大图 1920×1080">L</button>` : ''}
                    <button class="admin-size-btn${!thumbLarge ? ' active' : ''}" onclick="adminSwitchPreview('${originalUrl}', this)" title="原图 ${img.width||'?'}×${img.height||'?'}">原图</button>
                    <button class="admin-size-btn" onclick="adminOpenFullscreen('${originalUrl}')" title="全屏查看">⛶</button>
                </div>
            </div>
            <div class="admin-detail-info">
                <div class="admin-info-section">
                    <h4>文件信息</h4>
                    <div class="detail-grid">
                        <div class="lbl">文件名</div><div class="val" title="${img.originalName||''}">${img.originalName||'-'}</div>
                        <div class="lbl">格式</div><div class="val">${(img.format||'-').toUpperCase()}</div>
                        <div class="lbl">尺寸</div><div class="val">${img.width&&img.height?img.width+'×'+img.height:'-'}</div>
                        <div class="lbl">大小</div><div class="val">${fmtSize(img.fileSize)}</div>
                        <div class="lbl">MIME</div><div class="val">${img.mimeType||'-'}</div>
                        <div class="lbl">UUID</div><div class="val" style="font-size:11px;word-break:break-all">${img.imageUuid||'-'}</div>
                        <div class="lbl">创建时间</div><div class="val">${img.createdAt||'-'}</div>
                        <div class="lbl">标签</div><div class="val">${tagHtml||'<span style="color:var(--muted)">无</span>'}</div>
                    </div>
                </div>
                <div class="admin-info-section">
                    <h4>多规格下载</h4>
                    <div class="admin-dl-group">
                        <div class="admin-dl-label">尺寸</div>
                        <div class="admin-dl-chips" id="adminDlSizeChips">
                            ${sizePresets.map((p, i) => `<span class="admin-dl-chip${i===0?' active':''}" data-w="${p.w}" data-h="${p.h}" onclick="adminSelectDlChip(this)">${p.label}</span>`).join('')}
                        </div>
                    </div>
                    <div class="admin-dl-group">
                        <div class="admin-dl-label">格式</div>
                        <div class="admin-dl-chips" id="adminDlFmtChips">
                            ${formatOptions.map((f, i) => `<span class="admin-dl-chip${i===0?' active':''}" data-fmt="${f.value}" onclick="adminSelectDlChip(this)">${f.label}</span>`).join('')}
                        </div>
                    </div>
                    <div class="admin-dl-group">
                        <div class="admin-dl-label">自定义</div>
                        <div style="display:flex;gap:6px;align-items:center">
                            <input type="number" id="adminDlW" placeholder="宽" style="width:70px;padding:4px 6px;border:1px solid var(--border);border-radius:4px;font-size:12px" min="1" max="10000">
                            <span style="color:var(--muted)">×</span>
                            <input type="number" id="adminDlH" placeholder="高" style="width:70px;padding:4px 6px;border:1px solid var(--border);border-radius:4px;font-size:12px" min="1" max="10000">
                            <button class="btn btn-outline btn-sm" style="font-size:11px;padding:3px 8px" onclick="adminApplyCustomSize()">应用</button>
                        </div>
                    </div>
                    <div style="display:flex;gap:6px;margin-top:10px">
                        <button class="btn btn-primary btn-sm" onclick="adminDoDownload(${img.id}, '${(img.originalName||'image').replace(/'/g,"\\'")}')">⬇ 下载</button>
                        <button class="btn btn-outline btn-sm" onclick="adminCopyLink(${img.id})">🔗 复制链接</button>
                        <a class="btn btn-outline btn-sm" href="${originalUrl}" target="_blank" download>⬇ 原图</a>
                    </div>
                </div>
            </div>
        </div>`;
    openModal();
}

function buildAdminSizePresets(origW, origH) {
    const presets = [{ label: '原始', w: 0, h: 0 }];
    const sizes = [
        { label: '大 1920', w: 1920, h: 1080 },
        { label: '中 1280', w: 1280, h: 720 },
        { label: '小 800', w: 800, h: 600 },
        { label: '缩略 400', w: 400, h: 300 },
        { label: '图标 150', w: 150, h: 150 },
    ];
    for (const s of sizes) {
        if (origW && origH && (s.w < origW || s.h < origH)) presets.push(s);
    }
    if (presets.length === 1 && origW && origH) {
        presets.push({ label: '中 1280', w: 1280, h: 720 });
        presets.push({ label: '小 800', w: 800, h: 600 });
    }
    return presets;
}
function buildAdminFormatOptions(originalFormat) {
    const fmt = (originalFormat||'').toLowerCase();
    const opts = [{ label: '原格式', value: '' }];
    for (const f of [{ label: 'JPEG', value: 'jpeg' },{ label: 'PNG', value: 'png' },{ label: 'WebP', value: 'webp' },{ label: 'AVIF', value: 'avif' }]) {
        if (f.value !== fmt) opts.push(f);
    }
    return opts;
}
function adminSelectDlChip(el) {
    el.closest('.admin-dl-chips').querySelectorAll('.admin-dl-chip').forEach(c => c.classList.remove('active'));
    el.classList.add('active');
}
function adminApplyCustomSize() {
    const w = parseInt(document.getElementById('adminDlW')?.value) || 0;
    const h = parseInt(document.getElementById('adminDlH')?.value) || 0;
    if (w<=0 && h<=0) { toast('请输入有效的宽高','error'); return; }
    const chips = document.getElementById('adminDlSizeChips');
    chips.querySelectorAll('.admin-dl-chip').forEach(c => c.classList.remove('active'));
    let custom = chips.querySelector('[data-custom]');
    if (!custom) { custom = document.createElement('span'); custom.className='admin-dl-chip active'; custom.dataset.custom='1'; custom.onclick=function(){adminSelectDlChip(this)}; chips.appendChild(custom); }
    custom.className='admin-dl-chip active'; custom.dataset.w=w; custom.dataset.h=h;
    custom.textContent=`${w||'auto'}×${h||'auto'}`;
    toast('已应用自定义尺寸','success');
}
function adminGetDlParams() {
    const sc = document.querySelector('#adminDlSizeChips .admin-dl-chip.active');
    const fc = document.querySelector('#adminDlFmtChips .admin-dl-chip.active');
    return { width: sc ? parseInt(sc.dataset.w)||0 : 0, height: sc ? parseInt(sc.dataset.h)||0 : 0, format: fc ? fc.dataset.fmt||'' : '', quality: 85 };
}
async function adminDoDownload(imageId, originalName) {
    const p = adminGetDlParams();
    if (p.width===0 && p.height===0 && !p.format) {
        const res = await api(`/images/${imageId}`);
        if (res?.code===200) { const a=document.createElement('a'); a.href=proxyImageUrl(res.data.downloadUrl); a.download=originalName; a.click(); }
        return;
    }
    const qs = new URLSearchParams();
    if (p.width>0) qs.set('width',p.width);
    if (p.height>0) qs.set('height',p.height);
    if (p.format) qs.set('format',p.format);
    if (p.quality>0) qs.set('quality',p.quality);
    const res = await api(`/images/${imageId}/process-url?${qs.toString()}`);
    if (res?.code===200 && res.data) {
        const url = proxyImageUrl(res.data);
        const ext = p.format || originalName.split('.').pop() || 'jpg';
        const base = originalName.replace(/\.[^.]+$/,'');
        const suffix = (p.width||p.height) ? `_${p.width||'auto'}x${p.height||'auto'}` : '';
        const a=document.createElement('a'); a.href=url; a.download=`${base}${suffix}.${ext}`; a.target='_blank'; a.click();
        toast('开始下载','success');
    } else { toast('获取处理链接失败','error'); }
}
async function adminCopyLink(imageId) {
    const p = adminGetDlParams();
    if (p.width===0 && p.height===0 && !p.format) {
        const res = await api(`/images/${imageId}`);
        if (res?.code===200) { const u=proxyImageUrl(res.data.downloadUrl); const full=u.startsWith('http')?u:location.origin+u; navigator.clipboard.writeText(full).then(()=>toast('链接已复制','success')).catch(()=>toast('复制失败','error')); }
        return;
    }
    const qs = new URLSearchParams();
    if (p.width>0) qs.set('width',p.width);
    if (p.height>0) qs.set('height',p.height);
    if (p.format) qs.set('format',p.format);
    if (p.quality>0) qs.set('quality',p.quality);
    const res = await api(`/images/${imageId}/process-url?${qs.toString()}`);
    if (res?.code===200 && res.data) {
        const u=proxyImageUrl(res.data); const full=u.startsWith('http')?u:location.origin+u;
        navigator.clipboard.writeText(full).then(()=>toast('链接已复制','success')).catch(()=>toast('复制失败','error'));
    } else { toast('获取处理链接失败','error'); }
}
function adminSwitchPreview(url, btn) {
    const img = document.getElementById('adminPreviewImg');
    if (img) { img.src = url; img.classList.remove('zoomed'); }
    btn.closest('.admin-preview-sizes').querySelectorAll('.admin-size-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
}
function adminToggleZoom(img) {
    img.classList.toggle('zoomed');
}
function adminOpenFullscreen(url) {
    const overlay = document.createElement('div');
    overlay.className = 'admin-fullscreen-overlay';
    overlay.innerHTML = `
        <div class="admin-fullscreen-close" onclick="this.parentElement.remove()">✕</div>
        <img src="${url}" class="admin-fullscreen-img" onclick="event.stopPropagation()">
    `;
    overlay.addEventListener('click', () => overlay.remove());
    document.body.appendChild(overlay);
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
