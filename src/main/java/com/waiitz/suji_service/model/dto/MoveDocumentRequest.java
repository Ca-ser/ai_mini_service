package com.waiitz.suji_service.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * 移动文档请求
 */
@Data
public class MoveDocumentRequest {

    /** 目标父文档ID，为 null 表示移动到根目录 */
    private UUID targetParentId;

}
