package com.waiitz.suji_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 文档详情视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVO {

    private UUID id;
    private UUID kbId;
    private UUID parentId;
    private String title;
    /** ProseMirror 富文本 JSON 内容 */
    private String contentJson;
    /** Markdown 格式内容 */
    private String contentMarkdown;
    /** HTML 格式内容 */
    private String contentHtml;
    private String contentFormat;
    /** 当前版本号 */
    private Integer versionNo;
    private String status;
    private UUID createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
