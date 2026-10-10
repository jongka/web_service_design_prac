package kr.ac.jbnu.bjh.web_service_design_hw1.config;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        System.out.println("[LoggingInterceptor] 요청 시작: " + method + " " + uri +
                (query != null ? "?" + query : ""));
        // 과제 시연용: 이 헤더가 없으면 정상 처리합니다.
        String demoError = request.getHeader("X-Demo-Error");
        if ("500".equals(demoError)) throw new IllegalStateException("실습용 내부 오류");
        if ("503".equals(demoError)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "실습용 서비스 일시 중단");
        return true; // false로 하면 컨트롤러로 안 넘어감
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        System.out.println("[LoggingInterceptor] 요청 완료, status=" + response.getStatus());
    }
}