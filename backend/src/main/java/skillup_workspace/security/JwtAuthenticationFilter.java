package skillup_workspace.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;
import skillup_workspace.entity.User;
import skillup_workspace.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_FAILURE_REASON_ATTRIBUTE = "jwtAuthenticationFailureReason";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        if (jwt.isBlank()) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTRIBUTE, "jwt_empty");
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String userEmail = jwtService.extractUsername(jwt);
            if (userEmail == null) {
                request.setAttribute(AUTH_FAILURE_REASON_ATTRIBUTE, "jwt_subject_missing");
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {

                Optional<User> userOptional = userRepository.findByEmail(userEmail);

                if (userOptional.isEmpty()) {
                    request.setAttribute(AUTH_FAILURE_REASON_ATTRIBUTE, "jwt_subject_user_not_found");
                } else {
                    User user = userOptional.get();

                    if (jwtService.isTokenValid(jwt, user)) {
                        String role = normalizeRole(user.getRole());
                        List<SimpleGrantedAuthority> authorities = role.isEmpty()
                                ? List.of()
                                : List.of(new SimpleGrantedAuthority(role));

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user, null, authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        request.setAttribute(AUTH_FAILURE_REASON_ATTRIBUTE, "jwt_validation_failed");
                    }
                }
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTRIBUTE, "jwt_expired");
            SecurityContextHolder.clearContext();
        } catch (JwtException ex) {
            request.setAttribute(AUTH_FAILURE_REASON_ATTRIBUTE, "jwt_invalid");
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return normalizedRole.startsWith("ROLE_")
                ? normalizedRole.substring("ROLE_".length())
                : normalizedRole;
    }
}