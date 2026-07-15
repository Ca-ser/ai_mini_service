package com.waiitz.suji_service.service;

import com.waiitz.suji_service.common.BizException;
import com.waiitz.suji_service.model.dto.CreateWorkspaceRequest;
import com.waiitz.suji_service.model.dto.InviteMemberRequest;
import com.waiitz.suji_service.model.dto.MemberVO;
import com.waiitz.suji_service.model.dto.WorkspaceVO;
import com.waiitz.suji_service.model.entity.AuditLog;
import com.waiitz.suji_service.model.entity.User;
import com.waiitz.suji_service.model.entity.Workspace;
import com.waiitz.suji_service.model.entity.WorkspaceMember;
import com.waiitz.suji_service.model.enums.RoleEnum;
import com.waiitz.suji_service.repository.AuditLogRepository;
import com.waiitz.suji_service.repository.KnowledgeBaseRepository;
import com.waiitz.suji_service.repository.UserRepository;
import com.waiitz.suji_service.repository.WorkspaceMemberRepository;
import com.waiitz.suji_service.repository.WorkspaceRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 工作空间服务
 * 负责工作空间创建、列表查询、成员邀请等业务逻辑
 */
@Service
public class WorkspaceService {

    @Resource
    private WorkspaceRepository workspaceRepository;

    @Resource
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private AuditLogRepository auditLogRepository;

    @Resource
    private PermissionService permissionService;

    /**
     * 创建工作空间
     * 创建者自动成为工作空间的 OWNER
     *
     * @param request 创建请求（name、description）
     * @return 创建成功后的工作空间视图
     */
    @Transactional
    public WorkspaceVO create(CreateWorkspaceRequest request) {
        UUID userId = permissionService.getCurrentUserId();

        // 创建 Workspace 实体
        Workspace workspace = new Workspace();
        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());
        workspace.setOwnerId(userId);
        workspace.setStatus("ACTIVE");
        workspace.setCreatedAt(OffsetDateTime.now());
        workspace.setUpdatedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);

        // 创建者自动添加为 OWNER 角色
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(userId);
        member.setRole(RoleEnum.OWNER.name());
        member.setCreatedAt(OffsetDateTime.now());
        member.setUpdatedAt(OffsetDateTime.now());
        workspaceMemberRepository.save(member);

        // 写入审计日志
        writeAuditLog(userId, workspace.getId(), "CREATE_WORKSPACE", "WORKSPACE", workspace.getId());

        return toWorkspaceVO(workspace, RoleEnum.OWNER);
    }

    /**
     * 查询当前用户加入的所有工作空间
     * 包含用户角色、成员数量、知识库数量等汇总信息
     *
     * @return 工作空间视图列表
     */
    public List<WorkspaceVO> listWorkspaces() {
        UUID userId = permissionService.getCurrentUserId();

        // 查询该用户在所有 workspace 中的成员记录
        List<WorkspaceMember> members = workspaceMemberRepository.findByUserId(userId);
        List<WorkspaceVO> result = new ArrayList<>();

        for (WorkspaceMember member : members) {
            Workspace workspace = workspaceRepository.findById(member.getWorkspaceId())
                    .orElse(null);
            if (workspace == null) {
                continue;
            }

            WorkspaceVO vo = new WorkspaceVO();
            vo.setId(workspace.getId());
            vo.setName(workspace.getName());
            vo.setDescription(workspace.getDescription());
            vo.setRole(member.getRole());
            vo.setMemberCount(workspaceMemberRepository.countByWorkspaceId(workspace.getId()));
            vo.setKbCount(knowledgeBaseRepository.countByWorkspaceId(workspace.getId()));
            vo.setCreatedAt(workspace.getCreatedAt());
            result.add(vo);
        }

        return result;
    }

    /**
     * 邀请成员加入工作空间
     * 通过邮箱查找用户，校验是否已是成员，然后添加成员记录
     *
     * @param workspaceId 工作空间ID
     * @param request     邀请请求（email、role）
     * @return 新成员的视图对象
     */
    @Transactional
    public MemberVO inviteMember(UUID workspaceId, InviteMemberRequest request) {
        UUID actorUserId = permissionService.getCurrentUserId();

        // 校验操作者是否有 OWNER 或 ADMIN 权限
        permissionService.checkWorkspaceAccess(workspaceId, RoleEnum.OWNER, RoleEnum.ADMIN);

        // 通过邮箱查找用户
        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BizException.notFound("用户不存在：" + request.getEmail()));

        // 校验该用户是否已是工作空间成员
        boolean alreadyMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId, targetUser.getId());
        if (alreadyMember) {
            throw BizException.conflict("该用户已是工作空间成员");
        }

        // 校验角色合法性
        RoleEnum targetRole;
        try {
            targetRole = RoleEnum.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            throw BizException.badRequest("无效的角色类型：" + request.getRole());
        }

        // 禁止通过邀请授予 OWNER 角色
        if (targetRole == RoleEnum.OWNER) {
            throw BizException.forbidden("不能通过邀请授予 OWNER 角色");
        }

        // 获取操作者角色，限制可授予的角色上限
        RoleEnum actorRole = permissionService.getWorkspaceRole(actorUserId, workspaceId);
        if (actorRole == RoleEnum.ADMIN) {
            // ADMIN 只能授予 EDITOR、VIEWER、GUEST
            if (targetRole.getLevel() > RoleEnum.EDITOR.getLevel()) {
                throw BizException.forbidden("ADMIN 不能授予 " + targetRole + " 角色");
            }
        }

        // 添加成员
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(targetUser.getId());
        member.setRole(request.getRole());
        member.setCreatedAt(OffsetDateTime.now());
        member.setUpdatedAt(OffsetDateTime.now());
        workspaceMemberRepository.save(member);

        // 写入审计日志
        writeAuditLog(actorUserId, workspaceId, "INVITE_MEMBER", "MEMBER", member.getId());

        // 构建返回对象
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setUserId(targetUser.getId());
        vo.setUsername(targetUser.getUsername());
        vo.setEmail(targetUser.getEmail());
        vo.setRole(member.getRole());
        vo.setCreatedAt(member.getCreatedAt());
        return vo;
    }

    /**
     * 将 Workspace 实体转换为 WorkspaceVO
     */
    private WorkspaceVO toWorkspaceVO(Workspace workspace, RoleEnum role) {
        WorkspaceVO vo = new WorkspaceVO();
        vo.setId(workspace.getId());
        vo.setName(workspace.getName());
        vo.setDescription(workspace.getDescription());
        vo.setRole(role.name());
        vo.setMemberCount(1L);
        vo.setKbCount(0L);
        vo.setCreatedAt(workspace.getCreatedAt());
        return vo;
    }

    /**
     * 写入审计日志
     *
     * @param actorId      操作人ID
     * @param workspaceId  工作空间ID
     * @param action       操作类型（如 CREATE_WORKSPACE、INVITE_MEMBER）
     * @param resourceType 资源类型（如 WORKSPACE、MEMBER）
     * @param resourceId   资源ID
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
