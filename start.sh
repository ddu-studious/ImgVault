#!/bin/bash
# ==========================================
# ImgVault v2.1.0 一键启动脚本
# ==========================================

set -e

echo "=== ImgVault v2.1.0 启动 ==="

# 1. 创建数据目录
mkdir -p data

# 2. 生成 imgproxy 签名密钥（首次运行）
if [ ! -f .env ]; then
    echo "IMGPROXY_KEY=$(openssl rand -hex 32)" > .env
    echo "IMGPROXY_SALT=$(openssl rand -hex 32)" >> .env
    echo "已生成 imgproxy 签名密钥 (.env)"
fi

# 3. 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "警告: Docker 未运行，请先启动 Docker Desktop"
    echo "跳过 Docker 服务启动，仅启动 Java 服务..."
else
    # 4. 启动 Docker 服务 (MinIO + imgproxy)
    echo "启动 Docker 服务 (MinIO + imgproxy)..."
    docker compose up -d

    # 5. 等待 MinIO 就绪
    echo "等待 MinIO 启动..."
    for i in $(seq 1 30); do
        if docker compose exec -T minio mc ready local 2>/dev/null; then
            echo "MinIO 已就绪"
            break
        fi
        sleep 2
    done

    # 6. 创建 MinIO Bucket
    docker compose exec -T minio mc mb local/imgvault --ignore-existing 2>/dev/null || true
fi

# 7. 构建项目
echo "构建 ImgVault..."
mvn package -DskipTests -q

# 8. 启动 Java 服务
echo "启动 ImgVault API..."
java -jar -Xmx256m imgvault-api/target/imgvault-api-2.1.0-SNAPSHOT.jar \
    --spring.profiles.active=dev &

echo ""
echo "=== ImgVault 已启动 ==="
echo "API:       http://localhost:8080"
echo "Swagger:   http://localhost:8080/swagger-ui.html"
echo "imgproxy:  http://localhost:8081"
echo "MinIO:     http://localhost:9001"
echo "Health:    http://localhost:8080/actuator/health"
