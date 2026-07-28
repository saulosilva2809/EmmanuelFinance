package com.emmanuelfinance.auth.user;

import com.emmanuelfinance.auth.user.dto.CreateUserDTO;
import com.emmanuelfinance.auth.user.dto.LoginRequestDTO;
import com.emmanuelfinance.auth.user.dto.ResponseUserDTO;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @PostMapping("/users")
    public ResponseEntity<ResponseUserDTO> create(@Valid @RequestBody CreateUserDTO data) {
        ResponseUserDTO response = userService.create(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO data) {
        String url = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        // montando o x-www-form-urlencoded
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("username", data.email());
        map.add("password", data.password());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url                                                                                            , request, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserSummaryDTO> getUserById(@PathVariable UUID id) {
        UserSummaryDTO user = userService.findById(id);
        return ResponseEntity.ok(user);
    }
}
