package com.waiitz.suji_service.controller;

import com.waiitz.suji_service.common.Result;
import com.waiitz.suji_service.model.dto.CreateDocumentRequest;
import com.waiitz.suji_service.model.dto.DocumentTreeVO;
import com.waiitz.suji_service.model.dto.DocumentVO;
import com.waiitz.suji_service.model.dto.DocumentVersionVO;
import com.waiitz.suji_service.model.dto.MoveDocumentRequest;
import com.waiitz.suji_service.model.dto.SaveDocumentRequest;
import com.waiitz.suji_service.service.DocumentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 文档控制器
 * 负责文档的创建、目录树查询、详情获取、内容保存、删除、移动、版本管理和回滚
 */
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    @Resource
    private DocumentService documentService;

    /**
     * 在指定知识库下创建新文档
     * 自动生成初始空快照和版本记录
     * 需要 EDITOR 及以上权限
     */
    @PostMapping("/knowledge-bases/{kbId}/documents")
    public Result<DocumentVO> createDocument(
            @PathVariable UUID kbId,
            @Valid @RequestBody CreateDocumentRequest request) {
        return Result.success(documentService.create(kbId, request));
    }

    /**
     * 获取知识库的文档目录树
     * 递归返回完整目录结构，只包含非删除状态的文档
     * 需要 VIEWER 及以上权限
     */
    @GetMapping("/knowledge-bases/{kbId}/document-tree")
    public Result<List<DocumentTreeVO>> getDocumentTree(@PathVariable UUID kbId) {
        return Result.success(documentService.getDocumentTree(kbId));
    }

    /**
     * 获取文档详情（含当前快照的完整内容）
     * 需要 VIEWER 及以上权限
     */
    @GetMapping("/documents/{documentId}")
    public Result<DocumentVO> getDocument(@PathVariable UUID documentId) {
        return Result.success(documentService.getDocument(documentId));
    }

    /**
     * 保存文档内容
     * 创建新快照和版本记录，更新 current_snapshot_id
     * 需要 EDITOR 及以上权限
     */
    @PutMapping("/documents/{documentId}/content")
    public Result<DocumentVO> saveDocumentContent(
            @PathVariable UUID documentId,
            @RequestBody SaveDocumentRequest request) {
        return Result.success(documentService.saveContent(documentId, request));
    }

    /**
     * 删除文档（软删除）
     * 将文档及其所有子文档标记为 DELETED 状态
     * 需要 EDITOR 及以上权限
     */
    @DeleteMapping("/documents/{documentId}")
    public Result<Void> deleteDocument(@PathVariable UUID documentId) {
        documentService.deleteDocument(documentId);
        return Result.success();
    }

    /**
     * 移动文档到新父节点
     * targetParentId 为 null 表示移动到根目录
     * 需要 EDITOR 及以上权限
     */
    @PatchMapping("/documents/{documentId}/move")
    public Result<Void> moveDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody MoveDocumentRequest request) {
        documentService.moveDocument(documentId, request);
        return Result.success();
    }

    /**
     * 查询文档的版本历史列表
     * 按版本号倒序排列
     * 需要 VIEWER 及以上权限
     */
    @GetMapping("/documents/{documentId}/versions")
    public Result<List<DocumentVersionVO>> listVersions(@PathVariable UUID documentId) {
        return Result.success(documentService.listVersions(documentId));
    }

    /**
     * 获取指定版本的文档详细内容
     * 需要 VIEWER 及以上权限
     */
    @GetMapping("/documents/{documentId}/versions/{versionNo}")
    public Result<DocumentVO> getVersion(
            @PathVariable UUID documentId,
            @PathVariable Integer versionNo) {
        return Result.success(documentService.getVersion(documentId, versionNo));
    }

    /**
     * 回滚文档到指定版本
     * 基于历史快照创建新的当前快照，保留完整回滚记录
     * 需要 EDITOR 及以上权限
     */
    @PostMapping("/documents/{documentId}/versions/{versionNo}/restore")
    public Result<DocumentVO> restoreVersion(
            @PathVariable UUID documentId,
            @PathVariable Integer versionNo) {
        return Result.success(documentService.restoreVersion(documentId, versionNo));
    }

}
