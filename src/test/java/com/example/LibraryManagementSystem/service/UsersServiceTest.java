package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.UserResponse;
import com.example.LibraryManagementSystem.dto.mapper.UserMapper;
import com.example.LibraryManagementSystem.exception.ActiveBorrowExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Member;
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
import org.springframework.security.access.AccessDeniedException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsersService Unit Tests")
class UsersServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UsersService usersService;

    // DTO out — UserResponse uses @Builder so we use the builder here
    private UserResponse userResponse;

    // adminUser — the one calling deleteUser in most tests (currentUsername)
    private Users adminUser;

    // memberUser — the account being deleted in MEMBER-role deletion tests
    private Users memberUser;

    // librarianUser — for role-specific deletion tests
    private Users librarianUser;

    // The Member row linked to memberUser
    private Member member;

    @BeforeEach
    void setUp() {
        // new Users() skips @Builder.Default — role and enabled must be set explicitly
        adminUser = new Users();
        adminUser.setId(1);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Users.Role.ADMIN);
        adminUser.setEnabled(true);
        adminUser.setCreatedAt(LocalDateTime.now());

        memberUser = new Users();
        memberUser.setId(2);
        memberUser.setUsername("alice");
        memberUser.setEmail("alice@example.com");
        memberUser.setRole(Users.Role.MEMBER);
        memberUser.setEnabled(true);
        memberUser.setCreatedAt(LocalDateTime.now());

        librarianUser = new Users();
        librarianUser.setId(3);
        librarianUser.setUsername("librarian");
        librarianUser.setEmail("librarian@example.com");
        librarianUser.setRole(Users.Role.LIBRARIAN);
        librarianUser.setEnabled(true);
        librarianUser.setCreatedAt(LocalDateTime.now());

        // Member row linked to memberUser — used in MEMBER-deletion path
        member = new Member();
        member.setId(10);
        member.setName("Alice");
        member.setEmail("alice@example.com");
        member.setUsers(memberUser);

        userResponse = UserResponse.builder()
                .id(1)
                .username("admin")
                .email("admin@example.com")
                .role(Users.Role.ADMIN)
                .enabled(true)
                .createdAt(adminUser.getCreatedAt())
                .build();
    }

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("Returns correct total element count")
        void returnsCorrectTotalElementCount() {
            when(usersRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(adminUser)));
            when(userMapper.toResponse(adminUser)).thenReturn(userResponse);

            Page<UserResponse> result = usersService.getAllUsers(0, 10, "id", "ASC");

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Maps username correctly in response")
        void mapsUsernameCorrectly() {
            when(usersRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(adminUser)));
            when(userMapper.toResponse(adminUser)).thenReturn(userResponse);

            Page<UserResponse> result = usersService.getAllUsers(0, 10, "id", "ASC");

            assertEquals("admin", result.getContent().get(0).getUsername());
        }

        @Test
        @DisplayName("Maps role correctly in response")
        void mapsRoleCorrectly() {
            when(usersRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(adminUser)));
            when(userMapper.toResponse(adminUser)).thenReturn(userResponse);

            Page<UserResponse> result = usersService.getAllUsers(0, 10, "id", "ASC");

            assertEquals(Users.Role.ADMIN, result.getContent().get(0).getRole());
        }

        @Test
        @DisplayName("Returns empty page when no users exist")
        void returnsEmptyPage() {
            when(usersRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

            Page<UserResponse> result = usersService.getAllUsers(0, 10, "id", "ASC");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Caps pageSize at 50 regardless of requested value")
        void capsPageSizeAt50() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(usersRepository.findAll(captor.capture())).thenReturn(Page.empty());

            usersService.getAllUsers(0, 999, "id", "ASC");

            assertEquals(50, captor.getValue().getPageSize());
        }

        @ParameterizedTest
        @ValueSource(strings = {"role", "phone", "unknown", ""})
        @DisplayName("Falls back to 'id' for disallowed sortBy values")
        void fallsBackToIdForInvalidSortField(String invalidSortBy) {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(usersRepository.findAll(captor.capture())).thenReturn(Page.empty());

            usersService.getAllUsers(0, 10, invalidSortBy, "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor("id"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"id", "username", "email"})
        @DisplayName("Accepts all allowed sortBy fields without falling back")
        void acceptsAllAllowedSortFields(String validSortBy) {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(usersRepository.findAll(captor.capture())).thenReturn(Page.empty());

            usersService.getAllUsers(0, 10, validSortBy, "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor(validSortBy));
        }

        @Test
        @DisplayName("Applies descending sort when sortDir is 'DESC'")
        void appliesDescendingSort() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(usersRepository.findAll(captor.capture())).thenReturn(Page.empty());

            usersService.getAllUsers(0, 10, "username", "DESC");

            Sort.Order order = captor.getValue().getSort().getOrderFor("username");
            assertNotNull(order);
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Nested
        @DisplayName("getCurrentUser()")
        class GetCurrentUser {

            @Test
            @DisplayName("Returns username correctly when user exists")
            void returnsUsernameCorrectly() {
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(userMapper.toResponse(adminUser)).thenReturn(userResponse);

                UserResponse result = usersService.getCurrentUser("admin");

                assertEquals("admin", result.getUsername());
            }

            @Test
            @DisplayName("Returns role correctly when user exists")
            void returnsRoleCorrectly() {
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(userMapper.toResponse(adminUser)).thenReturn(userResponse);

                UserResponse result = usersService.getCurrentUser("admin");

                assertEquals(Users.Role.ADMIN, result.getRole());
            }

            @Test
            @DisplayName("Returns enabled flag correctly when user exists")
            void returnsEnabledFlagCorrectly() {
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(userMapper.toResponse(adminUser)).thenReturn(userResponse);

                UserResponse result = usersService.getCurrentUser("admin");

                assertTrue(result.getEnabled());
            }

            @Test
            @DisplayName("Throws ResourceNotFoundException when username is not found")
            void throwsWhenUsernameNotFound() {
                when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> usersService.getCurrentUser("ghost"));
                verify(userMapper, never()).toResponse(any());
            }
        }

        @Nested
        @DisplayName("deleteUser()")
        class DeleteUser {

            @Test
            @DisplayName("Throws AccessDeniedException when user tries to delete their own account")
            void throwsWhenDeletingOwnAccount() {
                // admin (id=1) is trying to delete id=1 — their own account
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

                assertThrows(AccessDeniedException.class, () -> usersService.deleteUser(1, "admin"));

                // Must never reach findById or delete
                verify(usersRepository, never()).findById(any());
                verify(usersRepository, never()).delete(any());
            }

            @Test
            @DisplayName("Skips self-deletion guard when currentUsername does not exist in DB")
            void skipsGuardWhenCurrentUserNotInDb() {
                // findByUsername returns empty → ifPresent is a no-op → proceeds to findById
                when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());
                when(usersRepository.findById(2)).thenReturn(Optional.of(memberUser));
                when(memberRepository.findByUsersId(2)).thenReturn(Optional.empty());

                usersService.deleteUser(2, "ghost");

                verify(usersRepository).delete(memberUser);
            }


            @Test
            @DisplayName("Throws ResourceNotFoundException when user to delete does not exist")
            void throwsWhenUserNotFound() {
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(usersRepository.findById(99)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> usersService.deleteUser(99, "admin"));

                verify(usersRepository, never()).delete(any());
            }


            @Test
            @DisplayName("Deletes ADMIN user directly without checking member row or borrows")
            void deletesAdminUserDirectly() {
                Users anotherAdmin = new Users();
                anotherAdmin.setId(5);
                anotherAdmin.setUsername("admin2");
                anotherAdmin.setRole(Users.Role.ADMIN);
                anotherAdmin.setEnabled(true);

                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(usersRepository.findById(5)).thenReturn(Optional.of(anotherAdmin));

                usersService.deleteUser(5, "admin");

                verify(memberRepository, never()).findByUsersId(any());
                verify(borrowRecordRepository, never()).existsByMemberIdAndStatusNot(any(), any());
                verify(usersRepository).delete(anotherAdmin);
            }


            @Test
            @DisplayName("Deletes LIBRARIAN user directly without checking member row or borrows")
            void deletesLibrarianUserDirectly() {
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(usersRepository.findById(3)).thenReturn(Optional.of(librarianUser));

                usersService.deleteUser(3, "admin");

                verify(memberRepository, never()).findByUsersId(any());
                verify(borrowRecordRepository, never()).existsByMemberIdAndStatusNot(any(), any());
                verify(usersRepository).delete(librarianUser);
            }


            @Test
            @DisplayName("Deletes member row before user row when MEMBER has no active borrows")
            void deletesMemberRowBeforeUserRow() {
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(usersRepository.findById(2)).thenReturn(Optional.of(memberUser));
                when(memberRepository.findByUsersId(2)).thenReturn(Optional.of(member));
                when(borrowRecordRepository.existsByMemberIdAndStatusNot(
                        10, BorrowRecord.BorrowStatus.RETURNED)).thenReturn(false);

                usersService.deleteUser(2, "admin");

                // Order matters — member row must be deleted first to satisfy FK constraint
                var inOrder = inOrder(memberRepository, usersRepository);
                inOrder.verify(memberRepository).delete(member);
                inOrder.verify(usersRepository).delete(memberUser);
            }

            @Test
            @DisplayName("Deletes user row directly when MEMBER has no linked member row")
            void deletesUserWhenNoMemberRowLinked() {
                // User has MEMBER role but no member row in DB — ifPresent is a no-op
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(usersRepository.findById(2)).thenReturn(Optional.of(memberUser));
                when(memberRepository.findByUsersId(2)).thenReturn(Optional.empty());

                usersService.deleteUser(2, "admin");

                verify(memberRepository, never()).delete((Member) any());
                verify(borrowRecordRepository, never()).existsByMemberIdAndStatusNot(any(), any());
                verify(usersRepository).delete(memberUser);
            }


            @Test
            @DisplayName("Throws ActiveBorrowExistsException when MEMBER has unreturned books")
            void throwsWhenMemberHasActiveBorrows() {
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(usersRepository.findById(2)).thenReturn(Optional.of(memberUser));
                when(memberRepository.findByUsersId(2)).thenReturn(Optional.of(member));
                when(borrowRecordRepository.existsByMemberIdAndStatusNot(
                        10, BorrowRecord.BorrowStatus.RETURNED)).thenReturn(true);

                assertThrows(ActiveBorrowExistsException.class, () -> usersService.deleteUser(2, "admin"));

                verify(memberRepository, never()).delete((Member) any());
                verify(usersRepository, never()).delete(any());
            }

            @Test
            @DisplayName("Borrow check uses member.getId() (10) not user.getId() (2)")
            void borrowCheckUsesMemberIdNotUserId() {
                // Critical distinction: borrow_record FK is member_id (10), not user_id (2)
                when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
                when(usersRepository.findById(2)).thenReturn(Optional.of(memberUser));
                when(memberRepository.findByUsersId(2)).thenReturn(Optional.of(member));
                when(borrowRecordRepository.existsByMemberIdAndStatusNot(
                        10, BorrowRecord.BorrowStatus.RETURNED)).thenReturn(false);

                usersService.deleteUser(2, "admin");

                verify(borrowRecordRepository)
                        .existsByMemberIdAndStatusNot(10, BorrowRecord.BorrowStatus.RETURNED);
            }
        }
    }
}