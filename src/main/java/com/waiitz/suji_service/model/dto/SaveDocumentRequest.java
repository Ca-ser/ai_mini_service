package com.waiitz.suji_service.model.dto;

import lombok.Data;

/**
 * 保存文档内容请求
 * 同时传入三种格式的内容：JSON（富文本原始格式）、Markdown、HTML
 */
@Data
public class SaveDocumentRequest {

    /** 文档标题（可更新） */
    private String title;

    /** ProseMirror 富文本 JSON 内容 */
    private String contentJson;

    /** Markdown 格式内容 */
    private String contentMarkdown;

    /** HTML 格式内容 */
    private String contentHtml;

    /** 本次变更说明 */
    private String changeSummary;

}
