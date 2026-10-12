# 技术决策

## 1. 使用 Spring Boot 分层实现学习版 Agent 系统

- 决策内容：采用 Controller、Service、Agent、Client、Model、Config 分层，不追求与原 Python 项目逐文件对应。
- 为什么这样设计：保留原章节的 Agent、工具调用和主链路思想，同时符合 Java/Spring 的依赖注入和可测试习惯。
- 其他方案：直接写一个大 Service；或做成微服务拆分。
- 未采用原因：大 Service 边界不清；微服务会增加部署和通信复杂度，不适合当前学习版主链路。
- 后续约束：新功能先判断属于编排、业务角色、外部调用还是数据模型，不能把不同职责继续堆进 Controller/PlannerAgent。

## 2. 外部事实优先使用高德，规则和 LLM 负责规划与分析

- 决策内容：景点、酒店、天气、路线尽量来自高德；规则负责可验证约束，LLM 负责自然语言需求理解、批评和结果润色。
- 为什么这样设计：POI、坐标、天气、路线需要可追溯事实，LLM 不适合作为这些数据的唯一来源。
- 其他方案：让 LLM 直接生成完整旅行计划，或加入 RAG 作为主要数据源。
- 未采用原因：会增加幻觉、过期数据和检索维护成本；当前高德 API 已提供项目所需的基础实时数据。
- 后续约束：LLM 不得直接编造价格、坐标、天气或 POI；它返回的动作必须经过规则校验和执行器。

## 3. Provider 接口 + 配置选择 + fallback

- 决策内容：通过 client 接口、Amap/规则实现和 `Configurable*Client` 按配置选择 provider；真实请求失败时保留明确的降级路径。
- 为什么这样设计：便于本地学习、API 不可用时运行、替换 provider 和测试核心规划逻辑。
- 其他方案：所有场景固定调用高德；失败直接返回空结果；或在每个 Agent 内写 if/else。
- 未采用原因：固定外部依赖不利于开发和测试；空结果体验差；Agent 内分支会造成耦合。
- 后续约束：必须同时记录 provider 元数据和 fallback 结果，不能用“模拟返回”掩盖真实调用失败。

## 4. 用 LoopPlanningAgent 统一编排闭环

- 决策内容：由 LoopPlanningAgent 管理生成、反思、批评、执行、重规划、停止和最佳方案选择。
- 为什么这样设计：把“一个方案生成器”和“多轮自校验控制器”分开，能明确体现 loop engineering。
- 其他方案：让 PlannerAgent 自己递归调用；或让 LLM 自由生成并直接覆盖整份计划。
- 未采用原因：递归边界、状态和失败恢复不清晰；LLM 直接覆盖结果不可控。
- 后续约束：PlannerAgent 只负责生成候选方案；LoopDecisionExecutor 才能修改下一轮请求；循环必须有最大轮次和无改善停止条件。

## 5. LLM 批评采用受限动作枚举

- 决策内容：LlmCriticAgent 只能选择 `REDUCE_BUDGET`、`COMPRESS_ROUTE`、`REPLACE_OUTDOOR_WITH_INDOOR`、`UPGRADE_EXPERIENCE`、`STOP`。
- 为什么这样设计：让 LLM 判断优先级，但把执行范围锁在可验证的策略内。
- 其他方案：让 LLM 返回任意修改后的 JSON 请求或完整 TripPlan。
- 未采用原因：自由 JSON 容易破坏硬性字段、造成幻觉和不可重复结果。
- 后续约束：必须校验动作值；LLM 不可用或响应不合法时走规则 fallback；最终停止由循环控制逻辑和规则评估共同约束。

## 6. 循环轨迹作为结果的一部分

- 决策内容：用 `LoopIterationTrace` 和 `LoopExecutionSummary`记录每轮评分、问题、动作、修改和最终采用轮次。
- 为什么这样设计：便于调试、前端解释“为什么这样规划”，也能验证 Agent 是否真的执行了循环。
- 其他方案：只返回最终 TripPlan，不暴露过程。
- 未采用原因：会让多 Agent 和 loop 行为不可观察，用户也无法区分真实 LLM、规则和 fallback。
- 后续约束：轨迹展示应默认对普通用户折叠；诊断信息不得泄露密钥、完整 prompt 或内部异常堆栈。
