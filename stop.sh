#!/bin/bash
# ==========================================
# ImgVault v2.1.0 停止脚本
# ==========================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== 停止 ImgVault ==="

# 停止 Java 服务
if [ -f data/imgvault-api.pid ]; then
    PID=$(cat data/imgvault-api.pid)
    if kill -0 "$PID" 2>/dev/null; then
        kill "$PID"
        echo "ImgVault API 已停止 (PID: $PID)"
    fi
    rm -f data/imgvault-api.pid
fi

# 停止 Docker 服务
if command -v docker &> /dev/null && docker info > /dev/null 2>&1; then
    docker compose down 2>/dev/null || true
    echo "Docker 服务已停止"
fi

echo "=== ImgVault 已停止 ==="
