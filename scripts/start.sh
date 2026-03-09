#!/bin/bash
# ==========================================
# ImgVault v2.1.0 一键启动脚本
# ==========================================
# 用法:
#   ./start.sh              # 完整启动 (编译 + Docker + API + Admin)
#   ./start.sh --skip-build # 跳过编译，直接启动
#   ./start.sh --api-only   # 仅启动 API 服务
#   ./start.sh --help       # 显示帮助
# ==========================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "${CYAN}[STEP]${NC} $1"; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

SKIP_BUILD=false
API_ONLY=false
WITH_IOPAINT=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build)   SKIP_BUILD=true; shift ;;
        --api-only)     API_ONLY=true; shift ;;
        --with-iopaint) WITH_IOPAINT=true; shift ;;
        --help|-h)
            echo "ImgVault v2.5.0 一键启动脚本"
            echo ""
            echo "用法: ./start.sh [选项]"
            echo ""
            echo "选项:"
            echo "  --skip-build     跳过 Maven 编译，直接启动"
            echo "  --api-only       仅启动 API 服务 (不启动 Admin)"
            echo "  --with-iopaint   同时启动 IOPaint AI 去水印服务 (端口 8085)"
            echo "  --help           显示此帮助信息"
            echo ""
            echo "说明:"
            echo "  首次运行会自动检查并安装 Docker (仅 Linux)"
            echo "  启动顺序: Docker(MinIO+imgproxy) → Maven 编译 → API(8080) → Admin(8082)"
            echo "  IOPaint 为可选服务，首次启动需下载模型 (~200MB)，建议有 GPU 环境时使用"
            exit 0
            ;;
        *) log_error "未知选项: $1"; exit 1 ;;
    esac
done

echo "=========================================="
echo "  ImgVault v2.5.0 启动"
echo "=========================================="
echo ""

# ==========================================
# 1. 检查 Java 环境
# ==========================================
log_step "检查 Java 环境..."
if ! command -v java &> /dev/null; then
    log_error "未检测到 Java，请先安装 JDK 8+"
    echo ""
    echo "  安装方式:"
    echo "    Ubuntu/Debian: sudo apt install openjdk-8-jdk"
    echo "    CentOS/RHEL:   sudo yum install java-1.8.0-openjdk-devel"
    echo "    macOS:          brew install openjdk@8"
    echo "    手动安装:       https://adoptium.net/"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1)
log_info "Java: $JAVA_VERSION"

# ==========================================
# 2. 检查 Maven
# ==========================================
log_step "检查 Maven 环境..."
if ! command -v mvn &> /dev/null; then
    log_error "未检测到 Maven，请先安装 Maven 3.6+"
    echo ""
    echo "  安装方式:"
    echo "    Ubuntu/Debian: sudo apt install maven"
    echo "    CentOS/RHEL:   sudo yum install maven"
    echo "    macOS:          brew install maven"
    echo "    手动安装:       https://maven.apache.org/download.cgi"
    exit 1
fi
log_info "Maven: $(mvn --version 2>&1 | head -1)"

# ==========================================
# 3. 检查并安装 Docker
# ==========================================
if ! command -v docker &> /dev/null; then
    log_warn "未检测到 Docker，开始自动安装..."

    # 检测操作系统
    OS_TYPE="$(uname -s)"
    case "$OS_TYPE" in
        Linux)
            if [ -f "$SCRIPT_DIR/scripts/install-docker.sh" ]; then
                chmod +x "$SCRIPT_DIR/scripts/install-docker.sh"
                bash "$SCRIPT_DIR/scripts/install-docker.sh"
            else
                log_error "Docker 安装脚本不存在: scripts/install-docker.sh"
                log_error "请手动安装 Docker: https://docs.docker.com/engine/install/"
                exit 1
            fi
            ;;
        Darwin)
            log_error "macOS 请手动安装 Docker Desktop: https://docs.docker.com/desktop/install/mac-install/"
            log_error "安装后请启动 Docker Desktop 再运行此脚本"
            exit 1
            ;;
        *)
            log_error "不支持的操作系统: $OS_TYPE"
            log_error "请手动安装 Docker: https://docs.docker.com/engine/install/"
            exit 1
            ;;
    esac
fi

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    log_warn "Docker 未运行，尝试启动..."
    OS_TYPE="$(uname -s)"
    if [ "$OS_TYPE" = "Linux" ]; then
        sudo systemctl start docker 2>/dev/null || true
        sleep 3
    fi

    if ! docker info > /dev/null 2>&1; then
        log_error "Docker 未运行，请手动启动 Docker"
        log_error "  Linux: sudo systemctl start docker"
        log_error "  macOS: 启动 Docker Desktop"
        exit 1
    fi
fi
log_info "Docker: $(docker --version)"

# ==========================================
# 4. 创建数据目录
# ==========================================
mkdir -p data
log_info "数据目录已就绪: data/"

# ==========================================
# 5. 生成 imgproxy 签名密钥（首次运行）
# ==========================================
if [ ! -f .env ]; then
    IMGPROXY_KEY=$(openssl rand -hex 32)
    IMGPROXY_SALT=$(openssl rand -hex 32)
    echo "IMGPROXY_KEY=$IMGPROXY_KEY" > .env
    echo "IMGPROXY_SALT=$IMGPROXY_SALT" >> .env
    log_info "已生成 imgproxy 签名密钥 (.env)"

    # 同步更新 application.yml 中的 key/salt
    log_warn "请将以下密钥更新到 application.yml 的 imgproxy 配置中:"
    echo "  imgproxy.key: $IMGPROXY_KEY"
    echo "  imgproxy.salt: $IMGPROXY_SALT"
