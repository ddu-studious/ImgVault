/**
 * ImgVault Frontend - 图片存储管理前端
 * 纯 Vanilla JS 单页应用，对接 ImgVault REST API
 */

// ==================== 配置 ====================
const BASE = '/imgvault/api/v1';
const PAGE_SIZE = 20;

/**
 * 将 MinIO 直接 URL 转换为 nginx 代理 URL
 * http://localhost:9000/imgvault/originals/... → /imgvault/storage/originals/...
 */
function proxyImageUrl(url) {
    if (!url) return '';
    // 去掉查询参数（presigned URL 签名），bucket 已设为公开读取
    const cleanUrl = url.split('?')[0];
    // 匹配 imgproxy URL: http://localhost:8081/签名/参数/plain/s3://...
    // 需要提取 host:port 后面的全部路径
    const imgproxyMatch = cleanUrl.match(/https?:\/\/[^/]+:8081\/(.+)/);
    if (imgproxyMatch) return '/imgvault/imgproxy/' + imgproxyMatch[1];
    // 匹配 MinIO URL: http://localhost:9000/imgvault/path
    const minioMatch = cleanUrl.match(/https?:\/\/[^/]+\/imgvault\/(.+)/);
    if (minioMatch) return '/imgvault/storage/' + minioMatch[1];
    return url;
}

// ==================== 状态 ====================
const state = {
    currentView: 'images',     // images | albums | tags | trash
    images: [],
    tags: [],
    albums: [],
    currentPage: 1,
    totalPages: 0,
    totalCount: 0,
    selectedImages: new Set(),
    searchKeyword: '',
    currentAlbumId: null,
    currentTagId: null,
    stats: null,
};

// ==================== API 工具 ====================
async function api(path, opts = {}) {
    const url = BASE + path;
    try {
        const resp = await fetch(url, {
            headers: { 'Content-Type': 'application/json', ...opts.headers },
            ...opts,
        });
        if (opts.raw) return resp;
        const data = await resp.json();
        return data;
    } catch (err) {
        console.error('API Error:', path, err);
        toast('网络请求失败: ' + err.message, 'error');
        return null;
    }
}

// ==================== 初始化 ====================
document.addEventListener('DOMContentLoaded', () => {
    initUploadZone();
    initSearch();
    initSidebarNav();
    loadImages();
    loadTags();
    loadAlbums();
});

// ==================== 图片列表 ====================
async function loadImages(page = 1) {
    state.currentPage = page;
    const grid = document.getElementById('grid');
    grid.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

    let path = `/images?page=${page}&size=${PAGE_SIZE}`;
    if (state.searchKeyword) path += `&keyword=${encodeURIComponent(state.searchKeyword)}`;

    const res = await api(path);
    if (!res || res.code !== 200) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">😕</div><div class="empty-state-text">加载失败</div></div>';
        return;
    }

    const pageData = res.data;
    state.images = pageData.records || [];
    state.totalCount = pageData.total;
    state.totalPages = pageData.pages;

    updateContentHeader('全部图片', `${state.totalCount} 张图片`);
    renderGrid(state.images);
    renderPagination();
    updateSidebarCounts();
}

async function loadTrash(page = 1) {
    state.currentPage = page;
    const grid = document.getElementById('grid');
    grid.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

    const res = await api(`/admin/trash?page=${page}&size=${PAGE_SIZE}`);
    if (!res || res.code !== 200) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🗑️</div><div class="empty-state-text">回收站为空</div></div>';
        return;
    }

    const pageData = res.data;
    state.images = pageData.records || [];
    state.totalCount = pageData.total;
    state.totalPages = pageData.pages;

    updateContentHeader('回收站', `${state.totalCount} 张已删除图片`);
    renderGrid(state.images);
    renderPagination();
}

