package com.socialcup.security;

import com.socialcup.barista.CafeDeviceAuthenticationFilter;
import com.socialcup.user.UserRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public SecurityConfig(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CafeDeviceAuthenticationFilter cafeDeviceAuthenticationFilter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden")))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health",
                                "/api/neighbourhoods",
                                "/api/coffee-preferences",
                                "/api/drinks/*/rating-summary",
                                "/api/cafes",
                                "/api/cafes/**"
                        )
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/stripe")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/barista/device/authenticate"
                        )
                        .permitAll()
                        .requestMatchers("/api/barista/**")
                        .hasAuthority("CAFE_DEVICE")
                        .requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        cafeDeviceAuthenticationFilter,
                        JwtAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
