package com.socialcup.barista;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class CafeDeviceAuthenticationFilter extends OncePerRequestFilter {

    public static final String DEVICE_TOKEN_HEADER = "X-Cafe-Device-Token";
    private static final String BARISTA_PATH = "/api/barista/";
    private static final String AUTHENTICATE_PATH =
            "/api/barista/device/authenticate";

    private final CafeDeviceService cafeDeviceService;

    public CafeDeviceAuthenticationFilter(CafeDeviceService cafeDeviceService) {
        this.cafeDeviceService = cafeDeviceService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith(BARISTA_PATH) || AUTHENTICATE_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            AuthenticatedCafeDevice principal = cafeDeviceService.authenticateToken(
                    request.getHeader(DEVICE_TOKEN_HEADER)
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("CAFE_DEVICE"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BaristaApiException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(exception.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"result\":\"FAILURE\",\"reason\":\"DEVICE_UNAUTHORIZED\"}"
            );
        }
    }
}
