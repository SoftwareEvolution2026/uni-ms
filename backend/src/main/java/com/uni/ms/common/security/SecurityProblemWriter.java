package com.uni.ms.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ms.common.exception.ErrorCode;
import com.uni.ms.common.exception.ProblemResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityProblemWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int status, String title,
                      ErrorCode code, String detail, String instance) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ProblemResponse.of(status, title, code, detail, instance));
    }
}
