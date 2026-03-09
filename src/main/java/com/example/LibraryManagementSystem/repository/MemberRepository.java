package com.example.LibraryManagementSystem.repository;

import com.example.LibraryManagementSystem.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer>,
        JpaSpecificationExecutor<Member> {

    boolean existsByEmail(String email);

    Optional<Member> findByName(String name); //search by name only

    Page<Member> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Member> findByPhone(String phone,Pageable pageable);

    Optional<Member> findByUsersId(Integer id);
}
