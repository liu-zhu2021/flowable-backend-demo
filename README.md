# Flowable Demo Project

本项目是一个基于 Spring Boot + Flowable + Vue 3 的简单审批流演示系统，实现了前后端分离的审批流程闭环。

## 1. 项目功能

- **发起审批**：填写业务Key和申请原因，启动审批流程。
- **待办任务**：查询当前用户的待办任务，支持审批通过或驳回。
- **审批历史**：查看运行中和已结束的流程实例状态及历史记录。

## 2. 技术栈

### 后端 (flowable-demo/flowable)
- JDK 17
- Spring Boot 3.2.0
- Flowable 7.0.0 (流程引擎)
- H2 Database (嵌入式数据库)
- Lombok

### 前端 (flowable-demo/flowable-front)
- Vue 3
- Vite
- Vue Router
- Element Plus (UI组件库)
- Axios

## 3. 目录结构

```
flowable-demo/
├── flowable/                # 后端项目
│   ├── src/main/java/       # Java 源码
│   ├── src/main/resources/  # 资源文件
│   │   ├── processes/       # BPMN 流程定义文件
│   │   └── application.properties
│   └── pom.xml
└── flowable-front/          # 前端项目
    ├── src/                 # Vue 源码
    │   ├── views/           # 页面组件
    │   └── router/          # 路由配置
    ├── package.json
    └── vite.config.js
```

## 4. 环境准备

- JDK 17+
- Node.js 16+ (建议 18+)
- Maven 3.6+ (可选，项目包含 mvnw)

## 5. 启动步骤

### 后端启动

1. 进入后端目录：
   ```bash
   cd flowable
   ```
2. 运行 Spring Boot 应用：
   ```bash
   # Linux/macOS
   ./mvnw spring-boot:run
   
   # Windows
   mvnw.cmd spring-boot:run
   ```
   或者使用 IDE (IntelliJ IDEA) 打开 `flowable` 目录，运行 `FlowableApplication.java`。
   
   启动成功后，后端服务运行在 `http://localhost:8080`。

### 前端启动

1. 进入前端目录：
   ```bash
   cd flowable-front
   ```
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
   启动成功后，通常运行在 `http://localhost:5173`。

## 6. 操作演示

1. **发起审批**
   - 打开浏览器访问前端地址 (e.g. `http://localhost:5173`)。
   - 默认进入“发起审批”页面。
   - 输入“业务Key”（如 `ORDER-001`）和“申请原因”。
   - 点击“提交申请”，提示成功并返回流程实例ID。

2. **待办任务处理**
   - 点击顶部导航栏“待办任务”。
   - 默认查询 `admin` 用户的待办任务（本演示中流程默认分配给 `managers` 组，代码逻辑设定 `admin` 可查看 `managers` 组任务）。
   - 在列表中可以看到刚才发起的任务。
   - 点击“通过”或“驳回”按钮进行审批。

3. **查看历史**
   - 点击顶部导航栏“审批历史”。
   - “运行中流程”表格显示当前活跃的流程实例。
   - “已结束流程”表格显示已完成的流程实例及其变量信息（如审批结果）。

## 7. 关键接口说明

所有接口前缀为 `/process`。

1. **发起流程**
   - `POST /process/start`
   - Body: `{ "processKey": "simple-approval", "businessKey": "123", "variables": { "reason": "test" } }`

2. **查询任务**
   - `GET /process/tasks?assignee=admin`

3. **完成任务**
   - `POST /process/complete`
   - Body: `{ "taskId": "xxx", "action": "approved", "variables": { ... } }`

4. **查询历史**
   - `GET /process/history`

## 8. 常见问题

- **端口冲突**：确保 8080 端口未被占用。如需修改，编辑 `flowable/src/main/resources/application.properties` 中的 `server.port`。
- **跨域问题**：前端 `vite.config.js` 已配置代理 `/api` 转发到 `localhost:8080`，后端 Controller 也添加了 `@CrossOrigin` 注解以支持跨域。
- **流程图修改**：流程定义文件位于 `flowable/src/main/resources/processes/simple-approval.bpmn20.xml`。修改后重启后端生效（`flowable.database-schema-update=true` 会尝试更新）。

