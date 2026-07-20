package com.uni.ms.academiccatalog.department.api;

import com.uni.ms.academiccatalog.department.application.DepartmentApplicationService;
import com.uni.ms.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Departments", description = "Department lifecycle, search and trash management")
@SecurityRequirement(name = "bearerAuth")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_MANAGER')")
public class DepartmentController {

    private final DepartmentApplicationService departmentService;

    @PostMapping
    @Operation(summary = "Create a Department",
            description = "Codes are normalized to uppercase and remain globally unique.")
    @CommonDepartmentResponses
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody CreateDepartmentRequest request,
            Authentication authentication) {
        DepartmentResponse created = departmentService.create(request, actor(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Search active Departments",
            description = "Excludes soft-deleted rows. Sort format is property,direction.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated Departments"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, page or sort"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public PageResponse<DepartmentResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String faculty,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Allowed fields: departmentName, departmentCode, faculty, "
                    + "status, createdAt, updatedAt", example = "departmentName,asc")
            @RequestParam(defaultValue = "departmentName,asc") String sort) {
        return departmentService.search(search, status, faculty, page, size, sort, false);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active Department by id")
    @CommonDepartmentResponses
    public DepartmentResponse getById(@PathVariable Long id) {
        return departmentService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fully update an active Department",
            description = "The request version must match the current optimistic-lock version.")
    @CommonDepartmentResponses
    public DepartmentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateDepartmentRequest request,
                                     Authentication authentication) {
        return departmentService.update(id, request, actor(authentication));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a Department",
            description = "Sets deletedAt. Repeated deletion returns a stable conflict.")
    @CommonDepartmentResponses
    public ResponseEntity<Void> softDelete(@PathVariable Long id,
                                           Authentication authentication) {
        departmentService.softDelete(id, actor(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    @Operation(summary = "Search soft-deleted Departments",
            description = "Supports the same filters, sorting and pagination as the normal list.")
    @CommonDepartmentResponses
    public PageResponse<DepartmentResponse> trash(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String faculty,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "departmentName,asc") String sort) {
        return departmentService.search(search, status, faculty, page, size, sort, true);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted Department",
            description = "Preserves status and revalidates the globally unique code.")
    @CommonDepartmentResponses
    public DepartmentResponse restore(@PathVariable Long id,
                                      Authentication authentication) {
        return departmentService.restore(id, actor(authentication));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete a Department",
            description = "ADMIN only. The Department must be in trash and have no Course rows.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Permanently deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Department not found"),
            @ApiResponse(responseCode = "409", description = "Not deleted or referenced by Courses")
    })
    public ResponseEntity<Void> permanentlyDelete(@PathVariable Long id,
                                                  Authentication authentication) {
        departmentService.permanentlyDelete(id, actor(authentication));
        return ResponseEntity.noContent().build();
    }

    private String actor(Authentication authentication) {
        return "userId:" + authentication.getName();
    }

    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Department not found"),
            @ApiResponse(responseCode = "409", description = "Lifecycle, code or version conflict"),
            @ApiResponse(responseCode = "429", description = "Authentication rate limited")
    })
    private @interface CommonDepartmentResponses {
    }
}
