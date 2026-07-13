---
name: dev-memory-2026-07-09-phase1
description: 第1次开发会话记忆 — 项目基础设施搭建 + Phase 1（知识库闭环）全部实现
session_date: 2026-07-09
phase: 1
---

# 会话记忆 — Phase 1 知识库闭环实现

## 会话成果摘要

| 维度 | 完成情况 |
|------|----------|
| 新建文件 | 33 个（枚举 4 + DTO/VO 15 + Service 5 + Controller 5 + 配置调整） |
| 修改文件 | 3 个（2 个 Repo + application.yaml） |
| API 接口 | 17 个（Workspace 3 + KB 4 + Document 9 + Asset 1） |
| 项目进度 | ~25% → ~50%（实体+仓库+Auth+Phase1 业务层完成） |

## 已实现的 API 清单

### 认证 Auth（已完成，先于本次会话）
| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/v1/auth/register` | 用户注册 | 无需登录 |
| POST | `/api/v1/auth/login` | 用户登录，返回 Bearer Token | 无需登录 |
| GET | `/api/v1/auth/me` | 获取当前登录用户信息 | 登录即可 |

### 工作空间 Workspace（本次会话实现）
| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/v1/workspaces` | 创建工作空间，自动成为 OWNER | 登录即可 |
| GET | `/api/v1/workspaces` | 列表（含角色、成员数、KB数） | 登录即可 |
| POST | `/api/v1/workspaces/{id}/members` | 通过邮箱邀请成员 | OWNER/ADMIN |

### 知识库 KnowledgeBase（本次会话实现）
| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/v1/workspaces/{id}/knowledge-bases` | 创建知识库 | EDITOR+ |
| GET | `/api/v1/workspaces/{id}/knowledge-bases` | 列表（含文档数统计） | VIEWER+ |
| PUT | `/api/v1/knowledge-bases/{id}` | 更新名称/描述/可见性 | EDITOR+ |
| DELETE | `/api/v1/knowledge-bases/{id}` | 软删除 KB + 级联软删除文档 | ADMIN+ |

### 文档 Document（本次会话实现）
| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/v1/knowledge-bases/{id}/documents` | 创建文档 + 初始空快照 v1 | EDITOR+ |
| GET | `/api/v1/knowledge-bases/{id}/document-tree` | 递归目录树 | VIEWER+ |
| GET | `/api/v1/documents/{id}` | 详情（含当前快照内容） | VIEWER+ |
| PUT | `/api/v1/documents/{id}/content` | 保存→新快照+版本+更新currentSnapshotId | EDITOR+ |
| DELETE | `/api/v1/documents/{id}` | 软删除 + 级联子文档 | EDITOR+ |
| PATCH | `/api/v1/documents/{id}/move` | 移动（含循环引用检测） | EDITOR+ |
| GET | `/api/v1/documents/{id}/versions` | 版本列表（按版本号倒序） | VIEWER+ |
| GET | `/api/v1/documents/{id}/versions/{v}` | 指定版本详情 | VIEWER+ |
| POST | `/api/v1/documents/{id}/versions/{v}/restore` | 回滚→基于历史快照创建新版本 | EDITOR+ |

### 附件 Asset（本次会话实现）
| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/v1/assets/upload` | 上传（MIME白名单、50MB限制、SHA-256） | EDITOR+ |

## 项目当前包结构

```
com.waiitz.suji_service/
├── AiMiniServiceApplication.java       # 启动类
├── client/
│   └── LimClient.java                  # DeepSeek AI 客户端（已有）
├── common/
│   ├── BizException.java               # 业务异常 + 静态工厂方法
│   ├── ErrorCode.java                  # 11 个错误码枚举
│   ├── GlobalExceptionHandler.java     # 全局异常（含 Sa-Token 异常处理）
│   └── Result.java                     # 统一响应包装
├── config/
│   └── SaTokenConfig.java              # Sa-Token 拦截器 + BCrypt
├── context/
│   └── PromptVersion.java              # AI Prompt 模板
├── controller/
│   ├── AiController.java               # AI 对话（已有，未完整）
│   ├── AssetController.java            # 附件上传 [本次新建]
│   ├── AuthController.java             # 认证接口
│   ├── DocumentController.java         # 文档 9 接口 [本次新建]
│   ├── KnowledgeBaseController.java    # 知识库 4 接口 [本次新建]
│   └── WorkspaceController.java        # 工作空间 3 接口 [本次新建]
├── model/
│   ├── dto/                            # 19 个 DTO/VO（含已有 Auth 4个 + 本次 15个）
│   ├── entity/                         # 19 个 JPA Entity（对应全部数据库表）
│   ├── enums/                          # 4 个枚举 [本次新建]
│   └── vo/                             # AiChatResponse（已有）
├── repository/                         # 19 个 JPA Repository + 本次补充查询方法
└── service/
    ├── AssetService.java               # 文件上传 [本次新建]
    ├── AuthService.java                # 认证服务
    ├── DocumentService.java            # 文档核心逻辑 [本次新建]
    ├── KnowledgeBaseService.java       # KB CRUD [本次新建]
    ├── PermissionService.java          # 权限校验中枢 [本次新建]
    └── WorkspaceService.java           # 工作空间 [本次新建]
