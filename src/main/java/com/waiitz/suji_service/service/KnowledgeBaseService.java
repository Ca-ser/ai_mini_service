package com.waiitz.suji_service.service;

import com.waiitz.suji_service.common.BizException;
import com.waiitz.suji_service.model.dto.CreateKnowledgeBaseRequest;
import com.waiitz.suji_service.model.dto.KnowledgeBaseVO;
import com.waiitz.suji_service.model.dto.UpdateKnowledgeBaseRequest;
import com.waiitz.suji_service.model.entity.AuditLog;
import com.waiitz.suji_service.model.entity.Document;
import com.waiitz.suji_service.model.entity.KnowledgeBase;
import com.waiitz.suji_service.model.enums.RoleEnum;
import com.waiitz.suji_service.repository.AuditLogRepository;
import com.waiitz.suji_service.repository.DocumentRepository;
import com.waiitz.suji_service.repository.KnowledgeBaseRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 知识库服务
 * 负责知识库的创建、查询、更新、软删除等业务逻辑
 */
@Service
public class KnowledgeBaseService {

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private DocumentRepository documentRepository;

    @Resource
    private AuditLogRepository auditLogRepository;

    @Resource
    private PermissionService permissionService;

    /**
     * 创建知识库
     * 校验用户在 workspace 中有 EDITOR 及以上权限，然后创建知识库记录
     *
     * @param workspaceId 所属工作空间ID
     * @param request     创建请求（name、description、visibility）
     * @return 创建成功后的知识库视图
     */
    @Transactional
    public KnowledgeBaseVO create(UUID workspaceId, CreateKnowledgeBaseRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验用户是否有创建知识库权限（EDITOR 及以上角色）
        permissionService.checkWorkspaceAccess(workspaceId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        // 创建 KnowledgeBase 实体
        KnowledgeBase kb = new KnowledgeBase();
        kb.setWorkspaceId(workspaceId);
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setVisibility(request.getVisibility());
        kb.setStatus("ACTIVE");
        kb.setCreatedBy(userId);
        kb.setCreatedAt(OffsetDateTime.now());
        kb.setUpdatedAt(OffsetDateTime.now());
        knowledgeBaseRepository.save(kb);

        // 写入审计日志
        writeAuditLog(userId, workspaceId, "CREATE_KNOWLEDGE_BASE", "KNOWLEDGE_BASE", kb.getId());

        return toKnowledgeBaseVO(kb);
    }

    /**
     * 查询指定工作空间下的所有活跃知识库列表
     * 每个知识库附带文档数量统计
     *
     * @param workspaceId 工作空间ID
     * @return 知识库视图列表
     */
    public List<KnowledgeBaseVO> listByWorkspace(UUID workspaceId) {
        // 校验用户有 VIEWER 及以上权限
        permissionService.checkWorkspaceAccess(workspaceId, RoleEnum.GUEST, RoleEnum.VIEWER, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        // 只查询非删除状态的知识库
        List<KnowledgeBase> kbs = knowledgeBaseRepository.findByWorkspaceIdAndStatusNot(workspaceId, "DELETED");
        List<KnowledgeBaseVO> result = new ArrayList<>();

        for (KnowledgeBase kb : kbs) {
            KnowledgeBaseVO vo = toKnowledgeBaseVO(kb);
            vo.setDocumentCount(documentRepository.countByKbIdAndStatusNot(kb.getId(), "DELETED"));
            result.add(vo);
        }

        return result;
    }

    /**
     * 更新知识库信息
     * 可更新名称、描述、可见性
     *
     * @param kbId    知识库ID
     * @param request 更新请求
     * @return 更新后的知识库视图
     */
    @Transactional
    public KnowledgeBaseVO update(UUID kbId, UpdateKnowledgeBaseRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验用户有 EDITOR 及以上权限
        permissionService.checkKnowledgeBaseAccess(kbId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> BizException.notFound("知识库不存在"));

        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setVisibility(request.getVisibility());
        kb.setUpdatedAt(OffsetDateTime.now());
        knowledgeBaseRepository.save(kb);

        // 获取知识库所属 workspace
        UUID workspaceId = kb.getWorkspaceId();
        writeAuditLog(userId, workspaceId, "UPDATE_KNOWLEDGE_BASE", "KNOWLEDGE_BASE", kbId);

        KnowledgeBaseVO vo = toKnowledgeBaseVO(kb);
        vo.setDocumentCount(documentRepository.countByKbIdAndStatusNot(kb.getId(), "DELETED"));
        return vo;
    }

    /**
     * 软删除知识库
     * 将知识库及其下属所有文档标记为 DELETED 状态，不物理删除
     *
     * @param kbId 知识库ID
     */
    @Transactional
    public void delete(UUID kbId) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验用户有 ADMIN 及以上权限（仅管理员及以上可删除）
        permissionService.checkKnowledgeBaseAccess(kbId, RoleEnum.ADMIN, RoleEnum.OWNER);

        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> BizException.notFound("知识库不存在"));

        // 软删除知识库
        kb.setStatus("DELETED");
        kb.setUpdatedAt(OffsetDateTime.now());
        knowledgeBaseRepository.save(kb);

        // 级联软删除该知识库下所有文档
        List<Document> documents = documentRepository.findByKbId(kbId);
        for (Document doc : documents) {
            doc.setStatus("DELETED");
            doc.setUpdatedAt(OffsetDateTime.now());
        }
        documentRepository.saveAll(documents);

        // 写入审计日志
        writeAuditLog(userId, kb.getWorkspaceId(), "DELETE_KNOWLEDGE_BASE", "KNOWLEDGE_BASE", kbId);
    }

    /**
     * 将 KnowledgeBase 实体转换为 KnowledgeBaseVO
     */
    private KnowledgeBaseVO toKnowledgeBaseVO(KnowledgeBase kb) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        vo.setId(kb.getId());
        vo.setWorkspaceId(kb.getWorkspaceId());
        vo.setName(kb.getName());
        vo.setDescription(kb.getDescription());
        vo.setVisibility(kb.getVisibility());
        vo.setCreatedAt(kb.getCreatedAt());
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
