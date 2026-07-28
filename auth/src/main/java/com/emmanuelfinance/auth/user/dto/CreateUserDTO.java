package com.emmanuelfinance.auth.user.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserDTO(
        @NotBlank(message = "The first name is required")
        String firstName,

        @NotBlank(message = "The last name is required")
        String lastName,

        @NotBlank(message = "The email is required")
        String email,

        @NotBlank(message = "The password is required")
        String password,

        @NotBlank(message = "The confirm password is required")
        String confirmPassword
) {}