else
    log_info "imgproxy 签名密钥已存在 (.env)"
fi

# ==========================================
# 6. 启动 Docker 服务 (MinIO + imgproxy)
# ==========================================
log_info "启动 Docker 服务 (MinIO + imgproxy)..."
if [ "$WITH_IOPAINT" = true ]; then
    log_info "同时启动 IOPaint AI 去水印服务..."
    docker compose --profile iopaint up -d
else
    docker compose up -d
fi

# 等待 MinIO 就绪
log_info "等待 MinIO 启动..."
MAX_WAIT=60
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if docker compose exec -T minio mc ready local 2>/dev/null; then
        log_info "MinIO 已就绪"
        break
    fi
    sleep 2
    WAITED=$((WAITED + 2))
done

if [ $WAITED -ge $MAX_WAIT ]; then
    log_warn "MinIO 启动超时，继续执行..."
fi

# 创建 MinIO Bucket
docker compose exec -T minio mc mb local/imgvault --ignore-existing 2>/dev/null || true
log_info "MinIO Bucket 'imgvault' 已就绪"

# ==========================================
# 7. 构建项目
# ==========================================
if [ "$SKIP_BUILD" = true ]; then
    log_info "跳过 Maven 编译 (--skip-build)"
    if [ ! -f "imgvault-api/target/imgvault-api-2.1.0-SNAPSHOT.jar" ]; then
        log_error "API JAR 不存在，请先编译: mvn package -DskipTests"
        exit 1
    fi
else
    log_step "构建 ImgVault (Maven)..."
    mvn package -DskipTests -q
    log_info "构建完成"
fi

# ==========================================
# 8. 初始化数据库
# ==========================================
if [ ! -f "data/imgvault.db" ]; then
    log_step "初始化数据库..."
    if command -v sqlite3 &> /dev/null && [ -f "sql/schema.sql" ]; then
        sqlite3 data/imgvault.db < sql/schema.sql
        log_info "数据库已初始化"
    else
        log_info "数据库将在首次启动时自动创建"
    fi
fi

# ==========================================
# 9. 启动 API 服务 (端口 8080)
# ==========================================
log_step "启动 ImgVault API (端口 8080)..."

# 停止已有进程
if [ -f data/imgvault-api.pid ]; then
    OLD_PID=$(cat data/imgvault-api.pid)
    if kill -0 "$OLD_PID" 2>/dev/null; then
        log_warn "检测到已运行的 API 进程 (PID: $OLD_PID)，先停止..."
        kill "$OLD_PID" 2>/dev/null || true
        sleep 2
    fi
    rm -f data/imgvault-api.pid
fi

java -jar -Xmx256m imgvault-api/target/imgvault-api-2.1.0-SNAPSHOT.jar \
    --spring.profiles.active=dev &

API_PID=$!
echo $API_PID > data/imgvault-api.pid
sleep 5

if kill -0 $API_PID 2>/dev/null; then
    log_info "ImgVault API 已启动 (PID: $API_PID)"
else
    log_error "ImgVault API 启动失败，请检查日志"
    exit 1
fi

# ==========================================
# 10. 启动 Admin 服务 (端口 8082)
# ==========================================
if [ "$API_ONLY" = false ]; then
    ADMIN_JAR="imgvault-admin/target/imgvault-admin-2.1.0-SNAPSHOT.jar"
    if [ -f "$ADMIN_JAR" ]; then
        log_step "启动 ImgVault Admin (端口 8082)..."

        if [ -f data/imgvault-admin.pid ]; then
            OLD_PID=$(cat data/imgvault-admin.pid)
            if kill -0 "$OLD_PID" 2>/dev/null; then
                kill "$OLD_PID" 2>/dev/null || true
                sleep 2
            fi
            rm -f data/imgvault-admin.pid
        fi

        java -jar -Xmx128m "$ADMIN_JAR" \
            --spring.profiles.active=dev &

        ADMIN_PID=$!
        echo $ADMIN_PID > data/imgvault-admin.pid
        sleep 3

        if kill -0 $ADMIN_PID 2>/dev/null; then
            log_info "ImgVault Admin 已启动 (PID: $ADMIN_PID)"
        else
            log_warn "ImgVault Admin 启动失败（非致命，API 仍正常运行）"
        fi
    else
        log_warn "Admin JAR 未找到: $ADMIN_JAR，跳过 Admin 启动"
    fi
fi

echo ""
echo "=========================================="
log_info "ImgVault v2.5.0 已启动"
echo "=========================================="
echo ""
echo "  服务地址:"
echo "    API:          http://localhost:8080"
echo "    Admin:        http://localhost:8082"
echo "    Swagger:      http://localhost:8080/swagger-ui.html"
echo "    Health:       http://localhost:8080/actuator/health"
echo "    合成编辑器:   http://localhost:8080/compose.html"
echo "    去水印工具:   http://localhost:8080/watermark.html"
echo ""
echo "  基础设施:"
echo "    imgproxy:     http://localhost:8081"
echo "    MinIO:        http://localhost:9001  (admin/minioadmin)"
if [ "$WITH_IOPAINT" = true ]; then
echo "    IOPaint:      http://localhost:8085  (AI 去水印)"
fi
echo ""
echo "  管理后台密码: imgvault-admin (可在 application.yml 修改)"
echo ""
echo "  提示:"
echo "    停止服务:         ./stop.sh"
echo "    单独启动 IOPaint: ./iopaint.sh start"
echo ""
