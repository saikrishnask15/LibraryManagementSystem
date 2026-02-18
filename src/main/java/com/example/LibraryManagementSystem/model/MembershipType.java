package com.example.LibraryManagementSystem.model;

import lombok.Getter;

@Getter
public enum MembershipType {
    BASIC(3, 7),
    STANDARD(5, 14),
    PREMIUM(10, 30);

    private final int maxBooksAllowed;
    private final int borrowPeriodDays;

    MembershipType(int maxBooksAllowed, int borrowPeriodDays) {
        this.maxBooksAllowed = maxBooksAllowed;
        this.borrowPeriodDays = borrowPeriodDays;
    }

}
