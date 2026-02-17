-- ==========================================
-- ImgVault v2.4.0 迁移脚本 - 访客隔离
-- 为 img_image 表新增 visitor_id 列
-- ==========================================

-- 新增访客ID列（UUID 格式，可为空）
ALTER TABLE img_image ADD COLUMN visitor_id TEXT;

-- 创建索引加速按访客查询
CREATE INDEX IF NOT EXISTS idx_image_visitor ON img_image(visitor_id);
