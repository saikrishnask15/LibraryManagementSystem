package com.example.LibraryManagementSystem.dto.mapper;

import com.example.LibraryManagementSystem.dto.memberDTO.MemberRequest;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberResponse;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.model.MembershipType;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemberMapper {

    public Member toEntity(MemberRequest memberRequest){
        return Member.builder()
                .name(memberRequest.getName())
                .email(memberRequest.getEmail())
                .phone(memberRequest.getPhone())
                .membershipType(memberRequest.getMembershipType()!=null ? memberRequest.getMembershipType() : MembershipType.BASIC)
                .build();
    }

    public void updateRequestToEntity(Member existingMember, MemberRequest request){
        if(request.getName() != null){
            existingMember.setName(request.getName());
        }
        if(request.getPhone() != null){
            existingMember.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            existingMember.setEmail(request.getEmail());
        }
        if (request.getMembershipType() != null) {
            existingMember.setMembershipType(request.getMembershipType());
        }
    }

    public MemberResponse toResponse(Member member){
        if (member == null) {
            return null;
        }
        return MemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .membershipDate(member.getMembershipDate())
                .membershipType(member.getMembershipType())
                .maxBooksAllowed(member.getMaxBooksAllowed())
                .borrowPeriodDays(member.getBorrowPeriodDays())
                .activeBorrowCount(calculateActiveBorrowCount(member))
                .build();
    }
    //helper method
    //private Integer extractActiveBorrowCount(Integer id){
    //return borrowRecordRepository.countByMemberIdAndIsArchivedFalse(id);
    //}
    //Calculate from loaded borrowRecords
    private Integer calculateActiveBorrowCount(Member member) {
        if (member.getBorrowRecords() == null) {
            return 0;
        }
        // Count active (non-archived) borrow records
        return (int) member.getBorrowRecords().stream()
                .filter(record -> !Boolean.TRUE.equals(record.getIsArchived()))
                .count();
    }

    public List<MemberResponse> toResponseList(List<Member> members){
        if (members == null || members.isEmpty()) {
            return new ArrayList<>();
        }
        return members.stream()
                .map(this::toResponse)
                .toList();
    }
}
