# 开发陷阱与历史问题

## 1. 用 `file://` 直接打开前端

- 现象：浏览器出现 `Cross origin requests are only supported for HTTP`、无法请求 `/api/trips/plan`。
- 根因：静态页面来源是 `file://`，不是 Spring Boot 提供的 HTTP origin。
- 已尝试过的错误方案：只刷新页面或反复点击生成按钮。
- 当前解决办法：启动 Spring Boot 后访问 `http://localhost:8080`，部署时让 Nginx 代理静态资源和 `/api/`。
- 后续注意：README 和排障说明都要明确禁止双击 `index.html`。

## 2. 把真实高德、mock 和 curl fallback 混淆

- 现象：页面能生成计划，但不清楚到底调用了高德还是规则数据；或以为 curl fallback 是切换 mock。
- 根因：provider 选择由 `travel.providers.*` 和 `amap.mock-response` 共同决定；curl fallback 仍然请求高德，只是换了进程调用方式。
- 已尝试过的错误方案：只看页面显示“高德”，不看配置、provider 元数据和日志。
- 当前解决办法：配置中明确 provider；结果保留来源；失败时记录 Amap/规则/curl 状态；路线限流时明确显示规则路线估算。
- 后续注意：排障必须同时核对配置、日志、响应 `status/infocode` 和结果来源。

## 3. 高德路线逐段请求触发配额限制

- 现象：出现 `CUQPS_HAS_EXCEEDED_THE_LIMIT`、`infocode=10021`，多次请求后路线切换为规则估算。
- 根因：每个“酒店 -> 景点/景点 -> 景点/景点 -> 酒店”相邻段单独请求高德；多轮 Loop 又会重复生成路线。
- 已尝试过的错误方案：把失败当成网络握手问题，继续重复请求或只切换 HTTP/HTTPS。
- 当前解决办法：`AmapRouteClient` 对限流/配额异常回退 `RuleBasedRouteClient`，并在结果摘要中说明。
- 后续注意：后续应考虑路线缓存、请求合并、重试退避和配额监控；不能通过无界重试解决。

## 4. POI 搜索结果不是天然的“景点列表”

- 现象：曾出现动物园重复出现在多天、或把餐饮公司/企业类 POI 放进景点行程；预算或城市变化后结果可能明显不合理。
- 根因：高德文本 POI 搜索会返回多个类别和候选项；当前系统还需要更严格的类别、城市、距离、去重和跨天分配约束。
- 已尝试过的错误方案：只按关键词返回顺序取前几个，或只依赖前端偏好控制结果。
- 当前解决办法：PlannerAgent 做基础清洗、评分、预算和距离筛选，并按日选择候选；但问题尚未系统性解决。
- 后续注意：不要把“返回了 POI”当作“返回了合格景点”；应增加可解释的类别白名单/黑名单、跨天去重和城市边界校验。

## 5. LLM 失败时容易被误认为“没有调用”

- 现象：RequirementAnalysisService 和 LlmCriticAgent 仍能返回结果，但页面看不出是否真实调用了 LLM。
- 根因：`LlmClient.isAvailable()` 受 enabled、Key、URL、model 影响；调用失败后会分别回退规则分析/规则批评。
- 已尝试过的错误方案：只根据最终文案推断 LLM 是否执行。
- 当前解决办法：模型结果记录 `analysisSource`/`critiqueSource`，轨迹和日志标注 llm、rule 或 rule-fallback；仍需在运行环境中核对配置和日志。
- 后续注意：LLM endpoint、模型名、Key 和网络必须一起验证；不要把 fallback 结果宣传成 LLM 结果。

## 6. 直接在终端或聊天中暴露密钥

- 现象：高德、LLM Key 和服务器凭据曾出现在命令、截图或对话内容中。
- 根因：调试时把敏感值直接写入命令或环境配置展示。
- 已尝试过的错误方案：把真实 Key 放进代码或 README 方便复现。
- 当前解决办法：代码通过环境变量读取，`.gitignore` 忽略本地配置；已暴露凭据应立即轮换。
- 后续注意：日志、截图、Git 历史、README 和测试输出都不能出现完整 Key；示例只能使用占位符。

## 7. 网络环境问题不等于 API Key 无效

- 现象：直连 OpenAI 或高德 Web JS 资源时出现 TLS/连接超时；另一些高德服务又可以成功。
- 根因：VPN、代理、IPv6、运行环境出口或浏览器安全策略可能影响不同域名；连接失败和 API 返回 `INVALID_USER_KEY` 是不同问题。
- 已尝试过的错误方案：只改 API Key、连续重试、用空输出的 `curl -s` 隐藏诊断信息。
- 当前解决办法：使用 `curl -v/-i` 区分 DNS、TCP、TLS、HTTP 和业务错误；在服务端 provider 失败时走已定义的 fallback。
- 后续注意：排查顺序应是网络连通性 -> TLS -> HTTP 状态 -> provider 业务码 -> Key/配额，不要混为一类。

## 8. 测试 JDK 与依赖环境不一致

- 现象：部分 Mockito/Byte Buddy 测试在较新 JDK 上可能因无法 self-attach 失败，但编译或针对性 API 测试可以通过。
- 根因：测试工具链与当前 JDK/运行参数不匹配，不一定是业务代码错误。
- 已尝试过的错误方案：看到测试失败就直接修改业务逻辑。
- 当前解决办法：区分编译失败、业务测试失败和测试运行时环境失败，在目标 JDK/CI 中复核。
- 后续注意：不要为了绕过测试工具限制而加入无关生产代码；应升级测试依赖或固定 CI JDK。
