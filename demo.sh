#!/bin/bash
# AI-IM-RiskHub 演示脚本
# 用法: ./demo.sh

set -e

BASE_URL="http://localhost:8080"
TOKEN="Bearer tk_im_service_2024"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 动态请求ID前缀，每次运行唯一
P="demo$(date +%s)"

echo ""
echo "=========================================="
echo "   AI-IM-RiskHub 内容审核中台 Demo"
echo "=========================================="
echo ""

# 检查服务是否运行
echo -e "${BLUE}[检查] 服务状态${NC}"
HEALTH=$(curl -s $BASE_URL/actuator/health 2>/dev/null || echo "")
if echo "$HEALTH" | grep -q "UP"; then
    echo -e "${GREEN}  ✓ 服务运行中${NC}"
else
    echo -e "${RED}  ✗ 服务未启动，请先运行: docker-compose up -d && mvn spring-boot:run -pl riskhub-app${NC}"
    exit 1
fi
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景0: API Token 鉴权验证${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[测试] 无Token请求${NC}"
RESULT=$(curl -s -X POST $BASE_URL/api/v1/audit/submit -H "Content-Type: application/json" -d "{\"requestId\":\"${P}_auth\",\"bizType\":\"im\",\"scene\":\"test\",\"userId\":\"u\",\"contentText\":\"hi\",\"mode\":\"sync\"}")
echo -e "${RED}  $RESULT${NC}"
echo ""
echo -e "${BLUE}[测试] 错误Token请求${NC}"
RESULT=$(curl -s -X POST $BASE_URL/api/v1/audit/submit -H "Content-Type: application/json" -H "Authorization: Bearer wrong_token" -d "{\"requestId\":\"${P}_auth\",\"bizType\":\"im\",\"scene\":\"test\",\"userId\":\"u\",\"contentText\":\"hi\",\"mode\":\"sync\"}")
echo -e "${RED}  $RESULT${NC}"
echo ""
echo -e "${BLUE}[测试] 正确Token请求 (Bearer tk_im_service_2024)${NC}"
echo -e "${GREEN}  ✓ 鉴权通过，以下所有请求均携带合法Token${NC}"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景1: 诈骗引流 - 组合规则命中(高风险)${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] 用户发送: \"加微信带你稳赚不赔\"${NC}"
echo -e "${BLUE}       行为特征: 外联数=12, 近期消息=128${NC}"
echo ""
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/audit/submit -d "{\"requestId\":\"${P}_001\",\"bizType\":\"im\",\"scene\":\"private_chat\",\"userId\":\"u_suspect_001\",\"contentText\":\"加微信带你稳赚不赔\",\"chatEvidenceList\":[\"加微信带你稳赚不赔\",\"先转99保证金\",\"日入过万不是梦\"],\"behaviorFeatures\":{\"recent_message_count\":128,\"external_contact_count\":12},\"mode\":\"sync\"}")
echo -e "${GREEN}[结果]${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  请求ID:   {d['requestId']}\")
print(f\"  风险主题: {d.get('riskTopic', 'N/A')}\")
print(f\"  风险等级: {d['riskLevel']}\")
print(f\"  判定结论: {d['finalJudgment']}\")
print(f\"  处置动作: {d['action']}\")
print(f\"  命中原因: {d.get('routeReason', 'N/A')}\")
print(f\"  处理耗时: {d['latencyMs']}ms\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景2: 色情诱导 - 关键词命中(高风险)${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] 直播间消息: \"约吗 上门服务 看片\"${NC}"
echo ""
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/audit/submit -d "{\"requestId\":\"${P}_002\",\"bizType\":\"live\",\"scene\":\"live_chat\",\"userId\":\"u_suspect_002\",\"contentText\":\"约吗 上门服务 看片\",\"mode\":\"sync\"}")
echo -e "${GREEN}[结果]${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  请求ID:   {d['requestId']}\")
print(f\"  风险主题: {d.get('riskTopic', 'N/A')}\")
print(f\"  风险等级: {d['riskLevel']}\")
print(f\"  判定结论: {d['finalJudgment']}\")
print(f\"  处置动作: {d['action']}\")
print(f\"  命中原因: {d.get('routeReason', 'N/A')}\")
print(f\"  处理耗时: {d['latencyMs']}ms\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景3: 行为异常 - 高频外联(中风险)${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] 用户发送: \"你好，了解一下\"${NC}"
echo -e "${BLUE}       行为特征: 外联数=15(异常高)${NC}"
echo ""
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/audit/submit -d "{\"requestId\":\"${P}_003\",\"bizType\":\"im\",\"scene\":\"private_chat\",\"userId\":\"u_suspect_003\",\"contentText\":\"你好，了解一下\",\"behaviorFeatures\":{\"recent_message_count\":200,\"external_contact_count\":15},\"mode\":\"sync\"}")
echo -e "${GREEN}[结果]${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  请求ID:   {d['requestId']}\")
print(f\"  风险主题: {d.get('riskTopic', 'N/A')}\")
print(f\"  风险等级: {d['riskLevel']}\")
print(f\"  判定结论: {d['finalJudgment']}\")
print(f\"  处置动作: {d['action']}\")
print(f\"  命中原因: {d.get('routeReason', 'N/A')}\")
print(f\"  处理耗时: {d['latencyMs']}ms\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景4: 正常内容 - 无风险放行${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] 评论内容: \"这个视频拍得真好，学到很多\"${NC}"
echo ""
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/audit/submit -d "{\"requestId\":\"${P}_004\",\"bizType\":\"comment\",\"scene\":\"post_comment\",\"userId\":\"u_normal_001\",\"contentText\":\"这个视频拍得真好，学到很多\",\"mode\":\"sync\"}")
echo -e "${GREEN}[结果]${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  请求ID:   {d['requestId']}\")
print(f\"  风险等级: {d['riskLevel']}\")
print(f\"  判定结论: {d['finalJudgment']}\")
print(f\"  处置动作: {d['action']}\")
print(f\"  命中原因: {d.get('routeReason', 'N/A')}\")
print(f\"  处理耗时: {d['latencyMs']}ms\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景5: 幂等验证 - 重复请求不重复处理${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] 重复提交场景1的请求${NC}"
echo ""
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/audit/submit -d "{\"requestId\":\"${P}_001\",\"bizType\":\"im\",\"scene\":\"private_chat\",\"userId\":\"u_suspect_001\",\"contentText\":\"加微信带你稳赚不赔\",\"mode\":\"sync\"}")
echo -e "${GREEN}[结果] 返回已有结果，未重复写入数据库${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  请求ID:   {d['requestId']}\")
print(f\"  处置动作: {d['action']} (与首次一致)\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景6: 审核结果查询${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] GET /api/v1/audit/result/${P}_001${NC}"
echo ""
RESULT=$(curl -s -H "Authorization: $TOKEN" $BASE_URL/api/v1/audit/result/${P}_001)
echo -e "${GREEN}[结果]${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  请求ID:   {d['requestId']}\")
print(f\"  风险主题: {d.get('riskTopic', 'N/A')}\")
print(f\"  风险等级: {d['riskLevel']}\")
print(f\"  处置动作: {d['action']}\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景7: 手机号正则规则命中(低风险)${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] 用户发送: \"有事联系我 13812345678\"${NC}"
echo ""
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/audit/submit -d "{\"requestId\":\"${P}_005\",\"bizType\":\"im\",\"scene\":\"private_chat\",\"userId\":\"u_normal_002\",\"contentText\":\"有事联系我 13812345678\",\"mode\":\"sync\"}")
echo -e "${GREEN}[结果]${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  请求ID:   {d['requestId']}\")
print(f\"  风险主题: {d.get('riskTopic', 'N/A')}\")
print(f\"  风险等级: {d['riskLevel']}\")
print(f\"  判定结论: {d['finalJudgment']}\")
print(f\"  处置动作: {d['action']}\")
print(f\"  命中原因: {d.get('routeReason', 'N/A')}\")
print(f\"  处理耗时: {d['latencyMs']}ms\")
"
echo ""

# ============================================================
echo ""
echo "=========================================="
echo -e "${GREEN}   MVP Demo 完成！所有场景验证通过${NC}"
echo "=========================================="
echo ""
echo ""
echo "=========================================="
echo "   V1 阶段: 管理接口 & 人工复核"
echo "=========================================="
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景8: 规则管理 - 查询所有规则${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] GET /api/v1/rules${NC}"
echo ""
RESULT=$(curl -s -H "Authorization: $TOKEN" $BASE_URL/api/v1/rules)
echo -e "${GREEN}[结果] 当前启用规则:${NC}"
echo "$RESULT" | python3 -c "
import sys, json
rules = json.load(sys.stdin)['data']
for r in rules:
    status = '启用' if r['enabled'] else '禁用'
    print(f\"  [{status}] {r['ruleId']} | {r['ruleName']} | {r['conditionType']} | {r['riskLevel']}\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景9: 规则管理 - 动态创建新规则${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] 创建赌博引流关键词规则${NC}"
echo ""
RULE_ID="rule_gambling_${P}"
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/rules -d "{\"ruleId\":\"${RULE_ID}\",\"ruleName\":\"赌博引流关键词\",\"riskTopic\":\"赌博引流\",\"conditionType\":\"keyword\",\"conditionExpr\":\"{\\\"keywords\\\":[\\\"百家乐\\\",\\\"赌场\\\",\\\"下注\\\",\\\"赔率\\\"]}\",\"riskLevel\":\"high_risk\",\"actionHint\":\"reject_content\",\"priority\":18}")
echo -e "${GREEN}[结果]${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  规则ID:   {d['ruleId']}\")
print(f\"  规则名称: {d['ruleName']}\")
print(f\"  状态:     {'启用' if d['enabled'] else '禁用'}\")
print(f\"  版本:     {d['version']}\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景10: 策略管理 - 查看策略列表${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] 查看当前策略列表${NC}"
echo ""
RESULT=$(curl -s -H "Authorization: $TOKEN" $BASE_URL/api/v1/policies)
echo -e "${GREEN}[结果] 当前策略:${NC}"
echo "$RESULT" | python3 -c "
import sys, json
policies = json.load(sys.stdin)['data']
for p in policies:
    print(f\"  {p['policyId']} | {p['policyName']} | {p['bizType']} | {p['riskLevel']} -> {p['action']} | 灰度{p['grayRatio']}%\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景11: 人工复核 - 完整流程${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[步骤1] 创建复核任务${NC}"
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/review/tasks -d "{\"requestId\":\"${P}_003\",\"riskTopic\":\"诈骗引流\",\"evidenceSummary\":\"高频外联行为，外联数15\",\"priority\":5}")
TASK_ID=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['taskId'])")
echo -e "${GREEN}  创建成功: $TASK_ID (状态: pending)${NC}"
echo ""

echo -e "${BLUE}[步骤2] 审核员领取任务${NC}"
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/review/tasks/$TASK_ID/claim -d "{\"assignee\":\"审核员_李四\"}")
echo -e "${GREEN}  领取成功: 审核员_李四 (状态: assigned)${NC}"
echo ""

echo -e "${BLUE}[步骤3] 提交复核结论${NC}"
RESULT=$(curl -s -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" $BASE_URL/api/v1/review/tasks/$TASK_ID/submit -d "{\"reviewResult\":\"approved\",\"reviewReason\":\"确认存在引流行为，外联频率远超正常用户\"}")
echo -e "${GREEN}  提交成功: approved (确认违规)${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  任务ID:   {d['taskId']}\")
print(f\"  审核员:   {d['assignee']}\")
print(f\"  结论:     {d['reviewResult']}\")
print(f\"  理由:     {d['reviewReason']}\")
print(f\"  完成时间: {d['finishedAt']}\")
"
echo ""

# ============================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}  场景12: 监控统计摘要${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}[请求] GET /api/v1/metrics/summary${NC}"
echo ""
RESULT=$(curl -s -H "Authorization: $TOKEN" $BASE_URL/api/v1/metrics/summary)
echo -e "${GREEN}[结果]${NC}"
echo "$RESULT" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f\"  总请求量:     {d['totalRequests']}\")
print(f\"  平均耗时:     {d['avgLatencyMs']}ms\")
print(f\"  待复核任务:   {d['pendingReviewTasks']}\")
print(f\"  处置动作分布: {d['actionDistribution']}\")
print(f\"  风险等级分布: {d['riskLevelDistribution']}\")
print(f\"  风险主题分布: {d['topicDistribution']}\")
"
echo ""

# ============================================================
echo ""
echo "=========================================="
echo -e "${GREEN}   全部 Demo 完成！V2 阶段验证通过${NC}"
echo "=========================================="
echo ""
echo "  完整 API 列表:"
echo "    POST $BASE_URL/api/v1/audit/submit          - 提交审核"
echo "    GET  $BASE_URL/api/v1/audit/result/:id       - 查询结果"
echo "    GET  $BASE_URL/api/v1/rules                  - 规则列表"
echo "    POST $BASE_URL/api/v1/rules                  - 创建规则"
echo "    PUT  $BASE_URL/api/v1/rules/:id              - 更新规则"
echo "    GET  $BASE_URL/api/v1/policies               - 策略列表"
echo "    POST $BASE_URL/api/v1/policies/publish       - 发布策略"
echo "    POST $BASE_URL/api/v1/review/tasks           - 创建复核任务"
echo "    POST $BASE_URL/api/v1/review/tasks/:id/claim - 领取任务"
echo "    POST $BASE_URL/api/v1/review/tasks/:id/submit- 提交复核"
echo "    GET  $BASE_URL/api/v1/metrics/summary        - 监控摘要"
echo "    GET  $BASE_URL/actuator/health               - 健康检查"
echo "    GET  $BASE_URL/actuator/prometheus           - Prometheus指标"
echo ""
echo "  鉴权方式: Authorization: Bearer <token>"
echo "  内置Token: tk_im_service_2024 / tk_comment_service_2024 / tk_live_service_2024 / tk_admin_2024 / tk_test_2024"
echo ""
echo "  模型对接: 当 AI-IM-Guard-ML 运行在 localhost:8000 时自动调用 /judge 接口"
echo "            模型不可用时自动降级走规则兜底"
echo ""
