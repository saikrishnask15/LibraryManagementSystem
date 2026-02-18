package com.example.LibraryManagementSystem.exception;

public class BorrowLimitExceededException extends RuntimeException{
    public BorrowLimitExceededException(String message){
        super(message);
    }
    public BorrowLimitExceededException(Integer allowed, String member){
        super(String.format("Member has reached the maximum borrowing limit of %d books for %s membership", allowed, member));
    }

}
