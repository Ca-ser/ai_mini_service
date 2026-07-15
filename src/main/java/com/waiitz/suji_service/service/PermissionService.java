package com.waiitz.suji_service.service;

import cn.dev33.satoken.stp.StpUtil;
import com.waiitz.suji_service.common.BizException;
import com.waiitz.suji_service.model.entity.Document;
import com.waiitz.suji_service.model.entity.KnowledgeBase;
import com.waiitz.suji_service.model.entity.WorkspaceMember;
import com.waiitz.suji_service.model.enums.RoleEnum;
import com.waiitz.suji_service.repository.DocumentRepository;
import com.waiitz.suji_service.repository.KnowledgeBaseRepository;
import com.waiitz.suji_service.repository.WorkspaceMemberRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PermissionService {

    @Resource
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private DocumentRepository documentRepository;

    public UUID getCurrentUserId() {
        String loginId = (String) StpUtil.getLoginId();
        return UUID.fromString(loginId);
    }

    public RoleEnum getWorkspaceRole(UUID userId, UUID workspaceId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(member -> RoleEnum.valueOf(member.getRole()))
                .orElse(null);
    }

    public void checkWorkspaceAccess(UUID workspaceId, RoleEnum... requiredRoles) {
        UUID userId = getCurrentUserId();
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> BizException.forbidden("您不是该工作空间的成员"));

        RoleEnum userRole = RoleEnum.valueOf(member.getRole());
        for (RoleEnum required : requiredRoles) {
            if (userRole.getLevel() >= required.getLevel()) {
                return;
            }
        }
        throw BizException.forbidden("权限不足，需要角色 " + joinRoleNames(requiredRoles));
    }

    public void checkKnowledgeBaseAccess(UUID kbId, RoleEnum... requiredRoles) {
        KnowledgeBase kb = knowledgeBaseRepository.findActiveById(kbId)
                .orElseThrow(() -> BizException.notFound("知识库不存在"));
        checkWorkspaceAccess(kb.getWorkspaceId(), requiredRoles);
    }

    public void checkDocumentAccess(UUID documentId, RoleEnum... requiredRoles) {
        Document document = documentRepository.findActiveById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));
        KnowledgeBase kb = knowledgeBaseRepository.findActiveById(document.getKbId())
                .orElseThrow(() -> BizException.notFound("知识库不存在"));
        checkWorkspaceAccess(kb.getWorkspaceId(), requiredRoles);
    }

    public UUID getDocumentWorkspaceId(UUID documentId) {
        Document document = documentRepository.findActiveById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));
        KnowledgeBase kb = knowledgeBaseRepository.findActiveById(document.getKbId())
                .orElseThrow(() -> BizException.notFound("知识库不存在"));
        return kb.getWorkspaceId();
    }

    private String joinRoleNames(RoleEnum... roles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roles.length; i++) {
            if (i > 0) {
                sb.append(" 或 ");
            }
            sb.append(roles[i].name());
        }
        return sb.toString();
    }
}
