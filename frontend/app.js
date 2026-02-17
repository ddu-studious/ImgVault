/**
 * ImgVault Frontend - 图片管理用户端
 * 瀑布流布局 + 悬浮效果 + 走马灯灯箱浏览
 */

// ==================== 配置 ====================
const BASE = '/imgvault/api/v1';
const PAGE_SIZE = 24;
const VISITOR_ID_KEY = 'imgvault_visitor_id';

/**
 * 获取访客唯一标识
 * 首次访问时生成 UUID 并存储到 localStorage
 * 隐私模式下降级到 sessionStorage
 */
function getVisitorId() {
    try {
        let id = localStorage.getItem(VISITOR_ID_KEY);
        if (!id) {
            id = crypto.randomUUID();
            localStorage.setItem(VISITOR_ID_KEY, id);
        }
        return id;
    } catch (e) {
        let id = sessionStorage.getItem(VISITOR_ID_KEY);
        if (!id) {
            id = crypto.randomUUID();
            sessionStorage.setItem(VISITOR_ID_KEY, id);
        }
        return id;
    }
}

/**
 * 处理图片 URL
 * 1. 后端已配置 external-url，返回的 URL 已经是 https 域名格式，直接使用
 * 2. 兼容旧版本: 如果仍返回 localhost URL，转换为 nginx 代理路径
 */
function proxyImageUrl(url) {
    if (!url) return '';
    const cleanUrl = url.split('?')[0];
    // 已经是当前域名的 URL，直接返回（去掉 presigned 签名参数）
    if (cleanUrl.includes('/imgvault/storage/') || cleanUrl.includes('/imgvault/imgproxy/')) {
        return cleanUrl;
    }
    // 兼容: 匹配 imgproxy URL: http://localhost:8081/签名/参数/plain/s3://...
    const imgproxyMatch = cleanUrl.match(/https?:\/\/[^/]+:8081\/(.+)/);
    if (imgproxyMatch) return '/imgvault/imgproxy/' + imgproxyMatch[1];
    // 兼容: 匹配 MinIO URL: http://localhost:9000/imgvault/path
    const minioMatch = cleanUrl.match(/https?:\/\/[^/]+\/imgvault\/(.+)/);
    if (minioMatch) return '/imgvault/storage/' + minioMatch[1];
    return url;
}

// ==================== 状态 ====================
const state = {
    images: [],
    currentPage: 1,
    totalPages: 0,
    totalCount: 0,
    searchKeyword: '',
    lbIndex: 0,
};

// ==================== 主题 ====================
function initTheme() {
    const saved = localStorage.getItem('imgvault-theme') || 'system';
    applyTheme(saved);
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if ((localStorage.getItem('imgvault-theme') || 'system') === 'system') {
            applyTheme('system');
        }
    });
}

function applyTheme(mode) {
    if (mode === 'system') {
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        document.documentElement.setAttribute('data-theme', prefersDark ? 'dark' : 'light');
    } else {
        document.documentElement.setAttribute('data-theme', mode);
    }
    updateThemeIcon(mode);
}

function toggleTheme() {
    const modes = ['light', 'dark', 'system'];
    const current = localStorage.getItem('imgvault-theme') || 'system';
    const next = modes[(modes.indexOf(current) + 1) % modes.length];
    localStorage.setItem('imgvault-theme', next);
    applyTheme(next);
}

function updateThemeIcon(mode) {
    const btn = document.getElementById('themeToggle');
    if (!btn) return;
    const icons = { light: '☀️', dark: '🌙', system: '💻' };
    const labels = { light: '浅色模式', dark: '深色模式', system: '跟随系统' };
    btn.textContent = icons[mode] || '💻';
    btn.title = labels[mode] || '跟随系统';
}

// ==================== API 工具 ====================
async function api(path, opts = {}) {
    const url = BASE + path;
    try {
        const resp = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
                'X-Visitor-Id': getVisitorId(),
                ...opts.headers,
            },
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
    initTheme();
    initUploadZone();
    initSearch();
    loadImages();
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
}

