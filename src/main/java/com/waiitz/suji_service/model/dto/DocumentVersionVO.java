package com.waiitz.suji_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 文档版本视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionVO {

    private UUID id;
    private Integer versionNo;
    private String changeSummary;
    /** 该版本的快照ID，用于获取版本详细内容 */
    private UUID snapshotId;
    /** 创建人ID */
    private UUID createdBy;
    /** 创建时间 */
    private OffsetDateTime createdAt;

}
