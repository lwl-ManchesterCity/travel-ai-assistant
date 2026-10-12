# 项目上下文

## 背景与目标

这是 Datawhale《hello-agents》第 13 章“智能旅行助手”的 Java 学习版实现。目标不是复刻 Python 原版，而是用 Spring Boot 把多 Agent 编排、外部工具调用、规则规划、反思和循环优化落成一个可运行的旅行规划项目，并作为简历项目使用。

## 核心业务流程

```text
POST /api/trips/plan
  -> TripPlanningService
  -> LoopPlanningAgent
  -> PlannerInputBuilder
  -> AttractionAgent / WeatherAgent / HotelAgent / RequirementAnalysisService
  -> PlannerAgent
  -> RouteAgent / RouteClient
  -> ReflectionAgent
  -> LlmCriticAgent（可用时真实调用 LLM，否则规则 fallback）
  -> LoopDecisionExecutor（修改下一轮请求）
  -> 重新规划，直到停止条件满足
  -> TripPlanResponse
```

行程调整接口 `POST /api/trips/adjust` 由 `TripAdjustmentAgent` 先把自然语言调整要求转为新的请求，再复用上述规划链路。

## 主要模块

- `TripPlanController`：提供 `/plan`、`/adjust` 两个接口，并触发参数校验。
- `TripPlanningService`：服务入口，负责调用 LoopPlanningAgent 或调整后重新规划。
- `PlannerInputBuilder`：并行职责上的输入汇总点，调用景点、天气、酒店和需求分析 Agent，组装 `PlannerInput`、来源和 Agent 轨迹。
- `AttractionAgent`、`HotelAgent`、`WeatherAgent`：分别调用对应 client 获取候选数据。
- `PlannerAgent`：基于候选数据、预算、偏好、天气和约束生成每日景点、酒店、餐饮、费用和路线查询。
- `RouteAgent`：把每日酒店和景点串成路线，调用 `RouteClient`。
- `ReflectionAgent`：用规则评估预算、总通勤、天气风险和体验保守程度，计算严重度与总分。
- `LlmCriticAgent`：在 LLM 可用时，根据反思结果选择一个受限动作；请求失败或内容非法时回退规则批评。
- `LoopDecisionExecutor`：通过 `RequestOptimizationStrategyService` 把动作落实为下一轮请求约束。
- `Configurable*Client`：根据 `travel.providers.*` 选择 Amap 或规则/mock 实现。
- `Amap*Client`：调用高德 POI、酒店、天气和路线接口；路线受限时可回退规则路线，部分请求还保留 curl fallback。
- `GlobalExceptionHandler`：把参数校验、业务异常和未预期异常转换为结构化错误响应。
- 静态前端：提交旅行请求、展示预算/天气/地图/每日行程和 Agent/Loop 轨迹。

## 关键数据流

1. `TripPlanRequest` 当前包含城市、开始/结束日期、预算、偏好、交通、住宿和额外要求；日期与基本字段由 Jakarta Validation 校验。
2. 信息 Agent 获取的是候选数据，不是最终行程。景点可能来自多个偏好关键词，酒店、天气分别来自对应查询。
3. `PlannerAgent` 对候选数据进行预算、地理距离、天气和约束处理，生成 `DayPlan` 和 `TripPlan`。
4. `RouteAgent` 会把酒店作为起点和终点，按景点顺序逐段请求路线并汇总距离、时间和步骤。
5. `ReflectionAgent` 产出 `TripPlanReflection`；`LoopIterationTrace` 记录每轮请求摘要、成本、路线、严重度、批评动作和实际修改。
6. `LoopExecutionSummary` 记录循环是否启用、执行轮次、最终采用轮次和停止原因。

## 当前已实现能力

- Spring Boot Java 17 后端和静态 HTML/CSS/JavaScript 前端。
- 高德 POI 景点搜索、酒店检索、天气预报和路线规划 provider 配置。
- Amap、规则/mock 两套实现及可配置选择；路线高德接口限流时可回退规则路线。
- 基于预算、距离、偏好、天气和额外要求的规则式日程规划。
- 预算明细、每日路线、地图 marker/polyline、天气信息和来源元数据展示。
- 需求分析的 LLM/规则双路径，以及 DashScope OpenAI-compatible LLM client。
- `PlannerAgent -> ReflectionAgent -> LlmCriticAgent -> LoopDecisionExecutor` 的多轮自校验链路，默认循环配置为启用、最多 3 轮、连续无改善 2 轮停止。
- `/api/map/config` 提供前端地图初始化配置。

## 尚未解决的系统性问题

- 高德 POI 是文本搜索结果，当前仍可能出现类别不相关、跨区域或非景点 POI；需要更严格的类别、距离和城市校验。
- 路线按相邻点逐段请求，容易触发高德 QPS/配额限制；当前能降级，但不等于解决配额和缓存问题。
- 景点图片来源不稳定，前端存在 fallback 视觉占位；真实图片 provider 和版权/可用性策略待确认。
- LLM 是否真实参与每次运行取决于 `TRAVEL_LLM_ENABLED`、Key、网络和 endpoint；不可只凭页面文案判断。
- 生产域名、HTTPS、备案状态、Nginx/systemd 配置和当前可访问性待确认；代码仓库本身不代表已完成生产部署。
- 自动化测试覆盖仍有限；Java 25 环境下部分 Mockito/Byte Buddy 测试可能有自附加限制，需在目标 JDK/CI 环境复核。
