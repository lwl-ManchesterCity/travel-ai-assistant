# 项目协作规则

## 技术栈

- Java 17、Spring Boot 3、Spring MVC、Jakarta Validation、Maven。
- Jackson 负责 JSON 序列化与反序列化，Java `HttpClient` 负责外部 HTTP 请求。
- 前端是 Spring Boot 静态资源目录下的 HTML/CSS/JavaScript，地图使用高德 Web JS API。
- 外部数据当前以高德地图 POI、酒店、天气、路线接口为主；LLM 通过 OpenAI-compatible Chat Completions 接口接入，默认配置指向阿里云 DashScope 兼容端点。

## 分层与边界

- `controller` 只负责 HTTP 入参、校验和响应，不承载旅行规划规则。
- `service` 负责请求编排、需求分析、规划输入构建和请求优化策略。
- `agent` 表达业务角色：信息查询、初步规划、反思、LLM 批评、循环编排和路线规划。
- `client`/`client.impl` 负责外部 provider 或本地规则实现；`selector` 根据配置选择真实实现或兜底实现。
- `model` 只承载请求、结果、约束、评估和轨迹数据，不放跨模块业务流程。
- `config` 负责 `@ConfigurationProperties` 和 Spring Bean；密钥只能来自环境变量或本地未提交配置。
- 新增外部服务时优先扩展 client 接口和实现，不直接把 HTTP 细节写进 Agent 或 Controller。

## 开发与验证

- 开始重要任务前，先读取 `docs/PROJECT_CONTEXT.md` 和 `docs/CURRENT_STATE.md`。
- 涉及架构或边界调整时，再读取 `docs/DECISIONS.md`；涉及历史问题和稳定性时，再读取 `docs/PITFALLS.md`。
- 完成一个阶段后更新 `docs/CURRENT_STATE.md`；只有形成重要、可复用经验时才更新决策或陷阱文档。
- 默认在项目根目录执行 `mvn -q -DskipTests compile`；改动测试或核心链路时再执行针对性测试。
- 外部 API 验证必须记录 provider、配置来源和是否发生 fallback，不能只看 HTTP 200。
- 前端必须通过 Spring Boot/HTTP 访问，不能用 `file://` 直接双击 `index.html`。

## 已确认的硬性约束

- 高德返回的景点、酒店、天气和路线是事实数据来源；LLM 只做需求理解、批评和叙述，不负责编造 POI、价格、坐标或天气。
- 规则评估负责可验证的预算、路线、天气和体验约束；LLM 只能在允许的动作枚举中提出下一步建议。
- `LoopDecisionExecutor` 才能真正修改下一轮请求；LLM 不能直接返回整份旅行计划并覆盖系统结果。
- 所有 provider 都必须保留可解释的来源信息；高德失败或限流时可以降级，但界面和轨迹应能说明实际来源。
- 不得把 API Key、服务器密码或其他凭据写入 Git、README、截图或提交信息；历史上已经暴露过的凭据应视为需要轮换。

## 协作方式

- 先讲本阶段解决的问题和主链路，再进入代码细节。
- 解释代码时说明“目的、和谁连接、位于主链路哪一环”，优先解释关键变化，不重复整类代码。
- 用户明确说“只整理、只解释”时，不修改业务代码；用户明确要求实现时再落地并验证。
- 不确定的实现或部署状态标记为“待确认”，不根据旧截图或旧对话把它说成当前事实。

## 反复出现且必须避免的问题

- 不要把 `mockResponse`、provider 选择和真实 API 成功混为一谈；配置值和返回结果都要核对。
- 不要把高德路线一次规划想当然地理解成一次请求；当前实现按酒店/景点相邻点分段请求，容易触发配额限制。
- 不要把 `curl fallback` 当成 mock；它仍然是真实高德请求，只是换了请求方式。
- 不要把外部 POI 搜索结果直接当作高质量景点；需要关注城市、类别、重复项和无关 POI。
- 不要用 `curl -s` 隐藏错误，也不要在 Shell 中把 API Key 写进命令历史。
- 不要为了让文档或页面“看起来完整”而虚构图片、天气、LLM 调用或部署结果。
