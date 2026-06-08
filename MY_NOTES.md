# AI-IM 风控系统 - 项目全景文档

> 这个文档用于从 0 到 1 理解整个项目体系。给人看，也给 AI 看。
> 更新时间：2026-06-08

---

## 一、项目是什么

一个完整的 **IM 私聊内容审核风控系统**，解决的问题是：当用户在直播平台/社交 App 里私聊时，自动识别违规内容（诈骗、色情、赌博、代刷等 13 种主题），并决定处置方式（放行、警告、封号等）。

系统由两个独立项目组成，模拟真实企业中"AI 团队"和"后端团队"协作的模式：

| 项目 | 角色 | 一句话 |
|------|------|--------|
| **AI-IM-Guard-ML** | AI 审核引擎 | "这条消息是什么？" — 语义理解、风险分级 |
| **AI-IM-RiskHub** | 风控中台 | "该怎么办？" — 接入、限流、策略、灰度、复核 |

---

## 一.五、两个项目详细对照

### 基础信息

| 维度 | AI-IM-Guard-ML | AI-IM-RiskHub |
|------|----------------|---------------|
| 定位 | AI 审核引擎（大脑） | 风控中台（身体） |
| 语言 | Python 3.11 | Java 17 |
| 框架 | FastAPI + Uvicorn | Spring Boot 3.2 |
| 数据库 | 无（无状态服务） | MySQL 8.0 |
| 缓存 | 无 | Redis 7 |
| 消息队列 | 无 | RocketMQ 5.1 |
| 端口 | :8000 | :8080 |
| 看板 | Guard-ML.html | RiskHub.html |
| GitHub | NickWilde-AI/AI-IM-Guard-ML | NickWilde-AI/AI-IM-RiskHub |

### 能力对照

| 能力 | Guard-ML | RiskHub | 谁负责 |
|------|----------|---------|--------|
| 关键词/语义审核 | ✅ 做 | ❌ 仅降级兜底 | Guard-ML |
| 上下文理解/灰区判定 | ✅ 做 | ❌ 不做 | Guard-ML |
| 风险分级 (high/mid/low) | ✅ 输出 | 信任 AI | Guard-ML |
| 主题识别 (13种) | ✅ 输出 | 信任 AI | Guard-ML |
| 处置建议 | ✅ 输出 | 结合策略做最终决策 | 协作 |
| 行为阈值判定 | ❌ 不做 | ✅ 做 | RiskHub |
| 幂等去重 | ❌ 不做 | ✅ Redis NX | RiskHub |
| 异步队列削峰 | ❌ 不做 | ✅ RocketMQ | RiskHub |
| 策略路由 + 灰度 | ❌ 不做 | ✅ 做 | RiskHub |
| 规则动态增删 | ❌ 不做 | ✅ API 热更新 | RiskHub |
| 人工复核 | ❌ 不做 | ✅ 完整工作流 | RiskHub |
| 审计追溯 | 简单日志 | ✅ 版本化记录 | RiskHub |
| 多业务线接入 | 单一 | ✅ im/comment/live/register/login | RiskHub |
| 模型训练 | ✅ SFT/LoRA | ❌ 不做 | Guard-ML |
| 离线评测 | ✅ 做 | ❌ 不做 | Guard-ML |
| 漂移检测 | ✅ 做 | ❌ 不做 | Guard-ML |
| 监控看板 | ✅ AI 数据流 | ✅ 中台运维 | 各自 |
| Token 鉴权 | 简单（可选） | ✅ Bearer Token | RiskHub |
| 限流 | ✅ 基础限流 | ✅ 基于 Redis | 各自 |

### 输入输出对照

