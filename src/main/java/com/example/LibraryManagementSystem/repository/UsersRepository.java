package com.example.LibraryManagementSystem.repository;

import com.example.LibraryManagementSystem.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Integer> {
    boolean existsByUsername(String username);

    boolean existsByEmail( String email);

    Optional<Users> findByUsername(String username);
}