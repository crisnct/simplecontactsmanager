package com.example.contacts.controller;

import com.example.contacts.dto.SignupRequest;
import com.example.contacts.dto.UserResponse;
import com.example.contacts.model.User;
import com.example.contacts.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request,
                                               HttpServletRequest servletRequest) {
        User user = userService.register(request);
        authenticateAndStoreSession(servletRequest, request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(user.getUsername()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UserResponse(principal.getUsername()));
    }

    private void authenticateAndStoreSession(HttpServletRequest servletRequest,
                                             String username,
                                             String rawPassword) {
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, rawPassword);
        Authentication authentication = authenticationManager.authenticate(authRequest);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        servletRequest.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
