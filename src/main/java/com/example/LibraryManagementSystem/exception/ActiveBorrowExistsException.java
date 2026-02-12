package com.example.LibraryManagementSystem.exception;

public class ActiveBorrowExistsException extends RuntimeException{
    public ActiveBorrowExistsException(String message){
        super(message);
    }
}
