package com.weeklyreport.common.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.weeklyreport.auth.exception.EmailAlreadyExistsException;
import com.weeklyreport.auth.exception.InvalidRefreshTokenException;
import com.weeklyreport.auth.exception.LoginRateLimitExceededException;
import com.weeklyreport.user.exception.InvalidInvitationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LoginRateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleLoginRateLimit(LoginRateLimitExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
        problem.setTitle("Too many login attempts");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(
            EmailAlreadyExistsException exception
    ) {

        ProblemDetail problem
                = ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle("Email already registered");

        return problem;
    }

    @ExceptionHandler({
        BadCredentialsException.class,
        DisabledException.class
    })
    public ProblemDetail handleAuthenticationFailure(
            RuntimeException exception
    ) {

        ProblemDetail problem
                = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                );

        problem.setTitle(
                "Authentication failed"
        );

        return problem;
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(
            InvalidRefreshTokenException exception
    ) {

        ProblemDetail problem
                = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid or expired refresh token"
                );

        problem.setTitle(
                "Authentication failed"
        );

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception
    ) {

        Map<String, String> errors
                = exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        error -> error.getField(),
                                        error -> error.getDefaultMessage(),
                                        (first, second) -> first
                                )
                        );

        ProblemDetail problem
                = ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Request validation failed"
                );

        problem.setTitle("Validation failed");

        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(
            ResourceNotFoundException exception
    ) {

        ProblemDetail problem
                = ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Resource not found");

        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(
            ConflictException exception
    ) {

        ProblemDetail problem
                = ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle("Conflict");

        return problem;
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(
            BadRequestException exception
    ) {

        ProblemDetail problem
                = ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        problem.setTitle("Invalid request");

        return problem;
    }

    @ExceptionHandler(InvalidInvitationException.class)
    public ProblemDetail handleInvalidInvitation(InvalidInvitationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid or expired invitation"
        );
        problem.setTitle("Invitation cannot be accepted");
        return problem;
    }
}