| | Guard-ML 输入 | Guard-ML 输出 | RiskHub 输入 | RiskHub 输出 |
|--|--|--|--|--|
| **格式** | JSON | JSON | JSON | JSON |
| **核心字段** | ticket_id, chat_evidence_list, behavior_abnormal_list | risk_level, topic, handling_suggestion, confidence | requestId, bizType, contentText, chatEvidenceList, behaviorFeatures, mode | requestId, riskLevel, finalJudgment, action, latencyMs |
| **示例** | `{"chat_evidence_list":[{"original_content":"加微信稳赚"}]}` | `{"risk_level":"mid_risk","topic":"诈骗引流","confidence":0.88}` | `{"requestId":"001","contentText":"加微信稳赚","mode":"sync"}` | `{"action":"human_review","riskLevel":"mid_risk"}` |

### 部署依赖对照

| 依赖 | Guard-ML | RiskHub |
|------|----------|---------|
| Python 3.11 | ✅ 必须 | ❌ |
| Java 17 | ❌ | ✅ 必须 |
| Docker | ❌ 不需要 | ✅ 需要（MySQL+Redis+MQ） |
| GPU | 可选（模型推理加速） | ❌ |
| pip 依赖 | fastapi, uvicorn, httpx, pyyaml | — |
| Maven 依赖 | — | spring-boot, mybatis-plus, redisson, rocketmq |

### 看板对照

| 维度 | Guard-ML 看板 | RiskHub 看板 |
|------|--------------|-------------|
| 地址 | http://localhost:8000/static/Guard-ML.html | http://localhost:8080/RiskHub.html |
| 刷新频率 | 2 秒 | 3 秒 |
| 核心指标 | 审核 QPS、违规率、主题分布、P95 延迟 | 总请求量、平均延迟、待复核数、违规率 |
| 独有内容 | 实时数据流表格、突发事件检测、模拟器速度控制 | 规则列表、策略列表、人工复核队列、系统健康 |
| 共同内容 | 处置动作分布、风险等级分布、风险主题分布 | 同左 |

## 二、完整架构流程（从一条消息进来到最终处置）

