package com.example.LibraryManagementSystem.specification;

import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.model.MembershipType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class MemberSpecification {

    public static Specification<Member> filterMembers(
            String name,
            String email,
            String phone,
            MembershipType membershipType){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            //WHERE LOWER(name) LIKE '%value%'
            if (name != null && !name.isEmpty()){
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")), //root for entity, .get("name) for column
                                "%" + name.toLowerCase() + "%"
                        )
                );
            }

            // Filter by email (contains, case-insensitive)
            if(email != null && !email.isEmpty()){
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("email")),
                                "%" + email.toLowerCase() + "%"
                        )
                );
            }

            // Filter by phone (exact match)
            if (phone != null && !phone.isEmpty()){
                predicates.add(
                        criteriaBuilder.equal(root.get("phone"), phone)
                );
            }

            // Filter by membership type
            if (membershipType != null){
                predicates.add(
                        criteriaBuilder.equal(root.get("membershipType"), membershipType)
                );
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            //new Predicate[0] is used to convert a List into a Predicate array required by CriteriaBuilder methods.
        };
    }
}
