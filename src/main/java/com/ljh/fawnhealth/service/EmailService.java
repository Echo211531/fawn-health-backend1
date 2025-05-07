package com.ljh.fawnhealth.service;

public interface EmailService {
    void sendVerificationCode(String email, String emailCode);
}