function renderGrid(images) {
    const grid = document.getElementById('grid');
    if (!images || images.length === 0) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">📷</div><div class="empty-state-text">图片库为空</div><div class="empty-state-hint">上传第一张图片开始使用</div></div>';
        return;
    }

    grid.innerHTML = images.map((img, idx) => {
        const thumbSrc = (img.thumbnails && img.thumbnails.medium) || img.downloadUrl;
        const thumbUrl = proxyImageUrl(thumbSrc);
        const ext = (img.format || 'jpg').toUpperCase();
        const sizeStr = formatSize(img.fileSize);
        const dimStr = img.width && img.height ? `${img.width}×${img.height}` : '';

        return `
        <div class="card" data-id="${img.id}" data-idx="${idx}" onclick="openLightbox(${idx})">
            <div class="card-img-wrap">
                ${thumbUrl
                    ? `<img class="card-img" src="${thumbUrl}" alt="${img.originalName || ''}" loading="lazy" onerror="this.outerHTML='<div class=\\'card-img-placeholder\\'>🖼</div>'">`
                    : `<div class="card-img-placeholder">🖼</div>`}
                <span class="card-badge">${ext}</span>
                ${dimStr ? `<span class="card-dim-badge">${dimStr}</span>` : ''}
                <div class="card-overlay">
                    <div class="card-overlay-info">
                        <div class="overlay-name">${img.originalName || 'untitled'}</div>
                        <div class="overlay-meta">
                            <span>${dimStr}</span>
                            <span>${sizeStr}</span>
                            <span>${ext}</span>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card-body">
                <div class="card-name" title="${img.originalName || ''}">${img.originalName || 'untitled'}</div>
                <div class="card-meta"><span>${dimStr}</span><span>${sizeStr}</span></div>
            </div>
        </div>`;
    }).join('');
}

// ==================== Lightbox (走马灯浏览) ====================
function openLightbox(idx) {
    if (!state.images || !state.images[idx]) return;
    state.lbIndex = idx;

    const lb = document.getElementById('lightbox');
    lb.classList.add('active');
    document.body.style.overflow = 'hidden';

    renderLightboxThumbs();
    showLightboxImage(idx);
}

function closeLightbox() {
    document.getElementById('lightbox').classList.remove('active');
    document.body.style.overflow = '';
}

function showLightboxImage(idx) {
    state.lbIndex = idx;
    const img = state.images[idx];
    if (!img) return;

    const lbImg = document.getElementById('lbImage');
    const fullSrc = proxyImageUrl(img.downloadUrl);
    lbImg.classList.add('fade');
    setTimeout(() => {
        lbImg.src = fullSrc;
        lbImg.onload = () => lbImg.classList.remove('fade');
    }, 150);

    const ext = (img.format || 'jpg').toUpperCase();
    const dimStr = img.width && img.height ? `${img.width}×${img.height}` : '';
    const sizeStr = formatSize(img.fileSize);

    document.getElementById('lbName').textContent = img.originalName || 'untitled';
    document.getElementById('lbMeta').innerHTML =
        `<span>${ext}</span><span>${dimStr}</span><span>${sizeStr}</span>`;
    document.getElementById('lbCounter').textContent =
        `${idx + 1} / ${state.images.length}`;

    document.querySelectorAll('.lightbox-thumb').forEach((t, i) =>
        t.classList.toggle('active', i === idx));
    document.querySelectorAll('.lightbox-thumb')[idx]?.scrollIntoView({
        behavior: 'smooth', inline: 'center' });
}

function renderLightboxThumbs() {
    const container = document.getElementById('lbThumbs');
    container.innerHTML = state.images.map((img, idx) => {
        const src = proxyImageUrl((img.thumbnails && img.thumbnails.small) || img.downloadUrl);
        return `<img class="lightbox-thumb${idx === state.lbIndex ? ' active' : ''}" src="${src}" onclick="showLightboxImage(${idx})" loading="lazy">`;
    }).join('');
}

function lbPrev() {
    showLightboxImage((state.lbIndex - 1 + state.images.length) % state.images.length);
}
function lbNext() {
    showLightboxImage((state.lbIndex + 1) % state.images.length);
}

