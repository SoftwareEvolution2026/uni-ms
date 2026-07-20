package com.uni.ms.common.exception;

import java.time.Instant;
import java.util.Map;

public record ProblemResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    private static final String TYPE_BASE = "https://api.uni.ms/problems/";

    public static ProblemResponse of(int status, String title, ErrorCode code,
                                     String detail, String instance) {
        return new ProblemResponse(
                TYPE_BASE + code.name().toLowerCase().replace('_', '-'),
                title,
                status,
                code.name(),
                detail,
                instance,
                Instant.now(),
                null
        );
    }

    public static ProblemResponse validation(String instance, Map<String, String> fieldErrors) {
        return new ProblemResponse(
                TYPE_BASE + "validation-error",
                "Validation failed",
                400,
                ErrorCode.VALIDATION_ERROR.name(),
                "One or more fields are invalid",
                instance,
                Instant.now(),
                fieldErrors
        );
    }
}
