---
name: openapi-docs
description: 知识库网站后端 OpenAPI 接口文档 — 实时反映开发进度与 API 状态
---

# 知识库网站后端 API 接口文档 (OpenAPI)

> **基准路径**: `/api/v1`
> **协议**: HTTP (WebSocket 另行标注)
> **认证**: Sa-Token Bearer Token (通过 `Authorization: Bearer <token>` 请求头传递)
> **数据格式**: JSON

---

## 统一响应格式

### 成功响应
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 错误响应
```json
{
  "code": 400001,
  "message": "Bad Request"
}
```

### 错误码表

| 错误码 | 说明 | HTTP 状态码 |
|--------|------|-------------|
| 0 | 成功 | 200 |
| 400001 | 参数错误 | 400 |
| 401001 | 未登录 | 401 |
| 403001 | 无权限 | 403 |
| 404001 | 资源不存在 | 404 |
| 409001 | 数据冲突 | 409 |
| 429001 | 请求过于频繁 | 429 |
| 500001 | 系统错误 | 500 |
| 500101 | AI 服务异常 | 500 |
| 500201 | 公众号接口异常 | 500 |
| 500301 | 异步任务异常 | 500 |

### 分页响应格式
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "size": 20,
    "total": 100
  }
}
```

---

## 状态标记说明

| 标记 | 含义 |
|------|------|
| ✅ **Done** | 已完成并上线 |
| 🚧 **In Progress** | 开发中 |
| 📋 **Planned** | 已规划，待开发 |

---

## 一、认证模块 (Auth)

> 基准路径: `/api/v1/auth`
> 免登录接口: `register`, `login`

### 1.1 用户注册

> ✅ **Done**

```
POST /api/v1/auth/register
```

**权限**: 无需登录

**Request Body**:
```json
{
  "username": "kone",
  "email": "user@example.com",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| username | string | ✅ | 2-50 字符 |
| email | string | ✅ | 合法邮箱格式 |
| password | string | ✅ | 6-64 字符 |

**Response `Result<UserVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "username": "kone",
    "email": "user@example.com",
    "avatarUrl": ""
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱 |
| avatarUrl | string | 头像 URL |

### 1.2 用户登录

> ✅ **Done**

```
POST /api/v1/auth/login
```

**权限**: 无需登录

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| email | string | ✅ | 合法邮箱格式 |
| password | string | ✅ | - |

**Response `Result<LoginResponse>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "sa-token-value",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | string | Sa-Token Bearer Token |
| tokenType | string | 固定为 `Bearer` |
| expiresIn | long | 过期时间（秒） |

### 1.3 获取当前用户信息

> ✅ **Done**

```
GET /api/v1/auth/me
```

**权限**: 登录即可

**Headers**:
```
Authorization: Bearer <token>
```

**Response `Result<UserVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "username": "kone",
    "email": "user@example.com",
    "avatarUrl": ""
  }
}
```

---

## 二、工作空间模块 (Workspace)

> 基准路径: `/api/v1/workspaces`

### 2.1 创建工作空间

> ✅ **Done**

```
POST /api/v1/workspaces
```

**权限**: 登录即可（创建者自动成为 OWNER）

**Request Body**:
```json
{
  "name": "产品研发部",
  "description": "团队知识库空间"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| name | string | ✅ | 1-100 字符 |
| description | string | ❌ | 最多 500 字符 |

**Response `Result<WorkspaceVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "name": "产品研发部",
    "description": "团队知识库空间",
    "role": "OWNER",
    "memberCount": 1,
    "kbCount": 0,
    "createdAt": "2026-07-09T10:00:00Z"
  }
}
```

### 2.2 查询工作空间列表

> ✅ **Done**

```
GET /api/v1/workspaces
```

**权限**: 登录即可

**Response `Result<List<WorkspaceVO>>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "uuid",
      "name": "产品研发部",
      "description": "团队知识库空间",
      "role": "OWNER",
      "memberCount": 5,
      "kbCount": 12,
      "createdAt": "2026-07-09T10:00:00Z"
    }
  ]
}
```

### 2.3 邀请成员

> ✅ **Done**

```
POST /api/v1/workspaces/{workspaceId}/members
```

**权限**: OWNER 或 ADMIN

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| workspaceId | UUID | 工作空间 ID |

**Request Body**:
```json
{
  "email": "member@example.com",
  "role": "EDITOR"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | ✅ | 被邀请人邮箱 |
| role | string | ✅ | 角色: OWNER / ADMIN / EDITOR / VIEWER / GUEST |

**Response `Result<MemberVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "username": "kone",
    "email": "member@example.com",
    "role": "EDITOR",
    "createdAt": "2026-07-09T10:00:00Z"
  }
}
```

---

## 三、知识库模块 (KnowledgeBase)

> 基准路径: `/api/v1`

### 3.1 创建知识库

> ✅ **Done**

```
POST /api/v1/workspaces/{workspaceId}/knowledge-bases
```

**权限**: EDITOR 及以上

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| workspaceId | UUID | 工作空间 ID |

**Request Body**:
```json
{
  "name": "技术架构",
  "description": "系统架构、数据库、AI、协同编辑等技术文档",
  "visibility": "PRIVATE"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| name | string | ✅ | 1-100 字符 |
| description | string | ❌ | 最多 500 字符 |
| visibility | string | ✅ | PRIVATE / INTERNAL / PUBLIC |

**Response `Result<KnowledgeBaseVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "workspaceId": "uuid",
    "name": "技术架构",
    "description": "系统架构相关文档",
    "visibility": "PRIVATE",
    "documentCount": 0,
    "createdAt": "2026-07-09T10:00:00Z"
  }
}
```

### 3.2 查询知识库列表

> ✅ **Done**

```
GET /api/v1/workspaces/{workspaceId}/knowledge-bases
```

**权限**: VIEWER 及以上

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| workspaceId | UUID | 工作空间 ID |

**Response `Result<List<KnowledgeBaseVO>>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "uuid",
      "workspaceId": "uuid",
      "name": "技术架构",
      "description": "系统架构相关文档",
      "visibility": "PRIVATE",
      "documentCount": 26,
      "createdAt": "2026-07-09T10:00:00Z"
    }
  ]
}
```

### 3.3 更新知识库

> ✅ **Done**

```
PUT /api/v1/knowledge-bases/{kbId}
```

**权限**: EDITOR 及以上

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| kbId | UUID | 知识库 ID |

**Request Body**:
```json
{
  "name": "技术架构",
  "description": "更新后的描述",
  "visibility": "PRIVATE"
}
```

**Response `Result<KnowledgeBaseVO>`**: 同 3.2

### 3.4 删除知识库

> ✅ **Done**

```
DELETE /api/v1/knowledge-bases/{kbId}
```

**权限**: ADMIN 及以上（软删除，级联删除下属文档）

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| kbId | UUID | 知识库 ID |

**Response**: `Result<Void>`

---

## 四、文档模块 (Document)

> 基准路径: `/api/v1`

### 4.1 创建文档

> ✅ **Done**

```
POST /api/v1/knowledge-bases/{kbId}/documents
```

**权限**: EDITOR 及以上（自动生成初始空快照和版本记录）

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| kbId | UUID | 知识库 ID |

**Request Body**:
```json
{
  "parentId": "uuid",
  "title": "AI 助手 RAG 方案",
  "contentFormat": "PROSEMIRROR_JSON"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| parentId | UUID | ❌ | 父文档 ID，null 表示根目录 |
| title | string | ✅ | 1-255 字符 |
| contentFormat | string | ✅ | PROSEMIRROR_JSON / MARKDOWN |

**Response `Result<DocumentVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "kbId": "uuid",
    "parentId": "uuid",
    "title": "AI 助手 RAG 方案",
    "contentJson": null,
    "contentMarkdown": null,
    "contentHtml": null,
    "contentFormat": "PROSEMIRROR_JSON",
    "versionNo": 1,
    "status": "DRAFT",
    "createdBy": "uuid",
    "createdAt": "2026-07-09T10:00:00Z",
    "updatedAt": "2026-07-09T10:00:00Z"
  }
}
```

### 4.2 获取文档目录树

> ✅ **Done**

```
GET /api/v1/knowledge-bases/{kbId}/document-tree
```

**权限**: VIEWER 及以上

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| kbId | UUID | 知识库 ID |

**Response `Result<List<DocumentTreeVO>>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "uuid",
      "title": "架构设计",
      "type": "DOCUMENT",
      "children": [
        {
          "id": "uuid",
          "title": "AI 助手 RAG 方案",
          "type": "DOCUMENT",
          "children": []
        }
      ]
    }
  ]
}
```

### 4.3 获取文档详情

> ✅ **Done**

```
GET /api/v1/documents/{documentId}
```

**权限**: VIEWER 及以上

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| documentId | UUID | 文档 ID |

**Response `Result<DocumentVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "kbId": "uuid",
    "parentId": "uuid",
    "title": "AI 助手 RAG 方案",
    "contentJson": "{}",
    "contentMarkdown": "# AI 助手 RAG 方案",
    "contentHtml": "<h1>AI 助手 RAG 方案</h1>",
    "contentFormat": "PROSEMIRROR_JSON",
    "versionNo": 18,
    "status": "PUBLISHED",
    "createdBy": "uuid",
    "createdAt": "2026-07-07T10:00:00Z",
    "updatedAt": "2026-07-07T10:00:00Z"
  }
}
```

### 4.4 保存文档内容

> ✅ **Done**

```
PUT /api/v1/documents/{documentId}/content
```

**权限**: EDITOR 及以上（创建新快照 + 新版本记录）

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| documentId | UUID | 文档 ID |

**Request Body**:
```json
{
  "title": "AI 助手 RAG 方案",
  "contentJson": "{}",
  "contentMarkdown": "# AI 助手 RAG 方案",
  "contentHtml": "<h1>AI 助手 RAG 方案</h1>",
  "changeSummary": "补充检索流程"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | ❌ | 不传则不更新标题 |
| contentJson | string | ❌ | ProseMirror 富文本 JSON |
| contentMarkdown | string | ❌ | Markdown 格式 |
| contentHtml | string | ❌ | HTML 格式 |
| changeSummary | string | ❌ | 变更说明 |

**Response `Result<DocumentVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "kbId": "uuid",
    "title": "AI 助手 RAG 方案",
    "contentJson": "{}",
    "contentMarkdown": "# AI 助手 RAG 方案",
    "contentHtml": "<h1>AI 助手 RAG 方案</h1>",
    "versionNo": 19,
    "updatedAt": "2026-07-09T10:00:00Z"
  }
}
```

### 4.5 删除文档

> ✅ **Done**

```
DELETE /api/v1/documents/{documentId}
```

**权限**: EDITOR 及以上（软删除，递归删除所有子文档）

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| documentId | UUID | 文档 ID |

**Response**: `Result<Void>`

### 4.6 移动文档

> ✅ **Done**

```
PATCH /api/v1/documents/{documentId}/move
```

**权限**: EDITOR 及以上（含循环引用检测）

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| documentId | UUID | 文档 ID |

**Request Body**:
```json
{
  "targetParentId": "uuid"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| targetParentId | UUID | ❌ | 目标父文档 ID，null 表示移到根目录 |

**Response**: `Result<Void>`

### 4.7 查询版本历史

> ✅ **Done**

```
GET /api/v1/documents/{documentId}/versions
```

**权限**: VIEWER 及以上（按版本号倒序）

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| documentId | UUID | 文档 ID |

**Response `Result<List<DocumentVersionVO>>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "uuid",
      "versionNo": 18,
      "changeSummary": "补充检索流程",
      "snapshotId": "uuid",
      "createdBy": "uuid",
      "createdAt": "2026-07-07T10:00:00Z"
    }
  ]
}
```

### 4.8 获取指定版本详情

> ✅ **Done**

```
GET /api/v1/documents/{documentId}/versions/{versionNo}
```

**权限**: VIEWER 及以上

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| documentId | UUID | 文档 ID |
| versionNo | integer | 版本号 |

**Response `Result<DocumentVO>`**: 同 4.3（返回该版本的历史快照内容）

### 4.9 回滚到指定版本

> ✅ **Done**

```
POST /api/v1/documents/{documentId}/versions/{versionNo}/restore
```

**权限**: EDITOR 及以上（基于历史快照创建新版本）

**Path Parameters**:
| 参数 | 类型 | 说明 |
|------|------|------|
| documentId | UUID | 文档 ID |
| versionNo | integer | 要回滚到的目标版本号 |

**Response `Result<DocumentVO>`**: 同 4.4（返回回滚后新生成的版本）

---

## 五、附件素材模块 (Asset)

### 5.1 上传附件

> ✅ **Done**

```
POST /api/v1/assets/upload
```

**权限**: EDITOR 及以上

**Request Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workspaceId | UUID | ✅ | 工作空间 ID |
| file | MultipartFile | ✅ | 文件（最大 50MB） |

**支持的 MIME 类型**: 图片(png/jpg/gif/webp/svg)、PDF、Office(doc/docx/xls/xlsx/ppt/pptx)、Markdown、压缩包(zip/rar/7z)

**Response `Result<AssetVO>`**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "fileName": "architecture.png",
    "fileSize": 204800,
    "mimeType": "image/png",
    "url": "/uploads/xxx.png",
    "createdAt": "2026-07-09T10:00:00Z"
  }
}
```

---

## 六、AI 助手模块

### 6.1 AI 对话（基础版）

> ✅ **Done** — 仅基础 LLM 调用，无 RAG/向量检索

```
GET /ai/chat?userId=xxx&message=xxx
```

**权限**: 登录即可

**Query Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | string | ✅ | 用户 ID |
| message | string | ✅ | 用户消息 |

**Response**:
```json
{
  "answer": "AI 返回的回答内容",
  "suggestions": ["建议问题1", "建议问题2"],
  "intent": "intent_type",
  "needTool": false
}
```

### 6.2 知识库问答 (RAG)

> 📋 **Planned** — Phase 3

```
POST /api/v1/ai/chat
```

**权限**: 对目标知识库具有 VIEWER 及以上权限

**Request Body**:
```json
{
  "conversation_id": "uuid",
  "workspace_id": "uuid",
  "kb_ids": ["uuid"],
  "question": "协同编辑为什么要用 CRDT?",
  "top_k": 8
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| conversation_id | UUID | ❌ | 会话 ID（为空则创建新会话） |
| workspace_id | UUID | ✅ | 工作空间 ID |
| kb_ids | UUID[] | ✅ | 目标知识库列表 |
| question | string | ✅ | 用户问题 |
| top_k | int | ❌ | 检索 chunk 数量，默认 8 |

**Response**:
```json
{
  "answer": "CRDT 可以在多人并发编辑时自动合并变更...",
  "citations": [
    {
      "document_id": "uuid",
      "title": "协同编辑协议",
      "chunk_id": "uuid",
      "quote": "CRDT 适合..."
    }
  ],
  "usage": {
    "prompt_tokens": 1200,
    "completion_tokens": 320,
    "total_tokens": 1520
  }
}
```

### 6.3 文档总结

> 📋 **Planned** — Phase 3

```
POST /api/v1/ai/documents/{documentId}/summary
```

**Request Body**:
```json
{
  "style": "brief"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| style | string | ❌ | brief / detailed，默认 brief |

**Response**:
```json
{
  "summary": "本文介绍了 AI 助手 RAG 方案，包括文档切片、向量检索、权限过滤和回答生成。"
}
```

### 6.4 AI 改写

> 📋 **Planned** — Phase 3

```
POST /api/v1/ai/rewrite
```

**Request Body**:
```json
{
  "text": "原始文本",
  "instruction": "改得更正式一些"
}
```

**Response**:
```json
{
  "result": "改写后的文本"
}
```

---

## 七、搜索模块

### 7.1 文档全文搜索

> 📋 **Planned** — Phase 3

```
GET /api/v1/search/documents
```

**权限**: 搜索结果自动按用户权限过滤

**Query Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | ✅ | 搜索关键词 |
| workspace_id | UUID | ❌ | 限定工作空间 |
| kb_id | UUID | ❌ | 限定知识库 |
| page | int | ❌ | 页码，默认 1 |
| size | int | ❌ | 每页数量，默认 20 |

**Response**:
```json
{
  "items": [
    {
      "document_id": "uuid",
      "title": "AI 助手 RAG 方案",
      "kb_name": "技术架构",
      "highlight": "系统采用向量检索和全文检索结合的方式...",
      "updated_at": "2026-07-07T10:00:00Z"
    }
  ],
  "total": 1
}
```

---

## 八、协同编辑模块 (Collaboration)

### 8.1 建立协同连接 (WebSocket)

> 📋 **Planned** — Phase 2

```
WS /ws/v1/documents/{documentId}/collaboration?token=<token>&client_id=<clientId>
```

**协议**: WebSocket

**Query Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | string | ✅ | 用户登录 token |
| client_id | string | ✅ | 前端客户端 ID |

### 8.2 协同更新消息

> 📋 **Planned** — Phase 2

```json
{
  "type": "doc_update",
  "document_id": "uuid",
  "client_id": "client-001",
  "update": "base64-binary",
  "seq": 1024
}
```

### 8.3 在线用户状态

> 📋 **Planned** — Phase 2

```json
{
  "type": "presence",
  "document_id": "uuid",
  "user_id": "uuid",
  "username": "kone",
  "cursor": {
    "anchor": 100,
    "head": 120
  }
}
```

### 8.4 协同快照合并

> 📋 **Planned** — Phase 2

自动触发合并条件：累计 N 条 update / 固定时间间隔 / 房间无人在线 / 手动保存文档

---

## 九、公众号发布模块 (Publication)

### 9.1 配置发布渠道

> 📋 **Planned** — Phase 4

```
POST /api/v1/publication/channels
```

**权限**: ADMIN 及以上

**Request Body**:
```json
{
  "workspace_id": "uuid",
  "type": "WECHAT_OFFICIAL_ACCOUNT",
  "name": "公司服务号",
  "app_id": "wx********",
  "app_secret": "secret"
}
```

### 9.2 创建发布任务

> 📋 **Planned** — Phase 4

```
POST /api/v1/publication/tasks
```

**Request Body**:
```json
{
  "document_id": "uuid",
  "channel_id": "uuid",
  "title": "如何为知识库系统设计一个可靠的 AI 助手?",
  "digest": "本文介绍知识库 AI 助手的架构设计。",
  "author": "kone",
  "cover_asset_id": "uuid"
}
```

### 9.3 查询发布任务列表

> 📋 **Planned** — Phase 4

```
GET /api/v1/publication/tasks?workspace_id=<wsId>&status=<status>&page=1&size=20
```

**Response**:
```json
{
  "items": [
    {
      "id": "uuid",
      "document_title": "AI 助手 RAG 方案",
      "channel_name": "公司服务号",
      "status": "DRAFT_CREATED",
      "external_draft_id": "xxx",
      "created_at": "2026-07-07T10:00:00Z"
    }
  ],
  "total": 1
}
```

---

## 十、审计日志模块 (Audit)

### 10.1 查询审计日志

> 📋 **Planned** — Phase 4

```
GET /api/v1/audit/logs?workspace_id=<wsId>&action=<action>&page=1&size=20
```

**权限**: ADMIN 及以上

---

## 十一、异步任务模块 (Job)

### 11.1 查询任务列表

> 📋 **Planned** — Phase 3

```
GET /api/v1/jobs?workspace_id=<wsId>&type=<type>&status=<status>&page=1&size=20
```

### 11.2 手动重试任务

> 📋 **Planned** — Phase 3

```
POST /api/v1/jobs/{jobId}/retry
```

### 任务类型枚举

| 类型 | 说明 |
|------|------|
| DOCUMENT_CHUNKING | 文档切片 |
| EMBEDDING_GENERATION | Embedding 生成 |
| SEARCH_INDEX_UPDATE | 全文索引更新 |
| COLLAB_SNAPSHOT_COMPACT | 协同快照合并 |
| WECHAT_DRAFT_CREATE | 创建公众号草稿 |
| WECHAT_IMAGE_UPLOAD | 上传公众号图片 |
| AI_SUMMARY_GENERATE | AI 总结生成 |
| CLEAN_EXPIRED_CACHE | 清理过期缓存 |

### 任务状态枚举

| 状态 | 说明 |
|------|------|
| PENDING | 待执行 |
| RUNNING | 执行中 |
| SUCCESS | 成功 |
| FAILED | 失败 |
| RETRYING | 等待重试 |
| CANCELLED | 已取消 |

---

## Schema 定义

### 统一响应 `Result<T>`

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 分页结果 `PageResult<T>`

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "total": 100
}
```

### 用户 `UserVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱 |
| avatarUrl | string | 头像 URL |

### 登录响应 `LoginResponse`

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | string | Bearer Token |
| tokenType | string | 固定为 `Bearer` |
| expiresIn | long | 过期时间（秒） |

### 工作空间 `WorkspaceVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 工作空间 ID |
| name | string | 名称 |
| description | string | 描述 |
| role | string | 当前用户角色 |
| memberCount | long | 成员数量 |
| kbCount | long | 知识库数量 |
| createdAt | datetime | 创建时间 |

### 工作空间成员 `MemberVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 成员记录 ID |
| userId | UUID | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱 |
| role | string | 角色 |
| createdAt | datetime | 加入时间 |

### 知识库 `KnowledgeBaseVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 知识库 ID |
| workspaceId | UUID | 所属工作空间 |
| name | string | 名称 |
| description | string | 描述 |
| visibility | string | PRIVATE / INTERNAL / PUBLIC |
| documentCount | long | 文档数量 |
| createdAt | datetime | 创建时间 |

### 文档 `DocumentVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 文档 ID |
| kbId | UUID | 所属知识库 |
| parentId | UUID | 父文档 ID |
| title | string | 标题 |
| contentJson | string | ProseMirror 富文本 JSON |
| contentMarkdown | string | Markdown 内容 |
| contentHtml | string | HTML 内容 |
| contentFormat | string | PROSEMIRROR_JSON / MARKDOWN |
| versionNo | integer | 当前版本号 |
| status | string | DRAFT / PUBLISHED / ARCHIVED / DELETED |
| createdBy | UUID | 创建人 ID |
| createdAt | datetime | 创建时间 |
| updatedAt | datetime | 更新时间 |

### 文档树节点 `DocumentTreeVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 文档 ID |
| title | string | 标题 |
| type | string | 固定为 `DOCUMENT` |
| children | DocumentTreeVO[] | 子节点列表 |

### 文档版本 `DocumentVersionVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 版本记录 ID |
| versionNo | integer | 版本号 |
| changeSummary | string | 变更说明 |
| snapshotId | UUID | 对应快照 ID |
| createdBy | UUID | 创建人 ID |
| createdAt | datetime | 创建时间 |

### 附件 `AssetVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 附件 ID |
| fileName | string | 文件名 |
| fileSize | long | 文件大小（字节） |
| mimeType | string | MIME 类型 |
| url | string | 访问 URL |
| createdAt | datetime | 上传时间 |

### AI 聊天响应 `AiChatResponse`

| 字段 | 类型 | 说明 |
|------|------|------|
| answer | string | AI 回答内容 |
| suggestions | string[] | 建议问题 |
| intent | string | 意图类型 |
| needTool | boolean | 是否需要工具调用 |

### 角色枚举 `RoleEnum`

| 角色 | 等级 | 说明 |
|------|------|------|
| OWNER | 0 | 所有者（最高权限） |
| ADMIN | 1 | 管理员 |
| EDITOR | 2 | 编辑者 |
| VIEWER | 3 | 阅读者 |
| GUEST | 4 | 访客 |

### 文档状态枚举 `DocumentStatus`

| 状态 | 说明 |
|------|------|
| DRAFT | 草稿 |
| PUBLISHED | 已发布 |
| ARCHIVED | 已归档 |
| DELETED | 已删除（软删除） |

### 可见性枚举 `Visibility`

| 可见性 | 说明 |
|--------|------|
| PRIVATE | 私有 |
| INTERNAL | 内部公开 |
| PUBLIC | 公开 |

### 内容格式枚举 `ContentFormat`

| 格式 | 说明 |
|------|------|
| PROSEMIRROR_JSON | ProseMirror 富文本 JSON |
| MARKDOWN | Markdown |

---

## API 总览与开发进度

### ✅ 已完成 (Phase 1) — 17 个接口

| # | 模块 | 方法 | 路径 | 权限 |
|---|------|------|------|------|
| 1 | Auth | POST | `/api/v1/auth/register` | 无需登录 |
| 2 | Auth | POST | `/api/v1/auth/login` | 无需登录 |
| 3 | Auth | GET | `/api/v1/auth/me` | 登录即可 |
| 4 | Workspace | POST | `/api/v1/workspaces` | 登录即可 |
| 5 | Workspace | GET | `/api/v1/workspaces` | 登录即可 |
| 6 | Workspace | POST | `/api/v1/workspaces/{workspaceId}/members` | OWNER/ADMIN |
| 7 | KnowledgeBase | POST | `/api/v1/workspaces/{workspaceId}/knowledge-bases` | EDITOR+ |
| 8 | KnowledgeBase | GET | `/api/v1/workspaces/{workspaceId}/knowledge-bases` | VIEWER+ |
| 9 | KnowledgeBase | PUT | `/api/v1/knowledge-bases/{kbId}` | EDITOR+ |
| 10 | KnowledgeBase | DELETE | `/api/v1/knowledge-bases/{kbId}` | ADMIN+ |
| 11 | Document | POST | `/api/v1/knowledge-bases/{kbId}/documents` | EDITOR+ |
| 12 | Document | GET | `/api/v1/knowledge-bases/{kbId}/document-tree` | VIEWER+ |
| 13 | Document | GET | `/api/v1/documents/{documentId}` | VIEWER+ |
| 14 | Document | PUT | `/api/v1/documents/{documentId}/content` | EDITOR+ |
| 15 | Document | DELETE | `/api/v1/documents/{documentId}` | EDITOR+ |
| 16 | Document | PATCH | `/api/v1/documents/{documentId}/move` | EDITOR+ |
| 17 | Document | GET | `/api/v1/documents/{documentId}/versions` | VIEWER+ |
| 18 | Document | GET | `/api/v1/documents/{documentId}/versions/{versionNo}` | VIEWER+ |
| 19 | Document | POST | `/api/v1/documents/{documentId}/versions/{versionNo}/restore` | EDITOR+ |
| 20 | Asset | POST | `/api/v1/assets/upload` | EDITOR+ |
| 21 | AI | GET | `/ai/chat` | 登录即可 |

### 📋 待开发 (Phase 2-4) — 12 个接口

| # | 模块 | 方法 | 路径 | 阶段 | 说明 |
|---|------|------|------|------|------|
| 22 | Collaboration | WS | `/ws/v1/documents/{documentId}/collaboration` | Phase 2 | WebSocket 协同编辑 |
| 23 | Search | GET | `/api/v1/search/documents` | Phase 3 | 全文搜索 |
| 24 | AI | POST | `/api/v1/ai/chat` | Phase 3 | 知识库 RAG 问答 |
| 25 | AI | POST | `/api/v1/ai/documents/{documentId}/summary` | Phase 3 | 文档总结 |
| 26 | AI | POST | `/api/v1/ai/rewrite` | Phase 3 | AI 改写 |
| 27 | Publication | POST | `/api/v1/publication/channels` | Phase 4 | 配置发布渠道 |
| 28 | Publication | POST | `/api/v1/publication/tasks` | Phase 4 | 创建发布任务 |
| 29 | Publication | GET | `/api/v1/publication/tasks` | Phase 4 | 查询发布任务 |
| 30 | Audit | GET | `/api/v1/audit/logs` | Phase 4 | 审计日志查询 |
| 31 | Job | GET | `/api/v1/jobs` | Phase 3 | 任务列表 |
| 32 | Job | POST | `/api/v1/jobs/{jobId}/retry` | Phase 3 | 手动重试 |
| 33 | AI | POST | `/api/v1/ai/documents/{documentId}/chunk` | Phase 3 | 手动触发切片 |

---

## 开发进度追踪

> 此文档随开发进度实时更新。新增或修改 API 时同步更新对应章节。

### 更新记录

| 日期 | 变更内容 |
|------|----------|
| 2026-07-13 | 初始版本 — 基于 Phase 1 代码生成的完整 API 文档 |
| -- | -- |

### 下一阶段更新计划

- **Phase 2 完成时**: 补充 8.1~8.4 协同编辑 WebSocket API 的详细请求/响应示例
- **Phase 3 完成时**: 补充 6.2~6.4 AI 模块 + 7.1 搜索 + 11.1~11.2 任务 API
- **Phase 4 完成时**: 补充 9.1~9.3 发布模块 + 10.1 审计日志 API
