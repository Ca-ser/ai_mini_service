package com.waiitz.suji_service.service;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.waiitz.suji_service.common.BizException;
import com.waiitz.suji_service.model.dto.LoginRequest;
import com.waiitz.suji_service.model.dto.LoginResponse;
import com.waiitz.suji_service.model.dto.RegisterRequest;
import com.waiitz.suji_service.model.dto.UserVO;
import com.waiitz.suji_service.model.entity.User;
import com.waiitz.suji_service.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserVO register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BizException.conflict("email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw BizException.conflict("username already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        return toUserVO(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BizException.unauthorized("invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BizException.unauthorized("invalid email or password");
        }

        StpUtil.login(user.getId().toString(), SaLoginModel.create().setIsLastingCookie(true));
        String token = StpUtil.getTokenValue();
        long expiresIn = StpUtil.getTokenTimeout();

        return LoginResponse.of(token, expiresIn);
    }

    public UserVO getCurrentUser() {
        String loginId = (String) StpUtil.getLoginId();
        User user = userRepository.findById(UUID.fromString(loginId))
                .orElseThrow(() -> BizException.notFound("user not found"));
        return toUserVO(user);
    }

    private UserVO toUserVO(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getEmail(), user.getAvatarUrl());
    }

}
