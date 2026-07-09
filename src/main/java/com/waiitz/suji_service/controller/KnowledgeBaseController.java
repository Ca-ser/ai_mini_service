package com.waiitz.suji_service.controller;

import com.waiitz.suji_service.common.Result;
import com.waiitz.suji_service.model.dto.CreateKnowledgeBaseRequest;
import com.waiitz.suji_service.model.dto.KnowledgeBaseVO;
import com.waiitz.suji_service.model.dto.UpdateKnowledgeBaseRequest;
import com.waiitz.suji_service.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 知识库控制器
 * 负责知识库的创建、列表查询、更新、删除
 */
@RestController
@RequestMapping("/api/v1")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 在指定工作空间下创建知识库
     * 需要 EDITOR 及以上权限
     */
    @PostMapping("/workspaces/{workspaceId}/knowledge-bases")
    public Result<KnowledgeBaseVO> createKnowledgeBase(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return Result.success(knowledgeBaseService.create(workspaceId, request));
    }

    /**
     * 查询指定工作空间下的所有知识库列表
     * 包含每个知识库的文档数量统计
     * 需要 VIEWER 及以上权限
     */
    @GetMapping("/workspaces/{workspaceId}/knowledge-bases")
    public Result<List<KnowledgeBaseVO>> listKnowledgeBases(@PathVariable UUID workspaceId) {
        return Result.success(knowledgeBaseService.listByWorkspace(workspaceId));
    }

    /**
     * 更新知识库信息（名称、描述、可见性）
     * 需要 EDITOR 及以上权限
     */
    @PutMapping("/knowledge-bases/{kbId}")
    public Result<KnowledgeBaseVO> updateKnowledgeBase(
            @PathVariable UUID kbId,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        return Result.success(knowledgeBaseService.update(kbId, request));
    }

    /**
     * 删除知识库（软删除）
     * 将知识库及其下属所有文档标记为 DELETED 状态
     * 需要 ADMIN 及以上权限
     */
    @DeleteMapping("/knowledge-bases/{kbId}")
    public Result<Void> deleteKnowledgeBase(@PathVariable UUID kbId) {
        knowledgeBaseService.delete(kbId);
        return Result.success();
    }

}
