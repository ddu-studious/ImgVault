/**
 * ImgVault Watermark Removal Tool
 * Fabric.js canvas with brush/rect/eraser for mask painting
 */

// ==================== Config ====================
const BASE = '/imgvault/api/v1';
const VISITOR_ID_KEY = 'imgvault_visitor_id';
const MASK_LAYER_NAME = 'watermark-mask';
const MASK_COLOR = 'rgba(255,0,0,0.4)';

// ==================== State ====================
const state = {
    canvas: null,
    imageObj: null,
    imageId: null,
    imageFile: null,
    imageUrl: null,
    resultUrl: null,
    tool: 'brush',
    brushSize: 20,
    isDrawingRect: false,
    rectStart: null,
    imagePickerPage: 1,
    imagePickerTotalPages: 1,
};

// ==================== Helpers ====================
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

function proxyImageUrl(url) {
    if (!url) return '';
    const cleanUrl = url.split('?')[0];
    if (cleanUrl.includes('/imgvault/storage/') || cleanUrl.includes('/imgvault/imgproxy/')) {
        return cleanUrl;
    }
    const imgproxyMatch = cleanUrl.match(/https?:\/\/[^/]+:8081\/(.+)/);
    if (imgproxyMatch) return '/imgvault/imgproxy/' + imgproxyMatch[1];
    const minioMatch = cleanUrl.match(/https?:\/\/[^/]+\/imgvault\/(.+)/);
    if (minioMatch) return '/imgvault/storage/' + minioMatch[1];
    return url;
}

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

function toast(msg, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.innerHTML = `<span>${type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'}</span> ${msg}`;
    container.appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 300); }, 3000);
}

// ==================== Theme ====================
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

// ==================== Fabric.js Canvas ====================
function initCanvas() {
    const el = document.getElementById('watermarkCanvas');
    if (!el) return;

    state.canvas = new fabric.Canvas('watermarkCanvas', {
        selection: false,
        preserveObjectStacking: true,
    });

    // Brush tool
    state.canvas.freeDrawingBrush = new fabric.PencilBrush(state.canvas);
    state.canvas.freeDrawingBrush.color = MASK_COLOR;
    state.canvas.freeDrawingBrush.width = state.brushSize;
    state.canvas.isDrawingMode = false;

    // Prevent selection of mask objects when not in eraser
    state.canvas.on('selection:created', (e) => {
        if (state.tool !== 'eraser') {
            state.canvas.discardActiveObject();
        }
    });

    // Mark brush paths as mask layer
    state.canvas.on('path:created', (e) => {
        if (e.path && state.tool === 'brush') {
            e.path.name = MASK_LAYER_NAME;
        }
    });

    // Rectangle drawing
    state.canvas.on('mouse:down', onCanvasMouseDown);
    state.canvas.on('mouse:move', onCanvasMouseMove);
    state.canvas.on('mouse:up', onCanvasMouseUp);

    // Brush size slider
    const slider = document.getElementById('brushSizeSlider');
    const label = document.getElementById('brushSizeLabel');
    if (slider && label) {
        slider.addEventListener('input', () => {
            state.brushSize = parseInt(slider.value, 10);
            label.textContent = state.brushSize + 'px';
            if (state.canvas && state.canvas.freeDrawingBrush) {
                state.canvas.freeDrawingBrush.width = state.brushSize;
            }
        });
    }

    // Tool buttons
    document.getElementById('btnBrush').addEventListener('click', () => setTool('brush'));
    document.getElementById('btnRect').addEventListener('click', () => setTool('rect'));
    document.getElementById('btnEraser').addEventListener('click', () => setTool('eraser'));
    document.getElementById('btnClearMask').addEventListener('click', clearMask);
    document.getElementById('btnUpload').addEventListener('click', () => document.getElementById('fileInput').click());
    document.getElementById('btnSelectLibrary').addEventListener('click', openImagePicker);
    document.getElementById('fileInput').addEventListener('change', onFileSelected);
    document.getElementById('btnRemove').addEventListener('click', removeWatermark);
    document.getElementById('btnDownload').addEventListener('click', downloadResult);

    // Engine radio
    document.querySelectorAll('.watermark-engine-option').forEach((opt) => {
        opt.addEventListener('click', () => {
            document.querySelectorAll('.watermark-engine-option').forEach((o) => o.classList.remove('selected'));
            opt.classList.add('selected');
            opt.querySelector('input').checked = true;
        });
    });
}