function renderGrid(images) {
    const grid = document.getElementById('grid');
    if (!images || images.length === 0) {
        const viewName = state.currentView === 'trash' ? '回收站' : '图片库';
        grid.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📷</div><div class="empty-state-text">${viewName}为空</div><div class="empty-state-hint">上传第一张图片开始使用</div></div>`;
        return;
    }

    grid.innerHTML = images.map(img => {
        // 优先使用缩略图(medium)，回退到原图
        const thumbSrc = (img.thumbnails && img.thumbnails.medium) || img.downloadUrl;
        const thumbUrl = proxyImageUrl(thumbSrc);
        const ext = (img.format || 'jpg').toUpperCase();
        const sizeStr = formatSize(img.fileSize);
        const dimStr = img.width && img.height ? `${img.width}x${img.height}` : '';
        const isSelected = state.selectedImages.has(img.id);

        return `
        <div class="card" data-id="${img.id}" onclick="openDetail(${img.id})">
            <div class="card-select ${isSelected ? 'selected' : ''}" onclick="event.stopPropagation(); toggleSelect(${img.id})">
                ${isSelected ? '✓' : ''}
            </div>
            ${thumbUrl
                ? `<img class="card-img" src="${thumbUrl}" alt="${img.originalName || ''}" loading="lazy" onerror="this.outerHTML='<div class=\\'card-img-placeholder\\'>🖼</div>'">`
                : `<div class="card-img-placeholder">🖼</div>`}
            <span class="card-badge">${ext}</span>
            <div class="card-body">
                <div class="card-name" title="${img.originalName || ''}">${img.originalName || 'untitled'}</div>
                <div class="card-meta"><span>${dimStr}</span><span>${sizeStr}</span></div>
            </div>
        </div>`;
    }).join('');
}

// ==================== 图片详情 ====================
async function openDetail(id) {
    const res = await api(`/images/${id}`);
    if (!res || res.code !== 200) {
        toast('无法加载图片详情', 'error');
        return;
    }
    const img = res.data;

    // 加载标签
    let tags = [];
    try {
        const tagRes = await api(`/tags/images/${id}/tags`);
        if (tagRes && tagRes.code === 200) tags = tagRes.data || [];
    } catch (e) { /* ignore */ }

    const modal = document.getElementById('detailModal');
    const imgUrl = proxyImageUrl(img.downloadUrl);
    const sizeStr = formatSize(img.fileSize);

    document.getElementById('detailContent').innerHTML = `
        <div class="detail-layout">
            <div class="detail-image-wrap">
                ${imgUrl ? `<img src="${imgUrl}" alt="${img.originalName}">` : '<div style="padding:40px;color:var(--text-muted)">无法加载图片</div>'}
            </div>
            <div class="detail-info">
                <div class="detail-section">
                    <div class="detail-section-title">文件信息</div>
                    <div class="detail-row"><span class="label">文件名</span><span class="value" title="${img.originalName}">${img.originalName || '-'}</span></div>
                    <div class="detail-row"><span class="label">格式</span><span class="value">${(img.format || '-').toUpperCase()}</span></div>
                    <div class="detail-row"><span class="label">尺寸</span><span class="value">${img.width && img.height ? img.width + 'x' + img.height : '-'}</span></div>
                    <div class="detail-row"><span class="label">大小</span><span class="value">${sizeStr}</span></div>
                    <div class="detail-row"><span class="label">UUID</span><span class="value" title="${img.imageUuid || ''}">${(img.imageUuid || '-').substring(0, 12)}...</span></div>
                    <div class="detail-row"><span class="label">创建时间</span><span class="value">${img.createdAt || '-'}</span></div>
                </div>

                <div class="detail-section">
                    <div class="detail-section-title">标签</div>
                    <div class="detail-tags" id="detailTags">
                        ${tags.map(t => `<span class="detail-tag">${t.name}</span>`).join('')}
                        <button class="btn btn-sm btn-outline" onclick="showAddTagDialog(${id})">+ 添加</button>
                    </div>
                </div>

                <div class="detail-actions">
                    <a class="btn btn-primary btn-sm" href="${imgUrl}" target="_blank" download>⬇ 下载</a>
                    <button class="btn btn-sm btn-outline" onclick="copyToClipboard('${imgUrl}')">🔗 复制链接</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteImage(${id})">🗑 删除</button>
                </div>
            </div>
        </div>`;

    modal.classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

// ==================== 上传 ====================
function initUploadZone() {
    const zone = document.getElementById('uploadZone');
    const input = document.getElementById('fileInput');
    if (!zone || !input) return;

    zone.addEventListener('click', () => input.click());
    zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('drag-over'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('drag-over');
        handleFiles(e.dataTransfer.files);
    });
    input.addEventListener('change', e => {
        handleFiles(e.target.files);
        input.value = '';
    });
}

async function handleFiles(files) {
    if (!files || files.length === 0) return;
    const progressContainer = document.getElementById('uploadProgress');

    for (const file of files) {
        const itemId = 'upload-' + Date.now() + Math.random().toString(36).substr(2, 4);
        progressContainer.innerHTML += `
            <div class="upload-item" id="${itemId}">
                <div class="upload-item-name">${file.name}</div>
                <div class="upload-item-size">${formatSize(file.size)}</div>
                <div class="progress-bar"><div class="progress-fill" style="width:0%"></div></div>
                <div class="upload-item-status uploading">上传中...</div>
            </div>`;

        try {
            const formData = new FormData();
            formData.append('file', file);

            const progressFill = document.querySelector(`#${itemId} .progress-fill`);
            const statusEl = document.querySelector(`#${itemId} .upload-item-status`);

            // 使用 XMLHttpRequest 获取上传进度
            const result = await new Promise((resolve, reject) => {
                const xhr = new XMLHttpRequest();
                xhr.open('POST', BASE + '/images/upload');
                xhr.upload.onprogress = (e) => {
                    if (e.lengthComputable) {
                        const pct = Math.round((e.loaded / e.total) * 100);
                        progressFill.style.width = pct + '%';
                    }
                };
                xhr.onload = () => {
                    try { resolve(JSON.parse(xhr.responseText)); }
                    catch { reject(new Error('Invalid response')); }
                };
                xhr.onerror = () => reject(new Error('Network error'));
                xhr.send(formData);
            });

            if (result && result.code === 200) {
                document.querySelector(`#${itemId} .progress-fill`).style.width = '100%';
                document.querySelector(`#${itemId} .upload-item-status`).textContent = '完成';
                document.querySelector(`#${itemId} .upload-item-status`).className = 'upload-item-status success';
                toast(`${file.name} 上传成功`, 'success');
            } else {
                document.querySelector(`#${itemId} .upload-item-status`).textContent = '失败';
                document.querySelector(`#${itemId} .upload-item-status`).className = 'upload-item-status error';
                toast(`${file.name} 上传失败: ${result?.message || '未知错误'}`, 'error');
            }
        } catch (err) {
            const el = document.getElementById(itemId);
            if (el) {
                el.querySelector('.upload-item-status').textContent = '失败';
                el.querySelector('.upload-item-status').className = 'upload-item-status error';
            }
            toast(`${file.name} 上传失败`, 'error');
        }
    }

    // 上传完成后刷新列表
    setTimeout(() => {
        if (state.currentView === 'images') loadImages(1);
        progressContainer.innerHTML = '';
    }, 2000);
}

