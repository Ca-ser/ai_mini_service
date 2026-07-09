package com.waiitz.suji_service.controller;

import com.waiitz.suji_service.common.Result;
import com.waiitz.suji_service.model.dto.CreateWorkspaceRequest;
import com.waiitz.suji_service.model.dto.InviteMemberRequest;
import com.waiitz.suji_service.model.dto.MemberVO;
import com.waiitz.suji_service.model.dto.WorkspaceVO;
import com.waiitz.suji_service.service.WorkspaceService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 工作空间控制器
 * 负责工作空间的创建、列表查询、成员邀请
 */
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    @Resource
    private WorkspaceService workspaceService;

    /**
     * 创建工作空间
     * 当前登录用户自动成为该工作空间的 OWNER
     */
    @PostMapping
    public Result<WorkspaceVO> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        return Result.success(workspaceService.create(request));
    }

    /**
     * 获取当前用户加入的所有工作空间列表
     * 返回每个工作空间的基本信息、当前用户角色、成员数量和知识库数量
     */
    @GetMapping
    public Result<List<WorkspaceVO>> listWorkspaces() {
        return Result.success(workspaceService.listWorkspaces());
    }

    /**
     * 邀请成员加入工作空间
     * 仅 OWNER 或 ADMIN 角色可操作
     */
    @PostMapping("/{workspaceId}/members")
    public Result<MemberVO> inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request) {
        return Result.success(workspaceService.inviteMember(workspaceId, request));
    }

}
