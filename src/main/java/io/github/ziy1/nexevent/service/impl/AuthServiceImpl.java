package io.github.ziy1.nexevent.service.impl;

import io.github.ziy1.nexevent.dto.AuthLoginResponseDto;
import io.github.ziy1.nexevent.dto.AuthRegisterRequestDto;
import io.github.ziy1.nexevent.entity.User;
import io.github.ziy1.nexevent.mapper.UserMapper;
import io.github.ziy1.nexevent.repository.UserRepository;
import io.github.ziy1.nexevent.security.JwtTokenProvider;
import io.github.ziy1.nexevent.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;

  public AuthServiceImpl(
      UserRepository userRepository,
      UserMapper userMapper,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.authenticationManager = authenticationManager;
  }

  @Override
  public AuthRegisterRequestDto register(AuthRegisterRequestDto authRegisterRequestDto) {
    if (userRepository.findById(authRegisterRequestDto.userId()).isPresent()) {
      return null;
    }

    User user = userMapper.toEntity(authRegisterRequestDto);
    user.setPassword(passwordEncoder.encode(user.getPassword()));

    return userMapper.toDto(userRepository.save(user));
  }

  @Override
  public AuthLoginResponseDto login(String userId, String password) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(userId, password));

      User user =
          userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

      return new AuthLoginResponseDto(
          jwtTokenProvider.generateToken(authentication),
          user.getFirstName() + " " + user.getLastName());
    } catch (AuthenticationException e) {
      return null;
    }
  }

  @Override
  public void logout(String token) {
    jwtTokenProvider.invalidateToken(token);
  }
}
