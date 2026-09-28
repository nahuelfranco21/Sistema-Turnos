package ar.uba.fi.ingsoft1.product_example.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Autowired
    JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            this.authenticateToken(request);
            filterChain.doFilter(request, response);
        } catch (ResponseStatusException e) {
            response.sendError(e.getStatusCode().value());
        }
    }

    private void authenticateToken(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String headerPrefix = "Bearer ";
        if (authHeader == null || !authHeader.startsWith(headerPrefix)) {
            return;
        }
        String token = authHeader.substring(headerPrefix.length());

        jwtService.extractVerifiedUserDetails(token).ifPresent(userDetails -> {
            var authorities = userDetails
                .role()
                .getImpliedRoles()
                .stream()
                .map(r -> new SimpleGrantedAuthority(r.toStringWithPrefix()))
                .collect(Collectors.toList());
            if (userDetails.verified()) {
                authorities.add(new SimpleGrantedAuthority("VERIFIED"));
            }

            var authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                authorities
            );

            authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
        });
    }
}
