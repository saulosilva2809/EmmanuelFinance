package com.emmanuelfinance.auth.keycloak.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class ErrorCreatingKeycloakUser extends APIException {

    public ErrorCreatingKeycloakUser(HttpStatus status, String rawMessage) {
        super(status, cleanMessage(rawMessage));
    }

    private static String cleanMessage(String rawJson) {
        if (rawJson == null || !rawJson.contains("errorMessage")) {
            return rawJson;
        }
        try {
            int start = rawJson.indexOf("\"errorMessage\":\"") + 16;
            int end = rawJson.indexOf("\"", start);
            return rawJson.substring(start, end);
        } catch (Exception e) {
            return rawJson;
        }
    }
}