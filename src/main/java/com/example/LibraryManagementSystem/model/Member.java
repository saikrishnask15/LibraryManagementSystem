package com.example.LibraryManagementSystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "member")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @CreationTimestamp
    @Column(name = "membershipDate", updatable = false)
    private LocalDate membershipDate;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("member")
    private List<BorrowRecord> borrowRecords;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    @Builder.Default
    private MembershipType membershipType = MembershipType.BASIC;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private Users users;

    @PrePersist void onCreate(){
        if (membershipType == null){
            membershipType = MembershipType.BASIC;
        }
    }

    public Integer getMaxBooksAllowed(){
        return membershipType.getMaxBooksAllowed();
    }

    public Integer getBorrowPeriodDays(){
        return membershipType.getBorrowPeriodDays();
    }
}
