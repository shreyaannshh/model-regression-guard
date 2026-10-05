package com.shreyansh.regressionguard.api;

import com.shreyansh.regressionguard.baseline.NothingCapturedException;
import com.shreyansh.regressionguard.store.DuplicateIdException;
import com.shreyansh.regressionguard.store.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns domain errors into standard HTTP error bodies (RFC 9457 problem details). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DuplicateIdException.class)
    public ProblemDetail conflict(DuplicateIdException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** 422: the request was understood, but there was nothing valid to store. The skipped list says why. */
    @ExceptionHandler(NothingCapturedException.class)
    public ProblemDetail nothingCaptured(NothingCapturedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
        problem.setProperty("skipped", e.skipped());
        return problem;
    }
}