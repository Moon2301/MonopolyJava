package com.game.monopoly.service;

import com.game.monopoly.dto.LoginRequest;
import com.game.monopoly.dto.RegisterRequest;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.model.enums.UserRole;
import com.game.monopoly.model.enums.AccountStatus;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userProfileRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        // 1. Tạo Account
        Account account = new Account();
        account.setEmail(request.getEmail());
        // Băm mật khẩu để lưu vào password_hash
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole(UserRole.USER);
        account.setStatus(AccountStatus.ACTIVE);
        account = accountRepository.save(account);

        // 2. Tạo UserProfile đi kèm
        UserProfile profile = new UserProfile();
        profile.setAccount(account);
        profile.setUsername(request.getUsername());
        profile.setGold(1000L);
        profile.setDiamonds(10L);
        userProfileRepository.save(profile);
    }
    public String login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }
        return jwtService.generateToken(account.getEmail());
    }

}