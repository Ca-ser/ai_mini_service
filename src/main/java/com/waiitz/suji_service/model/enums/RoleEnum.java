package com.waiitz.suji_service.model.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {

    OWNER(100),
    ADMIN(80),
    EDITOR(60),
    VIEWER(40),
    GUEST(20);

    private final int level;

    RoleEnum(int level) {
        this.level = level;
    }

    public boolean canManage(RoleEnum target) {
        return this.level > target.level;
    }

    public static RoleEnum min(RoleEnum a, RoleEnum b) {
        return a.level <= b.level ? a : b;
    }
}
