package com.huaxing.controller;

import com.huaxing.config.JwtUtil;
import com.huaxing.dto.LoginRequest;
import com.huaxing.dto.LoginResponse;
import com.huaxing.dto.UserInfo;
import com.huaxing.entity.SysUser;
import com.huaxing.repository.SysUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(SysUserRepository sysUserRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var userOpt = sysUserRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
        }

        SysUser user = userOpt.get();
        if (!user.getEnabled()) {
            return ResponseEntity.status(401).body(Map.of("message", "账户已禁用"));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
        }

        String token = jwtUtil.generateToken(user.getUsername());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> currentUser(@AuthenticationPrincipal SysUser user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未认证"));
        }
        UserInfo info = UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .build();
        return ResponseEntity.ok(info);
    }
}
