package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.UserResponse;
import com.example.LibraryManagementSystem.dto.mapper.UserMapper;
import com.example.LibraryManagementSystem.exception.ActiveBorrowExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Users;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;
import com.example.LibraryManagementSystem.repository.MemberRepository;
import com.example.LibraryManagementSystem.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;

    private final BorrowRecordRepository borrowRecordRepository;

    private final  MemberRepository memberRepository;

    private final UserMapper userMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "username", "email");

    public Page<UserResponse> getAllUsers(int pageNo, int pageSize, String sortBy, String sortDir) {

        pageSize = Math.min(pageSize, 50);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)){
            sortBy = "id";
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Users> usersPage = usersRepository.findAll(pageable);

        return usersPage.map(userMapper::toResponse);
    }

    public UserResponse getCurrentUser(String username) {
        log.info("Getting user details - user: {}", username);
        Users users = usersRepository.findByUsername(username)
                .orElseThrow(()-> {
                         log.warn("User not found: {}", username);
                        return new ResourceNotFoundException("User Not Found");
        });
        return userMapper.toResponse(users);
    }

    @Transactional
    public void deleteUser(Integer id, String currentUsername) {

        log.info("Deleting user - ID: {}", id);

        usersRepository.findByUsername(currentUsername).ifPresent(currentUser -> {
            if (currentUser.getId().equals(id)) {
                throw new AccessDeniedException("You are not allowed to delete your own account");
            }
        });

        Users user = usersRepository.findById(id).orElseThrow(()-> {
            log.warn("User not found for deletion - ID: {}", id);
           return new ResourceNotFoundException("User", "id", id);
        });

        if (user.getRole() == Users.Role.MEMBER) {
            memberRepository.findByUsersId(id).ifPresent(member -> {
                // 1. Checking for active borrows
                if (borrowRecordRepository.existsByMemberIdAndStatusNot(member.getId(), BorrowRecord.BorrowStatus.RETURNED)) {
                    log.warn("User deletion failed - Has unreturned books - ID: {}, Name: '{}'",member.getId(), member.getName());
                    throw new ActiveBorrowExistsException("Cannot delete user with active borrows");
                }
                // 2. Deleting Member first to satisfy Foreign Key constraints
                memberRepository.delete(member);
            });
        }

        usersRepository.delete(user);
        log.info("User deleted - ID: {}, Name: '{}'", user.getId(), user.getUsername());
    }
}
