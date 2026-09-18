package com.huaxing.controller;

import com.huaxing.entity.Dealer;
import com.huaxing.service.DealerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** luohuai codeX generate: top-level dealer management and authenticated product-form lookup endpoints. */
@RestController
public class DealerController {
    private final DealerService service;

    public DealerController(DealerService service) { this.service = service; }

    @GetMapping("/api/dealers")
    public ResponseEntity<List<Dealer>> selectable() { return ResponseEntity.ok(service.list(true)); }

    @GetMapping("/api/setting/dealers")
    public ResponseEntity<List<Dealer>> list() { return ResponseEntity.ok(service.list(false)); }

    @PostMapping("/api/setting/dealers")
    public ResponseEntity<Dealer> create(@RequestBody Dealer request) { return ResponseEntity.ok(service.create(request)); }

    @PutMapping("/api/setting/dealers/{id}")
    public ResponseEntity<Dealer> update(@PathVariable Long id, @RequestBody Dealer request) { return ResponseEntity.ok(service.update(id, request)); }

    @DeleteMapping("/api/setting/dealers/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}
