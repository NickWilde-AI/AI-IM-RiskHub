#!/bin/bash
# AI-IM-RiskHub + Guard-ML 一键启动脚本
# 用法: ./start-all.sh [--no-riskhub] [--no-simulator]

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

RISKHUB_DIR="/Users/chenpeng/WorkSpace/文稿/Tencent/TencentCodeing/AI-IM-RiskHub/ai-im-riskhub"
GUARDML_DIR="/Users/chenpeng/WorkSpace/文稿/Tencent/TencentCodeing/AI-IM-Guard-ML"
LOG_DIR="/tmp/riskhub-logs"

SKIP_RISKHUB=false
SKIP_SIMULATOR=false

for arg in "$@"; do
  case $arg in
    --no-riskhub) SKIP_RISKHUB=true ;;
    --no-simulator) SKIP_SIMULATOR=true ;;
  esac
done

# 清理旧进程
echo -e "${YELLOW}清理旧进程...${NC}"
pkill -f "im-guard serve" 2>/dev/null || true
pkill -f "im_guard_ml.simulator" 2>/dev/null || true
pkill -f "spring-boot:run.*riskhub-app" 2>/dev/null || true
sleep 1

mkdir -p "$LOG_DIR"

cleanup() {
  echo ""
  echo -e "${YELLOW}正在停止所有服务...${NC}"
  # 停模拟器
  if [ -n "$SIM_PID" ] && kill -0 "$SIM_PID" 2>/dev/null; then
    kill "$SIM_PID" 2>/dev/null
    echo -e "  ${GREEN}模拟器已停止${NC}"
  fi
  # 停 Guard-ML
  if [ -n "$GUARD_PID" ] && kill -0 "$GUARD_PID" 2>/dev/null; then
    kill "$GUARD_PID" 2>/dev/null
    echo -e "  ${GREEN}Guard-ML 已停止${NC}"
  fi
  # 停 RiskHub
  if [ -n "$RISKHUB_PID" ] && kill -0 "$RISKHUB_PID" 2>/dev/null; then
    kill "$RISKHUB_PID" 2>/dev/null
    echo -e "  ${GREEN}RiskHub 已停止${NC}"
  fi
  echo -e "${GREEN}全部停止。日志目录: ${LOG_DIR}${NC}"
  exit 0
}
trap cleanup SIGINT SIGTERM

echo ""
echo -e "${BLUE}══════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   AI-IM-RiskHub + Guard-ML 一键启动${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════${NC}"
echo ""

# ===== 1. Docker 基础设施 =====
if [ "$SKIP_RISKHUB" = false ]; then
  echo -e "${YELLOW}[1/4] 启动 Docker 基础设施 (MySQL + Redis + RocketMQ)...${NC}"
  if ! docker info >/dev/null 2>&1; then
    echo -e "${BLUE}  Docker 未运行，正在启动 Docker Desktop...${NC}"
    open -a "Docker Desktop"
    echo -n "  等待 Docker 就绪"
    for i in $(seq 1 60); do
      if docker info >/dev/null 2>&1; then
        echo ""
        break
      fi
      echo -n "."
      sleep 2
    done
    if ! docker info >/dev/null 2>&1; then
      echo -e "\n${RED}  Docker 启动超时，请手动启动后重试${NC}"
      exit 1
    fi
  fi
  cd "$RISKHUB_DIR"
  docker-compose up -d
  echo -e "${GREEN}  基础设施就绪${NC}"
  echo ""
else
  echo -e "${YELLOW}[1/4] 跳过 Docker 基础设施${NC}"
  echo ""
fi

# ===== 2. Guard-ML 服务 =====
echo -e "${YELLOW}[2/4] 启动 Guard-ML 审核服务 (端口 8000)...${NC}"
cd "$GUARDML_DIR"
source .venv/bin/activate
im-guard serve --port 8000 > "$LOG_DIR/guard-ml.log" 2>&1 &
GUARD_PID=$!
echo -e "  ${GREEN}Guard-ML PID: $GUARD_PID${NC}"

# 等待 Guard-ML 就绪
echo -n "  等待服务启动"
for i in $(seq 1 20); do
  if curl -s http://localhost:8000/health >/dev/null 2>&1; then
    echo ""
    echo -e "  ${GREEN}Guard-ML 就绪${NC}"
    break
  fi
  echo -n "."
  sleep 1
done
echo ""

# ===== 3. RiskHub Spring Boot =====
if [ "$SKIP_RISKHUB" = false ]; then
  echo -e "${YELLOW}[3/4] 启动 RiskHub Spring Boot (端口 8080)...${NC}"
  cd "$RISKHUB_DIR"
  mvn spring-boot:run -pl riskhub-app -q > "$LOG_DIR/riskhub.log" 2>&1 &
  RISKHUB_PID=$!
  echo -e "  ${GREEN}RiskHub PID: $RISKHUB_PID${NC}"
  echo -n "  等待服务启动 (首次编译较慢)"
  for i in $(seq 1 90); do
    if curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP"; then
      echo ""
      echo -e "  ${GREEN}RiskHub 就绪${NC}"
      break
    fi
    echo -n "."
    sleep 2
  done
  echo ""
else
  echo -e "${YELLOW}[3/4] 跳过 RiskHub${NC}"
  echo ""
fi

# ===== 4. 数据模拟器 =====
if [ "$SKIP_SIMULATOR" = false ]; then
  echo -e "${YELLOW}[4/4] 启动数据模拟器...${NC}"
  cd "$GUARDML_DIR"
  python -m im_guard_ml.simulator --interval 0.3 --port 8000 > "$LOG_DIR/simulator.log" 2>&1 &
  SIM_PID=$!
  echo -e "  ${GREEN}模拟器 PID: $SIM_PID${NC}"
  echo ""
else
  echo -e "${YELLOW}[4/4] 跳过模拟器${NC}"
  echo ""
fi

# ===== 启动完成 =====
echo -e "${GREEN}══════════════════════════════════════════════════${NC}"
echo -e "${GREEN}   全部启动完成！${NC}"
echo -e "${GREEN}══════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${BLUE}Guard-ML 看板:${NC}  http://localhost:8000/static/Guard-ML.html"
echo -e "  ${BLUE}Guard-ML API:${NC}   http://localhost:8000/health"
if [ "$SKIP_RISKHUB" = false ]; then
  echo -e "  ${BLUE}RiskHub  看板:${NC}  http://localhost:8080/dashboard.html"
  echo -e "  ${BLUE}RiskHub  API:${NC}   http://localhost:8080/actuator/health"
  echo -e "  ${BLUE}RiskHub Demo:${NC}   cd ai-im-riskhub && ./demo.sh"
fi
echo ""
echo -e "  ${YELLOW}日志目录:${NC} $LOG_DIR"
echo -e "    tail -f $LOG_DIR/guard-ml.log"
echo -e "    tail -f $LOG_DIR/riskhub.log"
echo -e "    tail -f $LOG_DIR/simulator.log"
echo ""
echo -e "  ${YELLOW}按 Ctrl+C 停止所有服务${NC}"
echo ""

# 保持前台运行，等待 Ctrl+C
wait
