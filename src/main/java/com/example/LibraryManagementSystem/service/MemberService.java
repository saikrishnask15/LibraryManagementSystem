package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.mapper.MemberMapper;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberRequest;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberResponse;
import com.example.LibraryManagementSystem.exception.ActiveBorrowExistsException;
import com.example.LibraryManagementSystem.exception.ResourceAlreadyExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;
import com.example.LibraryManagementSystem.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private MemberMapper memberMapper;

    public List<MemberResponse> getAllMembers() {
       return memberMapper.toResponseList(memberRepository.findAll());
    }

    public MemberResponse getMemberById(Integer memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member","id",memberId));
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse addMember(MemberRequest request) {
        if(memberRepository.existsByEmail(request.getEmail())){
            throw new ResourceAlreadyExistsException("Member", "email", request.getEmail());
        }

        //using mapper to convert requestDTO into entity
        Member member = memberMapper.toEntity(request);

        Member savedMember = memberRepository.save(member);
        return memberMapper.toResponse(savedMember);
    }

    @Transactional
    public MemberResponse updateMember(Integer memberId, MemberRequest request) {
        Member existingMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member","id",memberId));

        if(request.getEmail() != null && !request.getEmail().equals(existingMember.getEmail())){
            if(memberRepository.existsByEmail(request.getEmail())){
                throw new ResourceAlreadyExistsException("Member", "email", request.getEmail());
            }
        }

        //using mapper to convert requestDTO into entity
        memberMapper.updateRequestToEntity(existingMember, request);
        Member savedMember = memberRepository.save(existingMember);
        return memberMapper.toResponse(savedMember);
    }

    @Transactional
    public void deleteMember(Integer memberId) {
        Member member =  memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member","id",memberId));

        //checking in borrow records if any book not returned by a member
        boolean hasUnreturnedBooks = borrowRecordRepository.existsByMemberIdAndStatusNot(memberId, BorrowRecord.BorrowStatus.RETURNED);

        if(hasUnreturnedBooks){
            throw new ActiveBorrowExistsException("Member still has active or overdue books and cannot be deleted.");
        }

        memberRepository.delete(member);
    }
}
