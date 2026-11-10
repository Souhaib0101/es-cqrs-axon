package net.montaki.demoescqrsaxon.web;

import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({AggregateNotFoundException.class, NoSuchElementException.class})
    public ProblemDetail notFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Account not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflict(IllegalStateException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ProblemDetail invalidRequest(Exception exception) {
        String detail = "Request body is invalid";
        if (exception instanceof MethodArgumentNotValidException validation) {
            detail = validation.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .reduce((first, next) -> first + "; " + next).orElse(detail);
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler({CompletionException.class, ExecutionException.class})
    public ProblemDetail asynchronousFailure(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof AggregateNotFoundException || cause instanceof NoSuchElementException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Account not found");
        }
        if (cause instanceof IllegalArgumentException argument) {
            return invalidArgument(argument);
        }
        if (cause instanceof IllegalStateException state) {
            return conflict(state);
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Request could not be completed");
    }
}
