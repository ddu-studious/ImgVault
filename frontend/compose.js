/**
 * ImgVault Compose Editor - 视觉合成编辑器
 * Fabric.js 画布 + Compose API
 */

// ==================== 配置 ====================
const BASE = '/imgvault/api/v1';
const VISITOR_ID_KEY = 'imgvault_visitor_id';

// ==================== 辅助函数 ====================
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

function formatSize(bytes) {
    if (!bytes) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function toast(msg, type) {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const el = document.createElement('div');
    el.className = 'toast ' + (type || 'info');
    el.innerHTML = '<span>' + (type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️') + '</span> ' + msg;
    container.appendChild(el);
    setTimeout(function () {
        el.style.opacity = '0';
        setTimeout(function () { el.remove(); }, 300);
    }, 3000);
}

// ==================== 主题 ====================
function initTheme() {
    const saved = localStorage.getItem('imgvault-theme') || 'system';
    applyTheme(saved);
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
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

// ==================== API ====================
async function api(path, opts) {
    opts = opts || {};
    const url = BASE + path;
    try {
        const resp = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
                'X-Visitor-Id': getVisitorId(),
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

// ==================== 画布状态 ====================
let canvas;
let zoomLevel = 1;
let historyState = [];
let historyIndex = -1;
let isLoadingState = false;
const MAX_HISTORY = 50;

// ==================== 初始化 ====================
document.addEventListener('DOMContentLoaded', function () {
    initTheme();
    initCanvas();
    loadTemplates();
    setupKeyboard();
});

function initCanvas() {
    const el = document.getElementById('fabricCanvas');
    const w = parseInt(document.getElementById('canvasW').value) || 800;
    const h = parseInt(document.getElementById('canvasH').value) || 600;

    canvas = new fabric.Canvas('fabricCanvas', {
        width: w,
        height: h,
        backgroundColor: document.getElementById('canvasBg').value || '#FFFFFF',
        selection: true,
    });

    canvas.on('selection:created', onSelectionChange);
    canvas.on('selection:updated', onSelectionChange);
    canvas.on('selection:cleared', onSelectionCleared);
    canvas.on('object:modified', saveState);
    canvas.on('object:added', function () {
        renderLayerList();
        saveState();
    });
    canvas.on('object:removed', function () {
        renderLayerList();
        saveState();
    });

    saveState();
    updateZoomUI();
    centerCanvas();

    var wrap = document.querySelector('.compose-canvas-wrap');
    if (wrap) {
        wrap.addEventListener('wheel', function (e) {
            if (e.ctrlKey || e.metaKey) {
                e.preventDefault();
                if (e.deltaY < 0) zoomIn();
                else zoomOut();
            }
        }, { passive: false });
    }
}

function centerCanvas() {
    const wrap = document.querySelector('.compose-canvas-wrap');
    const container = document.getElementById('canvasContainer');
    if (wrap && container) {
        wrap.scrollLeft = (wrap.scrollWidth - wrap.clientWidth) / 2;
        wrap.scrollTop = (wrap.scrollHeight - wrap.clientHeight) / 2;
    }
}

// ==================== 缩放 ====================
function zoomIn() {
    zoomLevel = Math.min(zoomLevel + 0.25, 3);
    applyZoom();
}

function zoomOut() {
    zoomLevel = Math.max(zoomLevel - 0.25, 0.25);
    applyZoom();
}

function zoomFit() {
    const wrap = document.querySelector('.compose-canvas-wrap');
    const container = document.getElementById('canvasContainer');
    if (!wrap || !container) return;
    const cw = canvas.getWidth();
    const ch = canvas.getHeight();
    const availW = wrap.clientWidth - 48;
    const availH = wrap.clientHeight - 48;
    zoomLevel = Math.min(availW / cw, availH / ch, 1);
    zoomLevel = Math.max(zoomLevel, 0.25);
    applyZoom();
}

function applyZoom() {
    const lower = document.querySelector('.canvas-container');
    if (lower) {
        lower.style.transform = 'scale(' + zoomLevel + ')';
        lower.style.transformOrigin = '0 0';
    }
    updateZoomUI();
}

function updateZoomUI() {
    const el = document.getElementById('zoomText');
    if (el) el.textContent = Math.round(zoomLevel * 100) + '%';
}

// ==================== 撤销/重做 ====================
function saveState() {
    if (isLoadingState) return;
    const json = canvas.toJSON(['imageId', 'layerType']);
    if (historyIndex < historyState.length - 1) {
        historyState = historyState.slice(0, historyIndex + 1);
    }
    historyState.push(JSON.stringify(json));
    if (historyState.length > MAX_HISTORY) {
        historyState.shift();
    } else {
        historyIndex = historyState.length - 1;
    }
}

function undo() {
    if (historyIndex <= 0) return;
    historyIndex--;
    loadState(historyState[historyIndex]);
}

function redo() {
    if (historyIndex >= historyState.length - 1) return;
    historyIndex++;
    loadState(historyState[historyIndex]);
}

function loadState(jsonStr) {
    try {
        isLoadingState = true;
        const data = JSON.parse(jsonStr);
        canvas.loadFromJSON(data, function () {
            canvas.renderAll();
            renderLayerList();
            isLoadingState = false;
        });
    } catch (e) {
        isLoadingState = false;
        toast('恢复状态失败', 'error');
    }
}

function setupKeyboard() {
    document.addEventListener('keydown', function (e) {
        if (e.ctrlKey || e.metaKey) {
            if (e.key === 'z') {
                e.preventDefault();
                if (e.shiftKey) redo();
                else undo();
            }
        }
    });
}

// ==================== 画布尺寸 ====================
function resizeCanvas() {
    const w = parseInt(document.getElementById('canvasW').value) || 800;
    const h = parseInt(document.getElementById('canvasH').value) || 600;
    const w2 = Math.min(Math.max(w, 1), 4096);
    const h2 = Math.min(Math.max(h, 1), 4096);
    document.getElementById('canvasW').value = w2;
    document.getElementById('canvasH').value = h2;
    canvas.setDimensions({ width: w2, height: h2 });
    canvas.setBackgroundColor(document.getElementById('canvasBg').value || '#FFFFFF', canvas.renderAll.bind(canvas));
}

function updateCanvasBg() {
    const color = document.getElementById('canvasBg').value || '#FFFFFF';
    canvas.setBackgroundColor(color, canvas.renderAll.bind(canvas));
}

// ==================== 图片选择器 ====================
let imagePickerPage = 1;

function openImagePicker() {
    document.getElementById('imagePickerModal').classList.add('active');
    imagePickerPage = 1;
    loadImagePicker();
}

function closeImagePicker() {
    document.getElementById('imagePickerModal').classList.remove('active');
}

async function loadImagePicker() {
    const grid = document.getElementById('imagePickerGrid');
    grid.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

    const res = await api('/images?page=' + imagePickerPage + '&size=20');
    if (!res || res.code !== 200) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">😕</div><div class="empty-state-text">加载失败</div></div>';
        return;
    }

    const images = (res.data && res.data.records) || [];
    if (images.length === 0) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">📷</div><div class="empty-state-text">暂无图片</div><div class="empty-state-hint">请先上传图片</div></div>';
        return;
    }

    grid.innerHTML = images.map(function (img) {
        const thumbSrc = (img.thumbnails && img.thumbnails.medium) || img.downloadUrl;
        const thumbUrl = proxyImageUrl(thumbSrc);
        return '<div class="compose-image-item" data-id="' + img.id + '" data-url="' + (proxyImageUrl(img.downloadUrl) || '') + '" onclick="addImageFromPicker(' + img.id + ',\'' + (proxyImageUrl(img.downloadUrl) || '').replace(/'/g, "\\'") + '\')">' +
            (thumbUrl ? '<img src="' + thumbUrl + '" alt="" loading="lazy">' : '<div style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;color:var(--text-muted)">🖼</div>') +
            '</div>';
    }).join('');
}

function addImageFromPicker(imageId, url) {
    if (!url) {
        toast('图片地址无效', 'error');
        return;
    }
    fabric.Image.fromURL(url, function (img) {
        if (!img) {
            toast('加载图片失败', 'error');
            return;
        }
        const cw = canvas.getWidth();
        const ch = canvas.getHeight();
        const scale = Math.min(cw / img.width, ch / img.height, 0.5) || 0.3;
        img.scale(scale);
        img.set({
            left: (cw - img.width * scale) / 2,
            top: (ch - img.height * scale) / 2,
            imageId: imageId,
            layerType: 'image',
        });
        canvas.add(img);
        canvas.setActiveObject(img);
        canvas.renderAll();
        closeImagePicker();
        toast('已添加图片', 'success');
    }, { crossOrigin: 'anonymous' });
}

// ==================== 添加文字 ====================
function addText() {
    const text = new fabric.IText('双击编辑文字', {
        left: 100,
        top: 100,
        fontSize: 32,
        fontFamily: 'Arial',
        fill: '#333333',
        layerType: 'text',
    });
    canvas.add(text);
    canvas.setActiveObject(text);
    canvas.renderAll();
    toast('已添加文字，双击可编辑', 'success');
}

// ==================== 添加形状 ====================
function addShape(shapeType) {
    let obj;
    if (shapeType === 'circle') {
        obj = new fabric.Circle({
            radius: 50,
            left: 100,
            top: 100,
            fill: '#cccccc',
            stroke: '#999999',
            strokeWidth: 1,
            layerType: 'shape',
            shape: 'circle',
        });
    } else {
        obj = new fabric.Rect({
            width: 120,
            height: 80,
            left: 100,
            top: 100,
            fill: '#cccccc',
            stroke: '#999999',
            strokeWidth: 1,
            layerType: 'shape',
            shape: 'rect',
        });
    }
    canvas.add(obj);
    canvas.setActiveObject(obj);
    canvas.renderAll();
    toast('已添加' + (shapeType === 'circle' ? '圆形' : '矩形'), 'success');
}

// ==================== 模板 ====================
async function loadTemplates() {
    const res = await api('/compose/templates');
    const select = document.getElementById('templateSelect');
    if (!res || res.code !== 200 || !select) return;

    const templates = res.data || [];
    select.innerHTML = '<option value="">选择模板</option>' + templates.map(function (t) {
        return '<option value="' + (t.id || '') + '">' + (t.name || t.id) + '</option>';
    }).join('');
}

function onTemplateSelect() {
    const select = document.getElementById('templateSelect');
    const val = select && select.value;
    if (!val) return;
    toast('模板功能需配合图片选择使用，请先添加图片', 'info');
    select.value = '';
}

// ==================== 图层列表 ====================
function renderLayerList() {
    const list = document.getElementById('layerList');
    const empty = document.getElementById('emptyLayers');
    if (!list) return;

    const objects = canvas.getObjects();
    if (objects.length === 0) {
        if (empty) empty.style.display = 'block';
        list.querySelectorAll('.compose-layer-item').forEach(function (el) { el.remove(); });
        return;
    }

    if (empty) empty.style.display = 'none';
    list.querySelectorAll('.compose-layer-item').forEach(function (el) { el.remove(); });

    const activeObj = canvas.getActiveObject();
    objects.forEach(function (obj, idx) {
        const type = obj.layerType || (obj.get ? obj.get('layerType') : '') || 'image';
        const icon = type === 'image' ? '🖼' : type === 'text' ? 'T' : '▢';
        const name = type === 'text' && obj.getText ? obj.getText().substring(0, 12) : (type === 'image' ? '图片' : type === 'circle' ? '圆形' : '矩形');

        const div = document.createElement('div');
        div.className = 'compose-layer-item' + (activeObj === obj ? ' selected' : '');
        div.setAttribute('data-idx', idx);
        div.innerHTML = '<span class="layer-icon">' + icon + '</span>' +
            '<span class="layer-name">' + (name || '图层 ' + (idx + 1)) + '</span>' +
            '<div class="layer-actions">' +
            '<button onclick="moveLayer(' + idx + ',-1)" title="上移">↑</button>' +
            '<button onclick="moveLayer(' + idx + ',1)" title="下移">↓</button>' +
            '<button onclick="deleteLayer(' + idx + ')" title="删除">×</button>' +
            '</div>';
        div.onclick = function (e) {
            if (!e.target.closest('.layer-actions')) {
                canvas.setActiveObject(obj);
                canvas.renderAll();
                renderLayerList();
                onSelectionChange({ selected: [obj] });
            }
        };
        list.appendChild(div);
    });
}

function moveLayer(idx, delta) {
    const objects = canvas.getObjects();
    const newIdx = idx + delta;
    if (newIdx < 0 || newIdx >= objects.length) return;
    const obj = objects[idx];
    canvas.remove(obj);
    canvas.insertAt(newIdx, obj);
    canvas.setActiveObject(obj);
    canvas.renderAll();
    renderLayerList();
}

function deleteLayer(idx) {
    const obj = canvas.getObjects()[idx];
    if (obj) {
        canvas.remove(obj);
        canvas.renderAll();
        renderLayerList();
        onSelectionCleared();
    }
}

// ==================== 属性面板 ====================
function onSelectionChange(e) {
    const obj = e.selected && e.selected[0];
    if (!obj) {
        onSelectionCleared();
        return;
    }
    document.getElementById('propsPanel').style.display = 'block';
    document.getElementById('emptyProps').style.display = 'none';

    const left = obj.left || 0;
    const top = obj.top || 0;
    const w = (obj.width || 0) * (obj.scaleX || 1);
    const h = (obj.height || 0) * (obj.scaleY || 1);
    const angle = obj.angle || 0;
    const opacity = Math.round((obj.opacity !== undefined ? obj.opacity : 1) * 100);

    document.getElementById('propX').value = Math.round(left);
    document.getElementById('propY').value = Math.round(top);
    document.getElementById('propW').value = Math.round(w);
    document.getElementById('propH').value = Math.round(h);
    document.getElementById('propRot').value = Math.round(angle);
    document.getElementById('propOpacity').value = opacity;

    renderLayerList();
}

function onSelectionCleared() {
    document.getElementById('propsPanel').style.display = 'none';
    document.getElementById('emptyProps').style.display = 'block';
    renderLayerList();
}

function updateProp(prop) {
    const obj = canvas.getActiveObject();
    if (!obj) return;

    const x = document.getElementById('propX');
    const y = document.getElementById('propY');
    const w = document.getElementById('propW');
    const h = document.getElementById('propH');
    const rot = document.getElementById('propRot');
    const op = document.getElementById('propOpacity');

    if (prop === 'left') obj.set('left', parseFloat(x.value) || 0);
    if (prop === 'top') obj.set('top', parseFloat(y.value) || 0);
    if (prop === 'width') {
        const scaleX = (parseFloat(w.value) || obj.width) / (obj.width || 1);
        obj.set('scaleX', scaleX);
    }
    if (prop === 'height') {
        const scaleY = (parseFloat(h.value) || obj.height) / (obj.height || 1);
        obj.set('scaleY', scaleY);
    }
    if (prop === 'angle') obj.set('angle', parseFloat(rot.value) || 0);
    if (prop === 'opacity') obj.set('opacity', (parseFloat(op.value) || 100) / 100);

    canvas.renderAll();
}

// ==================== 序列化为 Compose API 格式 ====================
function serializeToComposeRequest() {
    const w = parseInt(document.getElementById('canvasW').value) || 800;
    const h = parseInt(document.getElementById('canvasH').value) || 600;
    const bg = document.getElementById('canvasBg').value || '#FFFFFF';

    const layers = [];
    const objects = canvas.getObjects();

    objects.forEach(function (obj) {
        const type = obj.layerType || obj.get('layerType') || 'image';
        const left = obj.left || 0;
        const top = obj.top || 0;
        const scaleX = obj.scaleX || 1;
        const scaleY = obj.scaleY || 1;
        const width = Math.round((obj.width || 0) * scaleX);
        const height = Math.round((obj.height || 0) * scaleY);
        const opacity = obj.opacity !== undefined ? obj.opacity : 1;
        const angle = obj.angle || 0;

        const layer = {
            type: type,
            x: Math.round(left),
            y: Math.round(top),
            width: width,
            height: height,
            opacity: opacity,
            rotation: angle,
        };

        if (type === 'image') {
            const imageId = obj.imageId || obj.get('imageId');
            if (imageId) layer.imageId = imageId;
            layer.fit = 'cover';
            layer.borderRadius = 0;
        } else if (type === 'text') {
            layer.content = obj.getText ? obj.getText() : (obj.text || '');
            layer.fontSize = obj.fontSize || 48;
            layer.fontFamily = obj.fontFamily || 'SansSerif';
            layer.fontWeight = obj.fontWeight || 'normal';
            layer.color = obj.fill || '#333333';
            layer.maxWidth = 0;
            layer.lineHeight = 1.5;
            layer.textAlign = 'left';
        } else if (type === 'shape') {
            const shape = obj.shape || obj.get('shape') || 'rect';
            layer.shape = shape;
            layer.color = obj.fill || '#cccccc';
            layer.borderColor = obj.stroke || null;
            layer.borderWidth = obj.strokeWidth || 0;
            if (shape === 'circle') {
                const r = obj.radius || Math.min(width, height) / 2;
                layer.width = Math.round(r * 2);
                layer.height = Math.round(r * 2);
            }
        }

        layers.push(layer);
    });

    return {
        canvas: {
            width: Math.min(Math.max(w, 1), 4096),
            height: Math.min(Math.max(h, 1), 4096),
            backgroundColor: bg,
        },
        layers: layers,
        output: {
            format: 'png',
            quality: 90,
        },
    };
}

// ==================== 生成 ====================
async function generate() {
    const objects = canvas.getObjects();
    if (objects.length === 0) {
        toast('请至少添加一个图层', 'error');
        return;
    }

    const btn = document.getElementById('generateBtn');
    const origText = btn.textContent;
    btn.disabled = true;
    btn.textContent = '生成中...';

    const body = serializeToComposeRequest();

    try {
        const res = await fetch(BASE + '/compose', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Visitor-Id': getVisitorId(),
            },
            body: JSON.stringify(body),
        });

        const data = await res.json();

        if (data && data.code === 200 && data.data) {
            const d = data.data;
            const url = proxyImageUrl(d.downloadUrl);
            const fullUrl = url.startsWith('http') ? url : window.location.origin + url;
            toast('生成成功！', 'success');
            const a = document.createElement('a');
            a.href = fullUrl;
            a.download = 'composed_' + (d.imageUuid || Date.now()) + '.' + (d.format || 'png');
            a.target = '_blank';
            a.click();
        } else {
            toast('生成失败: ' + (data && data.message ? data.message : '未知错误'), 'error');
        }
    } catch (err) {
        toast('生成失败: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = origText;
    }
}
