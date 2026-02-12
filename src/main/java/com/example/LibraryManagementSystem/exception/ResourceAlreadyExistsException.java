package com.example.LibraryManagementSystem.exception;

public class ResourceAlreadyExistsException extends RuntimeException{

    public ResourceAlreadyExistsException(String message){
        super(message);
    }

    public ResourceAlreadyExistsException(String resourceName, String fieldName,  Object fieldValue){
        super(String.format("%s already exists with %s: '%s'",resourceName, fieldName, fieldValue));
    }
}
