package com.emmanuelfinance.auth.user;

import com.emmanuelfinance.auth.keycloak.exceptions.ErrorCreatingKeycloakUser;
import com.emmanuelfinance.auth.user.dto.CreateUserDTO;
import com.emmanuelfinance.auth.user.dto.ResponseUserDTO;
import com.emmanuelfinance.auth.user.exceptions.PasswordsDoNotMatch;
import com.emmanuelfinance.auth.user.exceptions.UserAlreadyExists;
import com.emmanuelfinance.auth.user.exceptions.UserNotFound;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    private ResponseUserDTO userAsDTO(User user) {
        return new ResponseUserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private void verifyPassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new PasswordsDoNotMatch();
        }
    }

    private void verifyUserByEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExists();
        }
    }

    private String createUserInKeycloak(CreateUserDTO data) {
        // configura os dados do usuário
        UserRepresentation kcUser = new UserRepresentation();

        kcUser.setUsername(data.email());
        kcUser.setFirstName(data.firstName());
        kcUser.setLastName(data.lastName());
        kcUser.setEmail(data.email());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        // configura a senha do usuário
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(data.password());
        credential.setTemporary(false);
        kcUser.setCredentials(Collections.singletonList(credential));

        // faz a request
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(kcUser);

        if (response.getStatus() != 201) {
            String errorMessage = response.readEntity(String.class);
            throw new ErrorCreatingKeycloakUser(HttpStatus.valueOf(
                    response.getStatus()),
                    errorMessage
            );
        }

        String path = response.getLocation().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Transactional
    public ResponseUserDTO create(CreateUserDTO data) {
        verifyPassword(data.password(), data.confirmPassword());
        verifyUserByEmail(data.email());

        String keycloakUserId = createUserInKeycloak(data);

        try {
            User newUser = new User();
            newUser.setId(UUID.fromString(keycloakUserId));
            newUser.setFirstName(data.firstName());
            newUser.setLastName(data.lastName());
            newUser.setEmail(data.email());

            User savedUser = userRepository.save(newUser);
            return userAsDTO(savedUser);

        } catch (Exception e) {
            // se der erro deleta o user no keycloak
            keycloak.realm(realm).users().get(keycloakUserId).remove();
            System.out.println("Rollback Keycloak: Usuário removido devido a falha no banco local.");
            throw e;
        }
    }
    
    public UserSummaryDTO findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFound());

        return new UserSummaryDTO(user.getId(), user.getEmail());
    }
}