function openDetailFromLightbox() {
    const img = state.images[state.lbIndex];
    if (img) openDetail(img.id);
}
function downloadFromLightbox() {
    const img = state.images[state.lbIndex];
    if (!img) return;
    const url = proxyImageUrl(img.downloadUrl);
    const a = document.createElement('a');
    a.href = url; a.download = img.originalName || 'image'; a.click();
}
function copyLightboxLink() {
    const img = state.images[state.lbIndex];
    if (!img) return;
    const proxied = proxyImageUrl(img.downloadUrl);
    const url = proxied.startsWith('http') ? proxied : window.location.origin + proxied;
    copyToClipboard(url);
}

document.addEventListener('keydown', e => {
    const lb = document.getElementById('lightbox');
    if (!lb.classList.contains('active')) return;
    if (e.key === 'ArrowLeft') lbPrev();
    if (e.key === 'ArrowRight') lbNext();
    if (e.key === 'Escape') closeLightbox();
});

// ==================== 图片详情 ====================
async function openDetail(id) {
    const res = await api(`/images/${id}`);
    if (!res || res.code !== 200) {
        toast('无法加载图片详情', 'error');
        return;
    }
    const img = res.data;

    let tags = [];
    try {
        const tagRes = await api(`/tags/images/${id}/tags`);
        if (tagRes && tagRes.code === 200) tags = tagRes.data || [];
    } catch (e) { /* ignore */ }

    const modal = document.getElementById('detailModal');
    const imgUrl = proxyImageUrl(img.downloadUrl);
    const sizeStr = formatSize(img.fileSize);
    const ext = (img.format || '-').toUpperCase();
    const fullLink = imgUrl.startsWith('http') ? imgUrl : window.location.origin + imgUrl;

    const sizePresets = buildSizePresets(img.width, img.height);
    const formatOptions = buildFormatOptions(img.format);

    document.getElementById('detailContent').innerHTML = `
        <div class="detail-layout">
            <div class="detail-image-wrap">
                ${imgUrl ? `<img src="${imgUrl}" alt="${img.originalName}">` : '<div style="padding:40px;color:var(--text-muted)">无法加载图片</div>'}
            </div>
            <div class="detail-info">
                <div class="detail-section">
                    <div class="detail-section-title">文件信息</div>
                    <div class="detail-row"><span class="label">文件名</span><span class="value" title="${img.originalName}">${img.originalName || '-'}</span></div>
                    <div class="detail-row"><span class="label">格式</span><span class="value">${ext}</span></div>
                    <div class="detail-row"><span class="label">尺寸</span><span class="value">${img.width && img.height ? img.width + '×' + img.height : '-'}</span></div>
                    <div class="detail-row"><span class="label">大小</span><span class="value">${sizeStr}</span></div>
                    <div class="detail-row"><span class="label">MIME</span><span class="value">${img.mimeType || '-'}</span></div>
                    <div class="detail-row"><span class="label">UUID</span><span class="value" title="${img.imageUuid || ''}">${(img.imageUuid || '-').substring(0, 12)}...</span></div>
                    <div class="detail-row"><span class="label">创建时间</span><span class="value">${img.createdAt || '-'}</span></div>
                </div>

                <div class="detail-section">
                    <div class="detail-section-title">标签</div>
                    <div class="detail-tags" id="detailTags">
                        ${tags.map(t => `<span class="detail-tag">${t.name}</span>`).join('')}
                        ${tags.length === 0 ? '<span style="color:var(--text-muted);font-size:12px">暂无标签</span>' : ''}
                    </div>
                </div>

                <div class="detail-actions">
                    <a class="btn btn-primary btn-sm" href="${imgUrl}" target="_blank" download>⬇ 原图下载</a>
                    <button class="btn btn-sm btn-outline" onclick="copyToClipboard('${fullLink}')">🔗 复制链接</button>
                </div>

                <div class="download-panel">
                    <div class="download-panel-title">多规格下载</div>
                    <div class="download-options">
                        <div class="download-option-group">
                            <div class="download-option-label">尺寸</div>
                            <div class="download-chips" id="dlSizeChips">
                                ${sizePresets.map((p, i) => `<span class="download-chip${i === 0 ? ' active' : ''}" data-w="${p.w}" data-h="${p.h}" onclick="selectDlSize(this)">${p.label}</span>`).join('')}
                            </div>
                        </div>
                        <div class="download-option-group">
                            <div class="download-option-label">格式</div>
                            <div class="download-chips" id="dlFormatChips">
                                ${formatOptions.map((f, i) => `<span class="download-chip${i === 0 ? ' active' : ''}" data-fmt="${f.value}" onclick="selectDlFormat(this)">${f.label}</span>`).join('')}
                            </div>
                        </div>
                        <div class="download-option-group">
                            <div class="download-option-label">自定义尺寸</div>
                            <div class="download-custom">
                                <input type="number" id="dlCustomW" placeholder="宽" min="1" max="10000">
                                <span class="download-custom-sep">x</span>
                                <input type="number" id="dlCustomH" placeholder="高" min="1" max="10000">
                                <button class="download-go-btn" onclick="applyCustomSize()">应用</button>
                            </div>
                        </div>
                        <div style="margin-top:10px;display:flex;gap:8px">
                            <button class="btn btn-primary btn-sm" onclick="doProcessedDownload(${img.id}, '${img.originalName || 'image'}')">⬇ 下载所选规格</button>
                            <button class="btn btn-sm btn-outline" onclick="copyProcessedLink(${img.id})">🔗 复制处理链接</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>`;

    modal.classList.add('active');
}

