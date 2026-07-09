package com.waiitz.suji_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 工作空间成员视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberVO {

    private UUID id;
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private OffsetDateTime createdAt;

}