function setTool(tool) {
    state.tool = tool;
    document.querySelectorAll('#btnBrush, #btnRect, #btnEraser').forEach((b) => b.classList.remove('active'));
    const activeBtn = document.getElementById('btn' + tool.charAt(0).toUpperCase() + tool.slice(1));
    if (activeBtn) activeBtn.classList.add('active');

    if (state.canvas) {
        if (tool === 'brush') {
            state.canvas.isDrawingMode = true;
            state.canvas.freeDrawingBrush.color = MASK_COLOR;
            state.canvas.freeDrawingBrush.width = state.brushSize;
        } else if (tool === 'eraser') {
            state.canvas.isDrawingMode = true;
            state.canvas.freeDrawingBrush.color = 'rgba(0,0,0,0)';
            state.canvas.freeDrawingBrush.width = state.brushSize;
        } else {
            state.canvas.isDrawingMode = false;
        }
    }
}

function onCanvasMouseDown(e) {
    if (state.tool !== 'rect' || !state.canvas || !state.imageObj) return;
    const ptr = state.canvas.getPointer(e.e);
    state.isDrawingRect = true;
    state.rectStart = { x: ptr.x, y: ptr.y };
}

function onCanvasMouseMove(e) {
    if (state.tool !== 'rect' || !state.isDrawingRect || !state.rectStart) return;
    const ptr = state.canvas.getPointer(e.e);
    const objects = state.canvas.getObjects();
    const lastObj = objects[objects.length - 1];
    if (lastObj && lastObj.name === 'rect-drawing') {
        state.canvas.remove(lastObj);
    }
    const x = Math.min(state.rectStart.x, ptr.x);
    const y = Math.min(state.rectStart.y, ptr.y);
    const w = Math.abs(ptr.x - state.rectStart.x);
    const h = Math.abs(ptr.y - state.rectStart.y);
    if (w > 2 && h > 2) {
        const rect = new fabric.Rect({
            left: x,
            top: y,
            width: w,
            height: h,
            fill: MASK_COLOR,
            selectable: false,
            evented: false,
            name: 'rect-drawing',
        });
        state.canvas.add(rect);
        state.canvas.renderAll();
    }
}

function onCanvasMouseUp(e) {
    if (state.tool === 'rect' && state.isDrawingRect) {
        const objects = state.canvas.getObjects();
        const lastObj = objects[objects.length - 1];
        if (lastObj && lastObj.name === 'rect-drawing') {
            lastObj.name = MASK_LAYER_NAME;
        }
    }
    state.isDrawingRect = false;
    state.rectStart = null;
}

function clearMask() {
    if (!state.canvas) return;
    const toRemove = state.canvas.getObjects().filter((o) => {
        if (o === state.imageObj) return false;
        return o.name === MASK_LAYER_NAME || o.name === 'rect-drawing' || (o.type === 'path' && o !== state.imageObj);
    });
    toRemove.forEach((o) => state.canvas.remove(o));
    state.canvas.renderAll();
    toast('已清除所有标记', 'info');
}

function getMaskObjects() {
    if (!state.canvas) return [];
    return state.canvas.getObjects().filter((o) => {
        if (o === state.imageObj) return false;
        return o.name === MASK_LAYER_NAME || o.name === 'rect-drawing' ||
            (o.type === 'path' && o !== state.imageObj);
    });
}

