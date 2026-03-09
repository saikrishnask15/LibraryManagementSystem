package com.example.LibraryManagementSystem.model;

import lombok.Getter;

@Getter
public enum MembershipType {
    BASIC(3, 7, 0.00),
    STANDARD(5, 14, 10.00),
    PREMIUM(10, 30, 20.00);

    private final int maxBooksAllowed;
    private final int borrowPeriodDays;
    private final double monthlyFee;

    MembershipType(int maxBooksAllowed, int borrowPeriodDays, double monthlyFee) {
        this.maxBooksAllowed = maxBooksAllowed;
        this.borrowPeriodDays = borrowPeriodDays;
        this.monthlyFee = monthlyFee;
    }

}