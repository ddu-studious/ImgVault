#!/bin/bash
# ==========================================
# IOPaint AI 去水印服务管理脚本
# ==========================================
# 用法:
#   ./iopaint.sh start    # 启动 IOPaint
#   ./iopaint.sh stop     # 停止 IOPaint
#   ./iopaint.sh restart  # 重启 IOPaint
#   ./iopaint.sh status   # 查看状态
#   ./iopaint.sh logs     # 查看日志
# ==========================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

ACTION="${1:-help}"

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装"
        exit 1
    fi
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker 未运行"
        exit 1
    fi
}

case "$ACTION" in
    start)
        check_docker
        echo "=== 启动 IOPaint AI 去水印服务 ==="
        log_info "首次启动需下载 LaMa 模型 (~200MB)，请耐心等待..."
        docker compose --profile iopaint up -d iopaint
        
        log_info "等待 IOPaint 服务就绪..."
        MAX_WAIT=120
        WAITED=0
        while [ $WAITED -lt $MAX_WAIT ]; do
            if curl -s -o /dev/null -w "%{http_code}" http://localhost:8085/ 2>/dev/null | grep -q "200\|302\|404"; then
                log_info "IOPaint 已就绪"
                break
            fi
            sleep 3
            WAITED=$((WAITED + 3))
            echo -ne "\r  等待中... ${WAITED}s / ${MAX_WAIT}s"
        done
        echo ""

        if [ $WAITED -ge $MAX_WAIT ]; then
            log_warn "IOPaint 启动超时，可能仍在下载模型"
            log_warn "请运行 './iopaint.sh logs' 查看详细日志"
        else
            echo ""
            log_info "IOPaint AI 去水印服务已启动"
            echo ""
            echo "  访问地址: http://localhost:8085"
            echo "  API 端点: http://localhost:8085/api/v1/inpaint"
            echo ""
            echo "  ImgVault 去水印工具: http://localhost:8080/watermark.html"
            echo "  选择 'IOPaint AI 高质量' 引擎即可使用"
        fi
        ;;

    stop)
        check_docker
        echo "=== 停止 IOPaint ==="
        docker compose --profile iopaint stop iopaint 2>/dev/null || true
        docker compose --profile iopaint rm -f iopaint 2>/dev/null || true
        log_info "IOPaint 已停止"
        ;;

    restart)
        check_docker
        echo "=== 重启 IOPaint ==="
        docker compose --profile iopaint restart iopaint
        log_info "IOPaint 已重启"
        ;;

    status)
        check_docker
        echo "=== IOPaint 服务状态 ==="
        if docker ps --filter "name=imgvault-iopaint" --format "{{.Status}}" 2>/dev/null | grep -q "Up"; then
            log_info "IOPaint: 运行中"
            docker ps --filter "name=imgvault-iopaint" --format "  容器: {{.Names}}  状态: {{.Status}}  端口: {{.Ports}}"
            
            if curl -s -o /dev/null -w "%{http_code}" http://localhost:8085/ 2>/dev/null | grep -q "200\|302\|404"; then
                log_info "HTTP 健康检查: 正常"
            else
                log_warn "HTTP 健康检查: 服务可能仍在初始化"
            fi
        else
            log_warn "IOPaint: 未运行"
            echo "  启动: ./iopaint.sh start"
        fi
        ;;

    logs)
        check_docker
        docker compose --profile iopaint logs -f --tail=100 iopaint
        ;;

    help|*)
        echo "IOPaint AI 去水印服务管理脚本"
        echo ""
        echo "用法: ./iopaint.sh <命令>"
        echo ""
        echo "命令:"
        echo "  start     启动 IOPaint 服务 (端口 8085)"
        echo "  stop      停止 IOPaint 服务"
        echo "  restart   重启 IOPaint 服务"
        echo "  status    查看 IOPaint 运行状态"
        echo "  logs      查看 IOPaint 实时日志"
        echo "  help      显示此帮助信息"
        echo ""
        echo "说明:"
        echo "  IOPaint 是基于 LaMa 模型的 AI 图像修复工具"
        echo "  适合处理复杂水印、大面积遮挡等场景"
        echo "  首次启动需下载模型文件 (~200MB)"
        echo "  CPU 模式下处理一张图约 5-15 秒"
        echo "  GPU 模式下处理一张图约 1-3 秒"
        echo ""
        echo "  ImgVault 去水印工具中选择 'IOPaint AI 高质量' 引擎即可使用"
        ;;
esac
