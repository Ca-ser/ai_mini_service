package com.waiitz.suji_service.controller;

import com.waiitz.suji_service.common.Result;
import com.waiitz.suji_service.model.dto.AssetVO;
import com.waiitz.suji_service.service.AssetService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 附件素材控制器
 * 负责文件上传到工作空间
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    @Resource
    private AssetService assetService;

    /**
     * 上传附件文件到指定工作空间
     * 支持图片、PDF、Office文档、Markdown、压缩包等类型
     * 需要 EDITOR 及以上权限
     */
    @PostMapping("/upload")
    public Result<AssetVO> upload(
            @RequestParam UUID workspaceId,
            @RequestParam MultipartFile file) {
        return Result.success(assetService.upload(workspaceId, file));
    }

}
