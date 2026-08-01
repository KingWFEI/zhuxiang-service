package com.zhuxiang.service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Uses the server secret to derive a stable six-digit appointment check-in code.
 * No plaintext verification code is persisted.
 */
@Component
public class AppointmentCheckinCodeService {

    private final byte[] secret;

    public AppointmentCheckinCodeService(
            @Value("${app.auth.token-secret}") String tokenSecret
    ) {
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String codeFor(String appointmentId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(
                    ("appointment-checkin:" + appointmentId).getBytes(StandardCharsets.UTF_8)
            );
            int value = ((digest[0] & 0xff) << 24)
                    | ((digest[1] & 0xff) << 16)
                    | ((digest[2] & 0xff) << 8)
                    | (digest[3] & 0xff);
            return String.format("%06d", Math.floorMod(value, 1_000_000));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法生成预约核验码", exception);
        }
    }

    public boolean matches(String appointmentId, String candidate) {
        if (candidate == null || candidate.length() != 6) {
            return false;
        }
        return constantTimeEquals(codeFor(appointmentId), candidate);
    }

    private boolean constantTimeEquals(String left, String right) {
        int difference = left.length() ^ right.length();
        int length = Math.min(left.length(), right.length());
        for (int index = 0; index < length; index++) {
            difference |= left.charAt(index) ^ right.charAt(index);
        }
        return difference == 0;
    }
}