```
用户发送一条私聊消息
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  RiskHub 风控中台 (Java / Spring Boot / :8080)                           │
│                                                                         │
│  ① 接入鉴权                                                             │
│     └─ 业务方携带 Bearer Token 调用 POST /api/v1/audit/submit            │
│     └─ ApiTokenInterceptor 校验 Token 合法性                             │
│                                                                         │
│  ② 幂等去重                                                             │
│     └─ Redis SETNX(requestId, 1h)                                       │
│     └─ 同一条消息重复提交 → 直接返回已有结果，不重复处理                    │
│                                                                         │
│  ③ 模式分流                                                             │
│     ├─ mode="sync"  → 立即执行完整链路，等结果返回                        │
│     └─ mode="async" → 投递 RocketMQ 队列，立即返回 "accepted"             │
│                                                                         │
│  ④ AI 主审 (双轨模式)                                                    │
│     └─ HTTP POST http://localhost:8000/judge (超时 3s)                   │
│         │                                                               │
│         ▼                                                               │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Guard-ML (Python / FastAPI / :8000)                             │    │
│  │                                                                  │    │
│  │  输入: ticket_id + chat_evidence_list + behavior_abnormal_list   │    │
│  │                                                                  │    │
│  │  处理: HeuristicJudge (关键词规则基线)                            │    │
│  │        或 TransformersJudge (微调 LLM checkpoint)                │    │
│  │        或 APIJudge (远程 API 如 Qwen)                            │    │
│  │                                                                  │    │
│  │  输出: {                                                         │    │
│  │    risk_level: "high_risk" | "mid_risk" | "low_risk",           │    │
│  │    topic: "诈骗引流" | "色情诱导" | ... (13种),                   │    │
│  │    handling_suggestion: "ban_account" | "limit_account" | ...,   │    │
│  │    final_judgment: "exist_violation" | "not_exist_violation",    │    │
│  │    confidence: 0.95  ← 关键字段，决定是否采信                     │    │
│  │  }                                                               │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│         │                                                               │
│         ▼                                                               │
│  ⑤ 判定分支                                                             │
│     ├─ AI 返回 + confidence >= 0.8 → 采信 AI 结果                       │
│     │   └─ 仅补充执行 behavior_threshold 规则（行为阈值判定）             │
│     │                                                                   │
│     └─ AI 超时/不可用/confidence < 0.8 → 降级走全量规则引擎              │
│         └─ keyword + regex + behavior_threshold + composite 全部执行     │
│                                                                         │
│  ⑥ 策略路由                                                             │
│     └─ 取最高优先级命中 → 查 policy_config 表                            │
│     └─ 匹配 bizType + riskLevel → 找到对应策略                           │
│     └─ 应用灰度比例 (gray_ratio: 0-100%)                                │
│     └─ 决定最终动作: ignore / warning / human_review / reject / ban      │
│                                                                         │
│  ⑦ 持久化 + 审计                                                        │
│     └─ 写入 audit_request + audit_result (MySQL)                        │
│     └─ 记录: rule_version + policy_version + model_version + latency    │
│                                                                         │
│  ⑧ 人工复核 (如需要)                                                    │
│     └─ mid_risk + human_review → 自动创建 review_task                   │
│     └─ 审核员领取 → 提交结论 → 完成                                     │
│                                                                         │
│  ⑨ 返回响应                                                             │
│     └─ { requestId, riskLevel, finalJudgment, action, latencyMs }       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、13 种违规主题

| # | 主题 | 典型话术 | 严重度 |
|---|------|---------|--------|
| 1 | 代刷/包榜 | "帮我冲周榜第一，预算两万" | 高 |
| 2 | 诈骗引流 | "加微信带你稳赚不赔" | 高 |
| 3 | 私下交易 | "别在平台充了，直接转我支付宝" | 中 |
| 4 | 赌博引流 | "跟着计划买，稳赢不亏" | 高 |
| 5 | 色情诱导 | "想约吗？加我另一个号" | 高 |
| 6 | 辱骂攻击 | "你是不是傻逼？" | 中 |
| 7 | 政治敏感 | "帮我扩散一下这条敏感内容" | 高 |
| 8 | 自伤诱导 | "活着真没意思，不想活了" | 高 |
| 9 | 未成年保护 | "你才14岁？加微信偷偷聊" | 高 |
| 10 | 版权侵犯 | "盗版资源需要吗？免费看" | 中 |
| 11 | 虚假信息 | "恭喜你中奖了！点击链接领取" | 中 |
| 12 | 违禁品交易 | "麻古、冰糖都有，私聊" | 高 |
| 13 | 无主题（安全） | "今天心情怎么样？" | 无 |

---

## 四、技术栈

### Guard-ML (AI 引擎)

| 层 | 技术 |
|---|---|
| 语言 | Python 3.11 |
| Web 框架 | FastAPI + Uvicorn |
| ML 推理 | HeuristicJudge (规则基线) / TransformersJudge (本地模型) / APIJudge (远程 API) |
| 训练 | PyTorch + Transformers + TRL + PEFT (LoRA) |
| 监控 | 自定义看板 + Prometheus 格式指标 |

### RiskHub (风控中台)

| 层 | 技术 |
|---|---|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2 |
| 数据库 | MySQL 8.0 + MyBatis-Plus 3.5 |
| 缓存 | Redis 7 (幂等、限流) |
| 消息队列 | Apache RocketMQ 5.1 (异步审核) |
| 监控 | Spring Boot Actuator + Micrometer + Prometheus |
| 容器 | Docker Compose |

---

## 五、目录结构

```
AI-IM-RiskHub/                          ← 工作目录（非 git）
├── MY_NOTES.md                         ← 你在看的这个
├── BACKEND_ENTERPRISE_REVIEW_PLATFORM_PRD.md
└── ai-im-riskhub/                      ← git 仓库
    ├── riskhub-common/                 ← 公共：DTO、鉴权 Filter、WebConfig
    ├── riskhub-store/                  ← 数据层：Entity + Mapper
    ├── riskhub-engine/                 ← 规则引擎 + 策略路由
    │   ├── RuleEngineService.java      ← executeRules() + executeBehaviorRulesOnly()
    │   ├── PolicyRouterService.java    ← route() 策略匹配 + 灰度
    │   └── RuleMatcher.java            ← keyword/regex/behavior/composite 匹配
    ├── riskhub-model-adapter/          ← Guard-ML 调用层
    │   └── ModelAdapterService.java    ← judge() + 超时降级 + shadow-mode
    ├── riskhub-api/                    ← REST + Service 编排
    │   ├── AuditController.java        ← /api/v1/audit/submit
    │   └── AuditService.java           ← submitInternal() 双轨模式核心
    ├── riskhub-app/                    ← Spring Boot 启动
    │   └── src/main/resources/
    │       ├── application.yml
    │       └── static/RiskHub.html     ← 运维看板
    ├── sql/init.sql                    ← 5张表 + 示例规则/策略
    ├── docker-compose.yml              ← MySQL + Redis + RocketMQ
    ├── demo.sh                         ← 完整场景演示
    └── start-all.sh                    ← 一键启动全部服务