// ==================== 多规格下载 ====================

function buildSizePresets(origW, origH) {
    const presets = [{ label: '原始尺寸', w: 0, h: 0 }];
    const sizes = [
        { label: '大 (1920)', w: 1920, h: 1080 },
        { label: '中 (1280)', w: 1280, h: 720 },
        { label: '小 (800)', w: 800, h: 600 },
        { label: '缩略 (400)', w: 400, h: 300 },
        { label: '图标 (150)', w: 150, h: 150 },
    ];
    for (const s of sizes) {
        if (origW && origH && (s.w < origW || s.h < origH)) {
            presets.push(s);
        }
    }
    if (presets.length === 1 && origW && origH) {
        presets.push({ label: '中 (1280)', w: 1280, h: 720 });
        presets.push({ label: '小 (800)', w: 800, h: 600 });
    }
    return presets;
}

function buildFormatOptions(originalFormat) {
    const fmt = (originalFormat || '').toLowerCase();
    const options = [{ label: '原始格式', value: '' }];
    const all = [
        { label: 'JPEG', value: 'jpeg' },
        { label: 'PNG', value: 'png' },
        { label: 'WebP', value: 'webp' },
        { label: 'AVIF', value: 'avif' },
    ];
    for (const f of all) {
        if (f.value !== fmt) options.push(f);
    }
    return options;
}

function selectDlSize(el) {
    el.closest('.download-chips').querySelectorAll('.download-chip').forEach(c => c.classList.remove('active'));
    el.classList.add('active');
    const wInput = document.getElementById('dlCustomW');
    const hInput = document.getElementById('dlCustomH');
    if (wInput && hInput) {
        const w = el.dataset.w, h = el.dataset.h;
        if (w !== '0' && h !== '0') { wInput.value = w; hInput.value = h; }
        else { wInput.value = ''; hInput.value = ''; }
    }
}

function selectDlFormat(el) {
    el.closest('.download-chips').querySelectorAll('.download-chip').forEach(c => c.classList.remove('active'));
    el.classList.add('active');
}

function applyCustomSize() {
    const w = parseInt(document.getElementById('dlCustomW').value) || 0;
    const h = parseInt(document.getElementById('dlCustomH').value) || 0;
    if (w <= 0 && h <= 0) { toast('请输入有效的宽高', 'error'); return; }
    const chips = document.getElementById('dlSizeChips');
    chips.querySelectorAll('.download-chip').forEach(c => c.classList.remove('active'));
    let custom = chips.querySelector('[data-custom]');
    if (!custom) {
        custom = document.createElement('span');
        custom.className = 'download-chip active';
        custom.dataset.custom = '1';
        custom.onclick = function() { selectDlSize(this); };
        chips.appendChild(custom);
    }
    custom.className = 'download-chip active';
    custom.dataset.w = w; custom.dataset.h = h;
    custom.textContent = `${w || 'auto'}x${h || 'auto'}`;
    toast('已应用自定义尺寸', 'success');
}

