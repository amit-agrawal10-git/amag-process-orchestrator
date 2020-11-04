package com.github.amag.processorchestrator.web.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class TaskMvcExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<List> validationErrorHandler(NotFoundException ex){
        List<String> errorsList = new ArrayList<>();
        return new ResponseEntity<>(errorsList, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<List> validationErrorHandler(DataIntegrityViolationException ex){
        List<String> errorsList = new ArrayList<>();
        errorsList.add(ex.getMessage());
        return new ResponseEntity<>(errorsList, HttpStatus.BAD_REQUEST);
    }

}
