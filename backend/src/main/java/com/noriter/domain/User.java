package com.noriter.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Role { USER, ADMIN }

    public static User create(String email, String password, String name) {
        User user = new User();
        user.email = email;
        user.password = password;
        user.name = name;
        user.role = Role.USER;
        user.createdAt = LocalDateTime.now();
        return user;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }
}