// ==================== Image Loading ====================
function loadImageFromUrl(url, imageId) {
    return new Promise((resolve, reject) => {
        fabric.Image.fromURL(url, (img) => {
            if (!img) {
                reject(new Error('Failed to load image'));
                return;
            }
            state.imageObj = img;
            state.imageId = imageId || null;
            state.imageFile = null;
            state.imageUrl = url;
            state.resultUrl = null;
            setupCanvasWithImage(img);
            updatePreviewBefore(url);
            updatePreviewAfter(null);
            document.getElementById('btnRemove').disabled = false;
            document.getElementById('btnDownload').disabled = true;
            resolve();
        }, { crossOrigin: 'anonymous' });
    });
}

function loadImageFromFile(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            fabric.Image.fromURL(e.target.result, (img) => {
                if (!img) {
                    reject(new Error('Failed to load image'));
                    return;
                }
                state.imageObj = img;
                state.imageId = null;
                state.imageFile = file;
                state.imageUrl = e.target.result;
                state.resultUrl = null;
                setupCanvasWithImage(img);
                updatePreviewBefore(e.target.result);
                updatePreviewAfter(null);
                document.getElementById('btnRemove').disabled = false;
                document.getElementById('btnDownload').disabled = true;
                resolve();
            }, { crossOrigin: 'anonymous' });
        };
        reader.onerror = () => reject(new Error('Failed to read file'));
        reader.readAsDataURL(file);
    });
}

function setupCanvasWithImage(img) {
    const canvas = state.canvas;
    if (!canvas) return;

    canvas.clear();
    canvas.backgroundColor = '#e0e0e0';

    const wrap = document.querySelector('.watermark-canvas-wrap');
    const maxW = wrap ? wrap.clientWidth - 40 : 800;
    const maxH = (wrap ? wrap.clientHeight - 40 : 500) || 500;

    const scale = Math.min(maxW / img.width, maxH / img.height, 1);
    img.scale(scale);
    img.set({ selectable: false, evented: false, name: 'background' });
    canvas.add(img);
    canvas.setDimensions({ width: img.width * scale, height: img.height * scale });
    canvas.renderAll();
}

function onFileSelected(e) {
    const file = e.target.files && e.target.files[0];
    if (!file || !file.type.startsWith('image/')) {
        toast('请选择图片文件', 'error');
        return;
    }
    loadImageFromFile(file).then(() => {
        toast('图片已加载', 'success');
    }).catch((err) => {
        toast('加载失败: ' + err.message, 'error');
    });
    e.target.value = '';
}

// ==================== Image Picker Modal ====================
async function openImagePicker() {
    const modal = document.getElementById('imagePickerModal');
    modal.classList.add('active');
    state.imagePickerPage = 1;
    await loadImagePickerPage(1);
}

function closeImagePicker() {
    document.getElementById('imagePickerModal').classList.remove('active');
}

