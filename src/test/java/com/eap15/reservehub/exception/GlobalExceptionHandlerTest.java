package com.eap15.reservehub.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationExceptions_singleError_returnsFieldMessage() {
        FieldError fieldError = new FieldError("userDTO", "email", "El correo no es válido");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("email", "El correo no es válido");
    }

    @Test
    void handleValidationExceptions_multipleErrors_returnsAllFields() {
        FieldError emailError = new FieldError("userDTO", "email", "El correo no es válido");
        FieldError nameError = new FieldError("userDTO", "firstName", "El nombre es obligatorio");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(emailError, nameError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsKey("email");
        assertThat(response.getBody()).containsKey("firstName");
    }

    @Test
    void rethrowAccessDenied_rethrowsExactException() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        assertThatThrownBy(() -> handler.rethrowAccessDenied(ex))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Forbidden");
    }

    @Test
    void handleIllegalArgument_returns400WithErrorMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Este correo ya esta registrado");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Este correo ya esta registrado");
    }

    @Test
    void handleRuntimeExceptions_returns404WithErrorMessage() {
        RuntimeException ex = new RuntimeException("Usuario no encontrado con ID: 99");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Usuario no encontrado con ID: 99");
    }

    @Test
    void handleIllegalArgument_emptyMessage_returnsNullBody() {
        IllegalArgumentException ex = new IllegalArgumentException((String) null);

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", null);
    }
}