AI-IM-Guard-ML/                         ← git 仓库
├── src/im_guard_ml/
│   ├── api.py                          ← FastAPI 服务 (/judge /dashboard/data /health)
│   ├── inference.py                    ← HeuristicJudge / TransformersJudge / APIJudge
│   ├── simulator.py                    ← 数据模拟器（同时打 :8000 和 :8080）
│   ├── schema.py                       ← RiskLevel / TOPICS 等枚举
│   ├── parsing.py                      ← JSON 解析 + 兜底
│   ├── postprocess.py                  ← 策略路由 (route_policy)
│   ├── training.py                     ← SFT 训练
│   ├── evaluation.py                   ← 离线评测
│   └── monitoring.py                   ← 漂移检测
├── static/
│   └── Guard-ML.html                   ← AI 审核监控看板
├── configs/default.yaml                ← rubrics + 配置
└── .venv/                              ← Python 3.11 虚拟环境
```

---

## 六、数据库设计

```sql
-- 审核请求（每条消息进来记一笔）
audit_request: request_id(UK), biz_type, scene, user_id_hash, status, mode

-- 审核结果（最终判定）
audit_result: request_id(IDX), risk_topic, risk_level, final_judgment, action, latency_ms,
              rule_version, policy_version, model_version

-- 风险规则（动态增删，热更新）
risk_rule: rule_id(UK), condition_type, condition_expr(JSON), risk_level, priority, enabled

-- 策略配置（灰度发布）
policy_config: policy_id(UK), biz_type, risk_level, action, gray_ratio(0-100), enabled

