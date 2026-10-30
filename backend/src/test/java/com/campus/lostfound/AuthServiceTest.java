package com.campus.lostfound;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.dao.*;
import com.campus.lostfound.dto.AuthUserDto;
import com.campus.lostfound.dto.RequestCodeRequest;
import com.campus.lostfound.dto.RequestCodeResponse;
import com.campus.lostfound.dto.StaffApprovalRequest;
import com.campus.lostfound.dto.VerifyCodeRequest;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.exception.ValidationException;
import com.campus.lostfound.model.AccountStatus;
import com.campus.lostfound.model.OtpToken;
import com.campus.lostfound.model.User;
import com.campus.lostfound.service.AuthService;
import com.campus.lostfound.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

class AuthServiceTest {

    private File tempDbFile;
    private DatabaseConfig databaseConfig;
    private UserDao userDao;
    private OtpDao otpDao;
    private AuditLogDao auditLogDao;
    private EmailService emailService;
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("test_auth_db_", ".db");
        tempDbFile.deleteOnExit();

        databaseConfig = new DatabaseConfig();
        ReflectionTestUtils.setField(databaseConfig, "databasePath", tempDbFile.getAbsolutePath());

        DatabaseInitializer initializer = new DatabaseInitializer(databaseConfig);
        initializer.initialize();

        userDao = new UserDao(databaseConfig);
        otpDao = new OtpDao(databaseConfig);
        auditLogDao = new AuditLogDao(databaseConfig);
        emailService = Mockito.mock(EmailService.class);
        Mockito.when(emailService.sendVerificationCode(anyString(), anyString())).thenReturn(false);

