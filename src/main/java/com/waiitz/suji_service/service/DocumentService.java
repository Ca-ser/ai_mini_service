package com.waiitz.suji_service.service;

import com.waiitz.suji_service.common.BizException;
import com.waiitz.suji_service.model.dto.CreateDocumentRequest;
import com.waiitz.suji_service.model.dto.DocumentTreeVO;
import com.waiitz.suji_service.model.dto.DocumentVO;
import com.waiitz.suji_service.model.dto.DocumentVersionVO;
import com.waiitz.suji_service.model.dto.MoveDocumentRequest;
import com.waiitz.suji_service.model.dto.SaveDocumentRequest;
import com.waiitz.suji_service.model.entity.AuditLog;
import com.waiitz.suji_service.model.entity.Document;
import com.waiitz.suji_service.model.entity.DocumentSnapshot;
import com.waiitz.suji_service.model.entity.DocumentVersion;
import com.waiitz.suji_service.model.enums.DocumentStatus;
import com.waiitz.suji_service.model.enums.RoleEnum;
import com.waiitz.suji_service.repository.AuditLogRepository;
import com.waiitz.suji_service.repository.DocumentRepository;
import com.waiitz.suji_service.repository.DocumentSnapshotRepository;
import com.waiitz.suji_service.repository.DocumentVersionRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档服务
 * 负责文档的创建、目录树构建、内容保存（快照+版本）、删除、移动、版本管理、回滚等核心业务
 *
 * 核心流程：
 * 1. 创建文档 → 生成初始空快照
 * 2. 保存文档 → 创建新快照 + 版本记录 + 更新 current_snapshot_id
 * 3. 回滚版本 → 基于历史快照创建新快照 + 新版本记录
 * 4. 删除文档 → 软删除（状态标记为 DELETED），级联处理子文档
 */
@Service
public class DocumentService {

    @Resource
    private DocumentRepository documentRepository;

    @Resource
    private DocumentSnapshotRepository documentSnapshotRepository;

    @Resource
    private DocumentVersionRepository documentVersionRepository;

    @Resource
    private AuditLogRepository auditLogRepository;

    @Resource
    private PermissionService permissionService;

