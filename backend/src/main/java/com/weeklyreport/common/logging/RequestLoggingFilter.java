package com.weeklyreport.common.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        boolean failed = false;
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            try {
                filterChain.doFilter(request, response);
            } catch (ServletException | IOException | RuntimeException exception) {
                failed = true;
                throw exception;
            } finally {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                int status = failed && response.getStatus() < 400
                        ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                        : response.getStatus();
                if (status >= 500) {
                    LOGGER.warn(
                            "http_request method={} path={} status={} durationMs={} requestId={}",
                            request.getMethod(), request.getRequestURI(), status, durationMs, requestId
                    );
                } else {
                    LOGGER.info(
                            "http_request method={} path={} status={} durationMs={} requestId={}",
                            request.getMethod(), request.getRequestURI(), status, durationMs, requestId
                    );
                }
            }
        }
    }
}
