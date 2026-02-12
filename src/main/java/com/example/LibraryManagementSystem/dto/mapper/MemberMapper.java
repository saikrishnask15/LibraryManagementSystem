package com.example.LibraryManagementSystem.dto.mapper;

import com.example.LibraryManagementSystem.dto.memberDTO.MemberRequest;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberResponse;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberMapper {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    public Member toEntity(MemberRequest memberRequest){
        return Member.builder()
                .name(memberRequest.getName())
                .email(memberRequest.getEmail())
                .Phone(memberRequest.getPhone())
                    .build();
    }

    public void updateRequestToEntity(Member existingMember, MemberRequest request){
        if(request.getName() != null){
            existingMember.setName(request.getName());
        }
        if(request.getPhone() != null){
            existingMember.setPhone(request.getPhone());
        }
    }

    public MemberResponse toResponse(Member member){
        return MemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .membershipDate(member.getMembershipDate())
                .activeBorrowCount(extractActiveBorrowCount(member.getId()))
                .build();
    }
    //helper method
    private Integer extractActiveBorrowCount(Integer id){
        return borrowRecordRepository.countByMemberIdAndIsArchivedFalse(id);
    }

    public List<MemberResponse> toReponseList(List<Member> members){
        return members.stream()
                .map(this::toResponse)
                .toList();
    }
}
