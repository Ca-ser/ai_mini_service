---
name: knowledge-base-backend
description: 知识库网站后端开发 — Spring Boot + PostgreSQL 模块化单体架构
---

# Knowledge Base Backend Development

## Project Background

本项目是一个支持团队知识沉淀、在线协作、AI 辅助问答和内容分发的知识库网站。第一阶段以后端 Java 为主，采用模块化单体架构。

**需求文档**: `src/main/resources/templates/需求文档.md`

## Architecture Principles

1. **模块化单体**: 第一阶段优先完成核心业务闭环，避免过早微服务化
2. **PostgreSQL 优先**: 业务存储、缓存、全文检索、向量存储统一使用 PostgreSQL
3. **异步解耦**: 所有异步能力通过任务表和状态机解耦
4. **可拆分预留**: 保留后续将 AI、协同编辑、发布等模块拆分为 Python/Go 服务的能力

## Module Structure

```
com.waiitz
├── auth              # 认证与用户 (注册、登录、JWT)
├── workspace         # 工作空间 (CRUD、成员邀请)
├── permission        # 权限 (Workspace > KB > Document 层级)
├── knowledge         # 知识库 (CRUD、目录树)
├── document          # 文档 (CRUD、快照、版本、移动)
├── collaboration     # 协同编辑 (WebSocket + CRDT)
├── search            # 搜索 (PostgreSQL tsvector + GIN)
├── ai                # AI 助手 (切片、Embedding、向量检索、RAG)
├── publication       # 公众号发布 (渠道配置、草稿创建)
├── asset             # 附件素材 (上传、存储、MIME 校验)
├── audit             # 审计日志 (操作记录)
├── job               # 异步任务 (状态机、重试、幂等)
└── common            # 通用能力 (异常、工具、配置、缓存)
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT |
| Data Access | MyBatis-Plus / JPA / jOOQ |
| Database | PostgreSQL + pgvector |
| Migration | Flyway / Liquibase |
| WebSocket | Spring WebSocket |
| AI | LangChain4j / Spring AI |
| Task | Spring Scheduler |
| API Docs | OpenAPI / Swagger |

## Database Conventions

### Schema 划分
- `app` — 用户、空间、权限
- `doc` — 文档、快照、版本、协同编辑
- `ai` — 文档切片、向量、AI 会话
- `pub` — 发布渠道、发布任务
- `audit` — 审计日志
- `job` — 异步任务
- `cache` — 缓存数据

### Key Domain Tables
| Table | Schema | Description |
|-------|--------|-------------|
| users | app | 用户账号 |
| workspaces | app | 工作空间 |
| workspace_members | app | 工作空间成员 |
| knowledge_bases | app | 知识库 |
| documents | doc | 文档（含目录树 parent_id） |
| document_snapshots | doc | 文档快照（三种格式） |
| document_versions | doc | 文档版本历史 |
| document_collab_updates | doc | 协同编辑增量 |
| document_collab_snapshots | doc | 协同编辑快照 |
| document_chunks | ai | 文档切片 |
| document_chunk_embeddings | ai | 文档向量 (VECTOR(1536)) |
| ai_conversations | ai | AI 会话 |
| ai_messages | ai | AI 消息 |
| publication_channels | pub | 发布渠道 |
| publication_tasks | pub | 发布任务 |
| assets | app | 附件素材 |
| audit_logs | audit | 审计日志 |
| jobs | job | 异步任务 |
| cache_entries | cache | 缓存 |

### Vector Table Template
```sql
CREATE TABLE ai.document_chunk_embeddings (
  id UUID PRIMARY KEY,
  chunk_id UUID NOT NULL,
  document_id UUID NOT NULL,
  kb_id UUID NOT NULL,
  embedding VECTOR(1536) NOT NULL,
  model VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## API Conventions

### Base Path
`/api/v1`

### Unified Response
```json
{ "code": 0, "message": "success", "data": {} }
```

### Paginated Response
```json
{ "code": 0, "message": "success", "data": { "items": [], "page": 1, "size": 20, "total": 100 } }
```

### Error Codes
| Code | Meaning |
|------|---------|
| 0 | Success |
| 400001 | Bad Request |
| 401001 | Unauthorized |
| 403001 | Forbidden |
| 404001 | Not Found |
| 409001 | Conflict |
| 429001 | Rate Limited |
| 500001 | System Error |
| 500101 | AI Service Error |
| 500201 | WeChat API Error |
| 500301 | Async Task Error |

## Role & Permission Model

```
Workspace (OWNER / ADMIN / EDITOR / VIEWER / GUEST)
  └── KnowledgeBase
        └── Document
              └── Asset / Publication
```

- AI 检索必须在召回阶段和返回阶段都执行权限过滤
- 所有文档访问必须进行权限校验

## Key Implementation Notes

### Document Flow
1. Create document → init snapshot → audit log
2. Save document → create snapshot/version → async: chunking + embedding + search index
3. Delete = soft delete, child docs handled accordingly

### Collaboration (WebSocket)
- Endpoint: `/ws/v1/documents/{documentId}/collaboration`
- Use CRDT for conflict resolution
- Periodic snapshot compaction (N updates / time interval / room empty / manual save)

### AI Assistant
- Chunk by heading hierarchy (300-800 tokens per chunk)
- Hybrid retrieval: vector + full-text search + re-rank
- Dual permission filtering (retrieval + response)
- Async: chunking → embedding → index (failure does not block document save)

### Async Tasks
- State machine: PENDING → RUNNING → SUCCESS / FAILED / RETRYING / CANCELLED
- Must support retry, error logging, timeout, idempotency
- Same-document index tasks must avoid concurrent conflicts

### WeChat Publication
- AppSecret encrypted storage
- Document → compatible HTML (image upload + URL replace)
- Async draft creation with status tracking

## MVP Scope
1. User login
2. Workspace CRUD
3. Knowledge base CRUD
4. Document tree, CRUD, save
5. Document versions & rollback
6. Basic WebSocket collaboration
7. PostgreSQL full-text search
8. Document chunking & embedding
9. pgvector retrieval
10. AI question answering
11. WeChat channel config & draft publishing
12. Async task & retry
13. Basic audit log

## Non-Functional Requirements
- Document read P95 < 300ms, search P95 < 800ms
- Document save must not depend on AI service availability
- WebSocket connections must be authenticated
- Upload files must validate MIME type and size
- HTML output must be sanitized (XSS prevention)

## Milestones
1. **Phase 1**: Basic KB (user, workspace, KB, doc CRUD, versions, permissions)
2. **Phase 2**: Collaboration (WebSocket, CRDT, presence, snapshots)
3. **Phase 3**: AI Assistant (chunking, embedding, vector search, RAG)
4. **Phase 4**: WeChat Publication (channel config, doc→HTML, draft creation)
