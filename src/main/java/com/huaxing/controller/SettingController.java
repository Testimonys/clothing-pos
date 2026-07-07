package com.huaxing.controller;

import com.huaxing.dto.CreateUserRequest;
import com.huaxing.dto.UserDTO;
import com.huaxing.entity.SysUser;
import com.huaxing.repository.SysUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/setting/users")
public class SettingController {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingController(SysUserRepository sysUserRepository,
                             PasswordEncoder passwordEncoder) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> list() {
        List<UserDTO> users = sysUserRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateUserRequest request) {
        if (sysUserRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }

        SysUser user = SysUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role(request.getRole())
                .build();
        user = sysUserRepository.save(user);
        return ResponseEntity.ok(toDTO(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CreateUserRequest request) {
        return sysUserRepository.findById(id)
                .map(user -> {
                    user.setUsername(request.getUsername());
                    if (request.getPassword() != null && !request.getPassword().isBlank()) {
                        user.setPassword(passwordEncoder.encode(request.getPassword()));
                    }
                    user.setDisplayName(request.getDisplayName());
                    user.setRole(request.getRole());
                    sysUserRepository.save(user);
                    return ResponseEntity.ok(toDTO(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return sysUserRepository.findById(id)
                .map(user -> {
                    sysUserRepository.delete(user);
                    return ResponseEntity.ok(Map.of("message", "删除成功"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private UserDTO toDTO(SysUser user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .createTime(user.getCreateTime())
                .build();
    }
}
