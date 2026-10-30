package com.campus.lostfound.service;

import com.campus.lostfound.dao.AuditLogDao;
import com.campus.lostfound.dao.OtpDao;
import com.campus.lostfound.dao.UserDao;
import com.campus.lostfound.dto.AuthUserDto;
import com.campus.lostfound.dto.RequestCodeRequest;
import com.campus.lostfound.dto.RequestCodeResponse;
import com.campus.lostfound.dto.VerifyCodeRequest;
import com.campus.lostfound.exception.ValidationException;
import com.campus.lostfound.model.AccountStatus;
import com.campus.lostfound.model.OtpToken;
import com.campus.lostfound.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    public static final String SESSION_USER_KEY = "LOGGED_IN_USER";

    private final UserDao userDao;
    private final OtpDao otpDao;
    private final EmailService emailService;
    private final AuditLogDao auditLogDao;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.dev-otp-enabled:true}")
    private boolean devOtpEnabled;

    @Value("${app.auth.campus-domains:}")
    private String allowedCampusDomains;

    @Value("${app.auth.initial-admin-emails:admin@campus.edu}")
    private String initialAdminEmails;

    public AuthService(UserDao userDao, OtpDao otpDao, EmailService emailService, AuditLogDao auditLogDao) {
        this.userDao = userDao;
        this.otpDao = otpDao;
        this.emailService = emailService;
        this.auditLogDao = auditLogDao;
    }

    public RequestCodeResponse requestCode(RequestCodeRequest req) throws SQLException {
        String email = req.getEmail().trim().toLowerCase();
        validateEmailDomain(email);

        // Rate limit: 1 request per 60 seconds per email
        Instant oneMinuteAgo = Instant.now().minus(60, ChronoUnit.SECONDS);
        int recentRequests = otpDao.countRecentRequests(email, oneMinuteAgo);
        if (recentRequests > 0) {
            throw new ValidationException("Please wait at least 60 seconds before requesting another verification code.");
        }

        // Generate cryptographically secure 6-digit code
        int codeInt = 100000 + secureRandom.nextInt(900000);
        String codeStr = String.valueOf(codeInt);

        // Generate salt & hash
        byte[] saltBytes = new byte[16];
        secureRandom.nextBytes(saltBytes);
        String saltHex = HexFormat.of().formatHex(saltBytes);
        String hashHex = hashOtp(codeStr, saltHex);

        OtpToken token = new OtpToken(
                UUID.randomUUID().toString(),
                email,
                hashHex,
                saltHex,
                0,
                5,
                Instant.now().plus(10, ChronoUnit.MINUTES),
                false,
                Instant.now()
        );

        otpDao.insert(token);

        // Dispatch via email service (handles real SMTP and local dev logging)
        boolean emailSent = emailService.sendVerificationCode(email, codeStr);

        RequestCodeResponse response = new RequestCodeResponse();
        response.setDevMode(devOtpEnabled);
        response.setEmailSent(emailSent);
        response.setCooldownSeconds(60);
        response.setExpiryMinutes(10);

        if (devOtpEnabled) {
            // Development mode ONLY: expose generated OTP for easy local testing
            response.setDevOtp(codeStr);
            if (emailSent) {
                response.setMessage("Verification code sent to your email and displayed below for development.");
            } else {
                response.setMessage("Development mode active: One-time verification code generated (email delivery disabled).");
            }
        } else {
            // Production mode: NEVER expose OTP in response
            response.setDevOtp(null);
            response.setMessage("Verification code dispatched to " + email + ". Please check your inbox.");
        }

        return response;
    }

    public AuthUserDto verifyCode(VerifyCodeRequest req, HttpServletRequest httpRequest) throws SQLException {
        String email = req.getEmail().trim().toLowerCase();
        String code = req.getCode().trim();

        OtpToken token = otpDao.findLatestActiveByEmail(email)
                .orElseThrow(() -> new ValidationException("No active verification code found for this email. Please request a new code."));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("Verification code has expired. Please request a new code.");
        }

        if (token.getAttempts() >= token.getMaxAttempts()) {
            otpDao.markConsumed(token.getId());
            throw new ValidationException("Too many incorrect attempts. This code has been invalidated. Please request a new code.");
        }

        String computedHash = hashOtp(code, token.getSalt());
        boolean matches = MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                token.getOtpHash().getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            otpDao.incrementAttempts(token.getId());
            int remaining = token.getMaxAttempts() - (token.getAttempts() + 1);
            if (remaining <= 0) {
                otpDao.markConsumed(token.getId());
                throw new ValidationException("Incorrect code. Limit exceeded. Please request a new code.");
            }
            throw new ValidationException("Invalid verification code. " + remaining + " attempt(s) remaining.");
        }

        // Mark OTP consumed immediately
        otpDao.markConsumed(token.getId());

        // Check if user exists
        Optional<User> existingUserOpt = userDao.findByEmail(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Automatically activate staff accounts without requiring admin approval
            if (user.getAccountStatus() == AccountStatus.PENDING_APPROVAL) {
                user.setAccountStatus(AccountStatus.ACTIVE);
                user.setApprovedBy("AUTO_STAFF");
                user.setApprovedAt(Instant.now());
            }
            user.setEmailVerified(true);
            user.setUpdatedAt(Instant.now());
            userDao.update(user);
            logger.info("User {} logged in with existing role [{}] and status [{}]", user.getCampusEmail(), user.getRole(), user.getAccountStatus());
        } else {
            // New user registration
            user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setCampusEmail(email);
            user.setFullName(req.getFullName() != null && !req.getFullName().isBlank() ? req.getFullName().trim() : deriveNameFromEmail(email));
            user.setEmailVerified(true);
            Instant now = Instant.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            // Check if email is on the Initial Admin allowlist
            if (isAllowlistedAdmin(email)) {
                user.setRole("ADMIN");
                user.setAccountStatus(AccountStatus.ACTIVE);
                user.setRequestedRole("ADMIN");
                user.setApprovedBy("SYSTEM_ALLOWLIST");
                user.setApprovedAt(now);
                logger.info("Provisioned bootstrap administrator account for {}", email);
            } else if ("STAFF".equalsIgnoreCase(req.getRole())) {
                user.setRole("STAFF");
                user.setAccountStatus(AccountStatus.ACTIVE);
                user.setRequestedRole("STAFF");
                user.setApprovedBy("AUTO_STAFF");
                user.setApprovedAt(now);
                logger.info("Registered new staff account for {} (Status: ACTIVE, auto-approved)", email);
            } else {
                user.setRole("STUDENT");
                user.setAccountStatus(AccountStatus.ACTIVE);
                user.setRequestedRole("STUDENT");
                user.setApprovedBy("AUTO_STUDENT");
                user.setApprovedAt(now);
                logger.info("Registered new student account for {}", email);
            }

            userDao.insert(user);

            auditLogDao.log(
                    user.getCampusEmail(),
                    "REGISTER_USER",
                    "USER",
                    user.getId(),
                    String.format("User registered as %s (Status: %s)", user.getRole(), user.getAccountStatus())
            );
        }

        // Establish secure server-managed HTTP session
        establishSession(user, httpRequest);

        return AuthUserDto.fromUser(user);
    }

    public void establishSession(User user, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SESSION_USER_KEY, user.getId());

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole()),
                new SimpleGrantedAuthority("STATUS_" + user.getAccountStatus().name())
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    public User getAuthenticatedUser(HttpServletRequest request) throws SQLException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        String userId = (String) session.getAttribute(SESSION_USER_KEY);
        if (userId == null) {
            return null;
        }

        return userDao.findById(userId).orElse(null);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private void validateEmailDomain(String email) {
        if (allowedCampusDomains == null || allowedCampusDomains.isBlank()) {
            return; // No restrictions configured
        }
        String[] domains = allowedCampusDomains.split(",");
        boolean allowed = false;
        for (String d : domains) {
            String clean = d.trim().toLowerCase();
            if (!clean.isEmpty() && (email.endsWith("@" + clean) || email.endsWith("." + clean))) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new ValidationException("Registration is restricted to authorized campus email domains (" + allowedCampusDomains + ").");
        }
    }

    private boolean isAllowlistedAdmin(String email) {
        if (initialAdminEmails == null || initialAdminEmails.isBlank()) return false;
        String[] list = initialAdminEmails.split(",");
        for (String adminEmail : list) {
            if (adminEmail.trim().equalsIgnoreCase(email.trim())) {
                return true;
            }
        }
        return false;
    }

    private String deriveNameFromEmail(String email) {
        int atIdx = email.indexOf('@');
        String prefix = atIdx > 0 ? email.substring(0, atIdx) : email;
        String[] parts = prefix.split("[._\\-]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
            }
        }
        return sb.isEmpty() ? "Campus User" : sb.toString();
    }

    private String hashOtp(String code, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
