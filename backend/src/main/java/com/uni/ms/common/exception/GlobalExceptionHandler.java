package com.uni.ms.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns exceptions from any module into a consistent RFC 9457-style response.
 * Extends {@link ResponseEntityExceptionHandler} so standard Spring MVC problems
 * (unmapped route -> 404, wrong method -> 405, unreadable body -> 400) keep their
 * correct status instead of being swallowed into a generic 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<ProblemResponse> handleProblem(ProblemException ex,
                                                         HttpServletRequest req) {
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemResponse.of(ex.getStatus().value(),
                        ex.getStatus().getReasonPhrase(), ex.getCode(), ex.getMessage(),
                        req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemResponse> handleAccessDenied(AccessDeniedException ex,
                                                              HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemResponse.of(403, "Forbidden", ErrorCode.ACCESS_DENIED,
                        "You do not have permission to access this resource",
                        req.getRequestURI()));
    }

    /** Keep our field-level validation shape instead of the framework default. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ProblemResponse body = ProblemResponse.validation(path(request), fieldErrors);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getAllValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            String name = parameter == null ? "request" : parameter;
            String message = result.getResolvableErrors().isEmpty()
                    ? "is invalid" : result.getResolvableErrors().get(0).getDefaultMessage();
            fieldErrors.put(name, message);
        });
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "One or more request parameters are invalid", path(request), fieldErrors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.REQUEST_NOT_READABLE,
                "The request body is missing or malformed", path(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "A request parameter has an invalid value", path(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Required request parameter is missing: " + ex.getParameterName(),
                path(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                "The requested resource was not found", path(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED,
                "The HTTP method is not supported for this resource", path(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.REQUEST_NOT_READABLE,
                "The request media type is not supported", path(request), null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> fieldErrors.put(
                violation.getPropertyPath().toString(), violation.getMessage()));
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "One or more request parameters are invalid", request.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleUnexpected(Exception ex,
                                                             HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemResponse.of(500, "Internal Server Error",
                        ErrorCode.INTERNAL_ERROR, "An unexpected error occurred",
                        req.getRequestURI()));
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }

    private ResponseEntity<Object> problem(HttpStatus status, ErrorCode code,
                                           String detail, String instance,
                                           Map<String, String> fieldErrors) {
        ProblemResponse body = fieldErrors == null
                ? ProblemResponse.of(status.value(), status.getReasonPhrase(), code,
                        detail, instance)
                : new ProblemResponse(
                        "https://api.uni.ms/problems/"
                                + code.name().toLowerCase().replace('_', '-'),
                        "Validation failed", status.value(), code.name(), detail,
                        instance, java.time.Instant.now(), fieldErrors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
