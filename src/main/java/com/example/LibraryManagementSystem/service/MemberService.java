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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final BorrowRecordRepository borrowRecordRepository;

    private final MemberMapper memberMapper;

    private final UsersRepository usersRepository;

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
                .orElseThrow(() -> {
                    log.warn("Member not found - ID: {}", memberId);
                    return new ResourceNotFoundException("Member","id",memberId);
                });
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse addMember(MemberRequest request) {

        log.info("Adding new Member - Name: {}, Email: {}", request.getName(), request.getEmail());

        if(memberRepository.existsByEmail(request.getEmail())){
            log.warn("Member creation failed - Email already exists: {}", request.getEmail());
            throw new ResourceAlreadyExistsException("Member", "email", request.getEmail());
        }

        //using mapper to convert requestDTO into entity
        Member member = memberMapper.toEntity(request);

        Member savedMember = memberRepository.save(member);

        log.info("Member added successfully - ID: {}, Name: '{}'", savedMember.getId(), savedMember.getName());

        return memberMapper.toResponse(savedMember);
    }

    @Transactional
    public MemberResponse updateMember(Integer memberId, MemberRequest request, String currentUsername) {

        log.info("Updating member - ID: {}", memberId);

        Member existingMember = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("Member not found for update - ID: {}", memberId);
                    return new ResourceNotFoundException("Member","id",memberId);
                });

        Users currentUser = usersRepository.findByUsername(currentUsername)
                .orElseThrow(()-> {
                    log.warn("User not found for update: {}", currentUsername);
                    return new ResourceNotFoundException("User not found");
                });

        if (currentUser.getRole() == Users.Role.MEMBER) {
            // Check if this member belongs to current user
            if (!existingMember.getUsers().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only update your own profile");
            }
        }

        if(request.getEmail() != null && !request.getEmail().equals(existingMember.getEmail())){
            if(memberRepository.existsByEmail(request.getEmail())){
                log.warn("Member update failed - Email already exists: {}", request.getEmail());
                throw new ResourceAlreadyExistsException("Member", "email", request.getEmail());
            }
        }

        //using mapper to convert requestDTO into entity
        memberMapper.updateRequestToEntity(existingMember, request);
        Member savedMember = memberRepository.save(existingMember);
        log.info("Member updated - ID: {}, Name: '{}'", memberId, savedMember.getName());
        return memberMapper.toResponse(savedMember);
    }

    @Transactional
    public void deleteMember(Integer memberId) {

        log.info("Deleting record - ID: {}", memberId);

        Member member =  memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("Member not found for deletion - ID: {}", memberId);
                    return new ResourceNotFoundException("Member","id",memberId);
                });

        //checking in borrow records if any book not returned by a member
        boolean hasUnreturnedBooks = borrowRecordRepository.existsByMemberIdAndStatusNot(memberId, BorrowRecord.BorrowStatus.RETURNED);

        if(hasUnreturnedBooks){
            log.warn("Member deletion failed - Has unreturned books - ID: {}, Name: '{}'", memberId, member.getName());
            throw new ActiveBorrowExistsException("Member still has active or overdue books and cannot be deleted.");
        }

        memberRepository.delete(member);
        log.info("Member deleted - ID: {}, Name: '{}'", memberId, member.getName());
    }

    public MemberResponse getMyProfile(String username) {

        log.info("Getting profile for: {}", username);

        // Finding user first
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(()-> {
                    log.warn("User not found: {}", username);
                    return new ResourceNotFoundException("User not found");
                });

        // Then finding member by user ID
        Member member = memberRepository.findByUsersId(user.getId())
                .orElseThrow(() -> {
                    log.warn("Member profile not found for user: {}", username);
                    return new ResourceNotFoundException("Member profile not found");
                });
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse upgradeMembership(Integer memberId, MembershipType newTier, String currentUsername) {

        log.info("Upgrading membership - ID: {}, New tier: {}", memberId, newTier);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> {
                    log.warn("Member not found for upgrade - ID: {}", memberId);
                   return new ResourceNotFoundException("Member", "id", memberId);
                });

        Users users = usersRepository.findByUsername(currentUsername)
                .orElseThrow(()-> {
                    log.warn("User not found: {}", currentUsername);
                    return new ResourceNotFoundException("User not found");
                });

        //if member tries to upgrade
        if (users.getRole() == Users.Role.MEMBER){
            Member currentMember = getCurrentMember(currentUsername);
            if (!currentMember.getId().equals(memberId)){
                throw new AccessDeniedException("You can only upgrade your own membership");
            }
        }

        // Validating upgrade (can only go up, not down)
        if(newTier.ordinal() <= member.getMembershipType().ordinal()){
            log.warn("Membership upgrade failed - Invalid tier change: {} → {} for member {}",
                    member.getMembershipType(), newTier, memberId);
            throw new IllegalArgumentException(
                    "Cannot downgrade or stay at same tier. Use downgrade endpoint or choose higher tier."
            );
        }

        // Storing old tier for logging
        MembershipType oldTier = member.getMembershipType();

        //payment integration i have to implement
        member.setMembershipType(newTier);
        Member savedMember = memberRepository.save(member);

        log.info("Membership upgraded - ID: {}, Name: '{}', {} → {}",
                memberId, member.getName(), oldTier, newTier);

        return memberMapper.toResponse(savedMember);
    }

    // Helper: Get current member from username
    private Member getCurrentMember(String username) {

        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new ResourceNotFoundException("User not found");
                });

        return memberRepository.findByUsersId(user.getId())
                .orElseThrow(() -> {
                    log.warn("Member profile not found for username: {}", username);
                    return new ResourceNotFoundException("Member profile not found");
                });
    }
}
