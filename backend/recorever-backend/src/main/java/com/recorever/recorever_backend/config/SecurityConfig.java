package com.recorever.recorever_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF since this is a REST API (no forms)
            .csrf(csrf -> csrf.disable())

            // Allow CORS (optional — useful if you have a frontend on a different port)
            .cors(cors -> {})

            // Stateless session — we use JWTs instead of sessions
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Define which endpoints are public and which need authentication
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/login-user",
                    "/api/register-user",
                    "/api/refresh-token"
                ).permitAll() // ✅ Public routes
                .anyRequest().authenticated() // 🔒 Everything else requires JWT
            )

            // Disable default login/logout (we’re using JWT)
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())

            // Add our JWT filter before the built-in username/password filter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