function getSelectedDownloadParams() {
    const sizeChip = document.querySelector('#dlSizeChips .download-chip.active');
    const fmtChip = document.querySelector('#dlFormatChips .download-chip.active');
    const w = sizeChip ? parseInt(sizeChip.dataset.w) || 0 : 0;
    const h = sizeChip ? parseInt(sizeChip.dataset.h) || 0 : 0;
    const fmt = fmtChip ? fmtChip.dataset.fmt || '' : '';
    return { width: w, height: h, format: fmt, quality: 85 };
}

async function doProcessedDownload(imageId, originalName) {
    const params = getSelectedDownloadParams();
    if (params.width === 0 && params.height === 0 && !params.format) {
        const img = state.images.find(i => i.id === imageId);
        const url = img ? proxyImageUrl(img.downloadUrl) : `/images/${imageId}`;
        const a = document.createElement('a');
        a.href = url; a.download = originalName; a.click();
        return;
    }
    try {
        const qs = new URLSearchParams();
        if (params.width > 0) qs.set('width', params.width);
        if (params.height > 0) qs.set('height', params.height);
        if (params.format) qs.set('format', params.format);
        if (params.quality > 0) qs.set('quality', params.quality);
        const res = await api(`/images/${imageId}/process-url?${qs.toString()}`);
        if (res && res.code === 200 && res.data) {
            const processedUrl = proxyImageUrl(res.data);
            const ext = params.format || originalName.split('.').pop() || 'jpg';
            const baseName = originalName.replace(/\.[^.]+$/, '');
            const sizeSuffix = (params.width || params.height) ? `_${params.width || 'auto'}x${params.height || 'auto'}` : '';
            const fileName = `${baseName}${sizeSuffix}.${ext}`;
            const a = document.createElement('a');
            a.href = processedUrl; a.download = fileName; a.target = '_blank'; a.click();
            toast('开始下载: ' + fileName, 'success');
        } else {
            toast('获取处理链接失败', 'error');
        }
    } catch (e) {
        toast('下载失败: ' + e.message, 'error');
    }
}

async function copyProcessedLink(imageId) {
    const params = getSelectedDownloadParams();
    if (params.width === 0 && params.height === 0 && !params.format) {
        const img = state.images.find(i => i.id === imageId);
        if (img) {
            const url = proxyImageUrl(img.downloadUrl);
            const full = url.startsWith('http') ? url : window.location.origin + url;
            copyToClipboard(full);
        }
        return;
    }
    try {
        const qs = new URLSearchParams();
        if (params.width > 0) qs.set('width', params.width);
        if (params.height > 0) qs.set('height', params.height);
        if (params.format) qs.set('format', params.format);
        if (params.quality > 0) qs.set('quality', params.quality);
        const res = await api(`/images/${imageId}/process-url?${qs.toString()}`);
        if (res && res.code === 200 && res.data) {
            const url = proxyImageUrl(res.data);
            const full = url.startsWith('http') ? url : window.location.origin + url;
            copyToClipboard(full);
        } else {
            toast('获取处理链接失败', 'error');
        }
    } catch (e) {
        toast('复制失败: ' + e.message, 'error');
    }
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

            const result = await new Promise((resolve, reject) => {
                const xhr = new XMLHttpRequest();
                xhr.open('POST', BASE + '/images/upload');
                xhr.setRequestHeader('X-Visitor-Id', getVisitorId());
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
                if (result.data && result.data.duplicate) {
                    document.querySelector(`#${itemId} .upload-item-status`).textContent = '已存在';
                    document.querySelector(`#${itemId} .upload-item-status`).className = 'upload-item-status success';
                    toast(`${file.name} 图片已存在，跳过重复上传`, 'info');
                } else {
                    document.querySelector(`#${itemId} .upload-item-status`).textContent = '完成';
                    document.querySelector(`#${itemId} .upload-item-status`).className = 'upload-item-status success';
                    toast(`${file.name} 上传成功`, 'success');
                }
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

    setTimeout(() => {
        loadImages(1);
        progressContainer.innerHTML = '';
    }, 2000);
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
            loadImages(1);
        }, 400);
    });
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
