package io.github.ziy1.nexevent.controller;

import io.github.ziy1.nexevent.dto.AuthLoginRequestDto;
import io.github.ziy1.nexevent.dto.AuthLoginResponseDto;
import io.github.ziy1.nexevent.dto.AuthRegisterRequestDto;
import io.github.ziy1.nexevent.dto.ResponseMessage;
import io.github.ziy1.nexevent.security.JwtTokenProvider;
import io.github.ziy1.nexevent.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseMessage<Void>> register(
            @Validated @RequestBody AuthRegisterRequestDto authRegisterRequestDto,
            HttpServletRequest request) {
        AuthRegisterRequestDto registeredUser = authService.register(authRegisterRequestDto);

        if (registeredUser == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseMessage.error(HttpStatus.CONFLICT, request.getRequestURI(),
                            "User already exists with ID (case-insensitive): " + authRegisterRequestDto.userId(), null));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("User registered successfully", request.getRequestURI(), null));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseMessage<AuthLoginResponseDto>> login(
            @Validated @RequestBody AuthLoginRequestDto authLoginRequestDto,
            HttpServletRequest request) {
        String token = authService.login(authLoginRequestDto.userId(), authLoginRequestDto.password());

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseMessage.error(HttpStatus.UNAUTHORIZED, request.getRequestURI(),
                            "Invalid credentials", null));
        }

        AuthLoginResponseDto authResponse = new AuthLoginResponseDto(token);
        return ResponseEntity.ok(ResponseMessage.success(request.getRequestURI(), authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseMessage<Void>> logout(
            HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token != null) {
            authService.logout(token);
        }

        return ResponseEntity.ok(ResponseMessage.success(request.getRequestURI()));
    }
}