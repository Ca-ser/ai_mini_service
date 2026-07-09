package com.waiitz.suji_service.service;

import com.waiitz.suji_service.common.BizException;
import com.waiitz.suji_service.model.dto.AssetVO;
import com.waiitz.suji_service.model.entity.Asset;
import com.waiitz.suji_service.model.entity.AuditLog;
import com.waiitz.suji_service.model.enums.RoleEnum;
import com.waiitz.suji_service.repository.AssetRepository;
import com.waiitz.suji_service.repository.AuditLogRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 附件素材服务
 * 负责文件上传、存储、MIME 校验
 * 第一阶段使用本地磁盘存储，后续可迁移至对象存储（MinIO / S3）
 */
@Service
public class AssetService {

    @Resource
    private AssetRepository assetRepository;

    @Resource
    private AuditLogRepository auditLogRepository;

    @Resource
    private PermissionService permissionService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /** 允许上传的文件 MIME 类型白名单 */
    private static final String[] ALLOWED_MIME_TYPES = {
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp",
            "image/svg+xml", "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/markdown", "text/plain",
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed"
    };

    /** 允许的最大文件大小：50MB */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * 上传附件文件
     * 校验 MIME 类型和文件大小后，存储到本地磁盘，记录元数据到数据库
     *
     * @param workspaceId 所属工作空间ID
     * @param file        上传的文件
     * @return 附件视图对象
     */
    @Transactional
    public AssetVO upload(UUID workspaceId, MultipartFile file) {
        UUID userId = permissionService.getCurrentUserId();

        // 校验 EDITOR 及以上权限
        permissionService.checkWorkspaceAccess(workspaceId, RoleEnum.EDITOR, RoleEnum.ADMIN, RoleEnum.OWNER);

        // 校验文件是否为空
        if (file.isEmpty()) {
            throw BizException.badRequest("上传文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.badRequest("文件大小不能超过 50MB");
        }

        // 校验 MIME 类型
        String mimeType = file.getContentType();
        if (!isAllowedMimeType(mimeType)) {
            throw BizException.badRequest("不支持的文件类型：" + mimeType);
        }

        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            // 生成存储文件名：UUID + 原始扩展名
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String storedFileName = UUID.randomUUID().toString() + extension;

            // 写入磁盘
            Path filePath = uploadPath.resolve(storedFileName);
            file.transferTo(filePath.toFile());

            // 计算文件哈希（用于去重校验）
            String checksum = calculateChecksum(filePath);

            // 创建 Asset 实体记录
            Asset asset = new Asset();
            asset.setWorkspaceId(workspaceId);
            asset.setUploaderId(userId);
            asset.setFileName(originalFileName != null ? originalFileName : storedFileName);
            asset.setFileSize(file.getSize());
            asset.setMimeType(mimeType);
            asset.setStorageType("LOCAL");
            asset.setStoragePath(storedFileName);
            asset.setChecksum(checksum);
            asset.setCreatedAt(OffsetDateTime.now());
            assetRepository.save(asset);

            // 写入审计日志
            writeAuditLog(userId, workspaceId, "UPLOAD_ASSET", "ASSET", asset.getId());

            // 构建返回对象
            AssetVO vo = new AssetVO();
            vo.setId(asset.getId());
            vo.setFileName(asset.getFileName());
            vo.setFileSize(asset.getFileSize());
            vo.setMimeType(asset.getMimeType());
            vo.setCreatedAt(asset.getCreatedAt());
            return vo;

        } catch (IOException e) {
            throw new BizException(500001, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 校验文件 MIME 类型是否在白名单中
     */
    private boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (allowed.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算文件 SHA-256 哈希值（hex 字符串）
     */
    private String calculateChecksum(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            return "";
        }
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
