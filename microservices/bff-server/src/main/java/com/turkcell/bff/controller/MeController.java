package com.turkcell.bff.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MeController {

    // /api/** gateway'e proxy edildiğinden bu endpoint /me altında.
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OidcUser oidcUser) {
        String username = oidcUser.getPreferredUsername() != null
                ? oidcUser.getPreferredUsername()
                : oidcUser.getName();
        String email = oidcUser.getEmail() != null ? oidcUser.getEmail() : "";
        return Map.of(
                "authenticated", true,
                "username", username,
                "email", email
        );
    }
}
