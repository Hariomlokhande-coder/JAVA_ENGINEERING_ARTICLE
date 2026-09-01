package com.technicalblog.controller;

import com.technicalblog.dto.request.ProgressRequest;
import com.technicalblog.dto.response.ProgressResponse;
import com.technicalblog.security.SecurityUtils;
import com.technicalblog.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Everything scoped to the signed in reader. Requires authentication. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ProgressService progressService;

    public MeController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/progress")
    public ResponseEntity<List<ProgressResponse>> myProgress() {
        return ResponseEntity.ok(progressService.findMine(SecurityUtils.currentUserEmail()));
    }

    @PutMapping("/progress/{articleId}")
    public ResponseEntity<ProgressResponse> saveProgress(@PathVariable Long articleId,
                                                         @RequestBody ProgressRequest request) {
        return ResponseEntity.ok(progressService.save(SecurityUtils.currentUserEmail(), articleId, request));
    }
}
