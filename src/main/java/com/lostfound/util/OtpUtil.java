package com.lostfound.util;

import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class OtpUtil {
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
