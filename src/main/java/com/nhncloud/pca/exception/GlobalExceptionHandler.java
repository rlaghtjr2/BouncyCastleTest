package com.nhncloud.pca.exception;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.MethodNotAllowedException;

import com.nhncloud.pca.constant.acme.ProblemType;
import com.nhncloud.pca.model.acme.Problem;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MethodNotAllowedException.class)
    public HttpEntity<Problem> handleMethodNotAllowedException(MethodNotAllowedException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return ResponseEntity.status(ex.getStatusCode())
                .headers(headers)
                .body(new Problem(ProblemType.MALFORMED));
    }
}
