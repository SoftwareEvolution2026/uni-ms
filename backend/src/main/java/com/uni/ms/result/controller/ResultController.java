package com.uni.ms.result.controller;

import com.uni.ms.result.dto.CreateResultRequest;
import com.uni.ms.result.dto.ResultResponse;
import com.uni.ms.result.dto.UpdateResultRequest;
import com.uni.ms.result.service.ResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public List<ResultResponse> list() {
        return resultService.listAll();
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public List<ResultResponse> listByStudent(@PathVariable Long studentId) {
        return resultService.listByStudent(studentId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ResultResponse getById(@PathVariable Long id) {
        return resultService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ResponseEntity<ResultResponse> create(
            @Valid @RequestBody CreateResultRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ResultResponse created = resultService.create(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ResultResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateResultRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return resultService.update(id, request, principal.getUsername());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        resultService.delete(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
