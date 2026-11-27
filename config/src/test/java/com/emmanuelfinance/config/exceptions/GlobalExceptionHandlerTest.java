package com.emmanuelfinance.config.exceptions;

import com.emmanuelfinance.config.exceptions.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    @Test
    void shouldAnswerConflictWhenTheDataChangedInTheMeantime() {
        ResponseEntity<ErrorResponseDTO> response = new GlobalExceptionHandler()
                .handleOptimisticLockingFailureException(
                        new OptimisticLockingFailureException("versão antiga"), mock(HttpServletRequest.class));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
