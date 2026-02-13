#!/bin/bash
# ==========================================
# Docker + Docker Compose 安装脚本 (Linux)
# 支持: Ubuntu/Debian, CentOS/RHEL/Fedora
# ==========================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检测操作系统
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        OS_VERSION=$VERSION_ID
    elif [ -f /etc/redhat-release ]; then
        OS="centos"
    else
        log_error "无法检测操作系统，请手动安装 Docker"
        exit 1
    fi
    log_info "检测到操作系统: $OS $OS_VERSION"
}

# 检查是否以 root 或有 sudo 权限运行
check_root() {
    if [ "$EUID" -ne 0 ]; then
        if ! command -v sudo &> /dev/null; then
            log_error "请以 root 用户运行或安装 sudo"
            exit 1
        fi
        SUDO="sudo"
    else
        SUDO=""
    fi
}

# 检查 Docker 是否已安装
check_docker_installed() {
    if command -v docker &> /dev/null; then
        DOCKER_VERSION=$(docker --version 2>/dev/null | awk '{print $3}' | tr -d ',')
        log_info "Docker 已安装: $DOCKER_VERSION"
        return 0
    fi
    return 1
}

# 检查 Docker Compose 是否已安装
check_compose_installed() {
    if docker compose version &> /dev/null; then
        COMPOSE_VERSION=$(docker compose version --short 2>/dev/null)
        log_info "Docker Compose 已安装: $COMPOSE_VERSION"
        return 0
    fi
    return 1
}

# 卸载旧版本 Docker
remove_old_docker() {
    log_info "移除旧版本 Docker（如果存在）..."
    case "$OS" in
        ubuntu|debian)
            $SUDO apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true
            ;;
        centos|rhel|fedora|rocky|almalinux)
            $SUDO yum remove -y docker docker-client docker-client-latest \
                docker-common docker-latest docker-latest-logrotate \
                docker-logrotate docker-engine 2>/dev/null || true
            ;;
    esac
}

# Ubuntu/Debian 安装 Docker
install_docker_debian() {
    log_info "在 $OS 上安装 Docker..."

    # 安装依赖
    $SUDO apt-get update -y
    $SUDO apt-get install -y \
        ca-certificates \
        curl \
        gnupg \
        lsb-release

    # 添加 Docker 官方 GPG 密钥
    $SUDO install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/$OS/gpg | $SUDO gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    $SUDO chmod a+r /etc/apt/keyrings/docker.gpg

    # 添加 Docker 仓库
    echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/$OS \
        $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
        $SUDO tee /etc/apt/sources.list.d/docker.list > /dev/null

    # 安装 Docker Engine + Compose 插件
    $SUDO apt-get update -y
    $SUDO apt-get install -y \
        docker-ce \
        docker-ce-cli \
        containerd.io \
        docker-buildx-plugin \
        docker-compose-plugin

    log_info "Docker 安装完成 (Debian/Ubuntu)"
}

# CentOS/RHEL/Fedora 安装 Docker
install_docker_rhel() {
    log_info "在 $OS 上安装 Docker..."

    # 安装 yum-utils
    $SUDO yum install -y yum-utils

    # 添加 Docker 仓库
    $SUDO yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

    # 安装 Docker Engine + Compose 插件
    $SUDO yum install -y \
        docker-ce \
        docker-ce-cli \
        containerd.io \
        docker-buildx-plugin \
        docker-compose-plugin

    log_info "Docker 安装完成 (RHEL/CentOS)"
}

# Fedora 安装 Docker
install_docker_fedora() {
    log_info "在 Fedora 上安装 Docker..."

    $SUDO dnf -y install dnf-plugins-core
    $SUDO dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
    $SUDO dnf install -y \
        docker-ce \
        docker-ce-cli \
        containerd.io \
        docker-buildx-plugin \
        docker-compose-plugin

    log_info "Docker 安装完成 (Fedora)"
}

# 启动 Docker 并设置开机启动
start_docker() {
    log_info "启动 Docker 服务..."
    $SUDO systemctl start docker
    $SUDO systemctl enable docker
    log_info "Docker 服务已启动并设为开机启动"
}

# 将当前用户加入 docker 组（免 sudo 运行 docker）
setup_user_group() {
    if [ -n "$SUDO_USER" ]; then
        CURRENT_USER=$SUDO_USER
    elif [ "$EUID" -ne 0 ]; then
        CURRENT_USER=$(whoami)
    else
        CURRENT_USER=""
    fi

    if [ -n "$CURRENT_USER" ]; then
        if ! groups "$CURRENT_USER" | grep -q docker; then
            $SUDO usermod -aG docker "$CURRENT_USER"
            log_info "已将用户 '$CURRENT_USER' 加入 docker 组"
            log_warn "请重新登录或运行 'newgrp docker' 使权限生效"
        else
            log_info "用户 '$CURRENT_USER' 已在 docker 组中"
        fi
    fi
}

# 验证安装
verify_installation() {
    log_info "验证 Docker 安装..."

    if docker --version &> /dev/null; then
        log_info "Docker: $(docker --version)"
    else
        log_error "Docker 安装失败"
        exit 1
    fi

    if docker compose version &> /dev/null; then
        log_info "Docker Compose: $(docker compose version --short)"
    else
        log_error "Docker Compose 安装失败"
        exit 1
    fi

    # 运行测试容器
    if $SUDO docker run --rm hello-world &> /dev/null; then
        log_info "Docker 运行测试通过"
    else
        log_warn "Docker 运行测试失败，可能需要重新登录"
    fi
}

# ==========================================
# 主流程
# ==========================================
main() {
    echo "=========================================="
    echo "  Docker + Docker Compose 安装脚本"
    echo "  适用于: Ubuntu/Debian/CentOS/RHEL/Fedora"
    echo "=========================================="
    echo ""

    detect_os
    check_root

    # 检查是否已安装
    NEED_DOCKER=true
    NEED_COMPOSE=true

    if check_docker_installed; then
        NEED_DOCKER=false
    fi

    if check_compose_installed; then
        NEED_COMPOSE=false
    fi

    if [ "$NEED_DOCKER" = false ] && [ "$NEED_COMPOSE" = false ]; then
        log_info "Docker 和 Docker Compose 均已安装，无需操作"
        exit 0
    fi

    if [ "$NEED_DOCKER" = true ]; then
        remove_old_docker

        case "$OS" in
            ubuntu|debian)
                install_docker_debian
                ;;
            centos|rhel|rocky|almalinux)
                install_docker_rhel
                ;;
            fedora)
                install_docker_fedora
                ;;
            *)
                log_error "不支持的操作系统: $OS"
                log_error "请手动安装 Docker: https://docs.docker.com/engine/install/"
                exit 1
                ;;
        esac

        start_docker
        setup_user_group
    fi

    verify_installation

    echo ""
    log_info "=========================================="
    log_info "  安装完成！"
    log_info "=========================================="
    echo ""
}

main "$@"