    /**
     * 创建文档
     * 在指定知识库下创建新文档，同时生成一个初始空快照
     *
     * @param kbId    知识库ID
     * @param request 创建请求（parentId、title、contentFormat）
     * @return 创建成功的文档视图
     */
    @Transactional
    public DocumentVO create(UUID kbId, CreateDocumentRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验 EDITOR 及以上权限
        permissionService.checkKnowledgeBaseAccess(kbId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        // 创建 Document 实体
        Document document = new Document();
        document.setKbId(kbId);
        document.setParentId(request.getParentId());
        document.setTitle(request.getTitle());
        document.setContentFormat(request.getContentFormat());
        document.setStatus(DocumentStatus.DRAFT.name());
        document.setCreatedBy(userId);
        document.setUpdatedBy(userId);
        document.setCreatedAt(OffsetDateTime.now());
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        // 创建初始快照（版本号为 1，内容为空）
        DocumentSnapshot snapshot = new DocumentSnapshot();
        snapshot.setDocumentId(document.getId());
        snapshot.setVersionNo(1);
        snapshot.setCreatedBy(userId);
        snapshot.setCreatedAt(OffsetDateTime.now());
        documentSnapshotRepository.save(snapshot);

        // 创建初始版本记录
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(document.getId());
        version.setSnapshotId(snapshot.getId());
        version.setVersionNo(1);
        version.setChangeSummary("创建文档");
        version.setCreatedBy(userId);
        version.setCreatedAt(OffsetDateTime.now());
        documentVersionRepository.save(version);

        // 更新文档的当前快照引用
        document.setCurrentSnapshotId(snapshot.getId());
        documentRepository.save(document);

        // 写入审计日志
        UUID workspaceId = permissionService.getDocumentWorkspaceId(document.getId());
        writeAuditLog(userId, workspaceId, "CREATE_DOCUMENT", "DOCUMENT", document.getId());

        return buildDocumentVO(document, snapshot);
    }

    /**
     * 获取知识库的文档目录树
     * 查询该知识库下所有非删除状态的文档，递归构建目录树结构
     *
     * @param kbId 知识库ID
     * @return 目录树根节点列表
     */
    public List<DocumentTreeVO> getDocumentTree(UUID kbId) {
        // 校验 VIEWER 及以上权限
        permissionService.checkKnowledgeBaseAccess(kbId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        // 查询该知识库下所有非删除状态的文档
        List<Document> documents = documentRepository.findByKbIdAndStatusNot(kbId, DocumentStatus.DELETED.name());

        // 构建 parentId → children 映射
        Map<UUID, List<Document>> childrenMap = new HashMap<>();
        List<Document> roots = new ArrayList<>();

        for (Document doc : documents) {
            if (doc.getParentId() == null) {
                roots.add(doc);
            } else {
                childrenMap.computeIfAbsent(doc.getParentId(), k -> new ArrayList<>()).add(doc);
            }
        }

        // 递归构建树
        List<DocumentTreeVO> tree = new ArrayList<>();
        for (Document root : roots) {
            tree.add(buildTreeNode(root, childrenMap));
        }
        return tree;
    }

    /**
     * 获取文档详情
     * 查询文档实体及其当前快照内容
     *
     * @param documentId 文档ID
     * @return 包含完整内容的文档视图
     */
    public DocumentVO getDocument(UUID documentId) {
        // 校验 VIEWER 及以上权限
        permissionService.checkDocumentAccess(documentId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        // 获取当前快照内容
        DocumentSnapshot snapshot = null;
        if (document.getCurrentSnapshotId() != null) {
            snapshot = documentSnapshotRepository.findById(document.getCurrentSnapshotId()).orElse(null);
        }

        return buildDocumentVO(document, snapshot);
    }

    /**
     * 保存文档内容
     * 创建新的文档快照和版本记录，更新文档的 current_snapshot_id
     * 注意：当前版本不阻塞等待 AI 切片/索引任务（后续异步实现）
     *
     * @param documentId 文档ID
     * @param request    保存请求（title、contentJson、contentMarkdown、contentHtml、changeSummary）
     * @return 保存后的文档视图（含新版本号）
     */
    @Transactional
    public DocumentVO saveContent(UUID documentId, SaveDocumentRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验 EDITOR 及以上权限
        permissionService.checkDocumentAccess(documentId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        // 计算新版本号：当前最大版本号 + 1
        List<DocumentSnapshot> existingSnapshots = documentSnapshotRepository
                .findByDocumentIdOrderByVersionNoDesc(documentId);
        int newVersionNo = existingSnapshots.isEmpty() ? 1 : existingSnapshots.get(0).getVersionNo() + 1;

        // 创建新的 DocumentSnapshot
        DocumentSnapshot snapshot = new DocumentSnapshot();
        snapshot.setDocumentId(documentId);
        snapshot.setVersionNo(newVersionNo);
        snapshot.setContentJson(request.getContentJson());
        snapshot.setContentMarkdown(request.getContentMarkdown());
        snapshot.setContentHtml(request.getContentHtml());
        snapshot.setCreatedBy(userId);
        snapshot.setCreatedAt(OffsetDateTime.now());
        documentSnapshotRepository.save(snapshot);

        // 创建新的 DocumentVersion 记录
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(documentId);
        version.setSnapshotId(snapshot.getId());
        version.setVersionNo(newVersionNo);
        version.setChangeSummary(request.getChangeSummary() != null ? request.getChangeSummary() : "");
        version.setCreatedBy(userId);
        version.setCreatedAt(OffsetDateTime.now());
        documentVersionRepository.save(version);

        // 更新文档的当前快照引用和基本信息
        document.setCurrentSnapshotId(snapshot.getId());
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            document.setTitle(request.getTitle());
        }
        document.setUpdatedBy(userId);
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        // 写入审计日志
        UUID workspaceId = permissionService.getDocumentWorkspaceId(documentId);
        writeAuditLog(userId, workspaceId, "SAVE_DOCUMENT", "DOCUMENT", documentId);

        return buildDocumentVO(document, snapshot);
    }

    /**
     * 删除文档（软删除）
     * 将当前文档及其所有后代子文档标记为 DELETED 状态
     *
     * @param documentId 文档ID
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验 EDITOR 及以上权限
        permissionService.checkDocumentAccess(documentId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        // 软删除当前文档
        document.setStatus(DocumentStatus.DELETED.name());
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        // 递归软删除所有子文档
        cascadeDeleteChildren(documentId);

        // 写入审计日志
        UUID workspaceId = permissionService.getDocumentWorkspaceId(documentId);
        writeAuditLog(userId, workspaceId, "DELETE_DOCUMENT", "DOCUMENT", documentId);
    }

    /**
     * 移动文档到新的父节点
     *
     * @param documentId 被移动的文档ID
     * @param request    移动请求（targetParentId：目标父文档ID，null 表示移到根目录）
     */
    @Transactional
    public void moveDocument(UUID documentId, MoveDocumentRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验 EDITOR 及以上权限
        permissionService.checkDocumentAccess(documentId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        // 防止将文档移动到自己的子节点下（循环引用）
        if (request.getTargetParentId() != null && isDescendant(documentId, request.getTargetParentId())) {
            throw BizException.badRequest("不能将文档移动到其子节点下");
        }

        document.setParentId(request.getTargetParentId());
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        // 写入审计日志
        UUID workspaceId = permissionService.getDocumentWorkspaceId(documentId);
        writeAuditLog(userId, workspaceId, "MOVE_DOCUMENT", "DOCUMENT", documentId);
    }

    /**
     * 查询文档的版本历史列表
     *
     * @param documentId 文档ID
     * @return 版本视图列表，按版本号倒序排列
     */
    public List<DocumentVersionVO> listVersions(UUID documentId) {
        // 校验 VIEWER 及以上权限
        permissionService.checkDocumentAccess(documentId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        List<DocumentVersion> versions = documentVersionRepository.findByDocumentIdOrderByVersionNoDesc(documentId);
        List<DocumentVersionVO> result = new ArrayList<>();

        for (DocumentVersion v : versions) {
            DocumentVersionVO vo = new DocumentVersionVO();
            vo.setId(v.getId());
            vo.setVersionNo(v.getVersionNo());
            vo.setChangeSummary(v.getChangeSummary());
            vo.setSnapshotId(v.getSnapshotId());
            vo.setCreatedBy(v.getCreatedBy());
            vo.setCreatedAt(v.getCreatedAt());
            result.add(vo);
        }

        return result;
    }

    /**
     * 获取指定版本的文档详细内容
     *
     * @param documentId 文档ID
     * @param versionNo  版本号
     * @return 该版本对应的文档视图（含快照内容）
     */
    public DocumentVO getVersion(UUID documentId, Integer versionNo) {
        // 校验 VIEWER 及以上权限
        permissionService.checkDocumentAccess(documentId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        // 查找指定版本的版本记录，再通过 snapshotId 获取快照内容
        DocumentVersion version = documentVersionRepository.findByDocumentIdAndVersionNo(documentId, versionNo)
                .orElseThrow(() -> BizException.notFound("版本不存在：v" + versionNo));

        DocumentSnapshot snapshot = documentSnapshotRepository.findById(version.getSnapshotId())
                .orElseThrow(() -> BizException.notFound("版本快照不存在"));

        return buildDocumentVO(document, snapshot);
    }

    /**
     * 回滚文档到指定版本
     * 基于历史快照创建新快照作为当前版本，保留完整的回滚记录
     *
     * @param documentId 文档ID
     * @param versionNo  要回滚到的目标版本号
     * @return 回滚后新生成的文档视图
     */
    @Transactional
    public DocumentVO restoreVersion(UUID documentId, Integer versionNo) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验 EDITOR 及以上权限
        permissionService.checkDocumentAccess(documentId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        // 获取目标版本的快照
        DocumentVersion targetVersion = documentVersionRepository.findByDocumentIdAndVersionNo(documentId, versionNo)
                .orElseThrow(() -> BizException.notFound("版本不存在：v" + versionNo));

        DocumentSnapshot targetSnapshot = documentSnapshotRepository.findById(targetVersion.getSnapshotId())
                .orElseThrow(() -> BizException.notFound("版本快照不存在"));

        // 计算新版本号
        List<DocumentSnapshot> existingSnapshots = documentSnapshotRepository
                .findByDocumentIdOrderByVersionNoDesc(documentId);
        int newVersionNo = existingSnapshots.isEmpty() ? 1 : existingSnapshots.get(0).getVersionNo() + 1;

        // 基于历史快照内容创建新的当前快照
        DocumentSnapshot newSnapshot = new DocumentSnapshot();
        newSnapshot.setDocumentId(documentId);
        newSnapshot.setVersionNo(newVersionNo);
        newSnapshot.setContentJson(targetSnapshot.getContentJson());
        newSnapshot.setContentMarkdown(targetSnapshot.getContentMarkdown());
        newSnapshot.setContentHtml(targetSnapshot.getContentHtml());
        newSnapshot.setCreatedBy(userId);
        newSnapshot.setCreatedAt(OffsetDateTime.now());
        documentSnapshotRepository.save(newSnapshot);

        // 创建回滚版本记录
        DocumentVersion newVersion = new DocumentVersion();
        newVersion.setDocumentId(documentId);
        newVersion.setSnapshotId(newSnapshot.getId());
        newVersion.setVersionNo(newVersionNo);
        newVersion.setChangeSummary("回滚至版本 v" + versionNo);
        newVersion.setCreatedBy(userId);
        newVersion.setCreatedAt(OffsetDateTime.now());
        documentVersionRepository.save(newVersion);

        // 更新文档的当前快照引用
        document.setCurrentSnapshotId(newSnapshot.getId());
        document.setUpdatedBy(userId);
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        // 写入审计日志
        UUID workspaceId = permissionService.getDocumentWorkspaceId(documentId);
        writeAuditLog(userId, workspaceId, "RESTORE_VERSION", "DOCUMENT", documentId);

        return buildDocumentVO(document, newSnapshot);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 递归构建目录树节点
     */
    private DocumentTreeVO buildTreeNode(Document doc, Map<UUID, List<Document>> childrenMap) {
        DocumentTreeVO node = new DocumentTreeVO(doc.getId(), doc.getTitle(), "DOCUMENT");
        List<Document> children = childrenMap.get(doc.getId());
        if (children != null) {
            for (Document child : children) {
                node.getChildren().add(buildTreeNode(child, childrenMap));
            }
        }
        return node;
    }

    /**
     * 递归软删除所有子文档
     */
    private void cascadeDeleteChildren(UUID parentId) {
        List<Document> children = documentRepository.findByParentId(parentId);
        for (Document child : children) {
            if (!DocumentStatus.DELETED.name().equals(child.getStatus())) {
                child.setStatus(DocumentStatus.DELETED.name());
                child.setUpdatedAt(OffsetDateTime.now());
                documentRepository.save(child);
                cascadeDeleteChildren(child.getId());
            }
        }
    }

    /**
     * 判断 targetId 是否为 sourceId 的后代节点（用于防止循环引用）
     */
    private boolean isDescendant(UUID sourceId, UUID targetId) {
        // sourceId 是父节点，targetId 是目标位置
        // 查找 targetId 的所有祖先，看是否包含 sourceId
        Document current = documentRepository.findById(targetId).orElse(null);
        while (current != null && current.getParentId() != null) {
            if (current.getParentId().equals(sourceId)) {
                return true;
            }
            current = documentRepository.findById(current.getParentId()).orElse(null);
        }
        return false;
    }

    /**
     * 构建 DocumentVO（文档视图对象）
     *
     * @param document 文档实体
     * @param snapshot 当前快照（可为 null 时内容为空）
     * @return 组装好的文档视图
     */
    private DocumentVO buildDocumentVO(Document document, DocumentSnapshot snapshot) {
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setKbId(document.getKbId());
        vo.setParentId(document.getParentId());
        vo.setTitle(document.getTitle());
        vo.setContentFormat(document.getContentFormat());
        vo.setStatus(document.getStatus());
        vo.setCreatedBy(document.getCreatedBy());
        vo.setCreatedAt(document.getCreatedAt());
        vo.setUpdatedAt(document.getUpdatedAt());

        if (snapshot != null) {
            vo.setContentJson(snapshot.getContentJson());
            vo.setContentMarkdown(snapshot.getContentMarkdown());
            vo.setContentHtml(snapshot.getContentHtml());
            vo.setVersionNo(snapshot.getVersionNo());
        }

        return vo;
    }

    /**
     * 写入审计日志
     */
    private void writeAuditLog(UUID actorId, UUID workspaceId, String action, String resourceType, UUID resourceId) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setWorkspaceId(workspaceId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setCreatedAt(OffsetDateTime.now());
        auditLogRepository.save(log);
    }

}