// ==================== 删除 ====================
async function deleteImage(id) {
    if (!confirm('确定要删除这张图片吗？')) return;
    const res = await api(`/images/${id}`, { method: 'DELETE' });
    if (res && res.code === 200) {
        toast('图片已删除', 'success');
        closeModal('detailModal');
        loadImages(state.currentPage);
    } else {
        toast('删除失败', 'error');
    }
}

async function batchDelete() {
    if (state.selectedImages.size === 0) { toast('请先选择图片', 'error'); return; }
    if (!confirm(`确定删除 ${state.selectedImages.size} 张图片吗？`)) return;

    const ids = Array.from(state.selectedImages);
    const res = await api('/admin/batch-delete', {
        method: 'POST',
        body: JSON.stringify(ids),
    });
    if (res && res.code === 200) {
        toast(res.message || '批量删除成功', 'success');
        state.selectedImages.clear();
        loadImages(state.currentPage);
    }
}

function toggleSelect(id) {
    if (state.selectedImages.has(id)) state.selectedImages.delete(id);
    else state.selectedImages.add(id);
    renderGrid(state.images);
}

// ==================== 标签 ====================
async function loadTags() {
    const res = await api('/tags');
    if (res && res.code === 200) {
        state.tags = res.data || [];
        renderTagSidebar();
    }
}

