package com.example.LibraryManagementSystem.specification;

import com.example.LibraryManagementSystem.model.Author;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AuthorSpecification {

    public static Specification<Author> filterAuthor(
            String name,
            String email){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicate = new ArrayList<>();

            if (name != null && !name.isEmpty()){
                predicate.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                "%" + name.toLowerCase() + "%"
                        )
                );
            }

            if (email != null && !email.isEmpty()){
                predicate.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("email")),
                                "%" + email.toLowerCase() + "%"
                        )
                );
            }

            return criteriaBuilder.and(predicate.toArray(new Predicate[0]));
        };
    }
}
