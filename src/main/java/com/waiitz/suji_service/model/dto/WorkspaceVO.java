package com.waiitz.suji_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 工作空间视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceVO {

    private UUID id;
    private String name;
    private String description;
    /** 当前用户在该工作空间中的角色 */
    private String role;
    /** 工作空间成员数量 */
    private long memberCount;
    /** 工作空间下的知识库数量 */
    private long kbCount;
    private OffsetDateTime createdAt;

}
