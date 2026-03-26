package com.example.LibraryManagementSystem.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final UserDetailsService userDetailsService;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        //token null - skipping authentication
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Try to extract username from token
        try {
            //Extract token
            final String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            //Extracting username from token
            final String username = jwtUtil.extractUsername(token);

            // Authenticate if username exists and no authentication in context
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(request, token, username);
            }

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired for request to {}: ", request.getRequestURI());
          request.setAttribute("jwtError", "JWT token has expired. Please login again.");

        } catch (MalformedJwtException e) {
            // Token structure is invalid
            // Someone might be tampering with tokens
            log.error("Potential Security Violation: Invalid JWT format from IP {}", request.getRemoteAddr());
            request.setAttribute("jwtError", "Invalid JWT token format.");

        } catch (SignatureException e) {
            // Token signature doesn't match
            // Token was modified or signed with wrong key
            log.error("Potential Security Violation: Invalid JWT signature from IP {}", request.getRemoteAddr());
            request.setAttribute("jwtError", "JWT signature verification failed.");

        } catch (UnsupportedJwtException e) {
            // Token format not supported
            log.error("Potential Security Violation: Unsupported JWT token from IP {}", request.getRemoteAddr());
            request.setAttribute("jwtError", "Unsupported JWT token.");

        } catch (IllegalArgumentException e) {
            // Token is null, empty, or whitespace only
            log.error("Potential Security Violation: Invalid JWT token from IP {}", request.getRemoteAddr());
            request.setAttribute("jwtError", "JWT token is invalid.");

        } catch (UsernameNotFoundException e) {
            // User from token doesn't exist in database
            // Maybe user was deleted after token was issued
            log.warn("Authenticated token presented for non-existent user");
            request.setAttribute("jwtError", "User not found.");

        } catch (Exception e) {
            // Unexpected error - catch all
            log.error("System error during JWT authentication");
            request.setAttribute("jwtError", "Authentication failed.");
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(HttpServletRequest request, String token, String username) {
            // Load user from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate token against user details
            if (!jwtUtil.validateToken(token, userDetails)) {
                log.warn("Invalid JWT token for user: {}", username);
                throw new BadCredentialsException("Token validation failed for user: " + username);
            }

            // Create authentication object // successful path
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
            );

            // Set request details
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("JWT authenticated user: {} for path: {}", username, request.getRequestURI());
    }
}
