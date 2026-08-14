package com.rishabh.microservices.auth.exception;

import com.rishabh.microservices.auth.dto.MessageResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

// Translates application exceptions and validation errors into HTTP status codes and uniform JSON responses.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IdentityNotFoundException.class, OtpNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public MessageResponseDto handleNotFoundException(RuntimeException ex) {
        return MessageResponseDto.builder().message(ex.getMessage()).build();
    }

    @ExceptionHandler({
            OtpExpiredException.class,
            OtpInvalidException.class,
            PasswordMismatchException.class,
            PasswordNotConfiguredException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleBadRequestException(RuntimeException ex) {
        return MessageResponseDto.builder().message(ex.getMessage()).build();
    }

    @ExceptionHandler({
            IdentityAlreadyExistsException.class,
            IdentityAlreadyVerifiedException.class,
            PasswordAlreadySetException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public MessageResponseDto handleConflictException(RuntimeException ex) {
        return MessageResponseDto.builder().message(ex.getMessage()).build();
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public MessageResponseDto handleForbiddenException(EmailNotVerifiedException ex) {
        return MessageResponseDto.builder().message(ex.getMessage()).build();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public MessageResponseDto handleUnauthorizedException(InvalidCredentialsException ex) {
        return MessageResponseDto.builder().message(ex.getMessage()).build();
    }

    @ExceptionHandler(EmailSendException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public MessageResponseDto handleEmailSendException(EmailSendException ex) {
        return MessageResponseDto.builder().message(ex.getMessage()).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Validation failed.");
        return MessageResponseDto.builder().message(errorMessage).build();
    }
}
