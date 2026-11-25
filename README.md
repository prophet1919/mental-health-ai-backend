# 心理健康 AI 助手 · 后端

基于 Spring Boot 4 + Spring AI 的高校心理健康陪伴系统后端，提供 AI 流式对话、会话管理、情绪记录与后台审阅能力。

> 前端仓库：[mental-health-ai-frontend](https://github.com/prophet1919/mental-health-ai-frontend)

---

## 项目简介

面向在校学生提供 7×24 小时的情绪倾诉入口。学生通过对话获得共情回应与初步疏导，对话记录沉淀为可回溯的咨询档案，心理中心可在后台进行审阅。

**当前已实现**：用户注册登录、JWT 鉴权、AI 流式对话、多轮上下文、会话历史、后台会话审阅、健康检查。
**规划中**：情绪自动标注、知识库文章、危机事件处置、数据看板。

---

## 技术栈

| 类别 | 选型 | 版本 |
| --- | --- | --- |
| 语言 | Java | 17 |
| 框架 | Spring Boot | 4.1.2 |
| AI 接入 | Spring AI（OpenAI 兼容协议） | 2.0.2 |
| 大模型 | DeepSeek `deepseek-flash` | — |
| ORM | MyBatis-Plus | 3.5.18 |
| 数据库 | MySQL | 8.0 |
| 安全 | Spring Security + Auth0 java-jwt | 4.4.0 |
| 工具 | Hutool | 5.8.25 |
| 构建 | Maven | 3.9+ |

---

## 快速开始

### 1. 环境要求

- JDK 17（**必须是 17**，JDK 21+ 上 Lombok 可能报 `NoSuchFieldError`）
- Maven 3.9+
- MySQL 8.0+

### 2. 建库建表

```bash
mysql -uroot -p -e "CREATE DATABASE mentalhealth_ds_test1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p mentalhealth_ds_test1 < sql/schema.sql
```

### 3. 配置密钥

项目**不包含任何明文密钥**。所有敏感项通过环境变量或本地配置文件提供。

```bash
# 复制模板
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填入你的真实值：

```yaml
spring:
  datasource:
    password: 你的数据库密码
  ai:
    openai:
      api-key: sk-你的DeepSeek密钥

jwt:
  secret: 至少32位的随机字符串
```

> `application-local.yml` 已被 `.gitignore` 忽略，不会误提交。

**获取 DeepSeek API Key**：https://platform.deepseek.com/api_keys

**生成 JWT 密钥**：
```bash
python -c "import secrets,string; print(''.join(secrets.choice(string.ascii_letters+string.digits) for _ in range(64)))"
```

### 4. 启动

```bash
./mvnw spring-boot:run
```

服务默认监听 `1236` 端口。启动时会校验 `JWT_SECRET`，若为空或短于 32 位会**直接启动失败**（防止用空密钥上线）。

看到下面这行说明配置正确：

```
配置校验通过：JWT 密钥已就绪（64 位），AI 密钥已配置。
```

### 5. 验证

```bash
curl http://localhost:1236/api/health
```

期望返回：

```json
{
  "code": "200",
  "data": {
    "status": "UP",
    "dbReachable": true,
    "memoryStore": "mysql",
    "database": "jdbc:mysql://.../mentalhealth_ds_test1"
  },
  "msg": "操作成功"
}
```

---

## 配置项

所有配置均支持环境变量覆盖，便于容器化部署。

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `DB_URL` | 数据库连接串 | 本机 `mentalhealth_ds_test1` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | 空 |
| `DEEPSEEK_API_KEY` | 大模型 API Key | 空（缺失时对话不可用，其余功能正常） |
| `AI_MODEL` | 模型名 | `deepseek-flash` |
| `AI_BASE_URL` | API 地址 | `https://api.deepseek.com` |
| `JWT_SECRET` | JWT 签名密钥，**必填**，≥32 位 | 空（为空则启动失败） |
| `SERVER_PORT` | 服务端口 | `1236` |
| `CHAT_MEMORY_STORE` | 上下文存储实现 | `mysql` |
| `CORS_ALLOWED_ORIGINS` | 允许的前端来源 | 本地 5173/5174 |

---

## 项目结构

```
src/main/java/com/wzu/aispringboot/
├── AiService/              AI 对话核心
│   ├── PromptManage.java           系统提示词
│   ├── PsychologicalSupportService.java  对话编排
│   ├── ChatStreamEvent.java        流式事件模型
│   └── memory/                     上下文存储抽象
│       ├── ChatMemoryStore.java            接口
│       └── MysqlChatMemoryStore.java       MySQL 实现
├── config/                 配置
│   ├── SecurityConfig.java        安全过滤链 + 白名单
│   ├── CorsConfig.java            跨域
│   ├── ChatClientConfig.java      ChatClient
│   ├── JwtConfig.java             JWT 参数
│   └── StartupConfigValidator.java 启动期密钥校验
├── controller/             接口层
│   ├── User.java                  注册/登录/当前用户
│   ├── PsychologicalChat.java     会话与流式对话
│   ├── AdminConsultationController.java  后台审阅
│   ├── HealthController.java      健康检查
│   └── Test.java
├── service/                业务层
│   ├── UserService.java
│   ├── ConsultationSessionService.java
│   ├── ConsultationMessageService.java
│   ├── ConsultationQueryService.java     查询与归属校验
│   ├── CurrentUserHolder.java            当前用户上下文
│   └── convert/UserConvert.java
├── mapper/                 MyBatis-Plus Mapper
├── entity/                 数据库实体
├── DTO/                    入参(command) / 出参(response)
├── common/                 统一响应与全局异常
├── util/                   JWT 工具与过滤器
├── exception/              业务异常
└── enumclass/              用户类型、状态枚举
```

---

## 核心设计说明

### 对话为什么拆成两个接口

| 接口 | 职责 |
| --- | --- |
| `POST /session/start` | 只创建空会话，返回 `sessionId`，**不写任何消息** |
| `POST /stream` | 写用户消息 → 重建上下文 → 流式返回 → 写 AI 消息 |

拆分原因：会话创建与回答的生命周期不同。前端需要先拿到 `sessionId` 才能渲染左侧列表和建立 SSE 连接。

早期实现是「创建时写入首条消息 + stream 时用消息数比对去重」，逻辑脆弱——文案改一个字或前端重发就会插入重复行。现在职责单一，`start` 不再产生数据。

### 上下文如何重建

`ChatMemoryStore` 是接口，当前实现 `MysqlChatMemoryStore` 每次请求按 `sessionId` 查最近 N 条消息（默认 20）重建。好处：

- 服务重启**不丢上下文**
- 不依赖 Redis 等中间件
- 读取失败降级为空上下文，不阻断对话

后续要接 Redis，只需实现 `ChatMemoryStore` 并切换 `CHAT_MEMORY_STORE=redis`，业务代码零改动。

### 流式事件模型

统一四类事件，前端按 `event` 名分派：

| 事件 | 含义 | 前端表现 |
| --- | --- | --- |
| `thinking` | 模型推理中 | 显示「正在思考…」 |
| `message` | 正文片段 | 逐字追加 |
| `done` | 正常结束 | 收起加载态 |
| `error` | 调用失败 | 显示可重试气泡，**不写 AI 消息落库** |

`thinking` 的存在是因为 `deepseek-flash` 是推理模型：实测流式前几帧 `delta.content` 全为 `null`，只有 `reasoning_content` 有值。若不做处理，用户点击发送后会空白数秒，像卡死。

### 安全设计

- **鉴权**：统一 `Authorization: Bearer <token>`，白名单仅登录/注册/健康检查/公开文章
- **越权防护**：`ConsultationQueryService.requireOwnedSession` 集中校验归属，他人会话返回「无权访问」
- **角色隔离**：`/api/admin/**` 要求 `ROLE_2`，普通用户访问返回 403
- **密钥管理**：全部走环境变量，仓库内无明文；`JWT_SECRET` 为空直接启动失败
- **日志**：不打印密码、token、消息正文

---

## 接口一览

基地址 `http://localhost:1236/api`，统一响应格式 `{ code, msg, data }`。

### 认证

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/user/add` | 注册 | 否 |
| POST | `/user/login` | 登录（用户名或邮箱） | 否 |
| GET | `/user/current` | 当前用户详情 | 是 |

### 对话

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/psychological-chat/session/start` | 创建空会话 | 是 |
| POST | `/psychological-chat/stream` | SSE 流式对话 | 是 |
| GET | `/psychological-chat/sessions` | 会话列表（分页） | 是 |
| GET | `/psychological-chat/sessions/{id}` | 会话详情 | 是 |
| GET | `/psychological-chat/sessions/{id}/messages` | 消息分页（游标） | 是 |

### 后台

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/admin/consultations` | 全部会话列表 | 管理员 |
| GET | `/admin/consultations/{id}` | 会话详情 | 管理员 |
| GET | `/admin/consultations/{id}/messages` | 会话消息 | 管理员 |

### 其他

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/health` | 健康检查 | 否 |

### 调用示例

**登录**
```bash
curl -X POST http://localhost:1236/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

**流式对话**
```bash
curl -N -X POST http://localhost:1236/api/psychological-chat/stream \
  -H "Authorization: Bearer <你的token>" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"session_26","userMessage":"我最近压力很大"}'
