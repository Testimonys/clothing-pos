package com.huaxing.controller;

import com.huaxing.dto.CreateUserRequest;
import com.huaxing.dto.UserDTO;
import com.huaxing.entity.SysUser;
import com.huaxing.repository.SysUserRepository;
import com.huaxing.service.BackupService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/setting")
public class SettingController {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final BackupService backupService;

    public SettingController(SysUserRepository sysUserRepository,
                             PasswordEncoder passwordEncoder,
                             BackupService backupService) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.backupService = backupService;
    }

    // ================= 用户管理 =================

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> list() {
        List<UserDTO> users = sysUserRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
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

    @PutMapping("/users/{id}")
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

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return sysUserRepository.findById(id)
                .map(user -> {
                    sysUserRepository.delete(user);
                    return ResponseEntity.ok(Map.of("message", "删除成功"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ================= 数据备份 =================

    @PostMapping("/backup")
    public ResponseEntity<Map<String, Object>> backup() {
        Map<String, Object> result = backupService.backup();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/backup/list")
    public ResponseEntity<List<Map<String, Object>>> listBackups() {
        List<Map<String, Object>> backups = backupService.listBackups();
        return ResponseEntity.ok(backups);
    }

    @GetMapping("/backup/download/{name}")
    public ResponseEntity<Resource> download(@PathVariable String name) {
        Resource resource = backupService.download(name);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
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
