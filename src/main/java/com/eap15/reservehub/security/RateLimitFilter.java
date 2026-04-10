package com.eap15.reservehub.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Identificaremos al usuario por su principal o la ip si es anónimo
        String userId = "anonymous-" + request.getRemoteAddr();
        String role = "ANONYMOUS";

        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            userId = userDetails.getUser().getId().toString();
            // Obtenemos el rol principal
            role = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("CLIENTE")
                    .replace("ROLE_", "");
        }

        // Si es ADMIN, no hay límite
        if ("ADMINISTRADOR".equals(role)) {
            filterChain.doFilter(request, response);
            return;
        }
        // Para usar en la función lambda, la variable debe ser effectively final
        final String currentRole = role;
        Bucket bucket = buckets.computeIfAbsent(userId, key -> createNewBucket(currentRole));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too Many Requests");
        }
    }

    private Bucket createNewBucket(String role) {
        int limit = 100; // Por defecto para ANONYMOUS y CLIENTE
        if ("PROVEEDOR".equals(role)) {
            limit = 300;
        }
        
        Bandwidth limitBandwidth = Bandwidth.classic(limit, Refill.greedy(limit, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limitBandwidth).build();
    }
}
