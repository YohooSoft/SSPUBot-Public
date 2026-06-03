package top.mryan2005.sspubot.sspubotbackend.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ThisEmailIsExisted.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ThisEmailIsExisted ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ThisUsernameIsExisted.class)
    public ResponseEntity<Map<String, Object>> handleUserExistedException(
            ThisUsernameIsExisted ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidLoginFormPassword.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPasswordException(
            InvalidLoginFormPassword ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(ThisUserNotFound.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(
            ThisUserNotFound ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidLoginInfo.class)
    public ResponseEntity<Map<String, Object>> handleInvalidLoginInfoException(
            InvalidLoginInfo ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
