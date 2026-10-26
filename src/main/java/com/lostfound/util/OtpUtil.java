package com.lostfound.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class OtpUtil {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
}
