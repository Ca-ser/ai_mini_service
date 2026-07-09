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

/**
 * 权限校验服务
 *
 * 权限链：Document → KnowledgeBase → Workspace → workspace_members.role
 * 通过 RoleEnum.ordinal() 比较角色级别（OWNER=0 最高，GUEST=4 最低）
 * 用户角色 ordinal <= 所需角色 ordinal 即放行
 */
@Service
public class PermissionService {

    @Resource
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private DocumentRepository documentRepository;

    /**
     * 从 Sa-Token 获取当前登录用户ID
     */
    public UUID getCurrentUserId() {
        String loginId = (String) StpUtil.getLoginId();
        return UUID.fromString(loginId);
    }

    /**
     * 获取当前用户在工作空间中的角色
     *
     * @param workspaceId 工作空间ID
     * @return 角色枚举，如果用户不属于该工作空间则返回 null
     */
    public RoleEnum getWorkspaceRole(UUID userId, UUID workspaceId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(member -> RoleEnum.valueOf(member.getRole()))
                .orElse(null);
    }

    /**
     * 校验当前用户是否有工作空间访问权限
     *
     * @param workspaceId   工作空间ID
     * @param requiredRoles 允许的角色列表（满足任一即可）
     */
    public void checkWorkspaceAccess(UUID workspaceId, RoleEnum... requiredRoles) {
        UUID userId = getCurrentUserId();
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> BizException.forbidden("您不是该工作空间的成员"));

        RoleEnum userRole = RoleEnum.valueOf(member.getRole());
        for (RoleEnum required : requiredRoles) {
            if (userRole.ordinal() <= required.ordinal()) {
                return;
            }
        }
        throw BizException.forbidden("权限不足，需要角色 " + joinRoleNames(requiredRoles));
    }

    /**
     * 校验当前用户是否有知识库访问权限
     * 通过知识库找到所属 workspace，再校验 workspace 权限
     *
     * @param kbId          知识库ID
     * @param requiredRoles 允许的角色列表
     */
    public void checkKnowledgeBaseAccess(UUID kbId, RoleEnum... requiredRoles) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> BizException.notFound("知识库不存在"));
        checkWorkspaceAccess(kb.getWorkspaceId(), requiredRoles);
    }

    /**
     * 校验当前用户是否有文档访问权限
     * 通过文档找到所属 KB → workspace，再校验 workspace 权限
     *
     * @param documentId    文档ID
     * @param requiredRoles 允许的角色列表
     */
    public void checkDocumentAccess(UUID documentId, RoleEnum... requiredRoles) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));
        KnowledgeBase kb = knowledgeBaseRepository.findById(document.getKbId())
                .orElseThrow(() -> BizException.notFound("知识库不存在"));
        checkWorkspaceAccess(kb.getWorkspaceId(), requiredRoles);
    }

    /**
     * 获取文档所属的工作空间ID（通过文档→KB→workspace 链查找）
     */
    public UUID getDocumentWorkspaceId(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BizException.notFound("文档不存在"));
        KnowledgeBase kb = knowledgeBaseRepository.findById(document.getKbId())
                .orElseThrow(() -> BizException.notFound("知识库不存在"));
        return kb.getWorkspaceId();
    }

    /**
     * 拼接角色名称，用于错误提示
     */
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
