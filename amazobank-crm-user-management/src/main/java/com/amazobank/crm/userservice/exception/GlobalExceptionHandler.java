package com.amazobank.crm.userservice.exception;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(RuntimeException ex) {
        log.error("Illegal argument error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() == null ? "An illegal input was received" : ex.getMessage()
        );
        problemDetail.setTitle("Invalid Request Parameters");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected runtime error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later." // User-friendly message
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://docs.aws.amazon.com/general/latest/gr/aws-errors.html")); // Generic AWS error link
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<ProblemDetail> handleSdkClientException(SdkClientException ex) {
        // Log the exception internally for debugging, but do not leak stack trace to client
        log.error("AWS Client Exception: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "A client-side error occurred while communicating with AWS."
        );
        problemDetail.setTitle("AWS Client Error");
        problemDetail.setType(URI.create("https://docs.aws.amazon.com/general/latest/gr/aws-errors.html")); // Generic AWS error link

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @ExceptionHandler(CognitoIdentityProviderException.class)
    public ResponseEntity<ProblemDetail> handleCognitoIdentityProviderException(CognitoIdentityProviderException ex) {
        // Log the exception internally for debugging, but do not leak stack trace to client
        log.error("Cognito Identity Provider Exception: StatusCode: {}, Message: {}", ex.statusCode(), ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.awsErrorDetails().errorMessage()
        );
        problemDetail.setTitle("Cognito Error: " + ex.statusCode());
        // Provide a link to Cognito error codes documentation
        problemDetail.setType(URI.create("https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-error-codes.html"));

        return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.atDebug().setMessage("Received request with type mismatch").addArgument(ex.getMessage()).log();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getLocalizedMessage()
        );
        problemDetail.setTitle("Invalid request parameters");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.atDebug().setMessage("Received request with invalid parameters").addArgument(ex.getMessage()).log();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()),
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid request parameters");
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        if (!fieldErrors.isEmpty()) {
            problemDetail.setProperty("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
}
