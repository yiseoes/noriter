package com.noriter.auth;

import com.noriter.domain.User;
import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import com.noriter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse signup(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new NoriterException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.create(email, passwordEncoder.encode(password), name);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoriterException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new NoriterException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoriterException(ErrorCode.INVALID_TOKEN));
        return new AuthResponse(null, user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }
}
