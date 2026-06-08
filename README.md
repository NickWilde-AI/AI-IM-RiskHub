# AI-IM-RiskHub

高并发内容审核与风控中台

## 技术栈

- Java 17 + Spring Boot 3.2
- MySQL 8.0 + Redis 7
- MyBatis-Plus + Docker Compose
- Prometheus + Micrometer

## 快速启动

### 前置要求

- JDK 17+
- Maven 3.9+
- Docker Desktop

### 1. 启动基础设施

```bash
cd ai-im-riskhub
docker-compose up -d
```

等待 MySQL 初始化完成（约 10 秒）。

### 2. 编译项目

```bash
mvn clean install -DskipTests
```

### 3. 启动应用

```bash
mvn spring-boot:run -pl riskhub-app
```

应用启动后监听 `http://localhost:8080`。

### 4. 运行演示

```bash
chmod +x demo.sh
./demo.sh
```

## 项目结构

```
ai-im-riskhub/
├── riskhub-common        # 公共模块：DTO、枚举、异常、Filter
├── riskhub-store         # 数据层：实体、Mapper、MyBatis-Plus
├── riskhub-engine        # 规则引擎 + 策略路由
├── riskhub-model-adapter # 模型服务调用（对接 AI-IM-Guard-ML）
├── riskhub-api           # REST 接口层
├── riskhub-app           # 启动模块
├── sql/                  # 数据库初始化脚本
├── docker-compose.yml    # MySQL + Redis
└── demo.sh              # 演示脚本
```

## API 接口

### 提交审核请求

```
POST /api/v1/audit/submit
Content-Type: application/json

{
  "requestId": "req_001",
  "bizType": "im",
  "scene": "private_chat",
  "userId": "u_001",
  "contentText": "加微信带你稳赚",
  "chatEvidenceList": ["加微信带你稳赚", "先转99保证金"],
  "behaviorFeatures": {"external_contact_count": 12},
  "mode": "sync"
}
```

### 查询审核结果

```
GET /api/v1/audit/result/{requestId}
```

### 健康检查

```
GET /actuator/health
```

### Prometheus 指标

```
GET /actuator/prometheus
```

## 规则引擎

内置规则类型：

| 类型 | 说明 | 示例 |
|------|------|------|
| keyword | 关键词匹配 | 加微信、约吗 |
| regex | 正则匹配 | 手机号 |
| behavior_threshold | 行为阈值 | 外联数>=10 |
| composite | 组合规则(AND/OR) | 引流+收益承诺 |

## 处置动作

| 动作 | 说明 |
|------|------|
| ignore | 放行 |
| warning | 警告 |
| human_review | 人工复核 |
| reject_content | 拦截内容 |
| ban_candidate | 封禁候选 |

## 停止服务

```bash
# 停止 Spring Boot（Ctrl+C）
# 停止基础设施
docker-compose down
```