        authService = new AuthService(userDao, otpDao, emailService, auditLogDao);
        ReflectionTestUtils.setField(authService, "allowedCampusDomains", "");
        ReflectionTestUtils.setField(authService, "initialAdminEmails", "admin@campus.edu");
        ReflectionTestUtils.setField(authService, "devOtpEnabled", true);
    }

    @AfterEach
    void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    void testRequestCodeStoresHashedOtpAndEnforcesRateLimit() throws Exception {
        RequestCodeRequest req = new RequestCodeRequest("student@campus.edu");
        RequestCodeResponse res = authService.requestCode(req);

        // Verify response contains dev OTP and metadata
        assertNotNull(res);
        assertTrue(res.isDevMode());
        assertNotNull(res.getDevOtp());
        assertEquals(6, res.getDevOtp().length(), "OTP must be 6 digits");
        assertFalse(res.isEmailSent());
        assertEquals(60, res.getCooldownSeconds());
        assertEquals(10, res.getExpiryMinutes());

        // Verify OTP is hashed in DB and not plaintext
        Optional<OtpToken> tokenOpt = otpDao.findLatestActiveByEmail("student@campus.edu");
        assertTrue(tokenOpt.isPresent());
        OtpToken token = tokenOpt.get();
        assertEquals(64, token.getOtpHash().length(), "OTP hash must be 64-char SHA-256 hex");
        assertNotNull(token.getSalt(), "Salt must not be null");

        // Requesting another code immediately must be rate-limited (60-second cooldown)
        ValidationException ex = assertThrows(ValidationException.class, () -> authService.requestCode(req));
        assertTrue(ex.getMessage().contains("Please wait at least 60 seconds"));
    }

    @Test
    void testVerifyCodeWithInvalidCodeIncrementsAttempts() throws Exception {
        RequestCodeRequest req = new RequestCodeRequest("test@campus.edu");
        authService.requestCode(req);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();

        VerifyCodeRequest badReq = new VerifyCodeRequest("test@campus.edu", "000000", "STUDENT", "Test User");

        ValidationException ex = assertThrows(ValidationException.class, () ->
                authService.verifyCode(badReq, httpReq)
        );
        assertTrue(ex.getMessage().contains("attempt(s) remaining"));

        OtpToken token = otpDao.findLatestActiveByEmail("test@campus.edu").orElseThrow();
        assertEquals(1, token.getAttempts());
    }

    @Test
    void testStudentRegistrationCreatesActiveAccount() throws Exception {
        String email = "john@campus.edu";
        // Seed known OTP token directly
        String code = "123456";
        String salt = "aabbccddeeff00112233445566778899";
        String hash = (String) ReflectionTestUtils.invokeMethod(authService, "hashOtp", code, salt);

        OtpToken token = new OtpToken(
                "token-1", email, hash, salt, 0, 5,
                java.time.Instant.now().plusSeconds(600), false, java.time.Instant.now()
        );
        otpDao.insert(token);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        VerifyCodeRequest verifyReq = new VerifyCodeRequest(email, code, "STUDENT", "John Doe");

        AuthUserDto dto = authService.verifyCode(verifyReq, httpReq);

        assertNotNull(dto);
        assertEquals("STUDENT", dto.getRole());
        assertEquals(AccountStatus.ACTIVE, dto.getAccountStatus());
        assertEquals("John Doe", dto.getFullName());
        assertTrue(dto.isEmailVerified());

        // Verify single-use: token is consumed
        Optional<OtpToken> activeToken = otpDao.findLatestActiveByEmail(email);
        assertTrue(activeToken.isEmpty(), "OTP token must be consumed upon successful verification");
    }

    @Test
    void testStaffRegistrationCreatesActiveAccountImmediately() throws Exception {
        String email = "staff_jane@campus.edu";
        String code = "654321";
        String salt = "11223344556677889900aabbccddeeff";
        String hash = (String) ReflectionTestUtils.invokeMethod(authService, "hashOtp", code, salt);

        OtpToken token = new OtpToken(
                "token-2", email, hash, salt, 0, 5,
                java.time.Instant.now().plusSeconds(600), false, java.time.Instant.now()
        );
        otpDao.insert(token);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        VerifyCodeRequest verifyReq = new VerifyCodeRequest(email, code, "STAFF", "Jane Staff");

        AuthUserDto dto = authService.verifyCode(verifyReq, httpReq);

        assertNotNull(dto);
        assertEquals("STAFF", dto.getRole());
        assertEquals(AccountStatus.ACTIVE, dto.getAccountStatus());
        assertEquals("Jane Staff", dto.getFullName());
    }

    @Test
    void testInitialAdminBootstrapAccount() throws Exception {
        String email = "admin@campus.edu";
        String code = "111222";
        String salt = "00112233445566778899aabbccddeeff";
        String hash = (String) ReflectionTestUtils.invokeMethod(authService, "hashOtp", code, salt);

        OtpToken token = new OtpToken(
                "token-3", email, hash, salt, 0, 5,
                java.time.Instant.now().plusSeconds(600), false, java.time.Instant.now()
        );
        otpDao.insert(token);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        VerifyCodeRequest verifyReq = new VerifyCodeRequest(email, code, "STUDENT", "Campus Admin");

        AuthUserDto dto = authService.verifyCode(verifyReq, httpReq);

        // Even though browser passed STUDENT, the initial-admin-email match sets role to ADMIN and status to ACTIVE
        assertEquals("ADMIN", dto.getRole());
        assertEquals(AccountStatus.ACTIVE, dto.getAccountStatus());
    }

    @Test
    void testExistingUserRoleCannotBeOverwrittenByClientInput() throws Exception {
        // Create an existing student user
        User existing = new User();
        existing.setId("user-existing-1");
        existing.setCampusEmail("existing@campus.edu");
        existing.setFullName("Existing Student");
        existing.setRole("STUDENT");
        existing.setAccountStatus(AccountStatus.ACTIVE);
        existing.setRequestedRole("STUDENT");
        existing.setEmailVerified(true);
        existing.setCreatedAt(java.time.Instant.now());
        existing.setUpdatedAt(java.time.Instant.now());
        userDao.insert(existing);

        // Login with OTP but request "ADMIN" or "STAFF" in payload
        String code = "999888";
        String salt = "fedcba98765432100123456789abcdef";
        String hash = (String) ReflectionTestUtils.invokeMethod(authService, "hashOtp", code, salt);

        OtpToken token = new OtpToken(
                "token-4", "existing@campus.edu", hash, salt, 0, 5,
                java.time.Instant.now().plusSeconds(600), false, java.time.Instant.now()
        );
        otpDao.insert(token);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        VerifyCodeRequest verifyReq = new VerifyCodeRequest("existing@campus.edu", code, "STAFF", "Hacker Name");

        AuthUserDto dto = authService.verifyCode(verifyReq, httpReq);

        // Role remains STUDENT! Cannot be overridden by browser
        assertEquals("STUDENT", dto.getRole());
        assertEquals("Existing Student", dto.getFullName());
    }

    @Test
    void testStaffApprovalWorkflow() throws Exception {
        User staffUser = new User();
        staffUser.setId("staff-1");
        staffUser.setCampusEmail("librarian@campus.edu");
        staffUser.setFullName("Campus Librarian");
        staffUser.setRole("STAFF");
        staffUser.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        staffUser.setRequestedRole("STAFF");
        staffUser.setEmailVerified(true);
        staffUser.setCreatedAt(java.time.Instant.now());
        staffUser.setUpdatedAt(java.time.Instant.now());
        userDao.insert(staffUser);

        // Approve staff request
        boolean updated = userDao.updateStaffStatus("staff-1", AccountStatus.ACTIVE, "admin@campus.edu");
        assertTrue(updated);

        User approved = userDao.findById("staff-1").orElseThrow();
        assertEquals(AccountStatus.ACTIVE, approved.getAccountStatus());
        assertEquals("STAFF", approved.getRole());
        assertEquals("admin@campus.edu", approved.getApprovedBy());
    }

    @Test
    void testRequestCodeInProductionDoesNotExposeDevOtp() throws Exception {
        ReflectionTestUtils.setField(authService, "devOtpEnabled", false);

        RequestCodeRequest req = new RequestCodeRequest("prod_student@campus.edu");
        RequestCodeResponse res = authService.requestCode(req);

        assertNotNull(res);
        assertFalse(res.isDevMode(), "Dev mode must be false in production");
        assertNull(res.getDevOtp(), "Dev OTP must NEVER be exposed in production mode");
        assertTrue(res.getMessage().contains("Verification code dispatched to prod_student@campus.edu"));
    }

    @Test
    void testVerifyCodeRejectsExpiredOtp() throws Exception {
        String email = "expired@campus.edu";
        String code = "554433";
        String salt = "1234567890abcdef1234567890abcdef";
        String hash = (String) ReflectionTestUtils.invokeMethod(authService, "hashOtp", code, salt);

        // Created in the past, expired 5 minutes ago
        OtpToken token = new OtpToken(
                "token-expired", email, hash, salt, 0, 5,
                Instant.now().minusSeconds(300), false, Instant.now().minusSeconds(900)
        );
        otpDao.insert(token);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        VerifyCodeRequest verifyReq = new VerifyCodeRequest(email, code, "STUDENT", "Expired User");

        ValidationException ex = assertThrows(ValidationException.class, () ->
                authService.verifyCode(verifyReq, httpReq)
        );
        assertTrue(ex.getMessage().contains("expired"), "Must reject expired OTP");
    }

    @Test
    void testVerifyCodeRejectsAlreadyConsumedOtp() throws Exception {
        String email = "consumed@campus.edu";
        String code = "778899";
        String salt = "fedcba0987654321fedcba0987654321";
        String hash = (String) ReflectionTestUtils.invokeMethod(authService, "hashOtp", code, salt);

        OtpToken token = new OtpToken(
                "token-consumed", email, hash, salt, 0, 5,
                Instant.now().plusSeconds(600), false, Instant.now()
        );
        otpDao.insert(token);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        VerifyCodeRequest verifyReq = new VerifyCodeRequest(email, code, "STUDENT", "Single Use User");

        // First verification succeeds
        AuthUserDto dto = authService.verifyCode(verifyReq, httpReq);
        assertNotNull(dto);

        // Second verification with the same OTP fails (single use)
        ValidationException ex = assertThrows(ValidationException.class, () ->
                authService.verifyCode(verifyReq, httpReq)
        );
        assertTrue(ex.getMessage().contains("No active verification code found"));
    }

    @Test
    void testVerifyCodeInvalidatesAfterFiveFailedAttempts() throws Exception {
        String email = "bruteforce@campus.edu";
        String code = "123123";
        String salt = "abcdef1234567890abcdef1234567890";
        String hash = (String) ReflectionTestUtils.invokeMethod(authService, "hashOtp", code, salt);

        OtpToken token = new OtpToken(
                "token-bf", email, hash, salt, 0, 5,
                Instant.now().plusSeconds(600), false, Instant.now()
        );
        otpDao.insert(token);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();

        // 4 failed attempts
        for (int i = 1; i <= 4; i++) {
            VerifyCodeRequest badReq = new VerifyCodeRequest(email, "999999", "STUDENT", "BF User");
            ValidationException ex = assertThrows(ValidationException.class, () ->
                    authService.verifyCode(badReq, httpReq)
            );
            assertTrue(ex.getMessage().contains("attempt(s) remaining"));
        }

        // 5th failed attempt -> exceeds limit and invalidates
        VerifyCodeRequest badReq5 = new VerifyCodeRequest(email, "999999", "STUDENT", "BF User");
        ValidationException ex5 = assertThrows(ValidationException.class, () ->
                authService.verifyCode(badReq5, httpReq)
        );
        assertTrue(ex5.getMessage().contains("Limit exceeded") || ex5.getMessage().contains("invalidated"));

        // Subsequent attempt with even the CORRECT code must fail because token is consumed
        VerifyCodeRequest goodReqAfterInvalidation = new VerifyCodeRequest(email, code, "STUDENT", "BF User");
        assertThrows(ValidationException.class, () ->
                authService.verifyCode(goodReqAfterInvalidation, httpReq)
        );
    }
}
