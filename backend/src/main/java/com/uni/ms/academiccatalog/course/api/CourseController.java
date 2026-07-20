package com.uni.ms.academiccatalog.course.api;

import com.uni.ms.academiccatalog.course.application.CourseApplicationService;
import com.uni.ms.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Validated
@Tag(name = "Courses", description = "Course lifecycle, search and trash management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_MANAGER')")
public class CourseController {

    private final CourseApplicationService courseService;

    @PostMapping
    @Operation(summary = "Create a Course")
    @CommonCourseResponses
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication) {
        CourseResponse created = courseService.create(request, actor(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Search active Courses",
            description = "Excludes soft-deleted rows. Sort format is property,direction.")
    @CommonCourseResponses
    public PageResponse<CourseResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Allowed fields: courseName, courseCode, creditUnits, "
                    + "semester, academicYear, status, createdAt, updatedAt",
                    example = "courseName,asc")
            @RequestParam(defaultValue = "courseName,asc") String sort) {
        return courseService.search(search, departmentId, semester, academicYear,
                status, page, size, sort, false);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active Course by id")
    @CommonCourseResponses
    public CourseResponse getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fully update an active Course",
            description = "The request version must match the current optimistic-lock version.")
    @CommonCourseResponses
    public CourseResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateCourseRequest request,
                                 Authentication authentication) {
        return courseService.update(id, request, actor(authentication));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a Course")
    @CommonCourseResponses
    public ResponseEntity<Void> softDelete(@PathVariable Long id,
                                           Authentication authentication) {
        courseService.softDelete(id, actor(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    @Operation(summary = "Search soft-deleted Courses")
    @CommonCourseResponses
    public PageResponse<CourseResponse> trash(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "courseName,asc") String sort) {
        return courseService.search(search, departmentId, semester, academicYear,
                status, page, size, sort, true);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted Course",
            description = "The linked Department must be active and not deleted.")
    @CommonCourseResponses
    public CourseResponse restore(@PathVariable Long id,
                                  Authentication authentication) {
        return courseService.restore(id, actor(authentication));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete a Course",
            description = "ADMIN only. The Course must already be in trash.")
    @CommonCourseResponses
    public ResponseEntity<Void> permanentlyDelete(@PathVariable Long id,
                                                  Authentication authentication) {
        courseService.permanentlyDelete(id, actor(authentication));
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
            @ApiResponse(responseCode = "400", description = "Validation or filter error"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "409", description = "Lifecycle, code or version conflict")
    })
    private @interface CommonCourseResponses {
    }
}
