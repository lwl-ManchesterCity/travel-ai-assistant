# 智能旅行助手 Agent 系统

一个基于 **Java 17 + Spring Boot 3** 的智能旅行规划项目，参考 Datawhale《hello-agents》第 13 章“智能旅行助手”的核心思想实现。

项目不是简单生成一段旅行文案，而是围绕：

```text
用户需求输入
  -> 多 Agent 信息收集
  -> 行程规划决策
  -> 路线、预算、天气评估
  -> 前端可视化展示
  -> 用户自然语言调整
```

构建了一条完整的智能旅行规划链路。

## 项目定位

本项目是一个学习版 Agent 应用项目，但已经具备较完整的工程雏形：

* 支持真实第三方 API 接入
* 支持多 Agent 协作规划
* 支持 LLM 自然语言理解与结果润色
* 支持前端旅行计划可视化展示
* 支持 Jar 包部署、Systemd 托管与 Nginx 反向代理

适合作为 Java 后端简历项目，用于展示 **Spring Boot 工程能力、第三方 API 接入能力、AI 应用落地能力和完整项目交付能力**。

## 项目亮点

* **多 Agent 协作**：拆分景点、酒店、天气、路线、规划、调整等 Agent，职责边界清晰。
* **真实地图能力**：接入高德地图 POI 搜索、酒店检索、天气预报、路线规划和 Web 地图 JS SDK。
* **LLM 能力接入**：通过 OpenAI-Compatible 接口接入通义千问，支持自然语言需求解析、行程调整和结果文案润色。
* **Loop Engineering 闭环**：`LoopPlanningAgent` 串联 `PlannerAgent -> ReflectionAgent -> LlmCriticAgent -> LoopDecisionExecutor`，实现“生成 - 评估 - 反思 - 修正”的多轮收敛。
* **Agent 轨迹可视化**：记录各 Agent 的输入、动作、输出和状态，前端可直接查看智能体执行过程。
* **结构化行程输出**：返回每日景点、酒店、餐饮、路线、预算、天气、地图点位等可消费数据。
* **预算与路线约束**：在规划阶段加入预算控制、跨天去重、通勤距离压缩、酒店优选、天气室内外切换和路线评估。
* **前端完整展示**：支持旅行表单、结果页、预算明细、真实地图、每日路线切换、行程编辑、调整反馈和 Agent 轨迹折叠查看。
* **可部署上线**：支持 Jar 包部署、Systemd 托管和 Nginx 反向代理。

## 技术栈

| 分类   | 技术                                               |
| ---- | ------------------------------------------------ |
| 后端   | Java 17, Spring Boot 3.3, Spring MVC, Validation |
| 前端   | HTML, CSS, JavaScript, 高德地图 JS SDK               |
| 外部服务 | 高德开放平台, 通义千问 DashScope OpenAI-Compatible API     |
| 工程化  | Maven, Git, Linux, Nginx, Systemd                |
| 数据处理 | Java HttpClient, Jackson                         |

## 核心功能

### 1. 旅行计划生成

用户输入目的地、日期、预算、交通方式、住宿偏好、旅行偏好和额外要求后，系统会生成多日旅行计划。

输出内容包括：

* 每日景点安排
* 酒店推荐
* 餐饮建议
* 每日路线规划
* 天气信息
* 预算明细
* 地图点位数据

### 2. 高德真实数据接入

系统接入高德开放平台，支持真实旅行数据查询：

* 景点：基于城市和偏好关键词搜索 POI
* 酒店：基于目的地和住宿偏好检索酒店
* 天气：查询未来天气预报
* 路线：根据每日酒店和景点顺序规划路线
* 地图：前端展示真实地图、Marker 和路线 Polyline

### 3. LLM 增强能力

通过 OpenAI-Compatible 接口接入通义千问，支持：

* 解析自然语言需求，例如“海鲜过敏”“节奏轻松一点”“想看地标”
* 根据用户输入调整已生成行程
* 对行程总览、推荐理由和规划备注进行文案润色
* 在第三方 API 数据基础上生成更自然的旅行说明

### 4. 规划评估与调整

系统在生成行程时加入多维度约束：

* 根据预算判断是否超支
* 根据天气倾向选择室内或室外景点
* 根据路线距离和通勤时长压缩景点数量
* 避免跨天重复安排同一景点
* 对第三方 API 异常提供规则兜底方案

