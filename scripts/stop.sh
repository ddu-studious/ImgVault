#!/bin/bash
# ==========================================
# ImgVault v2.5.0 停止脚本
# ==========================================
# 用法:
#   ./stop.sh              # 停止所有服务
#   ./stop.sh --keep-docker  # 保留 Docker 容器
# ==========================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }

KEEP_DOCKER=false
[[ "$1" == "--keep-docker" ]] && KEEP_DOCKER=true

echo "=== 停止 ImgVault ==="

# 停止 Admin 服务
if [ -f data/imgvault-admin.pid ]; then
    PID=$(cat data/imgvault-admin.pid)
    if kill -0 "$PID" 2>/dev/null; then
        kill "$PID"
        log_info "ImgVault Admin 已停止 (PID: $PID)"
    fi
    rm -f data/imgvault-admin.pid
fi

# 停止 API 服务
if [ -f data/imgvault-api.pid ]; then
    PID=$(cat data/imgvault-api.pid)
    if kill -0 "$PID" 2>/dev/null; then
        kill "$PID"
        log_info "ImgVault API 已停止 (PID: $PID)"
    fi
    rm -f data/imgvault-api.pid
fi

# 停止 Docker 服务 (含 IOPaint)
if [ "$KEEP_DOCKER" = false ]; then
    if command -v docker &> /dev/null && docker info > /dev/null 2>&1; then
        docker compose --profile iopaint down 2>/dev/null || true
        docker compose down 2>/dev/null || true
        log_info "Docker 服务已停止 (MinIO + imgproxy + IOPaint)"
    fi
else
    log_warn "保留 Docker 容器运行 (--keep-docker)"
fi

echo "=== ImgVault 已停止 ==="
