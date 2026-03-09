package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.config.JwtUtil;
import com.example.LibraryManagementSystem.dto.auth.AuthenticationRequest;
import com.example.LibraryManagementSystem.dto.auth.AuthenticationResponse;
import com.example.LibraryManagementSystem.dto.auth.RegisterRequest;
import com.example.LibraryManagementSystem.exception.MemberProfileCreationException;
import com.example.LibraryManagementSystem.exception.ResourceAlreadyExistsException;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.model.MembershipType;
import com.example.LibraryManagementSystem.model.Users;
import com.example.LibraryManagementSystem.repository.MemberRepository;
import com.example.LibraryManagementSystem.repository.UsersRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Autowired
    public UsersRepository usersRepository;

    @Autowired
    public PasswordEncoder passwordEncoder;

    @Autowired
    public AuthenticationManager authenticationManager;

    @Autowired
    public MemberRepository memberRepository;

    @Autowired
    public JwtUtil jwtUtil;

    @Transactional
    public AuthenticationResponse register(@Valid RegisterRequest request) {

        if (usersRepository.existsByUsername(request.getUsername())){
            throw new ResourceAlreadyExistsException("User", "Username", request.getUsername());
        }

        if (usersRepository.existsByEmail(request.getEmail())){
            throw new ResourceAlreadyExistsException("User", "Email", request.getEmail());
        }

        if (memberRepository.existsByEmail(request.getEmail())){
            throw new ResourceAlreadyExistsException("User", "Email", request.getEmail());
        }

        //creating user
        Users users = Users.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(Users.Role.MEMBER)
                .enabled(true)
                .build();

        Users savedUser = usersRepository.save(users);

        // if user is member, creating member profile with BASIC tier
        if(savedUser.getRole() == Users.Role.MEMBER){
            try{
                Member member = Member.builder()
                        .name(request.getUsername())
                        .email(request.getEmail())
                        .membershipType(MembershipType.BASIC)
                        .users(savedUser)
                        .build();
                memberRepository.save(member);
            } catch (Exception e) {
                throw new MemberProfileCreationException("Failed to create member profile " + request.getUsername());
            }
        }

        //generating token
        String token = jwtUtil.generateToken(users);

        return AuthenticationResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(Users.Role.MEMBER)
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

            String token = jwtUtil.generateToken(user);
            return AuthenticationResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .message("Login successful")
                    .build();

        }catch (BadCredentialsException exception){
            throw new BadCredentialsException("Invalid username or password");
        }catch (UsernameNotFoundException exception){
            throw new UsernameNotFoundException("Invalid username or password");
        }catch (AuthenticationException exception){
            throw new BadCredentialsException("Authentication failed", exception);
        }
    }
}