async function loadImagePickerPage(page) {
    const loading = document.getElementById('imagePickerLoading');
    const grid = document.getElementById('imagePickerGrid');
    const pagination = document.getElementById('imagePickerPagination');
    loading.style.display = 'flex';
    grid.style.display = 'none';
    pagination.style.display = 'none';

    const res = await api(`/images?page=${page}&size=24`);
    if (!res || res.code !== 200) {
        loading.innerHTML = '<div class="empty-state"><div class="empty-state-icon">😕</div><div class="empty-state-text">加载失败</div></div>';
        return;
    }

    const pageData = res.data;
    const images = pageData.records || [];
    state.imagePickerTotalPages = pageData.pages || 1;

    loading.style.display = 'none';
    grid.style.display = 'block';

    grid.innerHTML = images.map((img) => {
        const thumbSrc = (img.thumbnails && img.thumbnails.medium) || img.downloadUrl;
        const thumbUrl = proxyImageUrl(thumbSrc);
        return `
            <div class="card" style="cursor:pointer;margin-bottom:10px" data-id="${img.id}" data-url="${proxyImageUrl(img.downloadUrl)}">
                <div class="card-img-wrap">
                    ${thumbUrl
                        ? `<img class="card-img" src="${thumbUrl}" alt="" loading="lazy" style="pointer-events:none">`
                        : `<div class="card-img-placeholder">🖼</div>`}
                </div>
                <div class="card-body">
                    <div class="card-name" style="font-size:11px">${(img.originalName || 'untitled').substring(0, 20)}</div>
                </div>
            </div>`;
    }).join('');

    grid.querySelectorAll('.card').forEach((card) => {
        card.addEventListener('click', () => {
            const id = card.dataset.id;
            const url = card.dataset.url;
            closeImagePicker();
            loadImageFromUrl(url, parseInt(id, 10)).then(() => {
                toast('已从图库加载', 'success');
            }).catch((err) => {
                toast('加载失败: ' + err.message, 'error');
            });
        });
    });

    if (state.imagePickerTotalPages > 1) {
        pagination.style.display = 'flex';
        pagination.innerHTML = `
            <button class="page-btn" onclick="loadImagePickerPage(${page - 1})" ${page <= 1 ? 'disabled' : ''}>‹</button>
            <span class="page-info">${page} / ${state.imagePickerTotalPages}</span>
            <button class="page-btn" onclick="loadImagePickerPage(${page + 1})" ${page >= state.imagePickerTotalPages ? 'disabled' : ''}>›</button>
        `;
    }
}

// ==================== Mask Generation ====================
function generateMaskDataSimple() {
    const canvas = state.canvas;
    const img = state.imageObj;
    if (!canvas || !img) return null;

    const maskObjs = getMaskObjects();
    if (maskObjs.length === 0) {
        toast('请先标记水印区域', 'error');
        return null;
    }

    // Mask must match original image dimensions for API
    const w = Math.round(img.width);
    const h = Math.round(img.height);
    const scaleX = img.scaleX || 1;
    const scaleY = img.scaleY || 1;
    const imgLeft = img.left || 0;
    const imgTop = img.top || 0;

    const offCanvas = document.createElement('canvas');
    offCanvas.width = w;
    offCanvas.height = h;
    const ctx = offCanvas.getContext('2d');

    ctx.fillStyle = '#000000';
    ctx.fillRect(0, 0, w, h);
    ctx.fillStyle = '#FFFFFF';

    ctx.save();
    ctx.scale(1 / scaleX, 1 / scaleY);
    ctx.translate(-imgLeft, -imgTop);

    maskObjs.forEach((obj) => {
        if (obj.type === 'rect') {
            const l = obj.left;
            const t = obj.top;
            const rw = obj.width * (obj.scaleX || 1);
            const rh = obj.height * (obj.scaleY || 1);
            ctx.fillRect(l, t, rw, rh);
        } else if (obj.type === 'path' && obj.path) {
            const path = obj.path;
            if (path && path.length > 0) {
                ctx.beginPath();
                path.forEach((cmd, i) => {
                    if (i === 0) ctx.moveTo(cmd[1], cmd[2]);
                    else if (cmd[0] === 'L') ctx.lineTo(cmd[1], cmd[2]);
                    else if (cmd[0] === 'Q') ctx.quadraticCurveTo(cmd[1], cmd[2], cmd[3], cmd[4]);
                    else if (cmd[0] === 'C') ctx.bezierCurveTo(cmd[1], cmd[2], cmd[3], cmd[4], cmd[5], cmd[6]);
                    else if (cmd[0] === 'Z') ctx.closePath();
                });
                ctx.fill();
            }
        }
    });

    ctx.restore();

    return offCanvas.toDataURL('image/png').replace(/^data:image\/png;base64,/, '');
}

