#!/bin/bash
# ============================================================
# 华兴服装店 - POS 系统启动脚本 (macOS / Linux)
# ============================================================
# 使用方法:
#   chmod +x start.sh && ./start.sh
# ============================================================

JAR_FILE="clothing-pos-1.0.0.jar"
JAVA_OPTS="-Xms256m -Xmx512m"

if [ ! -f "$JAR_FILE" ]; then
    echo "[错误] 未找到 $JAR_FILE"
    echo "[提示] 请先执行构建: cd frontend && npm run build && cd .. && mvn clean package -DskipTests"
    exit 1
fi

echo "========================================"
echo "  华兴服装店 POS 系统 v1.0.0"
echo "  启动中..."
echo "========================================"
echo "  JVM 参数: $JAVA_OPTS"
echo "  JAR 文件: $JAR_FILE"
echo "  访问地址: http://localhost:8080"
echo "========================================"

java $JAVA_OPTS -jar "$JAR_FILE"
