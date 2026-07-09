package com.waiitz.suji_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工作空间请求
 */
@Data
public class CreateWorkspaceRequest {

    @NotBlank(message = "工作空间名称不能为空")
    @Size(min = 1, max = 100, message = "名称长度需在1-100字符之间")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

}
