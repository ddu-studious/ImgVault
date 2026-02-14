#!/bin/bash
# ==========================================
# ImgVault v2.1.0 一键启动脚本
# ==========================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  ImgVault v2.1.0 启动"
echo "=========================================="
echo ""

# ==========================================
# 1. 检查 Java 环境
# ==========================================
if ! command -v java &> /dev/null; then
    log_error "未检测到 Java，请先安装 JDK 8+"
    log_error "  Ubuntu: sudo apt install openjdk-8-jdk"
    log_error "  CentOS: sudo yum install java-1.8.0-openjdk-devel"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1)
log_info "Java: $JAVA_VERSION"

# ==========================================
# 2. 检查 Maven
# ==========================================
if ! command -v mvn &> /dev/null; then
    log_error "未检测到 Maven，请先安装 Maven 3.6+"
    log_error "  Ubuntu: sudo apt install maven"
    log_error "  CentOS: sudo yum install maven"
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
docker compose up -d

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
log_info "构建 ImgVault..."
mvn package -DskipTests -q
log_info "构建完成"

# ==========================================
# 8. 启动 Java 服务
# ==========================================
log_info "启动 ImgVault API..."
java -jar -Xmx256m imgvault-api/target/imgvault-api-2.1.0-SNAPSHOT.jar \
    --spring.profiles.active=dev &

API_PID=$!
echo $API_PID > data/imgvault-api.pid
sleep 5

# 检查服务是否启动
if kill -0 $API_PID 2>/dev/null; then
    log_info "ImgVault API 已启动 (PID: $API_PID)"
else
    log_error "ImgVault API 启动失败"
    exit 1
fi

echo ""
echo "=========================================="
log_info "ImgVault v2.1.0 已启动"
echo "=========================================="
echo ""
echo "  API:       http://localhost:8080"
echo "  Swagger:   http://localhost:8080/swagger-ui.html"
echo "  Health:    http://localhost:8080/actuator/health"
echo "  imgproxy:  http://localhost:8081"
echo "  MinIO:     http://localhost:9001  (admin/minioadmin)"
echo ""
echo "  停止服务: ./stop.sh"
echo ""
