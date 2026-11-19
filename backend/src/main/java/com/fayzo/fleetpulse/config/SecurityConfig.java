package com.fayzo.fleetpulse.config;

import com.fayzo.fleetpulse.security.JwtAuthFilter;
import com.fayzo.fleetpulse.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtAuthFilter jwtFilter, RateLimitFilter rateLimit, AppProperties props) throws Exception {
        http.csrf(c -> c.disable())   // stateless bearer-token API, no cookies
            .cors(c -> c.configurationSource(cors(props)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.deny()).contentTypeOptions(c -> {}))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/v1/public/**", "/api/v1/auth/**", "/api/v1/registration/**",
                        "/api/v1/admin/auth/login", "/actuator/health", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"Access denied\"}");
                }))
            .addFilterBefore(rateLimit, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource cors(AppProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.cors().allowedOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
