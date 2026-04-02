package com.noriter.repository;

import com.noriter.domain.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, String> {

    Optional<Setting> findByKey(String key);
}
