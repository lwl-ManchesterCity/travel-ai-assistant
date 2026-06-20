# 智能旅行助手

这是一个基于 Java + Spring Boot 的《hello-agents》第 13 章学习版实现。

项目目标不是 1:1 复刻 Python 原版，而是把这一章最核心的主链路落成一个能跑通、能展示、能继续扩展的 Java 项目：

- 前端输入旅行需求
- 后端接收 `TripPlanRequest`
- 多个 Agent 协作生成中间结果
- `PlanningContext` 收口上下文
- `RequirementAnalysisService` 将额外要求解析为规划约束
- `PlannerAgent` 输出结构化旅行方案
- 前端展示预算、地图、天气、每日行程等结果

## 项目结构

```text
src/main/java/com/lwl/travelassistant
├── agent         # 各类 Agent，负责业务处理
├── client        # 外部能力抽象层，当前用学习版实现
├── controller    # HTTP 接口入口
├── exception     # 全局异常处理
├── model         # DTO / 领域对象 / 上下文对象
└── service       # 主编排服务与知识服务
```

## 后端主链路

```text
TripPlanController
  -> TripPlanningService
    -> PlanningContextBuilder
      -> AttractionAgent -> AttractionClient -> TravelKnowledgeService
      -> WeatherAgent    -> WeatherClient
      -> HotelAgent      -> HotelClient
      -> RequirementAnalysisService
    -> PlannerAgent
      -> TripPlanResponse
```

这条链路的核心变化有两点：

1. `PlanningContext` 把原来散着传递的中间结果统一收口。
2. `client` 层把“数据来源”从 `agent` 里再抽出来，让职责边界更清楚。
3. 额外要求不再只是前端文本，而是会被解析成 `PlanningConstraints` 参与实际规划。

## 更结构化的结果输出

当前 `DayPlan` 不只是简单的 `theme + activities`，还补充了更适合前端展示和项目讲解的结构化信息：

- `description`
- `transportMode`
- `accommodation`
- `meals`

这样这个项目在简历里不只是“返回一段旅行文案”，而是“返回结构化、可消费的旅行规划结果”。

## 前端说明

前端页面放在：

- [src/main/resources/static/index.html](src/main/resources/static/index.html)

当前是 Spring Boot 静态资源模式，不是单独的 React / Vue 项目。

所以前端不能直接双击 `index.html` 用 `file://` 打开，必须通过 Spring Boot 服务访问：

- 正确访问方式：`http://localhost:8080`
- 错误访问方式：`file:///.../index.html`

如果直接用 `file://` 打开，会出现：

- `Cross origin requests are only supported for HTTP`
- `Fetch API cannot load file:///api/trips/plan`

## 启动方式

进入项目根目录：

```bash
cd /Users/lwl/Documents/智能旅游助手-1
```

启动项目：

```bash
mvn spring-boot:run
```

浏览器访问：

```text
http://localhost:8080
```

## 示例请求

接口：

```text
POST /api/trips/plan
```

请求体示例：

```json
{
  "origin": "上海",
  "destination": "杭州",
  "departureDate": "2026-07-01",
  "days": 3,
  "budget": 3000,
  "preferences": ["美食", "自然风景", "轻松节奏"]
}
```

## 异常处理

项目里有统一的全局异常处理：

- `TripPlanningException`
  业务异常，手动 `throw`
- `MethodArgumentNotValidException`
  参数校验异常，Spring 自动抛出
- `Exception`
  最后的通用兜底

它们会被 `GlobalExceptionHandler` 统一包装成更友好的错误响应返回给前端。

## 当前完成度

已完成：

- 第 13 章学习版后端主链路
- `PlanningContext` 上下文收口
- 额外要求分析与约束提取
- `client` 抽象层
- 全局异常处理
- 前端首页 / 结果页展示
- 基础测试

当前仍是学习版 / 骨架优先：

- 地图是 mock 展示，不是真实地图服务
- 景点图片是视觉占位，不是真实图片检索
- 酒店 / 天气 / 景点仍是规则与内置数据驱动
- 没接真实 MCP 生态和第三方旅游平台

## 测试

运行测试：

```bash
mvn test
```

当前包含的测试重点：

- `TripPlanControllerTest`
  验证接口成功返回和参数校验失败
- `PlannerAgentTest`
  验证 `PlanningContext -> TripPlanResponse` 的核心编排

## 后续可扩展方向

- 用真实 client 替换当前规则实现
- 给结果页接真实地图坐标和路线
- 继续细化每日行程、景点、预算结构
- 增加 README 中的架构图和演示截图
- 引入第 12 章的 benchmark / evaluator 做结果评估
