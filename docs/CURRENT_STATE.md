# 当前状态

## 当前正在处理的任务

建立项目长期上下文文档机制。本阶段只整理 `AGENTS.md`、`docs/PROJECT_CONTEXT.md`、`docs/DECISIONS.md`、`docs/PITFALLS.md` 和本文件，不修改任何业务代码。

## 已完成的工作

- 已确认当前主链路：Controller -> TripPlanningService -> LoopPlanningAgent -> PlannerInputBuilder -> 信息 Agent -> PlannerAgent -> RouteAgent -> ReflectionAgent -> LlmCriticAgent -> LoopDecisionExecutor -> TripPlan。
- 已实现高德景点、酒店、天气、路线 provider 及规则/mock 实现和配置选择。
- 已实现预算、距离、天气、偏好和额外要求参与的规则式规划。
- 已实现高德路线地图展示、每日切换、预算/天气/行程展示和 Agent/Loop 轨迹数据。
- 已实现 LLM 需求分析和 LLM 批评的兼容接口；当前默认配置为 DashScope `qwen-plus`，是否启用由环境变量控制。
- 已实现 Loop 的最大轮数、无改善停止、重复调整检测、最佳方案保留和规则 fallback。
- 已记录本项目在真实开发中出现过的前端启动、provider 混用、路线配额、POI 质量、网络和密钥安全问题。

## 当前仍存在的问题

- 景点候选的语义质量和跨天去重仍不够强，可能出现无关 POI、重复景点或城市边界不理想。
- 路线请求可能因多段请求和多轮 Loop 触发高德 QPS/配额，当前主要依赖规则路线降级。
- LLM 的真实调用比例、生产 endpoint、模型名、网络可达性和 Key 状态需要在实际环境确认。
- 图片数据来源、版权和稳定性尚未形成明确方案；当前不能保证每个 POI 都有真实图片。
- 自动化测试和 CI 仍需补强，尤其是外部 provider mock、循环停止条件、降级来源和 JDK 兼容性。
- 生产域名、HTTPS、备案、Nginx/systemd 和当前公网可访问性待确认；不要只根据本地页面或 DNS 记录判断部署完成。

## 下一步建议

1. 先确认这 5 份文档中的“待确认”信息和当前线上配置，不要立即继续扩展业务功能。
2. 继续开发前先补一条端到端验证：固定请求 -> provider 来源 -> Loop 轨迹 -> 最终方案，确保页面能看出真实/规则/fallback。
3. 再补 POI 质量、跨天去重、路线缓存/限流和核心循环测试。
4. 最后再做部署脚本、HTTPS、监控和 README 的发布版同步。

## 当前明确不应该做的事情

- 不要在未确认线上配置前继续增加新的外部 API 或更换 LLM provider。
- 不要为了让页面更“智能”而让 LLM 直接生成或覆盖真实 POI、价格、坐标和天气。
- 不要通过无限重试、关闭错误处理或去掉 fallback 来掩盖高德配额问题。
- 不要把完整密钥、服务器密码或带密钥的截图提交到 GitHub。
- 不要在本阶段修改业务代码；后续业务改动必须先更新上下文判断，再单独验证和更新本文件。

## 需要用户确认

- 生产环境当前是否仍使用 `8.148.254.183`，以及 `interviewcoach.asia` 是否继续用于本项目：待确认。
- 生产域名备案、HTTPS 证书、Nginx/systemd 配置是否已完成：待确认。
- 当前线上真实启用的 LLM provider、base URL、model 和是否允许产生调用费用：待确认。
- 是否把景点图片作为下一阶段必做能力，还是继续使用可解释的 fallback 占位图：待确认。
