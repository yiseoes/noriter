package com.noriter.util;

import java.util.UUID;

public class IdGenerator {

    private IdGenerator() {
    }

    public static String generateProjectId() {
        return "prj_" + shortUuid();
    }

    public static String generateStageId() {
        return "stg_" + shortUuid();
    }

    public static String generateArtifactId() {
        return "art_" + shortUuid();
    }

    public static String generateLogId() {
        return "log_" + shortUuid();
    }

    public static String generateAuditId() {
        return "audit_" + shortUuid();
    }

    public static String generateMessageId() {
        return "msg_" + shortUuid();
    }

    public static String generateSettingId() {
        return "set_" + shortUuid();
    }

    public static String generateTokenUsageId() {
        return "tok_" + shortUuid();
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
