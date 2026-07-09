package com.waiitz.suji_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 附件素材视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetVO {

    private UUID id;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    /** 文件访问URL */
    private String url;
    private OffsetDateTime createdAt;

}
