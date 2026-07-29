package com.emmanuelfinance.auth;

import com.emmanuelfinance.auth.keycloak.exceptions.ErrorCreatingKeycloakUser;
import com.emmanuelfinance.auth.user.User;
import com.emmanuelfinance.auth.user.UserRepository;
import com.emmanuelfinance.auth.user.UserService;
import com.emmanuelfinance.auth.user.dto.CreateUserDTO;
import com.emmanuelfinance.auth.user.dto.ResponseUserDTO;
import com.emmanuelfinance.auth.user.exceptions.PasswordsDoNotMatch;
import com.emmanuelfinance.auth.user.exceptions.UserAlreadyExists;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private Response response;

    private final String realmName = "emmanuelfinance";

    @BeforeEach
    void setUp() {
        lenient().when(keycloak.realm(any())).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
        lenient().when(usersResource.get(anyString())).thenReturn(userResource);
    }

    @Test
    void shouldThrowExceptionWhenPasswordsDoNotMatch() {
        CreateUserDTO userDTO = new CreateUserDTO(
                "Saulo",
                "Emmanuel",
                "sauloteste@gmail.com",
                "Camila",
                "Camila017."
        );

        assertThrows(PasswordsDoNotMatch.class, () -> {
            userService.create(userDTO);
        });

        verifyNoInteractions(keycloak);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        CreateUserDTO userDTO = new CreateUserDTO(
                "Saulo",
                "Emmanuel",
                "sauloteste@gmail.com",
                "Camila017.",
                "Camila017."
        );
        String userId = UUID.randomUUID().toString();

        when(userRepository.existsByEmail(userDTO.email())).thenReturn(true);

        assertThrows(UserAlreadyExists.class, () -> {
            userService.create(userDTO);
        });

        verifyNoInteractions(keycloak);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldCreateUserInKeycloakAndLocalDatabaseSuccessfully() {
        CreateUserDTO userDTO = new CreateUserDTO(
                "Saulo",
                "Emmanuel",
                "sauloteste@gmail.com",
                "Camila017.",
                "Camila017."
        );
        String userId = UUID.randomUUID().toString();

        when(userRepository.existsByEmail(userDTO.email())).thenReturn(false);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://localhost:8080/admin/realms/realm/users/" + userId));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseUserDTO result = userService.create(userDTO);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());

        verify(usersResource, times(1)).create(any(UserRepresentation.class));
        verify(userRepository, times(1)).save(any(User.class));
        verify(usersResource, never()).get(any()); // garante não ter feito rollback
    }

    @Test
    void shouldThrowExceptionWhenKeycloakReturnsErrorStatus() {
        CreateUserDTO userDTO = new CreateUserDTO(
                "Saulo",
                "Emmanuel",
                "sauloteste@gmail.com",
                "Camila017.",
                "Camila017."
        );

        when(userRepository.existsByEmail(userDTO.email())).thenReturn(false);

        // simula um erro 400 no keycloak
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn("Bad Request: invalid data");

        assertThrows(ErrorCreatingKeycloakUser.class, () -> {
            userService.create(userDTO);
        });

        verify(usersResource, times(1)).create(any(UserRepresentation.class));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRollbackKeycloakUserWhenDatabaseFails() {
        CreateUserDTO userDTO = new CreateUserDTO(
                "Saulo",
                "Emmanuel",
                "sauloteste@gmail.com",
                "Camila017.",
                "Camila017."
        );
        String userId = UUID.randomUUID().toString();

        when(userRepository.existsByEmail(userDTO.email())).thenReturn(false);

        // keycloak aceita a criação
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://localhost:8080/admin/realms/realm/users/" + userId));

        // banco de dados local lança erro ao salvar
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Erro de conexão com o banco local"));

        assertThrows(RuntimeException.class, () -> {
            userService.create(userDTO);
        });

        // garante que removou o usuário do keycloak
        verify(usersResource, times(1)).get(userId);
        verify(userResource, times(1)).remove();
    }
}
