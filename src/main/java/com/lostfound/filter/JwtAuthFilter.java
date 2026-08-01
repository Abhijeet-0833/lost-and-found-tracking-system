package com.lostfound.filter;

import com.lostfound.service.AuthService;
import com.lostfound.util.JwtUtil;
import com.lostfound.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Check if the header contains a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the token (removing "Bearer " prefix)
        jwt = authHeader.substring(7);
        
        try {
            userEmail = JwtUtil.extractEmail(jwt);

            // 3. If email is valid and user isn't already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Use the validateSession method we added to AuthService earlier
                User user = authService.validateSession(jwt);

                if (user != null) {
                    if ("BLOCKED".equals(user.getRole())) {
                        throw new RuntimeException("Account is blocked");
                    }
                    // Map the role from DB ('USER' or 'ADMIN') to a Spring Authority
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            Collections.singletonList(authority)
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 4. Update the Security Context - The user is now "Logged In" for this request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // If token is invalid (expired, malformed, "null"), ignore it and leave authentication empty
            System.out.println("Invalid JWT token: " + e.getMessage());
        }
        
        // Continue to the next filter or the Controller
        filterChain.doFilter(request, response);
    }
}
