# Flowable Demo 后端学习文档

> 本文档面向零基础初学者，帮你从零开始理解这个基于 Flowable 的简单审批流程项目。
> 建议按文档顺序从头到尾阅读，每一节都会告诉你"学什么、看哪里、搞懂什么"。

---

## 目录

1. [项目简介](#1-项目简介)
2. [技术栈一览](#2-技术栈一览)
3. [整体架构与调用链路](#3-整体架构与调用链路)
4. [项目目录结构](#4-项目目录结构)
5. [推荐学习路径（按顺序来）](#5-推荐学习路径按顺序来)
6. [Model 实体类详解：字段从何而来](#6-model-实体类详解字段从何而来)
7. [BPMN 流程定义文件详解](#7-bpmn-流程定义文件详解)
8. [如何自己创建一个新的 BPMN 流程](#8-如何自己创建一个新的-bpmn-流程)
9. [端到端流程串讲](#9-端到端流程串讲)
10. [实战：如何新增一个审批流程](#10-实战如何新增一个审批流程)
11. [Flowable 学习路线图](#11-flowable-学习路线图)
12. [常见问题 FAQ](#12-常见问题-faq)

---

## 1. 项目简介

这是一个**极简的审批流程 Demo 项目**，用来演示如何用 Spring Boot + Flowable 实现一个"提交申请 → 主管审批 → 通过/驳回"的业务流程。

**它能做什么？**

- 发起一个审批申请（比如请假、报销）
- 查看当前待审批的任务列表
- 审批人可以"通过"或"驳回"
- 查看运行中和已结束的流程历史

**项目的核心价值：** 让你理解"工作流引擎"（Flowable）是如何与普通的 Spring Boot 后端结合的，为你以后做更复杂的业务流程（多级审批、会签、自动通知等）打基础。

---

## 2. 技术栈一览

| 技术 | 版本 | 作用 |
|------|------|------|
| **Java** | 17 | 编程语言 |
| **Spring Boot** | 3.2.0 | 后端框架，提供 Web 服务、依赖注入、自动配置等 |
| **Flowable** | 7.0.0 | 工作流 / BPM 引擎，负责流程的定义、启动、推进、查询 |
| **H2 Database** | Spring Boot 管理 | 内嵌数据库，程序启动时自动创建，重启后数据清空 |
| **Lombok** | Spring Boot 管理 | 通过注解自动生成 getter/setter/构造函数等样板代码 |
| **Maven** | - | 项目构建和依赖管理工具 |

> **关键依赖说明（pom.xml）：**
> - `spring-boot-starter-web`：让项目变成一个 Web 应用，能处理 HTTP 请求
> - `flowable-spring-boot-starter`：Flowable 与 Spring Boot 的集成包，启动时自动配置流程引擎
> - `h2`：轻量级内存数据库，Flowable 的所有流程数据（流程定义、运行实例、历史记录）都存在这里

---

## 3. 整体架构与调用链路

### 3.1 架构图（文字版）

```
┌─────────────┐     HTTP      ┌────────────────────────────────────────────┐
│             │  ──────────▶  │            Spring Boot 后端                │
│   前端页面   │              │                                            │
│  (Vue 3)    │  ◀──────────  │  Controller ──▶ Service ──▶ Flowable 引擎  │
│             │    JSON 响应   │                               │            │
└─────────────┘              │                          ┌────▼────┐       │
                              │                          │ H2 数据库 │       │
                              │                          └─────────┘       │
                              └────────────────────────────────────────────┘
```

### 3.2 一次完整请求的调用链路

以"发起审批"为例：

```
1. 前端发 POST /api/process/start（带上 processKey、businessKey、variables）
        │
        ▼
2. ProcessController.startProcess() 接收请求，解析 JSON 为 StartProcessRequest 对象
        │
        ▼
3. ProcessService.startProcess() 被调用
        │
        ▼
4. runtimeService.startProcessInstanceByKey("simple-approval", businessKey, variables)
   └── Flowable 引擎根据流程定义 (simple-approval.bpmn20.xml) 创建一个流程实例
   └── 流程走到第一个 userTask（Manager Approval），等待人工处理
   └── 流程数据持久化到 H2 数据库
        │
        ▼
5. 返回流程实例 ID 给 Controller
        │
        ▼
6. Controller 包装成 Result<String> 返回给前端
```

### 3.3 各层职责

| 层 | 职责 | 对应本项目 |
|----|------|-----------|
| **Controller** | 接收 HTTP 请求、参数校验、调用 Service、返回响应 | `ProcessController.java` |
| **Service** | 业务逻辑，调用 Flowable 引擎 API | `ProcessService.java` |
| **Model** | 数据传输对象（DTO），定义请求和响应的数据结构 | `model/` 包下 4 个类 |
| **Common** | 通用工具类，如统一响应格式 | `Result.java` |
| **Flowable 引擎** | 流程管理（不需要你写代码，通过 API 调用即可） | 由 `flowable-spring-boot-starter` 自动注入 |
| **数据库** | 持久化所有流程数据 | H2 内存数据库，Flowable 自动建表 |

---

## 4. 项目目录结构

```
flowable/
├── pom.xml                                          # Maven 配置（依赖、构建）
├── src/
│   ├── main/
│   │   ├── java/org/example/flowable/
│   │   │   ├── FlowableApplication.java             # 启动类 + 健康检查接口
│   │   │   ├── controller/
│   │   │   │   └── ProcessController.java           # REST 接口层（5 个接口）
│   │   │   ├── service/
│   │   │   │   └── ProcessService.java              # 业务逻辑层（调用 Flowable API）
│   │   │   ├── model/
│   │   │   │   ├── StartProcessRequest.java         # 发起流程的请求体
│   │   │   │   ├── TaskActionRequest.java           # 完成任务的请求体
│   │   │   │   ├── TaskRepresentation.java          # 任务信息的响应体
│   │   │   │   └── ProcessInstanceRepresentation.java # 流程实例信息的响应体
│   │   │   └── common/
│   │   │       └── Result.java                      # 统一响应格式 {code, message, data}
│   │   └── resources/
│   │       ├── application.properties               # 应用配置
│   │       └── processes/
│   │           └── simple-approval.bpmn20.xml       # BPMN 流程定义文件
│   └── test/
│       └── java/org/example/flowable/
│           └── FlowableApplicationTests.java        # 基础测试
```

> **重要：** 本项目没有 Repository 层（没有 JPA / MyBatis），因为流程数据全部通过 Flowable 引擎 API 来读写，Flowable 内部自己管理了数据库表。

---

## 5. 推荐学习路径（按顺序来）

### 第一步：先看配置，理解项目怎么跑起来

**看什么文件：**
- `src/main/resources/application.properties`

**关键配置项解释：**

| 配置项 | 值 | 含义 |
|--------|-----|------|
| `server.port=8080` | 8080 | 后端服务端口号 |
| `spring.datasource.url=jdbc:h2:mem:flowable` | H2 内存库 | 数据存内存，重启后清空 |
| `spring.h2.console.enabled=true` | true | 可以通过 `/h2-console` 访问数据库管理界面 |
| `flowable.database-schema-update=true` | true | 启动时 Flowable 自动创建/更新数据库表 |
| `flowable.async-executor-activate=true` | true | 激活异步执行器（用于定时任务等） |

**看完你应该能回答：**
- 项目用的什么数据库？数据会不会丢？
- Flowable 的表是怎么创建的？需要我手动建表吗？
- 如何访问数据库管理界面？

---

### 第二步：看启动类，理解项目入口

**看什么文件：**
- `src/main/java/org/example/flowable/FlowableApplication.java`

**关注点：**
- `@SpringBootApplication`：标记这是 Spring Boot 应用的入口
- `main()` 方法：程序启动点
- `/hello` 接口：一个简单的健康检查，验证服务是否正常

**看完你应该能回答：**
- 如何启动这个项目？（运行 `FlowableApplication.main()`）
- 启动后怎么验证它活着？（访问 `http://localhost:8080/hello`）

---

### 第三步：看 Controller，理解有哪些接口

**看什么文件：**
- `src/main/java/org/example/flowable/controller/ProcessController.java`

**API 接口一览：**

| HTTP 方法 | 路径 | 对应方法 | 作用 |
|-----------|------|---------|------|
| POST | `/process/start` | `startProcess()` | 发起一个新的审批流程 |
| GET | `/process/tasks?assignee=admin` | `getTasks()` | 查询某用户的待办任务 |
| POST | `/process/complete` | `completeTask()` | 完成（通过/驳回）一个任务 |
| GET | `/process/history` | `getHistory()` | 查看已结束的流程 |
| GET | `/process/running` | `getRunning()` | 查看运行中的流程 |

**关注点：**
- `@CrossOrigin(origins = "*")`：允许跨域请求，因为前端和后端端口不同
- 每个方法都返回 `Result<T>`：统一响应格式
- 方法内部都是调用 `processService` 的对应方法，Controller 层本身不含业务逻辑

**看完你应该能回答：**
- 前端能调哪些接口？
- 每个接口需要什么参数、返回什么数据？
- Controller 层做了什么、没做什么？

---

### 第四步：看 Service，理解核心业务逻辑

**看什么文件：**
- `src/main/java/org/example/flowable/service/ProcessService.java`

这是整个项目**最重要的文件**，因为它展示了如何与 Flowable 引擎交互。

**Flowable 提供的三大核心 Service：**

| Service | 作用 | 本项目怎么用 |
|---------|------|-------------|
| `RuntimeService` | 管理运行中的流程实例（启动、查询、删除等） | 启动流程、查询运行中的流程 |
| `TaskService` | 管理人工任务（查询、认领、完成等） | 查询待办、完成任务 |
| `HistoryService` | 查询历史数据（已完成的流程、任务等） | 查询已结束的流程 |

**逐方法解读：**

**① `startProcess(processKey, businessKey, variables)`**
```java
ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
    processKey,    // 流程定义的 key，对应 BPMN 文件中 process 的 id
    businessKey,   // 业务键，关联你的业务数据（如订单号 ORDER-001）
    variables      // 流程变量，如 {reason: "出差申请"}
);
```
- 调用后，Flowable 根据 `processKey` 找到对应的 BPMN 定义，创建一个流程实例
- 流程从 startEvent 开始，自动走到第一个需要人工处理的节点（userTask）

**② `getTasks(assignee)`**
```java
List<Task> tasks = taskService.createTaskQuery()
    .taskAssignee(assignee)  // 查询直接分配给该用户的任务
    .list();
```
- 如果 assignee 是 "admin"，还会额外查询候选组为 "managers" 的任务
- 返回任务列表（包含任务 ID、名称、创建时间等）

**③ `completeTask(taskId, outcome, variables)`**
```java
    variables.put("outcome", outcome);  // outcome = "approved" 或 "rejected"
taskService.complete(taskId, variables);
```
- 完成任务时，把审批结果作为流程变量传入
- Flowable 引擎会根据这个变量，在排他网关处决定走哪条分支

**④ `getHistory()` / `getRunning()`**
- 分别查询已结束和运行中的流程实例信息

**看完你应该能回答：**
- Flowable 的 RuntimeService、TaskService、HistoryService 分别干什么？
- 启动一个流程需要哪些参数？
- 完成任务时，审批结果（approved/rejected）是怎么传给流程引擎的？
- 流程引擎怎么根据审批结果决定走哪条路？（答：通过流程变量 + 排他网关条件表达式）

---

### 第五步：看 Model 类，理解数据结构

**看什么文件：**
- `src/main/java/org/example/flowable/model/` 下的 4 个类

这些类不是数据库实体，而是 **DTO（Data Transfer Object）数据传输对象**，用来定义前后端之间传输数据的格式。

详见下方 [第 6 节](#6-model-实体类详解字段从何而来)。

---

### 第六步：看 BPMN 文件，理解流程定义

**看什么文件：**
- `src/main/resources/processes/simple-approval.bpmn20.xml`

这是整个审批流程的"蓝图"，定义了流程有哪些步骤、怎么流转。

详见下方 [第 7 节](#7-bpmn-流程定义文件详解)。

---

## 6. Model 实体类详解：字段从何而来

### 6.1 四个 Model 类概览

| 类名 | 角色 | 业务含义 |
|------|------|---------|
| `StartProcessRequest` | 请求体 DTO | 前端发起流程时传给后端的数据 |
| `TaskActionRequest` | 请求体 DTO | 前端完成任务时传给后端的数据 |
| `TaskRepresentation` | 响应体 DTO | 后端返回给前端的任务信息 |
| `ProcessInstanceRepresentation` | 响应体 DTO | 后端返回给前端的流程实例信息 |

### 6.2 逐类分析

#### ① StartProcessRequest — 发起流程请求

**文件路径：** `src/main/java/org/example/flowable/model/StartProcessRequest.java`

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| `processKey` | String | Flowable 需要 | 流程定义的 key，对应 BPMN 中 `<process id="simple-approval">`，告诉 Flowable 启动哪个流程 |
| `businessKey` | String | 业务需求 | 业务关联键，如 "ORDER-001"，用于把流程和你的业务数据关联起来 |
| `variables` | Map<String, Object> | 业务需求 + Flowable | 流程变量，如 `{reason: "出差"}` 会作为流程上下文，在后续节点中可以使用 |

**字段设计思路：**
- `processKey`：必须的，Flowable API 要求用 key 来启动流程
- `businessKey`：可选但推荐，方便以后根据业务 ID 查询对应的流程
- `variables`：灵活的 Map 结构，可以传入任意键值对作为流程变量

---

#### ② TaskActionRequest — 完成任务请求

**文件路径：** `src/main/java/org/example/flowable/model/TaskActionRequest.java`

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| `taskId` | String | Flowable 产生 | 任务 ID，由 Flowable 生成，前端从任务列表中获取 |
| `action` | String | 业务需求 | 审批动作，如 "approved" 或 "rejected"，会被映射为流程变量 `outcome` |
| `assignee` | String | 业务需求 | 可选，用于认领任务场景 |
| `variables` | Map<String, Object> | 业务需求 | 可选，完成任务时额外传入的流程变量 |

**字段设计思路：**
- `taskId`：必须的，告诉 Flowable 要完成哪个任务
- `action`：核心字段，Service 层会把它放入 `outcome` 变量，驱动排他网关的分支判断
- `variables`：扩展用，如果以后需要在审批时填写额外信息（如审批意见）

---

#### ③ TaskRepresentation — 任务响应

**文件路径：** `src/main/java/org/example/flowable/model/TaskRepresentation.java`

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| `id` | String | Flowable 产生 | 任务唯一 ID |
| `name` | String | BPMN 定义 | 任务名称，来自 BPMN 中 `<userTask name="Manager Approval">` |
| `assignee` | String | Flowable 管理 | 任务当前的处理人 |
| `createTime` | Date | Flowable 产生 | 任务创建时间 |
| `processInstanceId` | String | Flowable 产生 | 该任务所属的流程实例 ID |
| `processDefinitionId` | String | Flowable 产生 | 流程定义 ID（含版本号） |

**字段设计思路：**
- 这些字段全部来自 Flowable 的 `Task` 对象，是对 Flowable 内部数据的"翻译"
- 前端拿到这些数据后，可以展示任务列表，并通过 `id` 来完成任务

---

#### ④ ProcessInstanceRepresentation — 流程实例响应

**文件路径：** `src/main/java/org/example/flowable/model/ProcessInstanceRepresentation.java`

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| `id` | String | Flowable 产生 | 流程实例唯一 ID |
| `name` | String | BPMN 定义 | 流程名称 |
| `processDefinitionId` | String | Flowable 产生 | 流程定义 ID |
| `startTime` | Date | Flowable 产生 | 流程启动时间 |
| `endTime` | Date | Flowable 产生 | 流程结束时间（运行中为 null） |
| `startUserId` | String | Flowable 产生 | 流程发起人 |
| `variables` | Map<String, Object> | 业务传入 | 流程变量（如 reason 等） |
| `completed` | boolean | Service 层设置 | 标识流程是否已结束 |

---

### 6.3 如何自己设计一个新实体类（方法论）

如果你要给项目增加一个新的业务实体（比如"报销单"），可以按以下步骤思考：

**Checklist：设计实体类字段的 5 步法**

- [ ] **Step 1：从业务场景出发**
  - 这个业务单据需要哪些信息？
  - 例如报销单：报销人、金额、报销类型、事由、发生日期、附件链接
  - 来源：产品需求文档或表单原型

- [ ] **Step 2：考虑流程引擎需要的字段**
  - 需要 `processKey`（启动哪个流程）
  - 可能需要 `businessKey`（关联业务数据）
  - 哪些字段需要作为"流程变量"在流程中使用？
  - 例如：金额 > 5000 走总监审批，则 `amount` 必须作为流程变量
  - 来源：BPMN 流程定义中的条件表达式

- [ ] **Step 3：考虑 Flowable 会产生的字段**
  - `processInstanceId`：流程实例 ID
  - `taskId`：当前任务 ID
  - 这些字段不需要你设计，而是从 Flowable API 的返回值中获取
  - 来源：Flowable API 文档

- [ ] **Step 4：考虑数据库存储**
  - 哪些字段需要持久化到你自己的业务表？
  - Flowable 的数据（流程、任务）它自己会存，你只需要存业务数据
  - 例如：报销单本身的信息存你的业务表，审批流程信息 Flowable 自己管
  - 如果用 JPA，考虑字段类型映射、索引、外键等

- [ ] **Step 5：考虑前后端传输**
  - 请求体 DTO：前端需要传什么给后端？
  - 响应体 DTO：后端需要返回什么给前端展示？
  - DTO 字段 ≠ 数据库字段，可以按需裁剪或组合

> **本项目的特殊之处：** 因为只使用了 Flowable 内置的数据存储（H2），没有自定义业务表，所以 model 包下的类全部是 DTO 而非 JPA Entity。如果以后你要做"报销单"等带自定义数据的功能，就需要新增 Entity + Repository。

---

## 7. BPMN 流程定义文件详解

### 7.1 文件位置

`src/main/resources/processes/simple-approval.bpmn20.xml`

> **为什么放在 `processes/` 目录下？**
> Flowable Spring Boot Starter 会自动扫描 `classpath:/processes/` 目录下的 `.bpmn20.xml` 和 `.bpmn` 文件，在启动时自动部署这些流程定义。

### 7.2 流程图（文字版）

```
  ┌───────────┐      ┌──────────────────┐      ┌─────────────────┐
  │ startEvent│─────▶│   approvalTask   │─────▶│ decisionGateway │
  │  (开始)    │      │ (Manager Approval)│      │   (排他网关)     │
  └───────────┘      │ 候选组: managers  │      └────────┬────────┘
                      └──────────────────┘               │
                                                   ┌─────┴─────┐
                                        outcome=   │           │  outcome=
                                       'approved'  │           │ 'rejected'
                                                   ▼           ▼
                                         ┌──────────┐  ┌──────────┐
                                         │ Approved │  │ Rejected │
                                         │ (结束)    │  │ (结束)    │
                                         └──────────┘  └──────────┘
```

### 7.3 逐节点解读

#### ① 开始事件 (Start Event)

```xml
<startEvent id="startEvent"/>
```

- **作用：** 流程的起点。当后端调用 `runtimeService.startProcessInstanceByKey("simple-approval", ...)` 时，流程从这里开始
- **与代码的关系：** `ProcessService.startProcess()` 方法触发

#### ② 顺序流 (Sequence Flow)

```xml
<sequenceFlow sourceRef="startEvent" targetRef="approvalTask"/>
```

- **作用：** 连接两个节点，表示流程从 startEvent 自动流转到 approvalTask
- 这里没有条件，是无条件直接流转

#### ③ 用户任务 (User Task)

```xml
<userTask id="approvalTask" name="Manager Approval" flowable:candidateGroups="managers">
    <documentation>Manager approval required.</documentation>
</userTask>
```

- **作用：** 需要"人"来处理的节点。流程走到这里会暂停，等待有人"完成"这个任务
- `id="approvalTask"`：任务的唯一标识
- `name="Manager Approval"`：任务名称，会显示在前端的待办列表中
- `flowable:candidateGroups="managers"`：候选组，表示 "managers" 组的成员都可以处理这个任务
- **与代码的关系：**
  - `ProcessService.getTasks()` 查询这类任务
  - `ProcessService.completeTask()` 完成这个任务

#### ④ 排他网关 (Exclusive Gateway)

```xml
<exclusiveGateway id="decisionGateway"/>
```

- **作用：** 决策点。根据条件判断流程走哪条路
- "排他"的意思是：只会走其中一条路（不会同时走两条）

#### ⑤ 条件顺序流

```xml
<sequenceFlow sourceRef="decisionGateway" targetRef="endEventApproved">
    <conditionExpression xsi:type="tFormalExpression">${outcome == 'approved'}</conditionExpression>
</sequenceFlow>
<sequenceFlow sourceRef="decisionGateway" targetRef="endEventRejected">
    <conditionExpression xsi:type="tFormalExpression">${outcome == 'rejected'}</conditionExpression>
</sequenceFlow>
```

- **作用：** 根据流程变量 `outcome` 的值决定走向
  - `outcome == 'approved'` → 走到 "Approved" 结束节点
  - `outcome == 'rejected'` → 走到 "Rejected" 结束节点
- **与代码的关系：** `ProcessService.completeTask()` 中 `variables.put("outcome", outcome)` 设置了这个变量

#### ⑥ 结束事件 (End Event)

```xml
<endEvent id="endEventApproved" name="Approved"/>
<endEvent id="endEventRejected" name="Rejected"/>
```

- **作用：** 流程的终点。走到这里流程就结束了
- 两个结束事件分别代表"审批通过"和"审批驳回"两种结果

### 7.4 关键概念串联

| BPMN 元素 | 对应代码/数据 | 通俗理解 |
|-----------|-------------|---------|
| `process id="simple-approval"` | `processKey` 参数 | 流程的"身份证号" |
| `userTask` | `TaskService` 相关操作 | 需要人来干活的步骤 |
| `candidateGroups="managers"` | `taskService.createTaskQuery().taskCandidateGroup("managers")` | 谁有权处理这个任务 |
| `${outcome == 'approved'}` | `variables.put("outcome", outcome)` | 流程"路口"的红绿灯 |
| `endEvent` | `historyService` 可查到 | 流程画上句号 |

---

## 8. 如何自己创建一个新的 BPMN 流程

### 8.1 本项目的 BPMN 文件是怎么来的？

从 `simple-approval.bpmn20.xml` 的内容来看，它是**手写的 XML**。判断依据：

- 没有可视化工具生成的 `BPMNDiagram` 布局信息（坐标、尺寸等）
- 结构简洁，缩进工整，没有工具生成的冗余属性
- 实际上对于简单流程，手写 XML 是完全可行的

### 8.2 创建 BPMN 的常用方式

| 方式 | 适合场景 | 优劣 |
|------|---------|------|
| **Flowable Modeler（Web UI）** | 有可视化需求，团队协作 | 拖拽式操作，简单直观；需要额外部署 Flowable UI 应用 |
| **Flowable Design（商业版）** | 企业级 | 功能更强大，但需要许可证 |
| **VS Code / IDEA 插件** | 开发者偏好 | 可以在 IDE 中直接编辑 |
| **手写 XML** | 简单流程，学习目的 | 完全可控，理解底层；但复杂流程容易出错 |

### 8.3 从零创建一个 BPMN 流程的完整步骤

以"请假审批"流程为例：

**Step 1：明确业务流程**

```
员工提交请假申请 → 直属主管审批 → 通过则结束，驳回也结束
```

**Step 2：选择工具**

推荐初学者使用 **Flowable Modeler**（开源免费）：

1. 从 Flowable GitHub 下载 flowable-ui 应用，或使用 Docker：
   ```bash
   docker run -p 8888:8080 flowable/flowable-ui
   ```
2. 访问 `http://localhost:8888/flowable-modeler`
3. 默认账号 `admin`，密码 `test`

**Step 3：在工具中设计流程**

1. 新建一个 "Process"
2. 从左侧面板拖入以下节点：
   - **Start Event**（开始事件）
   - **User Task**（用户任务）— 命名为 "主管审批"
   - **Exclusive Gateway**（排他网关）
   - 两个 **End Event**（结束事件）— 分别命名为 "已通过" 和 "已驳回"
3. 用箭头把它们连接起来
4. 配置 User Task：
   - `id`：如 `managerApproval`
   - `name`：如 "主管审批"
   - `Candidate Groups` 或 `Assignee`：如 `managers` 或 `${applicant_manager}`
5. 配置排他网关出去的两条线的条件：
   - 通过：`${outcome == 'approved'}`
   - 驳回：`${outcome == 'rejected'}`

**Step 4：导出 BPMN 文件**

- 在 Flowable Modeler 中点击"导出"或"下载"，保存为 `.bpmn20.xml` 文件

**Step 5：放入项目**

- 把文件放到 `src/main/resources/processes/` 目录下
- 文件名建议和流程 id 一致，如 `leave-request.bpmn20.xml`
- Spring Boot 启动时，Flowable 会**自动扫描并部署**这个目录下的所有 BPMN 文件

**Step 6：在后端代码中启动这个流程**

```java
runtimeService.startProcessInstanceByKey("leave-request", businessKey, variables);
```

- 这里的 `"leave-request"` 就是 BPMN 文件中 `<process id="leave-request">` 的 id

### 8.4 边看项目文件边学习的建议

1. 打开 `simple-approval.bpmn20.xml`
2. 对照上面第 7 节的逐节点解读，理解每个 XML 标签的含义
3. 尝试修改条件表达式，比如加一个 `${outcome == 'pending'}` 的分支
4. 重启项目，测试修改是否生效
5. 如果安装了 Flowable Modeler，可以尝试导入这个文件查看可视化效果

---

## 9. 端到端流程串讲

以"简单审批流"为例，走完一个完整的链路：

### 场景：用户提交一个订单审批申请，主管审批通过

```
                    前端                           后端                          Flowable 引擎
                     │                              │                              │
 ── Step 1 ─────────┤                              │                              │
 用户在"发起审批"页面│                              │                              │
 填写 businessKey =  │  POST /api/process/start     │                              │
 "ORDER-001"，      │─────────────────────────────▶│                              │
 reason = "采购申请" │  {processKey:"simple-approval"│                              │
                     │   businessKey:"ORDER-001",   │                              │
                     │   variables:{reason:"采购"}}  │                              │
                     │                              │                              │
 ── Step 2 ──────────┤                              │                              │
                     │               ProcessController.startProcess()              │
                     │                     │        │                              │
                     │               ProcessService.startProcess()                 │
                     │                     │        │                              │
                     │                     │   runtimeService                      │
                     │                     │──.startProcessInstanceByKey(───────────▶
                     │                     │    "simple-approval",                  │
                     │                     │    "ORDER-001",                        │── 创建流程实例
                     │                     │    {reason:"采购"})                    │── 走到 approvalTask
                     │                     │                                       │── 创建一个待办任务
                     │                     │◀──── 返回 processInstanceId ───────────│
                     │                     │        │                              │
                     │◀── {code:200, data:"12345"} ─│                              │
                     │                              │                              │
 ── Step 3 ─────────┤                              │                              │
 用户切换到"待办任务"│  GET /api/process/tasks       │                              │
 页面               │      ?assignee=admin          │                              │
                     │─────────────────────────────▶│                              │
                     │               ProcessService.getTasks("admin")              │
                     │                     │   taskService                         │
                     │                     │──.createTaskQuery()───────────────────▶│
                     │                     │  .taskCandidateGroup("managers")       │── 查询 managers 组任务
                     │                     │◀── 返回 [Task{id:"67890",...}] ────────│
                     │◀── {code:200, data:[{id:"67890", name:"Manager Approval"}]}─│
                     │                              │                              │
 ── Step 4 ─────────┤                              │                              │
 用户点击"通过"按钮  │  POST /api/process/complete   │                              │
                     │  {taskId:"67890",            │                              │
                     │   action:"approved"}         │                              │
                     │─────────────────────────────▶│                              │
                     │               ProcessService.completeTask()                 │
                     │                     │   variables.put("outcome","approved")  │
                     │                     │   taskService.complete("67890", vars)──▶
                     │                     │                                       │── 完成任务
                     │                     │                                       │── 到达排他网关
                     │                     │                                       │── outcome="approved"
                     │                     │                                       │── 走到 endEventApproved
                     │                     │                                       │── 流程结束！
                     │◀── {code:200, data:"Task completed"} ───────────────────────│
                     │                              │                              │
 ── Step 5 ─────────┤                              │                              │
 用户查看"审批历史"  │  GET /api/process/history     │                              │
                     │─────────────────────────────▶│                              │
                     │               ProcessService.getHistory()                   │
                     │                     │   historyService                      │
                     │                     │──.createHistoricProcessInstanceQuery()─▶
                     │                     │  .finished()                           │── 查询已结束流程
                     │                     │◀── 返回 [HistoricProcessInstance] ─────│
                     │◀── {code:200, data:[{id:"12345", completed:true, ...}]} ────│
```

**关键文件对照：**

| 步骤 | 前端文件 | 后端文件 | 流程文件 |
|------|---------|---------|---------|
| 1. 提交申请 | `Apply.vue` → `onSubmit()` | `ProcessController.startProcess()` → `ProcessService.startProcess()` | BPMN: startEvent |
| 2. 创建待办 | - | - | BPMN: startEvent → approvalTask（自动） |
| 3. 查询待办 | `Tasks.vue` → `fetchTasks()` | `ProcessController.getTasks()` → `ProcessService.getTasks()` | - |
| 4. 审批通过 | `Tasks.vue` → `handleTask()` | `ProcessController.completeTask()` → `ProcessService.completeTask()` | BPMN: approvalTask → gateway → endEventApproved |
| 5. 查看历史 | `History.vue` → `fetchData()` | `ProcessController.getHistory()` → `ProcessService.getHistory()` | - |

---

## 10. 实战：如何新增一个审批流程

假设你要新增一个"请假审批"流程，以下是完整步骤：

### Step 1：创建 BPMN 流程定义

在 `src/main/resources/processes/` 下新建 `leave-request.bpmn20.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://www.flowable.org/processdef">

    <process id="leave-request" name="Leave Request Process" isExecutable="true">
        <startEvent id="startEvent"/>
        <sequenceFlow sourceRef="startEvent" targetRef="managerApproval"/>

        <userTask id="managerApproval" name="主管审批"
                  flowable:candidateGroups="managers"/>
        <sequenceFlow sourceRef="managerApproval" targetRef="decision"/>

        <exclusiveGateway id="decision"/>
        <sequenceFlow sourceRef="decision" targetRef="approved">
            <conditionExpression xsi:type="tFormalExpression">
                ${outcome == 'approved'}
            </conditionExpression>
        </sequenceFlow>
        <sequenceFlow sourceRef="decision" targetRef="rejected">
            <conditionExpression xsi:type="tFormalExpression">
                ${outcome == 'rejected'}
            </conditionExpression>
        </sequenceFlow>

        <endEvent id="approved" name="已批准"/>
        <endEvent id="rejected" name="已驳回"/>
    </process>
</definitions>
```

### Step 2：前端发起时使用新的 processKey

```javascript
axios.post('/api/process/start', {
  processKey: 'leave-request',     // 对应 BPMN 中 process id
  businessKey: 'LEAVE-001',
  variables: {
    applicant: '张三',
    days: 3,
    reason: '家中有事'
  }
})
```

### Step 3：重启后端

Spring Boot 启动时会自动检测到新的 BPMN 文件并部署。现有代码不需要任何修改，因为 Controller 和 Service 已经是通用的。

### Step 4：测试

1. 调用发起接口
2. 查看待办任务列表
3. 完成审批
4. 查看历史

> **核心心得：** 本项目的后端代码是通用的，新增流程只需要新增 BPMN 文件 + 在前端指定对应的 `processKey` 即可，不需要改后端代码。

---

## 11. Flowable 学习路线图

### 11.1 从"只会 Spring Boot"到"能用 Flowable 搭审批流程"

| 阶段 | 学什么 | 对照本项目 | 建议时间 |
|------|--------|-----------|---------|
| **阶段 1：理解概念** | 什么是 BPM/工作流引擎？为什么需要它？ | 想象没有 Flowable 时，你要自己写状态机、记录审批状态、管理流转逻辑的痛苦 | 1 天 |
| **阶段 2：跑通 Demo** | 把本项目跑起来，走完一遍完整流程 | 启动后端 → 启动前端 → 发起审批 → 审批通过 → 查看历史 | 半天 |
| **阶段 3：读懂 BPMN** | 学习 BPMN 2.0 的核心元素 | 精读 `simple-approval.bpmn20.xml`，对照本文第 7 节 | 1-2 天 |
| **阶段 4：读懂代码** | 理解 Flowable 三大 Service API | 精读 `ProcessService.java`，对照本文第 5 节第四步 | 1-2 天 |
| **阶段 5：动手改造** | 修改流程、新增流程、新增接口 | 参考本文第 10 节，自己动手做一个请假流程 | 2-3 天 |
| **阶段 6：深入学习** | 多级审批、会签、监听器、定时器、子流程等 | 阅读 Flowable 官方文档 | 持续 |

### 11.2 Flowable 官方文档推荐阅读顺序

1. **BPMN 2.0 Introduction**（BPMN 2.0 入门）
   - 链接：Flowable 官方文档 → BPMN 2.0 章节
   - 重点：Start Event、User Task、Service Task、Exclusive Gateway、Sequence Flow
   - 对照本项目：`simple-approval.bpmn20.xml`

2. **Process Engine API**（流程引擎 API）
   - 重点：RepositoryService、RuntimeService、TaskService、HistoryService
   - 对照本项目：`ProcessService.java` 中的三个 Service 注入

3. **Spring Boot Integration**（Spring Boot 集成）
   - 重点：自动配置、流程自动部署、事务管理
   - 对照本项目：`pom.xml` 中的 `flowable-spring-boot-starter` + `application.properties` 中的配置

4. **Forms and Variables**（表单和流程变量）
   - 重点：如何在流程中传递数据
   - 对照本项目：`startProcess()` 中的 `variables` 参数，`completeTask()` 中的 `outcome` 变量

5. **Gateways**（网关）
   - 重点：Exclusive、Parallel、Inclusive Gateway 的区别
   - 对照本项目：排他网关 `decisionGateway`

### 11.3 推荐外部学习资源

| 资源 | 地址/说明 |
|------|----------|
| Flowable 官方文档 | https://www.flowable.com/open-source/docs/ |
| Flowable GitHub | https://github.com/flowable/flowable-engine |
| Flowable 官方示例 | https://github.com/flowable/flowable-engine/tree/main/modules/flowable-spring-boot/flowable-spring-boot-samples |
| BPMN 2.0 规范（通俗版） | 搜索 "BPMN 2.0 by Example"（OMG 官方出的入门文档） |
| Flowable 中文社区 | 搜索 "Flowable 中文网" |
| B 站 / YouTube 视频 | 搜索 "Flowable Spring Boot 实战" |

---

## 12. 常见问题 FAQ

### Q1：重启项目后数据为什么没了？

因为用的是 H2 内存数据库（`jdbc:h2:mem:flowable`），数据只存在内存中。如果想持久化，可以改为文件模式或 MySQL：

```properties
# 文件模式（重启不丢数据）
spring.datasource.url=jdbc:h2:file:./data/flowable

# 或者 MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/flowable?useSSL=false
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=your_password
```

### Q2：如何查看 Flowable 在数据库里建了哪些表？

启动项目后访问 `http://localhost:8080/h2-console`：
- JDBC URL：`jdbc:h2:mem:flowable`
- 用户名：`sa`
- 密码：空

你会看到大量以 `ACT_` 开头的表，这些都是 Flowable 自动创建的：
- `ACT_RE_*`：Repository（流程定义）
- `ACT_RU_*`：Runtime（运行时数据）
- `ACT_HI_*`：History（历史数据）
- `ACT_GE_*`：General（通用数据）

### Q3：action 的值到底是 "approve" 还是 "approved"？

BPMN 中条件表达式写的是 `${outcome == 'approved'}` 和 `${outcome == 'rejected'}`，所以传给后端的 action 值应该是 **`approved`** 和 **`rejected`**（注意有 "d" 后缀）。前端代码 `Tasks.vue` 中传的就是 `'approved'` 和 `'rejected'`。

### Q4：如果流程走到排他网关，但 outcome 的值不是 approved 也不是 rejected，会怎样？

Flowable 会抛出异常，因为排他网关找不到满足条件的出口。最佳实践是设置一个**默认出口**（default flow），确保流程不会卡死。

### Q5：candidateGroups 和 assignee 有什么区别？

- `assignee`：直接指定一个人来处理任务，如 `flowable:assignee="zhangsan"`
- `candidateGroups`：指定一个"候选组"，组内的人都可以"认领"这个任务并处理
- 本项目用的是 `candidateGroups="managers"`，但代码里通过 `taskAssignee("admin")` 查询的是直接分配的任务，又用 `taskCandidateGroup("managers")` 查询候选组任务，做了合并展示

### Q6：什么是流程变量 (Process Variables)？

流程变量是在流程执行过程中携带的数据。你可以把它理解为流程的"上下文"或"背包"：
- 启动流程时可以放入变量（如 `reason: "出差"`）
- 流程执行过程中可以读取或修改变量
- 排他网关的条件表达式可以引用变量（如 `${outcome == 'approved'}`）
- 完成任务时可以新增或修改变量（如 `outcome: "approved"`）
