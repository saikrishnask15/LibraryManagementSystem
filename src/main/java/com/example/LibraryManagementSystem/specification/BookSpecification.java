package com.example.LibraryManagementSystem.specification;

import com.example.LibraryManagementSystem.model.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BookSpecification {

    public static Specification<Book> filterBooks(
            String title,
            String isBn,
            String authorName,
            Boolean available,
            Integer minYear,
            Integer maxYear,
            List<Integer> categoryIds,
            Integer minCopies,
            Integer maxCopies){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(title != null && !title.isEmpty()){
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("title")),
                                "%" + title.toLowerCase() + "%"
                        )
                );
            }

            if (isBn != null && !isBn.isEmpty()){
                predicates.add(
                        criteriaBuilder.equal(root.get("isBn"), isBn)
                );
            }
            if (authorName != null){
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.join("author").get("name")
                                ),
                                "%" + authorName.toLowerCase() + "%"
                        )
                );
            }

            if (available != null){
                predicates.add(
                        criteriaBuilder.equal(root.get("available"), available)
                );
            }

            if (minYear != null){
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("publishedYear"), minYear));
            }
            if (maxYear != null){
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("publishedYear"), maxYear));
            }

            if (minCopies != null){
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("availableCopies"), minCopies));
            }
            if (maxCopies != null){
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("availableCopies"), maxCopies));
            }

            if (categoryIds != null && !categoryIds.isEmpty()){
                predicates.add(
                        root.join("categories").get("id").in(categoryIds)
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
