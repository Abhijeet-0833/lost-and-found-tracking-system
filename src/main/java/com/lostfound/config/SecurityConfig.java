package com.lostfound.config;


import com.lostfound.filter.JwtAuthFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter authFilter;

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF since we are using JWT tokens
            .csrf(csrf -> csrf.disable())
            
            // 2. Configure CORS so your React frontend can talk to this backend
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of(
                        "http://localhost:3000", 
                        "http://127.0.0.1:5500", "http://localhost:5500", 
                        "http://127.0.0.1:5501", "http://localhost:5501"
                )); // React default port & Live Server ports
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                return config;
            }))

            // 3. Set the Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Allow everyone to access the login/otp endpoints, static resources, and uploads
                .requestMatchers("/api/auth/**", "/", "/*.html", "/*.css", "/*.js", "/error", "/uploads/**").permitAll()
                // Only users with 'ADMIN' role can touch admin endpoints
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN") 
                // All other requests (reporting items, etc.) require a valid JWT
                .anyRequest().authenticated() 
            )

            // 4. Make the session stateless (No cookies, only JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 5. Plug in our custom providers and filters
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

