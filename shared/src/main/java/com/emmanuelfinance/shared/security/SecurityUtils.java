package com.emmanuelfinance.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto de segurança.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            try {
                return UUID.fromString(subject);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("O subject do token JWT não é um UUID válido: " + subject);
            }
        }

        throw new IllegalStateException("O principal no contexto de segurança não é um JWT válido. Tipo encontrado: "
                + (principal != null ? principal.getClass().getName() : "null"));
    }
}