package com.emmanuelfinance.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/dummy-url")
class JwtSecurityConfigTest {

    @RestController
    static class PingController {

        @GetMapping("/ping")
        String ping() {
            return "ok";
        }

        @PostMapping("/ping")
        String create() {
            return "created";
        }
    }

    @Configuration
    @EnableWebMvc
    @Import({JwtSecurityConfig.class, PingController.class})
    static class TestConfig {

        // no lugar de chamar o keycloak: só o token "valido" passa
        @Bean
        @Primary
        JwtDecoder fakeJwtDecoder() {
            return token -> {
                if (!"valido".equals(token)) {
                    throw new BadJwtException("token invalido");
                }
                return Jwt.withTokenValue(token).header("alg", "none").subject("usuario")
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
            };
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void shouldAnswer401WhenThereIsNoToken() throws Exception {
        mockMvc.perform(get("/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAnswer401WhenTheTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/ping").header("Authorization", "Bearer qualquer-coisa"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLetAValidTokenIn() throws Exception {
        mockMvc.perform(get("/ping").header("Authorization", "Bearer valido")).andExpect(status().isOk());
    }

    // sem csrf: um post sem token é "não autenticado" (401) e não "csrf inválido" (403), e um post com token passa
    @Test
    void shouldNotAskForCsrfOnPost() throws Exception {
        mockMvc.perform(post("/ping")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/ping").header("Authorization", "Bearer valido")).andExpect(status().isOk());
    }

    @Test
    void shouldNotBlockTheErrorPage() throws Exception {
        int status = mockMvc.perform(get("/error")).andReturn().getResponse().getStatus();

        assertNotEquals(401, status);
    }

    @Test
    void shouldAcceptAnyIssuerWhenNoIssuerIsConfigured() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "none").claims(c -> c.putAll(Map.of("iss", "http://qualquer"))).build();

        assertFalse(JwtSecurityConfig.tokenValidator("").validate(token).hasErrors());
    }

    @Test
    void shouldOnlyAcceptTheConfiguredIssuer() {
        Jwt fromTheRightIssuer = Jwt.withTokenValue("t").header("alg", "none").claims(c -> c.put("iss", "http://keycloak/realms/app")).build();
        Jwt fromAnotherIssuer = Jwt.withTokenValue("t").header("alg", "none").claims(c -> c.put("iss", "http://outro/realms/app")).build();

        OAuth2TokenValidatorResult right = JwtSecurityConfig.tokenValidator("http://keycloak/realms/app").validate(fromTheRightIssuer);
        OAuth2TokenValidatorResult wrong = JwtSecurityConfig.tokenValidator("http://keycloak/realms/app").validate(fromAnotherIssuer);

        assertFalse(right.hasErrors());
        assertTrue(wrong.hasErrors());
    }
}
