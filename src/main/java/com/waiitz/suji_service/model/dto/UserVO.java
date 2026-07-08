package com.waiitz.suji_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserVO {

    private UUID id;
    private String username;
    private String email;
    private String avatarUrl;

}