```

## 核心架构决策

### 1. 权限校验链
```
PermissionService {
  getCurrentUserId()                       // StpUtil.getLoginId()
  checkWorkspaceAccess(wsId, roles...)     // 查 workspace_members.role，ordinal 比较
  checkKnowledgeBaseAccess(kbId, roles...) // 查 KB→workspace，委托 workspace 校验
  checkDocumentAccess(docId, roles...)     // 查 Doc→KB→workspace，委托 workspace 校验
  getDocumentWorkspaceId(docId)            // 用于审计日志中定位 workspace
}
```

### 2. 角色枚举 ordinal 设计
```
OWNER (0) > ADMIN (1) > EDITOR (2) > VIEWER (3) > GUEST (4)
权限检查：userRole.ordinal <= requiredRole.ordinal 即通过
```

### 3. 文档保存流程（saveContent）
```
@Transactional
1. 权限校验 (EDITOR+)
2. 获取当前文档
3. 计算 version_no = max(existingSnapshots.versionNo) + 1
4. 创建 DocumentSnapshot（content_json/markdown/html + created_by）
5. 创建 DocumentVersion（snapshot_id + version_no + change_summary）
6. 更新 Document.current_snapshot_id + updated_by + updated_at + title
7. 写入 audit_logs
8. （未来：异步触发 切片 + 全文索引 + Embedding 任务）
```

### 4. 版本回滚流程（restoreVersion）
```
@Transactional
1. 权限校验 (EDITOR+)
2. 获取目标版本的 DocumentVersion → 找出目标快照 DocumentSnapshot
3. 计算 newVersionNo = max + 1
4. 拷贝历史快照内容创建新 DocumentSnapshot
5. 创建新 DocumentVersion（changeSummary = "回滚至版本 v{N}"）
6. 更新 Document.current_snapshot_id
7. 写入审计日志
```

### 5. 软删除 + 级联
- 所有删除操作（KB、Document）均为软删除：只设置 status="DELETED"
- 删除 KB → 级联软删除该 KB 下所有文档
- 删除文档 → 递归软删除所有后代子文档
- 不执行物理删除，不丢失数据

### 6. 审计日志规范
- 所有写操作调用 `writeAuditLog(actorId, workspaceId, action, resourceType, resourceId)`
- 通过 AuditLogRepository 直接 save，简洁内联
- action 命名：`CREATE_WORKSPACE`, `INVITE_MEMBER`, `CREATE_KNOWLEDGE_BASE`, `DELETE_KNOWLEDGE_BASE`, `CREATE_DOCUMENT`, `SAVE_DOCUMENT`, `DELETE_DOCUMENT`, `MOVE_DOCUMENT`, `RESTORE_VERSION`, `UPLOAD_ASSET`

### 7. 移动文档循环引用检测
- `isDescendant(sourceId, targetId)` 向上遍历 parentId 链，禁止将文档移动到自己的子节点下

## 代码约定（本次会话建立的）

1. **所有方法必须使用中文注释**: `/** 方法业务逻辑描述 */`
2. **Controller 接口必须使用多行注释**: 标注功能、权限要求、业务细节
3. **统一异常处理**: 使用 `BizException.xxx()` 静态工厂方法（如 `BizException.badRequest()`, `BizException.notFound()`）
4. **事务边界**: 涉及多表写入的方法标注 `@Transactional`
5. **返回值统一包装**: Controller 所有返回用 `Result.success()` / `Result.error()`
6. **分页**: 使用 `PageResult<T>` 作为 `Result.data`
7. **审计日志**: 每个 Service 包含私有的 `writeAuditLog()` 方法

## 当前项目状态

### 已完成（~50%）

| 模块 | Entity | Repo | Service | Controller | 完成度 |
|------|--------|------|---------|------------|--------|
| Auth | ✅ | ✅ | ✅ | ✅ | 100% |
| Workspace | ✅ | ✅ | ✅ | ✅ | 100% |
| KnowledgeBase | ✅ | ✅ | ✅ | ✅ | 100% |
| Document | ✅ | ✅ | ✅ | ✅ | 100% |
| DocumentSnapshot | ✅ | ✅ | ✅* | ✅* | 100% |
| DocumentVersion | ✅ | ✅ | ✅* | ✅* | 100% |
| Asset | ✅ | ✅ | ✅ | ✅ | 100% |
| AuditLog | ✅ | ✅ | ✅* | ✅* | 100% |
| PermissionService | - | - | ✅ | - | 100% |

*DocumentSnapshot/DocumentVersion/AuditLog 的业务逻辑集成在 DocumentService 中

### 待开发（~50%）

| 模块 | 当前状态 |
|------|----------|
| Collaboration | 实体+Repo 已有，WebSocket/CRDT 服务未实现 |
| Search | 完全未实现 |
| AI Assistant | 仅基础 LimClient，切片/Embedding/向量检索/RAG 未实现 |
| Publication | 实体+Repo 已有，服务未实现 |
| Job | 实体+Repo 已有，任务调度框架未实现 |
| Cache | 实体+Repo 已有，缓存逻辑未实现 |

## 下一步开发方向（Phase 2-4）

1. **Phase 2 协同编辑**: WebSocket 鉴权、文档房间、CRDT update 持久化、在线用户状态、协同快照合并
2. **Phase 3 AI 助手**: 文档切片算法、Embedding Provider 接入、pgvector 向量检索、RAG 问答
3. **Phase 4 发布**: 公众号渠道配置、文档→HTML 转换、图片上传到微信、草稿创建、任务调度

## 技术栈版本

| 组件 | 版本 |
|------|------|
| Spring Boot | 4.1.0 |
| Java | 21 |
| Sa-Token | 1.45.0 (spring-boot4-starter) |
| PostgreSQL | (远程 101.34.79.46:5432) |
| Auth | Sa-Token Bearer Token + BCrypt 密码加密 |
| ORM | Spring Data JPA + Hibernate |
