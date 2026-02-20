package com.example.LibraryManagementSystem.repository;

import com.example.LibraryManagementSystem.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Integer> ,
        JpaSpecificationExecutor<Author> {
    Optional<Author> findByEmail(String email);


}
