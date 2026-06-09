package io.marcus.api.config;

import java.util.List;

import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.api.security.CustomAuthenticationEntryPoint;
import io.marcus.api.security.CustomAccessDeniedHandler;
import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.security.filter.RequestCachingFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public RequestCachingFilter requestCachingFilter(
            @Value("${marcus.security.compressed-request.max-bytes:5242880}") int maxCompressedRequestBytes
    ) {
        return new RequestCachingFilter(maxCompressedRequestBytes);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "https://marcus.tromoi.xyz",
                "http://localhost:3000"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source
                = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, RequestCachingFilter requestCachingFilter) throws Exception {
        return httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register", "/auth/login", "/auth/refresh", "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/signal/**", "/signals/**", "/api/signals/**", "/api/v1/signals").permitAll()
                .requestMatchers(HttpMethod.GET, "/signals", "/api/signals", "/api/v1/signals").permitAll()
                .requestMatchers("/routing/**", "/api/v1/routing/**").permitAll()
                .requestMatchers("/ws/**", "/api/v1/ws/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/bots", "/api/bots", "/api/v1/bots").permitAll()
                .requestMatchers(
                        HttpMethod.GET,
                        "/market/overview", "/api/market/overview", "/api/v1/market/overview",
                        "/academy/courses", "/api/academy/courses", "/api/v1/academy/courses",
                        "/academy/metrics", "/api/academy/metrics", "/api/v1/academy/metrics",
                        "/content/blog/posts", "/api/content/blog/posts", "/api/v1/content/blog/posts",
                        "/content/research/reports", "/api/content/research/reports", "/api/v1/content/research/reports",
                        "/content/research/reports/library", "/api/content/research/reports/library", "/api/v1/content/research/reports/library",
                        "/public/marketing/**", "/api/public/marketing/**", "/api/v1/public/marketing/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/bots/my-bots", "/api/bots/my-bots", "/api/v1/bots/my-bots").hasRole(Role.DEVELOPER.name())
                .requestMatchers(HttpMethod.GET, "/bots/*/analytics/**", "/api/bots/*/analytics/**", "/api/v1/bots/*/analytics/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/bots/*/telemetry/latest", "/api/bots/*/telemetry/latest", "/api/v1/bots/*/telemetry/latest").permitAll()
                .requestMatchers(HttpMethod.POST, "/bots/*/telemetry", "/api/bots/*/telemetry", "/api/v1/bots/*/telemetry").permitAll()
                .requestMatchers(HttpMethod.GET, "/bots/*/dry-run/latest", "/api/bots/*/dry-run/latest", "/api/v1/bots/*/dry-run/latest").permitAll()
                .requestMatchers(HttpMethod.POST, "/bots/*/dry-run/sync", "/api/bots/*/dry-run/sync", "/api/v1/bots/*/dry-run/sync").permitAll()
                .requestMatchers(HttpMethod.POST, "/bots/*/backtest-results", "/api/bots/*/backtest-results", "/api/v1/bots/*/backtest-results").permitAll()
                .requestMatchers(HttpMethod.GET, "/bots/*/integration-health", "/api/bots/*/integration-health", "/api/v1/bots/*/integration-health").hasRole(Role.DEVELOPER.name())
                .requestMatchers(HttpMethod.GET, "/bots/*/credentials", "/api/bots/*/credentials", "/api/v1/bots/*/credentials").hasRole(Role.DEVELOPER.name())
                .requestMatchers(HttpMethod.POST, "/bots", "/api/bots", "/api/v1/bots", "/bots/register", "/api/bots/register", "/api/v1/bots/register").hasRole(Role.DEVELOPER.name())
                .requestMatchers(HttpMethod.GET, "/subscriptions", "/subscriptions/my-subscriptions", "/api/subscriptions", "/api/subscriptions/my-subscriptions", "/api/v1/subscriptions", "/api/v1/subscriptions/my-subscriptions").hasRole(Role.TRADER.name())
                .requestMatchers(HttpMethod.POST, "/subscriptions/**", "/api/subscriptions/**", "/api/v1/subscriptions/**").hasRole(Role.TRADER.name())
                .requestMatchers(HttpMethod.GET, "/subscriptions/*/delivery-summary", "/api/subscriptions/*/delivery-summary", "/api/v1/subscriptions/*/delivery-summary").hasRole(Role.DEVELOPER.name())
                .anyRequest().authenticated())
                .addFilterBefore(requestCachingFilter, SecurityContextHolderFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, SecurityContextHolderFilter.class)
                .build();
    }
}