// ==================== API Submit ====================
async function removeWatermark() {
    const maskData = generateMaskDataSimple();
    if (!maskData) return;

    const engine = document.querySelector('input[name="engine"]:checked').value;
    const btn = document.getElementById('btnRemove');
    btn.disabled = true;
    btn.textContent = '处理中...';

    try {
        const formData = new FormData();
        formData.append('maskData', maskData);
        formData.append('engine', engine);

        if (state.imageId) {
            formData.append('imageId', state.imageId);
        } else if (state.imageFile) {
            formData.append('file', state.imageFile);
        } else {
            toast('请先上传或选择图片', 'error');
            btn.disabled = false;
            btn.textContent = '去除水印';
            return;
        }

        const resp = await fetch(BASE + '/watermark/remove', {
            method: 'POST',
            headers: {
                'X-Visitor-Id': getVisitorId(),
            },
            body: formData,
        });

        const data = await resp.json();
        if (data.code === 200 && data.data) {
            const url = proxyImageUrl(data.data.downloadUrl);
            state.resultUrl = url.startsWith('http') ? url : window.location.origin + url;
            updatePreviewAfter(state.resultUrl);
            document.getElementById('btnDownload').disabled = false;
            toast('去水印完成', 'success');
        } else {
            toast(data.message || '处理失败', 'error');
        }
    } catch (err) {
        toast('请求失败: ' + err.message, 'error');
    }

    btn.disabled = false;
    btn.textContent = '去除水印';
}

function downloadResult() {
    if (!state.resultUrl) {
        toast('暂无结果可下载', 'error');
        return;
    }
    const a = document.createElement('a');
    a.href = state.resultUrl;
    a.download = 'watermark-removed-' + Date.now() + '.png';
    a.target = '_blank';
    a.click();
    toast('下载已开始', 'success');
}

// ==================== Preview ====================
function updatePreviewBefore(url) {
    const el = document.getElementById('previewBefore');
    if (!el) return;
    el.innerHTML = url
        ? `<img src="${url}" alt="原图" crossorigin="anonymous" onerror="this.parentElement.innerHTML='<span class=placeholder>加载失败</span>'">`
        : '<span class="placeholder">原图</span>';
}

function updatePreviewAfter(url) {
    const el = document.getElementById('previewAfter');
    if (!el) return;
    el.innerHTML = url
        ? `<img src="${url}" alt="结果" crossorigin="anonymous" onerror="this.parentElement.innerHTML='<span class=placeholder>加载失败</span>'">`
        : '<span class="placeholder">结果</span>';
}

// ==================== Eraser Mode ====================
function setupEraserErasing() {
    if (state.tool !== 'eraser' || !state.canvas) return;
    state.canvas.on('path:created', (e) => {
        const path = e.path;
        if (path && path.fill === 'rgba(0,0,0,0)') {
            const maskObjs = getMaskObjects();
            const pathBounds = path.getBoundingRect();
            maskObjs.forEach((obj) => {
                const objBounds = obj.getBoundingRect();
                if (obj !== path && fabric.Intersection.intersectObjectBounds(path, obj)) {
                    state.canvas.remove(obj);
                }
            });
            state.canvas.remove(path);
            state.canvas.renderAll();
        }
    });
}

function setupEraserErasingV2() {
    if (!state.canvas) return;
    state.canvas.on('path:created', (e) => {
        const path = e.path;
        if (!path) return;
        if (state.tool === 'eraser' && path.fill === 'rgba(0,0,0,0)') {
            const pathBounds = path.getBoundingRect();
            const toRemove = [];
            state.canvas.getObjects().forEach((obj) => {
                if (obj === state.imageObj || obj === path) return;
                const objBounds = obj.getBoundingRect();
                const overlap = !(pathBounds.left + pathBounds.width < objBounds.left ||
                    objBounds.left + objBounds.width < pathBounds.left ||
                    pathBounds.top + pathBounds.height < objBounds.top ||
                    objBounds.top + objBounds.height < pathBounds.top);
                if (overlap) toRemove.push(obj);
            });
            toRemove.forEach((o) => state.canvas.remove(o));
            state.canvas.remove(path);
            state.canvas.renderAll();
        }
    });
}

// ==================== Init ====================
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initCanvas();
    setTool('brush');
    setupEraserErasingV2();
});