-- 人工复核任务
review_task: task_id(UK), request_id, assignee, status, review_result
```

---

## 七、快速启动

```bash
cd AI-IM-RiskHub/ai-im-riskhub
./start-all.sh
```

自动执行：清理旧进程 → Docker(MySQL+Redis+RocketMQ) → Guard-ML(:8000) → RiskHub(:8080) → 模拟器

看板：
- http://localhost:8000/static/Guard-ML.html （AI 审核数据流）
- http://localhost:8080/RiskHub.html （中台运维全景）

---

## 八、API 速查

### RiskHub (:8080) — 需要 Token: `Bearer tk_admin_2024`

```
POST /api/v1/audit/submit          提交审核（mode: sync/async）
GET  /api/v1/audit/result/{id}     查询结果
GET  /api/v1/rules                 规则列表
POST /api/v1/rules                 创建规则
PUT  /api/v1/rules/{id}            更新规则
GET  /api/v1/policies              策略列表
POST /api/v1/review/tasks          创建复核任务
POST /api/v1/review/tasks/{id}/claim   领取
POST /api/v1/review/tasks/{id}/submit  提交结论
GET  /api/v1/metrics/summary       监控统计
GET  /actuator/health              健康检查（免鉴权）
```

### Guard-ML (:8000) — 无需鉴权

```
POST /judge                        AI 审核判定
GET  /health                       健康检查
GET  /dashboard/data?window=all    看板数据
GET  /metrics                      Prometheus 指标
POST /simulator/speed              调节模拟速度
```

---

## 九、已解决问题清单

### P0: 双轨模式未真正生效 ✅ 已解决

**现象**：所有请求都走"规则兜底"模式，AI 主审从未触发。

**根因（三重）**：
1. Guard-ML 的 `HeuristicJudge.predict()` 没有返回 `confidence` 字段 → RiskHub 拿到 null → 判定低置信
2. `ModelJudgeResponse` DTO 字段是 camelCase，Guard-ML 返回 snake_case → Jackson 映射全为 null
3. RestTemplate 默认 converter 不识别自定义 ObjectMapper 配置

**修复**：
- Guard-ML `inference.py`: 加 `"confidence": 0.95/0.88/0.85`
- RiskHub `ModelJudgeResponse.java`: 加 `@JsonProperty` 注解
- RiskHub `ModelAdapterService.java`: 改为手动用 SNAKE_CASE ObjectMapper 解析 JSON 字符串

**验证**：日志输出 "AI主审模式"，11 个主题正确透传到 RiskHub 看板。

### P1: RiskHub 看板只显示 3 个主题 ✅ 已解决

**修复**：P0 修复后自动解决。AI 主审模式下 topic 直接从 Guard-ML 透传，不依赖 RiskHub 规则匹配。

**当前状态**：11 个主题正常显示（代刷/诈骗/赌博/色情/私下交易/辱骂/违禁品/版权/政治/自伤/无主题）。

### P2: 模拟器 RiskHub payload 不完整 ✅ 已解决

**修复**：模拟器正确传入 chatEvidenceList 和 behaviorFeatures，RiskHub 能完整接收数据。

### P3: 看板跨域检测 Guard-ML 健康状态 ⚠️ 低优先级

**现象**：RiskHub 看板从 :8080 检查 :8000 健康状态时偶尔因浏览器 CORS 失败。

**影响**：系统健康面板里 Guard-ML 状态可能偶尔显示异常（实际正常），不影响功能。

**修复方向**：RiskHub 后端加代理端点。优先级低，暂不处理。

### P4: mvn spring-boot:run 不更新子模块 ✅ 已解决

**现象**：修改 riskhub-model-adapter 等子模块后，`mvn spring-boot:run -pl riskhub-app` 仍用旧 class。

**修复**：start-all.sh 启动前先执行 `mvn clean install -DskipTests`，确保所有模块重新编译打包。

---

## 十、下一步可做的事

- [x] Guard-ML HeuristicJudge 增加 confidence 返回，激活双轨模式
- [x] ModelAdapterService 正确解析 snake_case 响应
- [x] 模拟器优化 RiskHub payload，完整传入 chatEvidenceList
- [x] start-all.sh 加 mvn clean install 防止子模块不更新
- [ ] RiskHub 看板增加"审核模式"指标（AI主审 vs 规则兜底的比例）
- [ ] 人工复核流程打通（模拟器产生 mid_risk 案例自动创建复核任务）
- [ ] 健康检查代理端点（解决跨域检测 Guard-ML 的问题）
- [ ] 加 Nginx 做统一入口（可选）

---

## 十一、面试要点

### 项目定位
> 两个项目组成完整 IM 风控系统。AI 项目做模型侧，后端项目做工程侧。模拟企业中 AI 团队和后端团队的协作模式。

### 高并发怎么做
> Redis 幂等防重 + RocketMQ 异步削峰 + AI 超时降级 + 无状态水平扩容 + 规则引擎内存匹配

### AI 怎么接入
> HTTP 调用，3s 超时降级，shadow-mode 灰度验证。AI 可用时采信 AI，不可用时规则兜底。

### 数据闭环
> 审核结果 → 人工复核确认/驳回 → 误判样本回流 → 训练数据 → 模型迭代

### 规则 vs AI 分工
> AI 做所有语义判定（13种主题），规则只做行为阈值判定（外联频率、注册频率）和降级兜底。两者不重叠。
