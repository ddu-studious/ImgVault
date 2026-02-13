-- ==========================================
-- ImgVault v2.1.0 - SQLite 建表脚本
-- 使用 WAL 模式，零部署嵌入式数据库
-- ==========================================

-- 开启 WAL 模式和外键约束
PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA foreign_keys=ON;
PRAGMA busy_timeout=5000;

-- ==========================================
-- 图片主表
-- ==========================================
CREATE TABLE IF NOT EXISTS img_image (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    image_uuid TEXT NOT NULL UNIQUE,
    file_hash TEXT NOT NULL,
    file_md5 TEXT NOT NULL,
    original_name TEXT,
    storage_path TEXT NOT NULL,
    bucket_name TEXT NOT NULL DEFAULT 'imgvault',
    file_size INTEGER NOT NULL,
    width INTEGER,
    height INTEGER,
    format TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    color_space TEXT,
    has_alpha INTEGER DEFAULT 0,
    uploader_id INTEGER,
    upload_source TEXT,
    status INTEGER DEFAULT 1,        -- 0-删除 1-正常 2-审核中
    access_level INTEGER DEFAULT 0,   -- 0-公开 1-私有 2-受限
    view_count INTEGER DEFAULT 0,
    description TEXT,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now')),
    deleted_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_image_hash ON img_image(file_hash);
CREATE INDEX IF NOT EXISTS idx_image_md5 ON img_image(file_md5);
CREATE INDEX IF NOT EXISTS idx_image_uploader ON img_image(uploader_id);
CREATE INDEX IF NOT EXISTS idx_image_status ON img_image(status);
CREATE INDEX IF NOT EXISTS idx_image_created ON img_image(created_at);
CREATE INDEX IF NOT EXISTS idx_image_format ON img_image(format);
CREATE UNIQUE INDEX IF NOT EXISTS idx_image_uuid ON img_image(image_uuid);

-- 更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_image_timestamp
    AFTER UPDATE ON img_image
    FOR EACH ROW
BEGIN
    UPDATE img_image SET updated_at = datetime('now') WHERE id = NEW.id;
END;

-- ==========================================
-- 图片 EXIF 元数据表
-- ==========================================
CREATE TABLE IF NOT EXISTS img_metadata (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    image_id INTEGER NOT NULL UNIQUE,
    camera_make TEXT,
    camera_model TEXT,
    lens_model TEXT,
    focal_length TEXT,
    aperture TEXT,
    shutter_speed TEXT,
    iso INTEGER,
    taken_at TEXT,
    gps_latitude REAL,
    gps_longitude REAL,
    orientation INTEGER,
    raw_exif TEXT,                    -- JSON 字符串
    created_at TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (image_id) REFERENCES img_image(id)
);

CREATE INDEX IF NOT EXISTS idx_metadata_taken ON img_metadata(taken_at);

-- ==========================================
-- 标签表
-- ==========================================
CREATE TABLE IF NOT EXISTS img_tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    usage_count INTEGER DEFAULT 0,
    created_at TEXT DEFAULT (datetime('now'))
);

-- ==========================================
-- 图片-标签关联表
-- ==========================================
CREATE TABLE IF NOT EXISTS img_image_tag (
    image_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    created_at TEXT DEFAULT (datetime('now')),
    PRIMARY KEY (image_id, tag_id),
    FOREIGN KEY (image_id) REFERENCES img_image(id),
    FOREIGN KEY (tag_id) REFERENCES img_tag(id)
);

CREATE INDEX IF NOT EXISTS idx_image_tag_tag ON img_image_tag(tag_id);

-- ==========================================
-- 相册表
-- ==========================================
CREATE TABLE IF NOT EXISTS img_album (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    cover_image_id INTEGER,
    owner_id INTEGER NOT NULL,
    image_count INTEGER DEFAULT 0,
    access_level INTEGER DEFAULT 0,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_album_owner ON img_album(owner_id);

-- ==========================================
-- 相册-图片关联表
-- ==========================================
CREATE TABLE IF NOT EXISTS img_album_image (
    album_id INTEGER NOT NULL,
    image_id INTEGER NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TEXT DEFAULT (datetime('now')),
    PRIMARY KEY (album_id, image_id),
    FOREIGN KEY (album_id) REFERENCES img_album(id),
    FOREIGN KEY (image_id) REFERENCES img_image(id)
);

CREATE INDEX IF NOT EXISTS idx_album_image_image ON img_album_image(image_id);

-- ==========================================
-- 文件指纹表（秒传用）
-- ==========================================
CREATE TABLE IF NOT EXISTS file_fingerprint (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_hash TEXT NOT NULL UNIQUE,
    file_md5 TEXT NOT NULL,
    storage_path TEXT NOT NULL,
    file_size INTEGER NOT NULL,
    ref_count INTEGER DEFAULT 1,
    created_at TEXT DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_fingerprint_hash ON file_fingerprint(file_hash);
CREATE INDEX IF NOT EXISTS idx_fingerprint_md5 ON file_fingerprint(file_md5);

-- ==========================================
-- 上传任务表（分片上传）
-- ==========================================
CREATE TABLE IF NOT EXISTS img_upload_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    upload_id TEXT NOT NULL UNIQUE,
    file_hash TEXT,
    file_name TEXT,
    file_size INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    total_chunks INTEGER NOT NULL,
    uploaded_chunks INTEGER DEFAULT 0,
    chunk_status TEXT,                -- JSON: 记录每个分片的状态
    status INTEGER DEFAULT 0,         -- 0-上传中 1-合并中 2-已完成 3-失败 4-过期
    uploader_id INTEGER,
    expires_at TEXT NOT NULL,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_upload_task_id ON img_upload_task(upload_id);
CREATE INDEX IF NOT EXISTS idx_upload_task_status ON img_upload_task(status);
CREATE INDEX IF NOT EXISTS idx_upload_task_expires ON img_upload_task(expires_at);

-- ==========================================
-- 异步任务表（替代 RocketMQ）
-- ==========================================
CREATE TABLE IF NOT EXISTS img_async_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_type TEXT NOT NULL,           -- thumbnail/convert/watermark/exif
    image_id INTEGER NOT NULL,
    payload TEXT,                      -- JSON 任务参数
    status INTEGER DEFAULT 0,          -- 0-待处理 1-处理中 2-完成 3-失败
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_message TEXT,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now')),
    executed_at TEXT,
    FOREIGN KEY (image_id) REFERENCES img_image(id)
);

CREATE INDEX IF NOT EXISTS idx_async_task_status ON img_async_task(status);
CREATE INDEX IF NOT EXISTS idx_async_task_type ON img_async_task(task_type, status);

-- ==========================================
-- 操作日志表
-- ==========================================
CREATE TABLE IF NOT EXISTS img_operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id INTEGER,
    operation_type TEXT NOT NULL,       -- upload/delete/update/download
    target_type TEXT NOT NULL,          -- image/album/tag
    target_id INTEGER,
    detail TEXT,                        -- JSON 操作详情
    ip_address TEXT,
    created_at TEXT DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_log_operator ON img_operation_log(operator_id);
CREATE INDEX IF NOT EXISTS idx_log_created ON img_operation_log(created_at);
