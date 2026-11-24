package com.empay.user_management.serviceimpl;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

	public String generateHashedPwd(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	public boolean validatePassword(String rawPassword, String storedHash) {
		return storedHash != null && passwordEncoder.matches(rawPassword, storedHash);
	}
}
