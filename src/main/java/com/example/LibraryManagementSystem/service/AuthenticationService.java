package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.config.JwtUtil;
import com.example.LibraryManagementSystem.dto.auth.AuthenticationRequest;
import com.example.LibraryManagementSystem.dto.auth.AuthenticationResponse;
import com.example.LibraryManagementSystem.dto.auth.RegisterRequest;
import com.example.LibraryManagementSystem.exception.MemberProfileCreationException;
import com.example.LibraryManagementSystem.exception.ResourceAlreadyExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.model.MembershipType;
import com.example.LibraryManagementSystem.model.Users;
import com.example.LibraryManagementSystem.repository.MemberRepository;
import com.example.LibraryManagementSystem.repository.UsersRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UsersRepository usersRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final MemberRepository memberRepository;

    private final JwtUtil jwtUtil;

    private final EmailService emailService;

    @Transactional
    public AuthenticationResponse register(@Valid RegisterRequest request) {

        log.info("Registration attempt for username: {}", request.getUsername());

        if (usersRepository.existsByUsername(request.getUsername())){
            log.warn("Registration failed - Username already exists: {}", request.getUsername());
            throw new ResourceAlreadyExistsException("User", "Username", request.getUsername());
        }

        if (usersRepository.existsByEmail(request.getEmail())){
            log.warn("Registration failed - Email already exists: {}", request.getEmail());
            throw new ResourceAlreadyExistsException("User", "Email", request.getEmail());
        }

        if (memberRepository.existsByEmail(request.getEmail())){
            log.warn("Registration failed - Email already exists in members: {}", request.getEmail());
            throw new ResourceAlreadyExistsException("User", "Email", request.getEmail());
        }

        //creating user
        Users users = Users.builder()
                .username(request.getUsername())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(Users.Role.MEMBER)
                .enabled(true)
                .build();

        Users savedUser = usersRepository.save(users);

        log.info("User created successfully - ID: {}, username: {}", savedUser.getId(), savedUser.getUsername());

        // if user is member, creating member profile with BASIC tier
        Member savedMember = null;
        if(savedUser.getRole() == Users.Role.MEMBER){
            try{
                Member member = Member.builder()
                        .name(request.getUsername())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .membershipType(MembershipType.BASIC)
                        .users(savedUser)
                        .build();
               savedMember = memberRepository.save(member);
               log.info("Member profile created sucessfully - ID: {}, Membership: {}",
                        savedMember.getId(), savedMember.getMembershipType());
                //send welcome email
                emailService.sendWelcomeEmail(savedUser, member);

            } catch (Exception e) {
                log.error("Failed to create member profile for username: {} - Error: {}", request.getUsername(), e.getMessage(), e);
                throw new MemberProfileCreationException("Failed to create member profile " + request.getUsername());
            }
        }

        //generating token
        String token = jwtUtil.generateToken(users);

        //response (response to dto)
        return AuthenticationResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(Users.Role.MEMBER)
                .memberId(savedMember != null ? savedMember.getId() : null)
                .message("User registered successfully")
                .build();
    }


    public AuthenticationResponse authenticate(@Valid AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            ));

            Users user = usersRepository.findByUsername(request.getUsername())
                    .orElseThrow(()-> new UsernameNotFoundException("User Not Found"));

            Member member = memberRepository.findByUsersId(user.getId())
                    .orElseThrow(()-> new ResourceNotFoundException("Member Not Found"));

            String token = jwtUtil.generateToken(user);
            log.info("Login successful for username: {} (Role: {})", user.getUsername(), user.getRole());
            // dto mapper
            return AuthenticationResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .memberId(member.getId())
                    .message("Login successful")
                    .build();

        }catch (BadCredentialsException exception){
            log.warn("Login failed - Invalid credentials for username: {}", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }catch (UsernameNotFoundException exception){
            log.warn("Login failed - User not found: {}", request.getUsername());
            throw new UsernameNotFoundException("Invalid username or password");
        }catch (AuthenticationException exception){
            log.warn("Authentication error for username: {} - {}", request.getUsername(), exception.getMessage());
            throw new BadCredentialsException("Authentication failed", exception);
        }
    }
}