### 5. Loop Planning 自循环优化

系统默认启用多轮循环优化，而不是只生成一次结果就直接返回。

完整闭环如下：

```text
第 1 步：PlannerAgent 生成候选方案
第 2 步：ReflectionAgent 对预算 / 路线 / 天气 / 体验进行打分和评估
第 3 步：LlmCriticAgent 给出下一步最优动作
第 4 步：LoopDecisionExecutor 修改下一轮请求条件
第 5 步：重新调用 PlannerAgent 生成新方案
重复以上过程，直到：
  - 达到可接受状态
  - 连续多轮无改善
  - 请求调整重复
  - 达到最大循环轮次
```

当前支持的典型优化动作：

* `REDUCE_BUDGET`：降低酒店档位、压缩高成本景点、收紧预算策略
* `COMPRESS_ROUTE`：优先更近的景点组合，减少远距离通勤
* `REPLACE_OUTDOOR_WITH_INDOOR`：恶劣天气时增加室内备选约束
* `UPGRADE_EXPERIENCE`：预算充足时提升体验档位

## 系统链路

### 主规划链路

```text
TripPlanController
  -> TripPlanningService
    -> LoopPlanningAgent
      -> PlannerInputBuilder
        -> AttractionAgent -> AttractionClient -> AmapAttractionClient
        -> HotelAgent      -> HotelClient      -> AmapHotelClient
        -> WeatherAgent    -> WeatherClient    -> AmapWeatherClient
        -> RequirementAnalysisService          -> LLM / 规则解析
      -> PlannerAgent
        -> RouteAgent      -> RouteClient      -> AmapRouteClient
      -> ReflectionAgent   -> DailyPlanEvaluator
      -> LlmCriticAgent    -> LLM / fallback critique
      -> LoopDecisionExecutor
    -> TripPlan（含 loopSummary / loopIterations / agentTraces）
```

### 行程调整链路

```text
TripPlanController
  -> TripPlanningService
    -> TripAdjustmentAgent
      -> LLM 解析调整意图
      -> 重新构造 TripPlanRequest
    -> planTrip
```

### 前端结果页链路

```text
index.html
  -> POST /api/trips/plan
  -> 渲染 TripPlan
    -> 行程总览
    -> 预算进度与预算明细
    -> 高德真实地图 + 每日路线切换
    -> 每日行程卡片
    -> 天气信息
    -> Agent 执行轨迹与 Loop 轮次
```

## 项目结构

```text
src/main/java/com/lwl/travelassistant
├── agent        # Agent 编排单元，例如景点、酒店、天气、路线、规划、反思、循环优化、调整
├── client       # 外部能力抽象与实现，例如高德、规则兜底、LLM Client
├── config       # 配置属性、客户端配置、运行时日志
├── controller   # HTTP 接口入口
├── evaluator    # 预算、路线、天气、每日计划评估
├── exception    # 全局异常处理
├── model        # DTO、领域模型、响应模型
└── service      # 主编排服务、输入构建、LLM 文案服务
```

前端页面位于：

```text
src/main/resources/static/index.html
```

## 接口说明

### 生成旅行计划

```http
POST /api/trips/plan
Content-Type: application/json
```

请求示例：

```json
{
  "city": "广州",
  "startDate": "2026-06-20",
  "endDate": "2026-06-22",
  "budget": 5000,
  "preferences": ["自然风景", "美食", "轻松节奏"],
  "transportation": "公共交通",
  "accommodation": "舒适型酒店",
  "extraRequirements": "海鲜过敏，希望节奏轻松一点"
}
```

### 调整旅行计划

```http
POST /api/trips/adjust
Content-Type: application/json
```

支持类似下面的自然语言调整：

```text
预算改成 8000，酒店换好一点，不要安排海鲜，多安排自然风景
```

### 获取地图配置

```http
GET /api/map/config
```

前端通过该接口获取高德 Web JS Key 和安全密钥配置。

## 本地启动

### 1. 环境要求

* JDK 17+
* Maven 3.8+
* 高德开放平台 Key
* DashScope API Key，可选；不开启 LLM 时不需要

### 2. 配置环境变量

不要把真实 Key 写死到代码或提交到 GitHub。推荐使用环境变量：

