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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("memberService unit test")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private MemberService memberService;

    private Member member;
    private MemberResponse memberResponse;
    private MemberRequest memberRequest;

    // Users linked to the member (for ownership checks in update/upgrade)
    private Users linkedUser;   // the Users row whose member row is `member`
    private Users adminUser;    // role ADMIN — can do anything
    private Users otherUser;    // role MEMBER but a different account
    private Users librarianUser;

    @BeforeEach
    void setUp(){
        // The Users row that "owns" this member profile
        linkedUser = new Users();
        linkedUser.setId(10);
        linkedUser.setUsername("alice");
        linkedUser.setEmail("alice@example.com");
        linkedUser.setRole(Users.Role.MEMBER);

        adminUser = new Users();
        adminUser.setId(1);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Users.Role.ADMIN);

        otherUser = new Users();
        otherUser.setId(99);
        otherUser.setUsername("bob");
        otherUser.setEmail("bob@example.com");
        otherUser.setRole(Users.Role.MEMBER);

        // LIBRARIAN: not MEMBER, so the ownership if-block is skipped entirely —
        // same behavior as ADMIN. Separate fixture makes this explicit in tests.
        librarianUser = new Users();
        librarianUser.setId(2);
        librarianUser.setUsername("librarian");
        librarianUser.setEmail("librarian@example.com");
        librarianUser.setRole(Users.Role.LIBRARIAN);

        // Member entity
        // MembershipType is the source of maxBooksAllowed and borrowPeriodDays —
        // no standalone setters exist on Member for those values.
        member = new Member();
        member.setId(1);
        member.setName("Alice");
        member.setEmail("alice@example.com");
        member.setPhone("1234567890");
        member.setMembershipType(MembershipType.BASIC);  // BASIC: maxBooks=3, period=7 days
        member.setUsers(linkedUser);  // ownership link used by updateMember

        // DTO in
        memberRequest = new MemberRequest();
        memberRequest.setName("Alice");
        memberRequest.setEmail("alice@example.com");
        memberRequest.setPhone("1234567890");
        memberRequest.setMembershipType(MembershipType.BASIC);

        // DTO out — flat response (no nested DTOs in MemberResponse)
        memberResponse = new MemberResponse();
        memberResponse.setId(1);
        memberResponse.setName("Alice");
        memberResponse.setEmail("alice@example.com");
        memberResponse.setPhone("1234567890");
        memberResponse.setMembershipType(MembershipType.BASIC);
        // maxBooksAllowed and borrowPeriodDays are derived from MembershipType by the mapper
        memberResponse.setMaxBooksAllowed(MembershipType.BASIC.getMaxBooksAllowed());
        memberResponse.setBorrowPeriodDays(MembershipType.BASIC.getBorrowPeriodDays());
    }

    @Nested
    @DisplayName("getAllMembers()")
    class GetAllMembers{

        @Test
        @DisplayName("Returns mapped page when valid params are provided")
        void returnsMappedPage(){
            Page<Member> memberPage = new PageImpl<>(List.of(member));
            when(memberRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(memberPage);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            Page<MemberResponse> result = memberService.getAllMembers(
              null, null, null, null, 0, 10, "id", "ASC"
            );

            assertEquals(1, result.getTotalElements());
            assertEquals("Alice", result.getContent().getFirst().getName());
            assertEquals(MembershipType.BASIC.getBorrowPeriodDays(), result.getContent().getFirst().getBorrowPeriodDays());
            assertEquals(MembershipType.BASIC.getMaxBooksAllowed(), result.getContent().getFirst().getMaxBooksAllowed());
        }

        @Test
        @DisplayName("Caps pageSize at 50 regardless of requested value")
        void capsPageSizeAt50(){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(memberRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());
            memberService.getAllMembers(null, null, null, null, 0, 100, "id", "ASC");

            assertEquals(50, captor.getValue().getPageSize());
        }

        @ParameterizedTest
        @DisplayName("Falls back to 'id' for disallowed sortBy values")
        @ValueSource(strings = {"membershipType", "bio", "unknown", ""})
        void fallsBackToIdForInvalidSortField(String invalidSortBy){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(memberRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            memberService.getAllMembers(null, null, null, null, 0, 10, invalidSortBy, "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor("id"));
        }

        @ParameterizedTest
        @DisplayName("Accepts all allowed sortBy fields without falling back")
        @ValueSource(strings = {"id", "name", "email", "phone"})
        void acceptsAllAllowedSortFields(String validSortBy){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(memberRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            memberService.getAllMembers(null, null, null,
                    null, 0, 10, validSortBy, "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor(validSortBy));
        }

        @Test
        @DisplayName("Applies descending sort when sortDir is 'DESC'")
        void appliesDescendingSort(){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(memberRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            memberService.getAllMembers(null, null, null, null, 0, 10, "name", "DESC");

            Sort.Order order = captor.getValue().getSort().getOrderFor("name");
            assertNotNull(order);
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        @DisplayName("Returns empty page when no members match the filter")
        void returnsEmptyPage(){
            when(memberRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<MemberResponse> result = memberService.getAllMembers(
                    "Nobody", null, null, null, 0, 10, "id", "ASC"
            );

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getMemberById()")
    class GetMemberById{

        @Test
        @DisplayName("Returns MemberResponse when member exists")
        void returnsResponseWhenFound(){

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            MemberResponse result = memberService.getMemberById(1);

            assertEquals("Alice", result.getName());
            verify(memberRepository).findById(1);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when member is missing")
        void throwsWhenNotFound(){
            when(memberRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->memberService.getMemberById(99));

            verify(memberMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("addMember()")
    class AddMember{

        @Test
        @DisplayName("Saves and returns MemberResponse when email is unique")
        void savesMemberSuccessfully(){

            when(memberMapper.toEntity(memberRequest)).thenReturn(member);
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            MemberResponse result = memberService.addMember(memberRequest);

            assertEquals("Alice", result.getName());
            assertEquals("alice@example.com", result.getEmail());
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("Throws ResourceAlreadyExistsException when email is already registered")
        void throwsWhenEmailAlreadyExists() {
            when(memberRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThrows(ResourceAlreadyExistsException.class, () -> memberService.addMember(memberRequest));

            verify(memberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateMember()")
    class UpdateMember{

        @Test
        @DisplayName("ADMIN can update any member's profile")
        void adminCanUpdateAnyMember(){
            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            MemberResponse result = memberService.updateMember(1, memberRequest, "admin");

            assertNotNull(result);
            verify(memberMapper).updateRequestToEntity(member, memberRequest);
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("LIBRARIAN can update any member's profile (bypasses ownership check)")
        void librarianCanUpdateAnyMember(){
            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("librarian")).thenReturn(Optional.ofNullable(librarianUser));
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            MemberResponse result = memberService.updateMember(1, memberRequest, "librarian");

            assertNotNull(result);
            verify(memberMapper).updateRequestToEntity(member, memberRequest);
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("MEMBER can update their own profile when linkedUser ID matches")
        void memberCanUpdateOwnProfile(){

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("alice")).thenReturn(Optional.ofNullable(linkedUser));
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            MemberResponse result = memberService.updateMember(1, memberRequest, "alice");

            assertNotNull(result);
            verify(memberMapper).updateRequestToEntity(member, memberRequest);
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("Throws AccessDeniedException when MEMBER tries to update another member's profile")
        void memberCannotUpdateOtherProfile(){
            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("bob")).thenReturn(Optional.ofNullable(otherUser));

            assertThrows(AccessDeniedException.class, ()->memberService.updateMember(1, memberRequest, "bob"));

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when member to update does not exist")
        void throwsWhenMemberNotFound(){
            when(memberRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->memberService.updateMember(99, memberRequest, "admin"));

            verify(memberRepository,never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when current user is not found")
        void throwsWhenCurrentUserNotFound(){
            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->memberService.updateMember(1, memberRequest, "ghost"));

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceAlreadyExistsException when new email is already taken")
        void throwsWhenNewEmailAlreadyTaken(){

            memberRequest.setEmail("taken@example.com");

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.ofNullable(adminUser));
            when(memberRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThrows(ResourceAlreadyExistsException.class, ()->memberService.updateMember(1, memberRequest, "admin"));

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Skips email uniqueness check when email in request is unchanged")
        void skipsEmailCheckWhenEmailUnchanged() {
            memberRequest.setEmail(null);

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.ofNullable(adminUser));
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            memberService.updateMember(1, memberRequest, "admin");

            verify(memberRepository, never()).existsByEmail(any());
            verify(memberRepository).save(member);
        }
    }

    @Nested
    @DisplayName("deleteMember()")
    class DeleteMember{

        @Test
        @DisplayName("Deletes member when they have no unreturned books")
        void deletesMemberSuccessfully(){
            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(borrowRecordRepository.existsByMemberIdAndStatusNot(1, BorrowRecord.BorrowStatus.RETURNED))
                    .thenReturn(false);

            memberService.deleteMember(1);
            verify(memberRepository).delete(member);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when member to delete does not exist")
        void throwsWhenMemberNotFound(){
            when(memberRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->memberService.deleteMember(99));

            verify(memberRepository, never()).delete((Member) any());
        }

        @Test
        @DisplayName("Throws ActiveBorrowExistsException when member has ACTIVE borrow records")
        void throwsWhenMemberHasActiveBorrows(){

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(borrowRecordRepository.existsByMemberIdAndStatusNot(1, BorrowRecord.BorrowStatus.RETURNED))
                    .thenReturn(true); // ACTIVE or OVERDUE records exist
            assertThrows(ActiveBorrowExistsException.class, ()->memberService.deleteMember(1));

            verify(memberRepository, never()).delete((Member) any());
        }

        @Test
        @DisplayName("Verifies borrow check uses the correct memberId")
        void borrowCheckUsesCorrectMemberId() {
            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(borrowRecordRepository.existsByMemberIdAndStatusNot(1, BorrowRecord.BorrowStatus.RETURNED))
                    .thenReturn(false);

            memberService.deleteMember(1);

            verify(borrowRecordRepository).existsByMemberIdAndStatusNot(1, BorrowRecord.BorrowStatus.RETURNED);
        }
    }

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile{

        @Test
        @DisplayName("Returns MemberResponse for the authenticated user's profile")
        void returnsProfileForCurrentUser(){
            when(usersRepository.findByUsername("alice")).thenReturn(Optional.ofNullable(linkedUser));
            when(memberRepository.findByUsersId(10)).thenReturn(Optional.ofNullable(member));
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            MemberResponse result = memberService.getMyProfile("alice");

            assertEquals(1, result.getId());
            assertEquals("alice@example.com", result.getEmail());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when username is not found")
        void throwsWhenUserNotFound(){
            when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, ()->memberService.getMyProfile("ghost"));

            verify(memberRepository, never()).findByUsersId(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when no member profile is linked to the user")
        void throwsWhenMemberProfileNotFound(){
            // User exists but has no member row linked yet
            when(usersRepository.findByUsername("alice")).thenReturn(Optional.ofNullable(linkedUser));
            when(memberRepository.findByUsersId(10)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->memberService.getMyProfile("alice"));

            verify(memberMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Uses user's ID (not username) to find the member row — two-step lookup")
        void usesTwoStepLookup() {
            when(usersRepository.findByUsername("alice")).thenReturn(Optional.of(linkedUser));
            when(memberRepository.findByUsersId(10)).thenReturn(Optional.of(member));
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            memberService.getMyProfile("alice");

            // Must look up by user.getId() (10), not by username directly
            verify(memberRepository).findByUsersId(linkedUser.getId());
        }
    }

    @Nested
    @DisplayName("upgradeMembership()")
    class UpgradeMembership{

        @Test
        @DisplayName("ADMIN can upgrade any member's tier to a higher one")
        void adminCanUpgradeAnyMember(){

            member.setMembershipType(MembershipType.BASIC);

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.ofNullable(adminUser));
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            memberService.upgradeMembership(1, MembershipType.STANDARD, "admin");

            assertEquals(MembershipType.STANDARD, member.getMembershipType());
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("ADMIN can upgrade to PREMIUM from BASIC (skipping STANDARD)")
        void adminCanUpgradeBasicToPremium(){
            member.setMembershipType(MembershipType.BASIC);

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.ofNullable(adminUser));
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            memberService.upgradeMembership(1, MembershipType.PREMIUM, "admin");

            assertEquals(MembershipType.PREMIUM, member.getMembershipType());
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("LIBRARIAN can upgrade any member's tier (bypasses ownership check)")
        void librarianCanUpgradeAnyMember(){
            member.setMembershipType(MembershipType.BASIC);

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("librarian")).thenReturn(Optional.ofNullable(librarianUser));
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            memberService.upgradeMembership(1, MembershipType.STANDARD, "librarian");

            assertEquals(MembershipType.STANDARD, member.getMembershipType());
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("MEMBER can upgrade their own membership")
        void memberCanUpgradeOwnTier(){
            member.setMembershipType(MembershipType.BASIC);

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("alice")).thenReturn(Optional.ofNullable(linkedUser));

            // getCurrentMember() internal helper: finds user → finds member by userId
            when(memberRepository.findByUsersId(linkedUser.getId())).thenReturn(Optional.ofNullable(member));

            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);

            memberService.upgradeMembership(1, MembershipType.STANDARD, "alice");

            assertEquals(MembershipType.STANDARD, member.getMembershipType());
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("Throws AccessDeniedException when MEMBER tries to upgrade another member's tier")
        void memberCannotUpgradeOtherMemberTier(){
            // otherUser's member profile has a different ID
            Member otherMember = new Member();
            otherMember.setId(99);
            otherMember.setMembershipType(MembershipType.BASIC);

            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("bob")).thenReturn(Optional.of(otherUser));
            // getCurrentMember() resolves bob's account to otherMember (id=99), not member (id=1)

            when(memberRepository.findByUsersId(otherUser.getId())).thenReturn(Optional.of(otherMember));

            assertThrows(AccessDeniedException.class, ()->memberService.upgradeMembership(1, MembershipType.STANDARD, "bob"));

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when trying to downgrade (PREMIUM → BASIC)")
        void throwsWhenDowngrading(){
            member.setMembershipType(MembershipType.PREMIUM);

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.ofNullable(adminUser));

            // BASIC.ordinal() (0) <= PREMIUM.ordinal() (2) → downgrade blocked
            assertThrows(IllegalArgumentException.class, ()->memberService.upgradeMembership(1, MembershipType.BASIC, "admin"));

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when trying to stay at the same tier")
        void throwsWhenSameTier(){
            member.setMembershipType(MembershipType.PREMIUM);

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.ofNullable(adminUser));

            // PREMIUM.ordinal() (0) <= PREMIUM.ordinal() (2) → downgrade blocked
            assertThrows(IllegalArgumentException.class, ()->memberService.upgradeMembership(1, MembershipType.PREMIUM, "admin"));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when member to upgrade does not exist")
        void throwsWhenMemberNotFound(){
            when(memberRepository.findById(99)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, ()->memberService.upgradeMembership(99, MembershipType.STANDARD, "admin"));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when current user is not found")
        void throwsWhenCurrentUserNotFound(){
            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->memberService.upgradeMembership(1, MembershipType.STANDARD, "ghost"));

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Verifies all three valid upgrade paths using ordinal boundaries")
        void coversAllUpgradePaths() {
            // BASIC(0) → STANDARD(1): ordinal diff = 1, valid
            member.setMembershipType(MembershipType.BASIC);
            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(memberRepository.save(member)).thenReturn(member);
            when(memberMapper.toResponse(member)).thenReturn(memberResponse);
            memberService.upgradeMembership(1, MembershipType.STANDARD, "admin");
            assertEquals(MembershipType.STANDARD, member.getMembershipType());

            // STANDARD(1) → PREMIUM(2): ordinal diff = 1, valid
            member.setMembershipType(MembershipType.STANDARD);
            memberService.upgradeMembership(1, MembershipType.PREMIUM, "admin");
            assertEquals(MembershipType.PREMIUM, member.getMembershipType());
        }
    }

}