package ar.uba.fi.ingsoft1.product_example.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity(debug = false)
public class SecurityConfig {

    public static final String[] PUBLIC_ENDPOINTS = {"/sessions", "/sessions/recover", "/sessions/reset-password", "/sessions/verify", "/sessions/resend-verification"};

    private final JwtAuthFilter authFilter;
    private final String[] corsAllowedOrigins;

    @Autowired
    SecurityConfig(JwtAuthFilter authFilter, @Value("${app.cors.allowed-origins}") String corsAllowedOrigins) {
        this.authFilter = authFilter;
        this.corsAllowedOrigins = corsAllowedOrigins.split(",");
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        .requestMatchers(HttpMethod.GET, "/users").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/password").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/email").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/profesion").authenticated()
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/agenda/**").hasAuthority("VERIFIED")
                        .requestMatchers("/agenda/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/turno", "/turno/cancelar-en-lote").hasAuthority("VERIFIED")
                        .requestMatchers(HttpMethod.PUT, "/turno/**").hasAuthority("VERIFIED")
                        .requestMatchers(HttpMethod.DELETE, "/turno/**").hasAuthority("VERIFIED")
                        .requestMatchers("/turno/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/servicio").hasAuthority("VERIFIED")
                        .requestMatchers(HttpMethod.DELETE, "/servicio/**").hasAuthority("VERIFIED")
                        .requestMatchers("/servicio/**").authenticated()

                        .anyRequest().authenticated())
                .sessionManagement(sessionManager -> sessionManager
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(corsAllowedOrigins));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
