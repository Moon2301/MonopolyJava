package com.game.monopoly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // Mã hóa mật khẩu
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(

                                // AUTH API
                                "/api/auth/**",

                                // LOGIN / REGISTER PAGES
                                "/login",
                                "/register",
                                "/auth/**",

                                // MAIN GAME UI PAGES
                                "/home",
                                "/home/**",
                                "/private-table",
                                "/game-board",
                                "/map-editor",
                                "/shop",

                                // ADMIN PAGE
                                "/admin",

                                // STATIC FILES
                                "/css/**",
                                "/js/**",
                                "/images/**"

                        ).permitAll()

                        // ADMIN API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ALL OTHER API REQUIRE AUTH
                        .anyRequest().authenticated()
                )

                // JWT FILTER
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
