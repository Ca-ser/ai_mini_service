package com.waiitz.suji_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 知识库视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseVO {

    private UUID id;
    private UUID workspaceId;
    private String name;
    private String description;
    private String visibility;
    /** 知识库下的文档数量 */
    private long documentCount;
    private OffsetDateTime createdAt;

}
