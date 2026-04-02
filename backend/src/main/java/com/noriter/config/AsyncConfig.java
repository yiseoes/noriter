package com.noriter.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 설정 — 파이프라인을 @Async로 비동기 실행하기 위함
 * 참조: 09_디렉토리 §3 config/AsyncConfig.java
 */
@Log4j2
@Configuration
@EnableAsync
public class AsyncConfig {
}
