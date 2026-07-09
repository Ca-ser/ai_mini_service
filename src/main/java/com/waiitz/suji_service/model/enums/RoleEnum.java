package com.waiitz.suji_service.model.enums;

/**
 * 系统角色枚举
 * ordinal 越小权限越高：OWNER(0) > ADMIN(1) > EDITOR(2) > VIEWER(3) > GUEST(4)
 */
public enum RoleEnum {

    /** 所有者 — 拥有工作空间内所有权限 */
    OWNER,

    /** 管理员 — 管理成员、知识库、发布渠道 */
    ADMIN,

    /** 编辑者 — 创建和编辑文档，使用 AI */
    EDITOR,

    /** 阅读者 — 查看文档和使用只读 AI 问答 */
    VIEWER,

    /** 访客 — 访问被授权的部分文档 */
    GUEST

}
