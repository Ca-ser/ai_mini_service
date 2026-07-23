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
import com.waiitz.suji_service.model.entity.KnowledgeBase;
import com.waiitz.suji_service.model.enums.DocumentStatus;
import com.waiitz.suji_service.model.enums.RoleEnum;
import com.waiitz.suji_service.repository.AuditLogRepository;
import com.waiitz.suji_service.repository.DocumentRepository;
import com.waiitz.suji_service.repository.DocumentSnapshotRepository;
import com.waiitz.suji_service.repository.DocumentVersionRepository;
import com.waiitz.suji_service.repository.KnowledgeBaseRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    @Resource
    private DocumentRepository documentRepository;

    @Resource
    private DocumentSnapshotRepository documentSnapshotRepository;

    @Resource
    private DocumentVersionRepository documentVersionRepository;

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private AuditLogRepository auditLogRepository;

    @Resource
    private PermissionService permissionService;

    @Transactional
    public DocumentVO create(UUID kbId, CreateDocumentRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        permissionService.checkKnowledgeBaseAccess(kbId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        KnowledgeBase kb = knowledgeBaseRepository.findActiveById(kbId)
                .orElseThrow(() -> BizException.notFound("知识库不存在"));

        if (request.getParentId() != null) {
            Document parent = documentRepository.findActiveById(request.getParentId())
                    .orElseThrow(() -> BizException.notFound("父文档不存在"));
            if (!parent.getKbId().equals(kbId)) {
                throw BizException.badRequest("父文档不属于该知识库");
            }
        }

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

        DocumentSnapshot snapshot = new DocumentSnapshot();
        snapshot.setDocumentId(document.getId());
        snapshot.setVersionNo(1);
        snapshot.setCreatedBy(userId);
        snapshot.setCreatedAt(OffsetDateTime.now());
        documentSnapshotRepository.save(snapshot);

        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(document.getId());
        version.setSnapshotId(snapshot.getId());
        version.setVersionNo(1);
        version.setChangeSummary("创建文档");
        version.setCreatedBy(userId);
        version.setCreatedAt(OffsetDateTime.now());
        documentVersionRepository.save(version);

        document.setCurrentSnapshotId(snapshot.getId());
        documentRepository.save(document);

        UUID workspaceId = permissionService.getDocumentWorkspaceId(document.getId());
        writeAuditLog(userId, workspaceId, "CREATE_DOCUMENT", "DOCUMENT", document.getId());

        return buildDocumentVO(document, snapshot);
    }

    public List<DocumentTreeVO> getDocumentTree(UUID kbId) {
        permissionService.checkKnowledgeBaseAccess(kbId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        List<Document> documents = documentRepository.findByKbIdAndStatusNot(kbId, DocumentStatus.DELETED.name());

        Map<UUID, List<Document>> childrenMap = new HashMap<>();
        List<Document> roots = new ArrayList<>();

        for (Document doc : documents) {
            if (doc.getParentId() == null) {
                roots.add(doc);
            } else {
                childrenMap.computeIfAbsent(doc.getParentId(), k -> new ArrayList<>()).add(doc);
            }
        }

        List<DocumentTreeVO> tree = new ArrayList<>();
        for (Document root : roots) {
            tree.add(buildTreeNode(root, childrenMap));
        }
        return tree;
    }

    public DocumentVO getDocument(UUID documentId) {
        permissionService.checkDocumentAccess(documentId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findActiveById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        DocumentSnapshot snapshot = null;
        if (document.getCurrentSnapshotId() != null) {
            snapshot = documentSnapshotRepository.findById(document.getCurrentSnapshotId()).orElse(null);
        }

        return buildDocumentVO(document, snapshot);
    }

    @Transactional
    public DocumentVO saveContent(UUID documentId, SaveDocumentRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        permissionService.checkDocumentAccess(documentId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findByIdWithLock(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        if (DocumentStatus.DELETED.name().equals(document.getStatus())) {
            throw BizException.notFound("文档不存在");
        }

        List<DocumentSnapshot> existingSnapshots = documentSnapshotRepository
                .findByDocumentIdOrderByVersionNoDescWithLock(documentId);
        int newVersionNo = existingSnapshots.isEmpty() ? 1 : existingSnapshots.get(0).getVersionNo() + 1;

        DocumentSnapshot snapshot = new DocumentSnapshot();
        snapshot.setDocumentId(documentId);
        snapshot.setVersionNo(newVersionNo);
        snapshot.setContentJson(request.getContentJson());
        snapshot.setContentMarkdown(request.getContentMarkdown());
        snapshot.setContentHtml(request.getContentHtml());
        snapshot.setCreatedBy(userId);
        snapshot.setCreatedAt(OffsetDateTime.now());
        documentSnapshotRepository.save(snapshot);

        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(documentId);
        version.setSnapshotId(snapshot.getId());
        version.setVersionNo(newVersionNo);
        version.setChangeSummary(request.getChangeSummary() != null ? request.getChangeSummary() : "");
        version.setCreatedBy(userId);
        version.setCreatedAt(OffsetDateTime.now());
        documentVersionRepository.save(version);

        document.setCurrentSnapshotId(snapshot.getId());
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            document.setTitle(request.getTitle());
        }
        document.setUpdatedBy(userId);
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        UUID workspaceId = permissionService.getDocumentWorkspaceId(documentId);
        writeAuditLog(userId, workspaceId, "SAVE_DOCUMENT", "DOCUMENT", documentId);

        return buildDocumentVO(document, snapshot);
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        UUID userId = permissionService.getCurrentUserId();

        permissionService.checkDocumentAccess(documentId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findActiveById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        UUID workspaceId = permissionService.getDocumentWorkspaceId(documentId);

        document.setStatus(DocumentStatus.DELETED.name());
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        cascadeDeleteChildren(documentId);

        writeAuditLog(userId, workspaceId, "DELETE_DOCUMENT", "DOCUMENT", documentId);
    }

    @Transactional
    public void moveDocument(UUID documentId, MoveDocumentRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        permissionService.checkDocumentAccess(documentId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findActiveById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        if (request.getTargetParentId() != null) {
            if (request.getTargetParentId().equals(documentId)) {
                throw BizException.badRequest("不能将文档自身设为其父节点");
            }

            Document targetParent = documentRepository.findActiveById(request.getTargetParentId())
                    .orElseThrow(() -> BizException.notFound("目标父文档不存在"));

            if (!targetParent.getKbId().equals(document.getKbId())) {
                throw BizException.badRequest("不能将文档移动到其他知识库");
            }

            if (isDescendant(documentId, request.getTargetParentId())) {
                throw BizException.badRequest("不能将文档移动到其子节点下");
            }
        }

        document.setParentId(request.getTargetParentId());
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        UUID workspaceId = permissionService.getDocumentWorkspaceId(documentId);
        writeAuditLog(userId, workspaceId, "MOVE_DOCUMENT", "DOCUMENT", documentId);
    }

    public List<DocumentVersionVO> listVersions(UUID documentId) {
        permissionService.checkDocumentAccess(documentId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        documentRepository.findActiveById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

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

    public DocumentVO getVersion(UUID documentId, Integer versionNo) {
        permissionService.checkDocumentAccess(documentId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findActiveById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        DocumentVersion version = documentVersionRepository.findByDocumentIdAndVersionNo(documentId, versionNo)
                .orElseThrow(() -> BizException.notFound("版本不存在：v" + versionNo));

        DocumentSnapshot snapshot = documentSnapshotRepository.findById(version.getSnapshotId())
                .orElseThrow(() -> BizException.notFound("版本快照不存在"));

        return buildDocumentVO(document, snapshot);
    }

    @Transactional
    public DocumentVO restoreVersion(UUID documentId, Integer versionNo) {
        UUID userId = permissionService.getCurrentUserId();

        permissionService.checkDocumentAccess(documentId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        Document document = documentRepository.findByIdWithLock(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));

        if (DocumentStatus.DELETED.name().equals(document.getStatus())) {
            throw BizException.notFound("文档不存在");
        }

        DocumentVersion targetVersion = documentVersionRepository.findByDocumentIdAndVersionNo(documentId, versionNo)
                .orElseThrow(() -> BizException.notFound("版本不存在：v" + versionNo));

        DocumentSnapshot targetSnapshot = documentSnapshotRepository.findById(targetVersion.getSnapshotId())
                .orElseThrow(() -> BizException.notFound("版本快照不存在"));

        List<DocumentSnapshot> existingSnapshots = documentSnapshotRepository
                .findByDocumentIdOrderByVersionNoDescWithLock(documentId);
        int newVersionNo = existingSnapshots.isEmpty() ? 1 : existingSnapshots.get(0).getVersionNo() + 1;

        DocumentSnapshot newSnapshot = new DocumentSnapshot();
        newSnapshot.setDocumentId(documentId);
        newSnapshot.setVersionNo(newVersionNo);
        newSnapshot.setContentJson(targetSnapshot.getContentJson());
        newSnapshot.setContentMarkdown(targetSnapshot.getContentMarkdown());
        newSnapshot.setContentHtml(targetSnapshot.getContentHtml());
        newSnapshot.setCreatedBy(userId);
        newSnapshot.setCreatedAt(OffsetDateTime.now());
        documentSnapshotRepository.save(newSnapshot);

        DocumentVersion newVersion = new DocumentVersion();
        newVersion.setDocumentId(documentId);
        newVersion.setSnapshotId(newSnapshot.getId());
        newVersion.setVersionNo(newVersionNo);
        newVersion.setChangeSummary("回滚至版本 v" + versionNo);
        newVersion.setCreatedBy(userId);
        newVersion.setCreatedAt(OffsetDateTime.now());
        documentVersionRepository.save(newVersion);

        document.setCurrentSnapshotId(newSnapshot.getId());
        document.setUpdatedBy(userId);
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);

        UUID workspaceId = permissionService.getDocumentWorkspaceId(documentId);
        writeAuditLog(userId, workspaceId, "RESTORE_VERSION", "DOCUMENT", documentId);

        return buildDocumentVO(document, newSnapshot);
    }

    // ==================== 私有辅助方法 ====================

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

    private boolean isDescendant(UUID sourceId, UUID targetId) {
        Set<UUID> visited = new HashSet<>();
        Document current = documentRepository.findById(targetId).orElse(null);
        while (current != null && current.getParentId() != null) {
            if (!visited.add(current.getParentId())) {
                return false;
            }
            if (current.getParentId().equals(sourceId)) {
                return true;
            }
            current = documentRepository.findById(current.getParentId()).orElse(null);
        }
        return false;
    }

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