```

响应：
```
event:thinking
data:{}

event:message
data:{"content":"我能"}

event:message
data:{"content":"理解"}

event:done
data:{}
```

---

## 数据库

9 张表，建表语句见 [`sql/schema.sql`](sql/schema.sql)。

| 表 | 用途 | 后端是否已接入 |
| --- | --- | --- |
| `user` | 用户 | ✅ |
| `consultation_session` | 咨询会话 | ✅ |
| `consultation_message` | 会话消息 | ✅ |
| `knowledge_category` | 文章分类 | ⬜ 规划中 |
| `knowledge_article` | 知识文章 | ⬜ 规划中 |
| `emotion_diary` | 情绪日记 | ⬜ 规划中 |
| `user_favorite` | 文章收藏 | ⬜ 规划中 |
| `ai_analysis_task` | AI 分析任务队列 | ⬜ 规划中 |
| `sys_file_info` | 文件元信息 | ⬜ 规划中 |

---

## 已知限制

1. **上下文窗口固定 20 条**，长对话会丢弃早期内容，可通过 `app.chat-memory.max-messages` 调整
2. **对话接口非幂等**，重复提交会重复落库与计费，前端已禁用自动重试
3. **无接口限流**，生产环境需在网关层补充
4. **无单元测试**，当前验证方式为接口联调
5. **`deepseek-flash` 为推理模型**，首字延迟受推理耗时影响

---

## 开发约定

- 分支：`main` 为稳定分支，功能在 `vibecoding` 等分支开发后合并
- 提交信息：`【后端】动词 + 内容`，正文说明**为什么改**
- 禁止提交任何密钥；本地配置放 `application-local.yml`

---

## 许可证

本项目为课程设计作品，仅供学习交流。
