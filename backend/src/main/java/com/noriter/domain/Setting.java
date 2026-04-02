package com.noriter.domain;

import com.noriter.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Setting {

    @Id
    @Column(length = 20)
    private String id;

    @Column(name = "`key`", nullable = false, unique = true, length = 100)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Setting create(String key, String value) {
        Setting setting = new Setting();
        setting.id = IdGenerator.generateSettingId();
        setting.key = key;
        setting.value = value;
        setting.updatedAt = LocalDateTime.now();
        return setting;
    }

    public void updateValue(String value) {
        this.value = value;
        this.updatedAt = LocalDateTime.now();
    }
}
