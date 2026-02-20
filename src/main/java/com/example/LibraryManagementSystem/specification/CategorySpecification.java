package com.example.LibraryManagementSystem.specification;

import com.example.LibraryManagementSystem.model.Category;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CategorySpecification {

    public static Specification<Category> filterCategories(
            String name,
            List<Integer> bookIds
    ){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(name != null && !name.isEmpty()){
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                "%" + name.toLowerCase() + "%"
                        )
                );
            }

            if(bookIds != null && !bookIds.isEmpty()){
                predicates.add(
                                root.join("books").get("id").in(bookIds)
                );
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
