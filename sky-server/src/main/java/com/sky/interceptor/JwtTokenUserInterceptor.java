package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        // 2、校验令牌
        try {
            log.info("jwt校验:{}", token);

            // 【修改点1】这里必须使用 UserSecret (用户端密钥) 进行解密，而不是 UserTokenName
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);

            // 【修改点2】这里必须获取 USER_ID，而不是 EMP_ID
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());

            // 3、设置到上下文
            BaseContext.setCurrentId(userId);
            log.info("当前用户id：{}", userId);

            return true;
        } catch (Exception ex) {
            log.error("用户端JWT校验失败", ex); // 建议打印异常日志以便调试
            response.setStatus(401);
            return false;
        }
    }
}
