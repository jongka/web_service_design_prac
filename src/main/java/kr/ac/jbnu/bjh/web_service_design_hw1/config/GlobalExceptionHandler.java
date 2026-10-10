package kr.ac.jbnu.bjh.web_service_design_hw1.config;

import org.springframework.http.*;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    public record ErrorResponse(int statusCode, String message) {}

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = "요청을 처리할 수 없습니다.";
        if (ex instanceof ErrorResponseException error && error.getBody().getDetail() != null) {
            message = error.getBody().getDetail();
        } else if (statusCode.value() == 400) {
            message = "요청 형식이 올바르지 않습니다. JSON과 ID를 확인하세요.";
        } else if (statusCode.value() == 404) {
            message = "요청한 경로를 찾을 수 없습니다.";
        }
        if (statusCode.is5xxServerError()) message = "서버 처리 중 오류가 발생했습니다.";
        return new ResponseEntity<>(new ErrorResponse(statusCode.value(), message), headers, statusCode);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        logger.error("요청 처리 중 예상하지 못한 오류", ex);
        return ResponseEntity.status(500).body(new ErrorResponse(500, "서버 처리 중 오류가 발생했습니다."));
    }
}