```bash
export AMAP_API_KEY="你的高德 Web 服务 Key"
export AMAP_WEB_JS_KEY="你的高德 Web 端 JS API Key"
export AMAP_SECURITY_JS_CODE="你的高德 Web 端安全密钥"
export DASHSCOPE_API_KEY="你的通义千问 API Key"
export TRAVEL_LLM_ENABLED=true
```

如果暂时不使用 LLM：

```bash
export TRAVEL_LLM_ENABLED=false
```

如果想关闭 loop 自循环，只保留单轮规划：

```bash
export TRAVEL_LOOP_ENABLED=false
```

### 3. 启动项目

```bash
mvn spring-boot:run
```

浏览器访问：

```text
http://localhost:8080
```

注意：前端页面由 Spring Boot 静态资源托管，不能直接双击 `index.html` 使用 `file://` 打开，否则浏览器可能会拦截接口请求。

## 配置说明

核心配置文件位于：

```text
src/main/resources/application.yml
```

关键配置示例：

```yaml
travel:
  loop:
    enabled: ${TRAVEL_LOOP_ENABLED:true}
    max-rounds: ${TRAVEL_LOOP_MAX_ROUNDS:3}
    max-no-improvement-rounds: ${TRAVEL_LOOP_MAX_NO_IMPROVEMENT_ROUNDS:2}

  llm:
    enabled: ${TRAVEL_LLM_ENABLED:false}
    provider: ${TRAVEL_LLM_PROVIDER:dashscope}
    base-url: ${TRAVEL_LLM_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
    api-key: ${TRAVEL_LLM_API_KEY:${DASHSCOPE_API_KEY:}}
    model: ${TRAVEL_LLM_MODEL:qwen-plus}

  providers:
    attraction: amap
    weather: amap
    hotel: amap
    route: amap
    amap:
      api-key: ${AMAP_API_KEY:}
      web-js-key: ${AMAP_WEB_JS_KEY:}
      security-js-code: ${AMAP_SECURITY_JS_CODE:}
      mock-response: false
```

Provider 支持切换为真实高德接口或本地兜底实现：

```yaml
travel:
  providers:
    attraction: amap # 或 mock
    weather: amap    # 或 mock
    hotel: amap      # 或 mock
    route: amap      # 或 mock
```

说明：

* `travel.loop.enabled=false` 时，系统会直接走单轮 `PlannerAgent`
* `travel.loop.enabled=true` 时，系统会走完整 Loop Planning 闭环
* 路线、天气、景点、酒店任一真实接口受限时，会自动回退到规则 provider，保证主链路可用

## 打包与部署

### 打包

```bash
mvn clean package -DskipTests
```

生成文件：

```text
target/travel-assistant-0.0.1-SNAPSHOT.jar
```

### Linux 运行示例

```bash
java -jar target/travel-assistant-0.0.1-SNAPSHOT.jar
```

生产环境建议使用：

* Systemd 管理后端进程
* Nginx 反向代理到 Spring Boot 服务
* 环境变量文件保存 API Key

## 测试

运行测试：

```bash
mvn test
```

当前测试重点：

* `TripPlanControllerTest`：验证接口成功返回和参数校验。
* `PlannerAgentTest`：验证规划链路核心逻辑。

建议手工验证场景：

* 低预算场景：确认系统会自动压缩酒店或景点
* 暴雨天气场景：确认系统会优先考虑室内备选
* 高预算场景：确认系统能够放宽体验约束
* 行程调整场景：确认自然语言编辑会触发重新规划
* Agent 轨迹场景：确认前端能看到 loop 轮次与执行步骤

## GitHub 提交前注意

请确认不要提交以下敏感信息：

* 高德 Web 服务 Key
* 高德 Web JS Key
* 高德安全密钥
* DashScope API Key
* 服务器密码、SSH 密钥、环境变量文件

建议提交前检查：

```bash
git status
git diff
```

## 后续优化方向

* 接入稳定的真实景点图片来源，替换当前占位图
* 将前端拆成 Vue / React 独立工程
* 增加用户登录和历史行程保存
* 增加更细粒度的 Reflection 评分机制和可解释性指标
* 引入缓存，降低高德 API 和 LLM 调用频率
* 增加 Dockerfile 和 Docker Compose，简化部署
