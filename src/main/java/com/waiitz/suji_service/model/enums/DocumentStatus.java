package com.waiitz.suji_service.model.enums;

/**
 * 文档状态枚举
 */
public enum DocumentStatus {

    /** 草稿 — 编辑中的文档 */
    DRAFT,

    /** 已发布 — 正式发布的文档 */
    PUBLISHED,

    /** 已归档 — 归档的文档 */
    ARCHIVED,

    /** 已删除 — 软删除的文档 */
    DELETED

}
