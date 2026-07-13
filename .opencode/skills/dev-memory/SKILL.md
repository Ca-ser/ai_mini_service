---
name: dev-memory
description: 项目级记忆管理器 — 每次会话结束时自动记录工作总结、更新进度、整理代码变更。使用 ONLY 在会话结束时或用户要求记录/总结进度时触发。
---

# Dev Memory — 会话记忆自动管理

## 核心职责

每次开发会话结束时，自动执行以下动作：

1. **分析本次会话变更** — 新增/修改/删除的文件清单
2. **生成会话记忆文件** — 保存到 `.opencode/skills/dev-memory/` 目录
3. **更新开发进度** — 同步更新 `knowledge-base-backend` 和 `openapi-docs` 技能的进度状态
4. **整理待办事项** — 更新 todo list 状态

---

## 会话记忆文件命名规范

```
dev-memory-{YYYY-MM-DD}-{topic}.md
```

- `{YYYY-MM-DD}`: 会话结束日期（如 `2026-07-13`）
- `{topic}`: 本次会话主题（如 `collaboration-phase2`、`ai-chunking`、`bug-fix`）

**目录**: `.opencode/skills/dev-memory/`

---

## 会话记忆文件模板

每次生成的 `.md` 文件必须包含以下章节：

```markdown
---
name: dev-memory-{YYYY-MM-DD}-{topic}
description: 第 N 次开发会话记忆 — {简短描述}
session_date: {YYYY-MM-DD}
phase: {1|2|3|4}
---

# 会话记忆 — {会话主题}

## 会话成果摘要

| 维度 | 完成情况 |
|------|----------|
| 新建文件 | {N} 个 |
| 修改文件 | {N} 个 |
| 删除文件 | {N} 个 |
| API 接口 | {N} 个（新增/修改） |
| 项目进度 | {X}% → {Y}% |

## 本次会话文件变更清单

### 新建文件
- `{文件路径}` — {说明}

### 修改文件
- `{文件路径}` — {修改内容摘要}

### 删除文件
- `{文件路径}` — {原因}

## 已实现/变更的 API 清单

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `{path}` | {说明} | {权限} |

## 核心架构决策

### {决策标题}
{决策描述和理由}

## 代码约定变更

{本次会话新增或修改的代码规范}

## 当前项目状态

### 已完成模块
| 模块 | 完成度 | 说明 |
|------|--------|------|
| {模块} | {X}% | {状态} |

### 待开发模块
| 模块 | 优先级 | 说明 |
|------|--------|------|
| {模块} | {高/中/低} | {状态} |

## 下一步计划

1. {任务} — {说明}
2. {任务} — {说明}

## 遇到的问题 & 解决方案

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| {问题描述} | {根因} | {处理方法} |
```

---

## 自动执行流程（会话结束时的标准操作步骤）

### Step 1: 分析变更
```bash
# 获取文件变更统计
git diff --name-status HEAD~1  # 相较于上次提交
git status --short             # 当前工作区状态
```

### Step 2: 生成记忆文件
- 使用上方模板创建新的 `dev-memory-{date}-{topic}.md`
- 确保 frontmatter 中的 `name` 与文件名匹配
- 所有章节必须填写完整，不能留空

### Step 3: 更新主技能文件
- **`knowledge-base-backend/SKILL.md`**: 更新 `## Development Progress` 章节，将完成项从 Pending 移到 Completed
- **`openapi-docs/SKILL.md`**: 更新 API 状态标记（Done / In Progress / Planned），补充新增 API 的详细文档

### Step 4: 提交到 Git
```bash
git add .opencode/skills/
git commit -m "docs: {YYYY-MM-DD} 开发会话记录 — {topic}"
```

> 注意：只有在用户明确要求提交时才执行 Step 4

---

## 进度百分比快速参考

| 阶段 | 模块 | 完成度 |
|------|------|--------|
| Phase 1 (基础) | Auth | ~16% |
| Phase 1 (基础) | Workspace | ~16% |
| Phase 1 (基础) | KnowledgeBase | ~16% |
| Phase 1 (基础) | Document | ~16% |
| Phase 1 (基础) | Asset | ~16% |
| Phase 1 (基础) | Permission | ~10% |
| Phase 1 (基础) | AuditLog | ~10% |
| **Phase 1 小计** | | **~50%** |
| Phase 2 | Collaboration | 0% |
| Phase 3 | Search | 0% |
| Phase 3 | AI Assistant | 0% |
| Phase 4 | Publication | 0% |
| - | Job Framework | 0% |
| - | Cache Logic | 0% |
| **待开发总计** | | **~50%** |

---

## 当前已有会话记录

| 文件 | 日期 | 主题 |
|------|------|------|
| `dev-memory-2026-07-09-phase1.md` | 2026-07-09 | Phase 1 知识库闭环实现 |
| ... | ... | ... |

---

## 注意事项

1. **每会话一记录**: 每次开发会话结束时只生成一个记忆文件
2. **不要覆盖历史**: 每次生成新的日期文件，不要修改旧记录
3. **准确性优先**: 文件数量、API 接口数量等统计数据必须从实际代码中提取，不能估算
4. **约定同步**: 如果本次会话建立了新的代码约定，务必在"代码约定变更"章节记录
