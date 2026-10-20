package com.smartparking.smart_parking.config;

import com.smartparking.smart_parking.security.BearerTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<BearerTokenFilter> bearerTokenFilterRegistration(
            BearerTokenFilter bearerTokenFilter) {
        FilterRegistrationBean<BearerTokenFilter> registration = new FilterRegistrationBean<>(bearerTokenFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerTokenFilter bearerTokenFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/slots", "/api/slots/available").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/time").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bookings").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/bookings/**").hasRole("ADMIN")
                .requestMatchers("/api/bookings/slot/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/slots/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/slots/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/slots/**").hasRole("ADMIN")
                .requestMatchers("/api/bookings/**", "/api/slots/**").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:8080,http://localhost:8081,https://amanchaurasia081-sketch.github.io}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
