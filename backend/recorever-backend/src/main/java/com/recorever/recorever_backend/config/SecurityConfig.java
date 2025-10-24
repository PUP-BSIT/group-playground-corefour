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
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF since we're using stateless JWTs
            .csrf(csrf -> csrf.disable())

            // Stateless session; Spring Security won't create session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/login-user", "/api/register-user").permitAll() // public endpoints
                .anyRequest().authenticated() // all other endpoints require JWT
            )

            // Disable form login and HTTP Basic
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())

            // Add our JWT filter before Spring Security's authentication filter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
