package com.riskhub.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskhub.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * API Token 鉴权拦截器
 * 从请求头 Authorization 中读取 Bearer Token，校验合法性。
 */
@Component
public class ApiTokenInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 内置 Token 表：token -> 业务方标识
     * 生产环境应从数据库或配置中心加载
     */
    private static final Map<String, String> TOKEN_REGISTRY = Map.of(
            "tk_im_service_2024", "im-service",
            "tk_comment_service_2024", "comment-service",
            "tk_live_service_2024", "live-service",
            "tk_admin_2024", "admin-console",
            "tk_test_2024", "test-client"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行健康检查和监控端点
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            return true;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            writeError(response, 401, "缺少有效的 Authorization 头");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        String clientId = TOKEN_REGISTRY.get(token);
        if (clientId == null) {
            writeError(response, 403, "无效的 API Token");
            return false;
        }

        // 将客户端标识存入 request 属性，供后续业务使用
        request.setAttribute("clientId", clientId);
        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code == 401 ? 401 : 403);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.fail(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
