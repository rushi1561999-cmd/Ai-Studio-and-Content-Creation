package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
    );

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path != null && PUBLIC_AUTH_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractEmail(jwt);
            System.out.println("JWT Filter: Processing token for email: " + userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtService.isTokenValid(jwt, userEmail)) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if ("ADMIN".equals(jwtService.extractRole(jwt))) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail, null, authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("JWT Filter: Successfully authenticated user: " + userEmail);
                } else {
                    System.out.println("JWT Filter: Token validation failed for email: " + userEmail);
                }
            }
        } catch (Exception e) {
            System.out.println("JWT Filter: Exception processing token: " + e.getMessage());
            // Invalid or expired token
        }

        filterChain.doFilter(request, response);
    }
}
