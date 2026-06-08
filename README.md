<div align="center">

# AI-IM-RiskHub

**高并发内容审核与风控中台 — AI 审核系统的企业级运行底座**

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Cache-Redis%207-red)](https://redis.io/)
[![RocketMQ](https://img.shields.io/badge/MQ-RocketMQ%205-blue)](https://rocketmq.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[快速开始](#快速开始) · [系统架构](#系统架构) · [请求流程](#请求流程) · [高并发设计](#高并发设计) · [API 文档](#api-文档) · [与 Guard-ML 的关系](#与-ai-im-guard-ml-的关系)

</div>

---

AI-IM-RiskHub 是一个面向 IM 场景的内容审核风控中台。它不做内容理解，不做语义分析——这些交给上游的 [AI-IM-Guard-ML](https://github.com/NickWilde-AI/AI-IM-Guard-ML) 来做。RiskHub 负责的是：把 AI 审核能力包装成一个可以在生产环境跑起来的系统。

## 为什么需要中台

AI 模型能判断一条消息是否违规，但把它放到生产环境里跑，你还需要解决：

- 同一条消息被重复提交怎么办？（幂等）
- 高峰期每秒上万条请求，AI 来不及处理怎么办？（异步队列 + 削峰）
- AI 服务挂了怎么办？（降级兜底）
- 新模型上线效果不确定怎么办？（灰度发布）
- 出了误封用户投诉怎么办？（审计追溯 + 人工复核）
- 运营想临时加一条紧急规则怎么办？（动态规则热更新）
- 不同业务线（IM、评论、直播）策略不同怎么办？（多业务策略路由）

RiskHub 就是解决这些问题的。它是 AI 审核的运行底座，让模型能安全、可控、可追溯地在线上跑起来。

## 项目亮点

| 能力 | 实现方式 |
| --- | --- |
| 异步审核队列 | RocketMQ 削峰填谷，请求先入队立即返回 |
| 幂等去重 | Redis SETNX + 1h TTL，防止重复处理 |
| AI 模型对接 | HTTP 调用 Guard-ML `/judge`，超时 3s 自动降级 |
| 规则引擎 | 关键词/正则/行为阈值/组合规则，动态增删，毫秒级匹配 |
| 策略灰度 | 按百分比灰度发布新策略，观察无误再全量 |
| 人工复核 | 任务创建→领取→提交，完整工作流 |
| 审计追溯 | 每条决策记录规则版本、策略版本、模型版本，可回查 |
| 多业务接入 | im/comment/live/register/login/payment 统一接入 |
| 可观测性 | Spring Boot Actuator + Prometheus + Micrometer |

## 当前状态

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| 审核主链路 | 已完成 | 同步/异步提交、结果查询、幂等校验 |
| 规则引擎 | 已完成 | 5 种规则类型、动态 CRUD、优先级排序 |
| 策略路由 | 已完成 | 多业务线策略、灰度比例、自动降级 |
| AI 模型对接 | 已完成 | Guard-ML 集成、超时降级、shadow-mode |
| 人工复核 | 已完成 | 任务创建、领取、提交、状态流转 |
| 监控指标 | 已完成 | Actuator + Prometheus 埋点 |
| 消息队列 | 已完成 | RocketMQ 异步处理 + 降级兜底 |

## 系统架构

```text
┌──────────────────────────────────────────────────────────────────────┐
│                         业务方 (IM / 评论 / 直播)                      │
└───────────────────────────────────┬──────────────────────────────────┘
                                    │ POST /api/v1/audit/submit
                                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        RiskHub 风控中台 (:8080)                        │
│                                                                      │
│  ┌─────────┐   ┌──────────┐   ┌──────────┐   ┌─────────────────┐   │
│  │ 鉴权限流 │ → │ 幂等校验  │ → │ 规则引擎  │ → │ AI 模型调用      │   │
│  │ (Filter) │   │ (Redis)  │   │ (Engine) │   │ (Model-Adapter) │   │
│  └─────────┘   └──────────┘   └──────────┘   └────────┬────────┘   │
│                                                         │            │
│                                                         ▼            │
│  ┌─────────────────┐   ┌──────────────┐   ┌───────────────────┐    │
│  │ 策略路由 + 灰度   │ ← │ 结果聚合      │ ← │ Guard-ML 返回     │    │
│  │ (PolicyRouter)  │   │              │   │ (riskLevel/topic) │    │
│  └────────┬────────┘   └──────────────┘   └───────────────────┘    │
│           │                                                          │
│           ▼                                                          │
│  ┌─────────────────┐   ┌──────────────┐   ┌───────────────────┐    │
│  │ 持久化 (MySQL)   │   │ 人工复核队列  │   │ 审计日志          │    │
│  └─────────────────┘   └──────────────┘   └───────────────────┘    │
└──────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ POST /judge (HTTP)
                                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    AI-IM-Guard-ML (:8000)                             │
│         语义理解 · 多证据融合 · 风险分级 · 主题识别                      │
└──────────────────────────────────────────────────────────────────────┘
```

## 请求流程

一条审核请求从进入到返回结果的完整路径：

```text
1. 请求进入
   ├─ Token 鉴权（Bearer 认证）
   └─ 请求体校验

2. 幂等检查
   └─ Redis SETNX(requestId, 1h)
      ├─ 已存在 → 返回已有结果
      └─ 不存在 → 继续处理

3. 执行模式分流
   ├─ sync  → 立即执行完整链路，等待结果
   └─ async → 投递 RocketMQ，立即返回 "accepted"
              └─ 消费者异步执行完整链路

4. 完整链路
   ├─ 规则引擎：加载启用规则，按优先级依次匹配
   │   ├─ keyword    : 关键词命中
   │   ├─ regex      : 正则匹配
   │   ├─ behavior   : 行为阈值判定
   │   └─ composite  : AND/OR 组合条件
   │
   ├─ AI 模型调用：POST Guard-ML /judge
   │   ├─ 成功 + 置信度 >= 0.8 → 模型结果加入命中列表
   │   └─ 超时/失败 → 降级，仅用规则结果
   │
   └─ 策略路由：取最高优先级命中
       ├─ 查询匹配的 policy_config
       ├─ 应用灰度比例
       └─ 输出最终动作 + 判定结论

5. 持久化 + 返回
   ├─ 写入 audit_request + audit_result
   ├─ 如需人工复核 → 创建 review_task
   └─ 返回: riskLevel / finalJudgment / action / latencyMs
```

## 高并发设计

| 手段 | 解决什么问题 | 实现 |
| --- | --- | --- |
| Redis 幂等锁 | 重复请求不重复处理 | SETNX + 1h TTL |
| RocketMQ 异步 | 高峰期不阻塞调用方 | 先入队返回 accepted，后台消费 |
| AI 超时降级 | 模型慢或挂了不拖垮主链路 | 3s connect timeout，失败走规则兜底 |
| 规则引擎内存匹配 | 毫秒级快速拦截明确违规 | 加载到内存，不走网络 |
| 策略灰度 | 新策略逐步放量，控制爆炸半径 | gray_ratio 百分比随机命中 |
| 无状态设计 | 水平扩容 | 多实例部署，共享 MySQL + Redis |
| 数据库索引 | 高效查询 | request_id 唯一索引，组合索引覆盖查询 |

## 技术栈

| 层 | 技术 |
| --- | --- |
| 应用框架 | Java 17 + Spring Boot 3.2 |
| 数据持久化 | MySQL 8.0 + MyBatis-Plus 3.5 |
| 缓存/限流/幂等 | Redis 7 + Redisson 3.27 |
| 消息队列 | Apache RocketMQ 5.1 |
| 可观测性 | Spring Boot Actuator + Micrometer + Prometheus |
| 容器化 | Docker Compose |

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.9+
- Docker Desktop

### 一键启动（推荐）

项目根目录提供了一键启动脚本，同时拉起所有服务：

```bash
./start-all.sh
```

启动后自动打开：
- Guard-ML 审核看板: http://localhost:8000/static/dashboard.html
- RiskHub API: http://localhost:8080/actuator/health

可选参数：
- `--no-riskhub`: 只启动 Guard-ML（看板演示）
- `--no-simulator`: 不启动数据模拟器

### 手动启动

```bash
# 1. 启动基础设施
cd ai-im-riskhub
docker-compose up -d

# 2. 编译项目
mvn clean install -DskipTests

# 3. 启动应用
mvn spring-boot:run -pl riskhub-app

# 4. 验证
curl http://localhost:8080/actuator/health
```

### 运行演示

```bash
chmod +x demo.sh
./demo.sh
```

演示脚本覆盖：诈骗引流、色情诱导、行为异常、正常放行、幂等验证、规则管理、策略管理、人工复核等完整场景。

## 项目结构

```text
ai-im-riskhub/
├── riskhub-common          # 公共模块：DTO、枚举、异常、鉴权 Filter
├── riskhub-store           # 数据层：实体、Mapper、MyBatis-Plus
├── riskhub-engine          # 规则引擎 + 策略路由
├── riskhub-model-adapter   # AI 模型服务调用层（对接 Guard-ML）
├── riskhub-api             # REST 接口 + Service 编排层
├── riskhub-app             # Spring Boot 启动模块
├── sql/                    # 数据库初始化脚本（4 张核心表 + 示例数据）
├── docker-compose.yml      # MySQL + Redis + RocketMQ
└── demo.sh                 # 完整演示脚本
```

## 数据库设计

| 表 | 职责 | 关键字段 |
| --- | --- | --- |
| `audit_request` | 审核请求记录 | request_id, biz_type, status, mode |
| `audit_result` | 审核决策结果 | risk_topic, risk_level, action, latency_ms |
| `risk_rule` | 规则配置（动态增删） | condition_type, condition_expr(JSON), priority |
| `policy_config` | 策略路由（灰度发布） | biz_type, risk_level, action, gray_ratio |
| `review_task` | 人工复核任务 | assignee, status, review_result |

## API 文档

### 核心接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/audit/submit` | 提交审核请求（同步/异步） |
| GET | `/api/v1/audit/result/{requestId}` | 查询审核结果 |
| GET | `/api/v1/rules` | 规则列表 |
| POST | `/api/v1/rules` | 创建规则 |
| PUT | `/api/v1/rules/{ruleId}` | 更新规则 |
| GET | `/api/v1/policies` | 策略列表 |
| POST | `/api/v1/policies/publish` | 发布策略 |
| POST | `/api/v1/review/tasks` | 创建复核任务 |
| POST | `/api/v1/review/tasks/{taskId}/claim` | 领取任务 |
| POST | `/api/v1/review/tasks/{taskId}/submit` | 提交复核结论 |
| GET | `/api/v1/metrics/summary` | 监控统计摘要 |

### 请求示例

```bash
# 提交审核（同步模式）
curl -X POST http://localhost:8080/api/v1/audit/submit \
  -H "Authorization: Bearer tk_im_service_2024" \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "req_001",
    "bizType": "im",
    "scene": "private_chat",
    "userId": "u_001",
    "contentText": "加微信带你稳赚不赔",
    "chatEvidenceList": ["加微信带你稳赚不赔", "先转99保证金"],
    "behaviorFeatures": {"external_contact_count": 12},
    "mode": "sync"
  }'
```

```json
{
  "code": 0,
  "data": {
    "requestId": "req_001",
    "status": "completed",
    "riskTopic": "诈骗引流",
    "riskLevel": "high_risk",
    "finalJudgment": "exist_violation",
    "action": "reject_content",
    "routeReason": "命中规则[引流+收益承诺组合], 类型=composite",
    "latencyMs": 45
  }
}
```

### 鉴权

所有接口需要携带 Bearer Token：

```
Authorization: Bearer <token>
```

内置 Token：`tk_im_service_2024` / `tk_comment_service_2024` / `tk_live_service_2024` / `tk_admin_2024`

## 规则引擎

支持 5 种规则类型，运营可通过 API 动态增删，无需重启服务：

| 类型 | 说明 | 条件表达式示例 |
| --- | --- | --- |
| keyword | 关键词命中 | `{"keywords": ["加微信", "稳赚"]}` |
| regex | 正则匹配 | `{"pattern": "1[3-9]\\d{9}"}` |
| behavior_threshold | 行为阈值 | `{"field": "external_contact_count", "operator": ">=", "threshold": 10}` |
| composite | AND/OR 组合 | `{"logic": "AND", "conditions": [...]}` |
| blacklist / whitelist | 黑白名单 | 框架就绪 |

内置示例规则覆盖：IM 消息、注册、登录、资料编辑、社交匹配、动态评论、充值交易等场景。

## 与 AI-IM-Guard-ML 的关系

```text
RiskHub (中台)                    Guard-ML (AI 引擎)
─────────────────                ─────────────────────
接入鉴权、限流                    语义理解、上下文分析
幂等去重                          多证据融合判定
异步队列削峰                      风险分级 + 主题识别
规则快速拦截                      灰区案例判断
策略路由 + 灰度                   模型训练 + 评测
人工复核流程                      监控看板 + 漂移检测
审计追溯                          样本回流
多业务线管理                      —
```

**定位清晰，互不重叠：**

- Guard-ML 是"大脑"——负责理解内容、做出判断
- RiskHub 是"身体"——负责把判断安全、可控地执行到线上

Guard-ML 运行在 `localhost:8000`，RiskHub 通过 `riskhub-model-adapter` 模块 HTTP 调用其 `/judge` 接口。Guard-ML 不可用时，RiskHub 自动降级走规则兜底，不影响主链路。

## 停止服务

```bash
# 一键启动的场景：Ctrl+C 即可全部停止

# 手动停止：
docker-compose down          # 停止基础设施
# Spring Boot: Ctrl+C
```

## 配置说明

关键配置项（`riskhub-app/src/main/resources/application.yml`）：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | 8080 | 服务端口 |
| `riskhub.model.base-url` | http://localhost:8000 | Guard-ML 地址 |
| `riskhub.model.timeout-ms` | 3000 | AI 调用超时 |
| `riskhub.model.shadow-mode` | false | 影子模式（只记录不决策） |
| `spring.datasource.url` | jdbc:mysql://localhost:3306/riskhub | 数据库 |
| `spring.data.redis.host` | localhost | Redis |
| `rocketmq.name-server` | localhost:9876 | RocketMQ |
</div>
