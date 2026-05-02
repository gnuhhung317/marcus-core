package io.marcus.api.config;

import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.security.filter.RequestCachingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public RequestCachingFilter requestCachingFilter() {
        return new RequestCachingFilter();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, RequestCachingFilter requestCachingFilter) throws Exception {
        return httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register", "/auth/login", "/auth/refresh", "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/signal/**", "/signals/**", "/api/signals/**", "/api/v1/signals/**").permitAll()
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
                    "/content/research/reports/library", "/api/content/research/reports/library", "/api/v1/content/research/reports/library"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/bots/my-bots", "/api/bots/my-bots", "/api/v1/bots/my-bots").hasRole(Role.DEVELOPER.name())
                .requestMatchers(HttpMethod.POST, "/bots", "/api/bots", "/api/v1/bots", "/bots/register", "/api/bots/register", "/api/v1/bots/register").hasRole(Role.DEVELOPER.name())
                .requestMatchers(HttpMethod.GET, "/subscriptions", "/subscriptions/my-subscriptions", "/api/subscriptions", "/api/subscriptions/my-subscriptions", "/api/v1/subscriptions", "/api/v1/subscriptions/my-subscriptions").hasRole(Role.USER.name())
                .requestMatchers(HttpMethod.POST, "/subscriptions/**", "/api/subscriptions/**", "/api/v1/subscriptions/**").hasRole(Role.USER.name())
                .anyRequest().authenticated())
                .addFilterBefore(requestCachingFilter, SecurityContextHolderFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, SecurityContextHolderFilter.class)
                .build();
    }
}
