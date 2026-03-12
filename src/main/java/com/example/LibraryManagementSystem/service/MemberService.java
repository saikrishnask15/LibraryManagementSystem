package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.mapper.MemberMapper;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberRequest;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberResponse;
import com.example.LibraryManagementSystem.exception.ActiveBorrowExistsException;
import com.example.LibraryManagementSystem.exception.ResourceAlreadyExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.model.MembershipType;
import com.example.LibraryManagementSystem.model.Users;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;
import com.example.LibraryManagementSystem.repository.MemberRepository;
import com.example.LibraryManagementSystem.repository.UsersRepository;
import com.example.LibraryManagementSystem.specification.MemberSpecification;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private UsersRepository usersRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "email", "phone");

    public Page<MemberResponse> getAllMembers(
            String name,
            String email,
            String phone,
            MembershipType membershipType,
            int pageNo,
            int pageSize,
            String sortBy,
            String sortDir) { //Pageable interface

        //validating sortBy
        if(!ALLOWED_SORT_FIELDS.contains(sortBy)){
            sortBy = "id";
        }

        //validating page size
        pageSize = Math.min(pageSize, 50);

        //using Sort class defining the sort direction
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

//        (OR) operation
//        Page<Member> memberPage;
//        if (name != null){
//            memberPage = memberRepository.findByNameContainingIgnoreCase(name, pageable);
//        } else if (email != null){
//            memberPage =  memberRepository.findByEmailContainingIgnoreCase(email, pageable);
//        } else if (phone != null) {
//            memberPage = memberRepository.findByPhone(phone, pageable);
//        }  else{
//            memberPage = memberRepository.findAll(pageable);
//        }
        //advance filtering (AND) operation
        Specification<Member> spec = MemberSpecification.filterMembers(
                name, email, phone, membershipType
        );

        //Executing query with specification
        Page<Member> memberPage  = memberRepository.findAll(spec,pageable);

        //Returning Page with all metadata
        return memberPage.map(memberMapper::toResponse);
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
    public MemberResponse updateMember(Integer memberId, MemberRequest request, String currentUsername) {
        Member existingMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member","id",memberId));

        Users currentUser = usersRepository.findByUsername(currentUsername).orElseThrow(()->
                new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() == Users.Role.MEMBER) {
            // Check if this member belongs to current user
            if (!existingMember.getUsers().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only update your own profile");
            }
        }

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

    public MemberResponse getMyProfile(String username) {
        // Finding user first
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        // Then finding member by user ID
        Member member = memberRepository.findByUsersId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse upgradeMembership(Integer memberId, MembershipType newTier, String currentUsername) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new ResourceNotFoundException("Member", "id", memberId));

        Users users = usersRepository.findByUsername(currentUsername)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        //if member tries to upgrade
        if (users.getRole() == Users.Role.MEMBER){
            Member currentMember = getCurrentMember(currentUsername);
            if (!currentMember.getId().equals(memberId)){
                throw new AccessDeniedException("You can only upgrade your own membership");
            }
        }

        // Validating upgrade (can only go up, not down)
        if(newTier.ordinal() <= member.getMembershipType().ordinal()){
            throw new IllegalArgumentException(
                    "Cannot downgrade or stay at same tier. Use downgrade endpoint or choose higher tier."
            );
        }
        //payment integration i have to implement
        member.setMembershipType(newTier);
        Member savedMember = memberRepository.save(member);

        return memberMapper.toResponse(savedMember);
    }

    // Helper: Get current member from username
    private Member getCurrentMember(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return memberRepository.findByUsersId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
    }
}
