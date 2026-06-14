package com.turkcell.bff.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ANGULAR_ORIGIN = "http://localhost:4200";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.authorizeHttpRequests(
                auth -> auth.requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                // Authorization Code flow: token'lar HTTP session içinde sunucuda tutulur,
                // frontend'e yalnızca JSESSIONID cookie'si gider.
                .oauth2Login(oauth2 -> oauth2
                        // Login sonrası her zaman Angular'a yönlendir (saved-request yerine).
                        .defaultSuccessUrl(ANGULAR_ORIGIN, true))
                .logout(logout -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)))
                // JSON isteği yapan Angular'a 302 yerine 401 dön; Angular kendi login
                // yönlendirmesini /oauth2/authorization/keycloak'a yaparak başlatır.
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    String accept = request.getHeader(HttpHeaders.ACCEPT);
                    if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak")
                                .commence(request, response, authException);
                    }
                }))
                .csrf(csrf -> csrf
                        // Frontend'in XSRF-TOKEN cookie'sini okuyup X-XSRF-TOKEN header'ı
                        // olarak geri gönderebilmesi için HttpOnly olmamalı.
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }

    // Logout sonrası Keycloak oturumunu da sonlandırır (RP-Initiated Logout).
    // SPA fetch ile POST /logout çağırdığında 302 yerine 200 + Location header
    // döner; tarayıcının fetch'i Keycloak'a redirect takip edip CORS'a takılmasın
    // diye yönlendirmeyi frontend window.location ile kendisi yapar.
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(
            ClientRegistrationRepository clientRegistrationRepository) {
        var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        // Keycloak'ın logout sonrası Angular'a yönlendirmesi için.
        // Keycloak Admin → bff-client → Valid post-logout redirect URIs'e
        // http://localhost:4200 eklenmesi gerekir.
        handler.setPostLogoutRedirectUri(ANGULAR_ORIGIN);
        handler.setRedirectStrategy((request, response, url) -> {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Location", url);
        });
        return handler;
    }

    // SPA'lar için Spring Security'nin önerdiği CSRF deseni:
    // header ile gelen token'lar BREACH koruması olmadan (düz değer),
    // parametre ile gelenler XOR'lu çözülür.
    static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        private final CsrfTokenRequestAttributeHandler plain = new CsrfTokenRequestAttributeHandler();
        private final XorCsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            this.xor.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                    .resolveCsrfTokenValue(request, csrfToken);
        }
    }

    // Deferred token yüzünden cookie'nin yazılmamasını engellemek için
    // her istekte token'ı çözümleyip XSRF-TOKEN cookie'sinin üretilmesini garanti eder.
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
            csrfToken.getToken();
            filterChain.doFilter(request, response);
        }
    }
}