function renderTagSidebar() {
    const container = document.getElementById('tagList');
    if (!container) return;
    container.innerHTML = state.tags.map(t => `
        <div class="sidebar-item ${state.currentView === 'tag' && state.currentTagId === t.id ? 'active' : ''}" onclick="filterByTag(${t.id}, '${t.name}')">
            <span class="sidebar-icon">🏷</span> ${t.name}
            <span class="sidebar-count">${t.imageCount || 0}</span>
        </div>`).join('');
}

async function filterByTag(tagId, tagName) {
    state.currentView = 'tag';
    state.currentTagId = tagId;
    updateSidebarActive();

    const grid = document.getElementById('grid');
    grid.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
    updateContentHeader(`标签: ${tagName}`, '');

    const res = await api(`/tags/${tagId}/images?page=1&size=${PAGE_SIZE}`);
    if (!res || res.code !== 200 || !res.data.records.length) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🏷</div><div class="empty-state-text">该标签下没有图片</div></div>';
        return;
    }

    // 加载每张图片的详情
    const imageDetails = await Promise.all(
        res.data.records.map(id => api(`/images/${id}`).then(r => r?.data).catch(() => null))
    );
    const images = imageDetails.filter(Boolean);
    updateContentHeader(`标签: ${tagName}`, `${res.data.total} 张图片`);
    renderGrid(images);
}

function showAddTagDialog(imageId) {
    const tagNames = state.tags.map(t => t.name).join(', ');
    const input = prompt(`为图片添加标签（多个用逗号分隔）\n\n现有标签: ${tagNames || '无'}`);
    if (!input) return;

    const names = input.split(/[,，]/).map(s => s.trim()).filter(Boolean);
    api(`/tags/images/${imageId}/tags`, {
        method: 'POST',
        body: JSON.stringify(names),
    }).then(res => {
        if (res && res.code === 200) {
            toast('标签已添加', 'success');
            openDetail(imageId);
            loadTags();
        } else {
            toast('添加失败: ' + (res?.message || ''), 'error');
        }
    });
}

async function showCreateTag() {
    const name = prompt('输入新标签名称:');
    if (!name) return;
    const res = await api('/tags', { method: 'POST', body: JSON.stringify({ name: name.trim() }) });
    if (res && res.code === 200) {
        toast('标签已创建', 'success');
        loadTags();
    } else {
        toast('创建失败: ' + (res?.message || ''), 'error');
    }
}

// ==================== 相册 ====================
async function loadAlbums() {
    const res = await api('/albums?page=1&size=100');
    if (res && res.code === 200) {
        state.albums = res.data.records || [];
        renderAlbumSidebar();
    }
}

function renderAlbumSidebar() {
    const container = document.getElementById('albumList');
    if (!container) return;
    container.innerHTML = state.albums.map(a => `
        <div class="sidebar-item ${state.currentView === 'album' && state.currentAlbumId === a.id ? 'active' : ''}" onclick="openAlbum(${a.id}, '${a.name}')">
            <span class="sidebar-icon">📁</span> ${a.name}
            <span class="sidebar-count">${a.imageCount || 0}</span>
        </div>`).join('');
}

async function openAlbum(albumId, albumName) {
    state.currentView = 'album';
    state.currentAlbumId = albumId;
    updateSidebarActive();

    const grid = document.getElementById('grid');
    grid.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
    updateContentHeader(`相册: ${albumName}`, '');

    const res = await api(`/albums/${albumId}/images?page=1&size=${PAGE_SIZE}`);
    if (!res || res.code !== 200 || !res.data.records.length) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">📁</div><div class="empty-state-text">相册为空</div><div class="empty-state-hint">请从图片库添加图片到相册</div></div>';
        return;
    }

    const imageDetails = await Promise.all(
        res.data.records.map(id => api(`/images/${id}`).then(r => r?.data).catch(() => null))
    );
    const images = imageDetails.filter(Boolean);
    updateContentHeader(`相册: ${albumName}`, `${res.data.total} 张图片`);
    renderGrid(images);
}

