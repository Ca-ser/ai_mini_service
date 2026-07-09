package com.waiitz.suji_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * 创建文档请求
 */
@Data
public class CreateDocumentRequest {

    /** 父文档ID，为 null 表示创建在根目录 */
    private UUID parentId;

    @NotBlank(message = "文档标题不能为空")
    @Size(min = 1, max = 255, message = "标题长度需在1-255字符之间")
    private String title;

    @NotBlank(message = "内容格式不能为空")
    private String contentFormat;

}
