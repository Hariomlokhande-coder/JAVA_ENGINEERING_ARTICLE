package com.technicalblog.config;

import com.technicalblog.security.JwtAuthenticationEntryPoint;
import com.technicalblog.security.JwtAuthenticationFilter;
import com.technicalblog.security.RestAccessDeniedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Reads are public, every write requires the ADMIN role.
 * This is the real authorization layer, the Angular guard only improves the user experience.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny())
                            .contentTypeOptions(options -> {
                            })
                            .referrerPolicy(referrer -> referrer
                                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                            .httpStrictTransportSecurity(hsts -> hsts
                                    .includeSubDomains(true)
                                    .maxAgeInSeconds(31536000))
                            // The API serves JSON and uploaded images only, so nothing may execute or embed it.
                            .contentSecurityPolicy(csp -> csp
                                    .policyDirectives("default-src 'none'; img-src 'self'; frame-ancestors 'none'; "
                                            + "base-uri 'none'; form-action 'none'"));
                    headers.permissionsPolicy(permissions -> permissions
                            .policy("camera=(), microphone=(), geolocation=(), payment=()"));
                })
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/verify-email",
                                "/api/auth/resend-verification", "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()
                        // Reader progress: any signed in account, admin rights not required
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers("/uploads/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/manage", "/api/articles/manage/**")
                        .hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/articles/**", "/api/tags/**")
                        .permitAll()
                        .requestMatchers("/api/files/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/articles/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/articles/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/articles/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/articles/**").hasRole(ADMIN)
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 keeps hashing meaningfully expensive for an offline attacker.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