async function showCreateAlbum() {
    const name = prompt('输入新相册名称:');
    if (!name) return;
    const desc = prompt('相册描述（可选）:') || '';
    const res = await api('/albums', { method: 'POST', body: JSON.stringify({ name: name.trim(), description: desc }) });
    if (res && res.code === 200) {
        toast('相册已创建', 'success');
        loadAlbums();
    } else {
        toast('创建失败: ' + (res?.message || ''), 'error');
    }
}

// ==================== 搜索 ====================
function initSearch() {
    const searchBox = document.getElementById('searchBox');
    if (!searchBox) return;
    let timer = null;
    searchBox.addEventListener('input', () => {
        clearTimeout(timer);
        timer = setTimeout(() => {
            state.searchKeyword = searchBox.value.trim();
            if (state.currentView === 'images') loadImages(1);
        }, 400);
    });
}

// ==================== 侧边栏导航 ====================
function initSidebarNav() {
    // 由 HTML 中的 onclick 处理
}

function navigateTo(view) {
    state.currentView = view;
    state.currentAlbumId = null;
    state.currentTagId = null;
    state.selectedImages.clear();
    updateSidebarActive();

    if (view === 'images') loadImages(1);
    else if (view === 'trash') loadTrash(1);
}

function updateSidebarActive() {
    document.querySelectorAll('.sidebar-item').forEach(el => el.classList.remove('active'));
    const viewMap = { images: 'nav-images', trash: 'nav-trash' };
    const navEl = document.getElementById(viewMap[state.currentView]);
    if (navEl) navEl.classList.add('active');
    renderTagSidebar();
    renderAlbumSidebar();
}

// ==================== 分页 ====================
function renderPagination() {
    const container = document.getElementById('pagination');
    if (!container || state.totalPages <= 1) { if (container) container.innerHTML = ''; return; }

    let html = `<button class="page-btn" onclick="loadImages(${state.currentPage - 1})" ${state.currentPage <= 1 ? 'disabled' : ''}>‹</button>`;

    const start = Math.max(1, state.currentPage - 2);
    const end = Math.min(state.totalPages, state.currentPage + 2);

    if (start > 1) html += `<button class="page-btn" onclick="loadImages(1)">1</button>`;
    if (start > 2) html += `<span class="page-info">...</span>`;

    for (let i = start; i <= end; i++) {
        html += `<button class="page-btn ${i === state.currentPage ? 'active' : ''}" onclick="loadImages(${i})">${i}</button>`;
    }

    if (end < state.totalPages - 1) html += `<span class="page-info">...</span>`;
    if (end < state.totalPages) html += `<button class="page-btn" onclick="loadImages(${state.totalPages})">${state.totalPages}</button>`;

    html += `<button class="page-btn" onclick="loadImages(${state.currentPage + 1})" ${state.currentPage >= state.totalPages ? 'disabled' : ''}>›</button>`;
    html += `<span class="page-info">${state.currentPage} / ${state.totalPages}</span>`;

    container.innerHTML = html;
}

// ==================== UI 辅助 ====================
function updateContentHeader(title, subtitle) {
    const t = document.getElementById('contentTitle');
    const s = document.getElementById('contentSubtitle');
    if (t) t.textContent = title;
    if (s) s.textContent = subtitle;
}

function updateSidebarCounts() {
    const el = document.getElementById('imageCount');
    if (el) el.textContent = state.totalCount;
}

function formatSize(bytes) {
    if (!bytes) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => toast('链接已复制', 'success')).catch(() => toast('复制失败', 'error'));
}

// ==================== Toast ====================
function toast(msg, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.innerHTML = `<span>${type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'}</span> ${msg}`;
    container.appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 300); }, 3000);
}
