package authentication;

import data.repository.Repositories;
import data.repository.StoredCredential;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import model.Validation;
import model.user.Admin;
import model.user.Role;
import model.user.Student;
import model.user.Tutor;
import model.user.User;
import util.OperationLog;

/** Credential rules and account flows, independent of JavaFX. */
public final class AuthenticationService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final Repositories data;
    private final OperationLog log;
    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    public AuthenticationService(Repositories data, OperationLog log) {
        this.data = Objects.requireNonNull(data, "data");
        this.log = Objects.requireNonNull(log, "log");
    }

    public boolean initialAdminSetupRequired() {
        return !data.authentication().bootstrapComplete();
    }

    public Admin setupInitialAdmin(String name, String email, char[] password, char[] confirmation) {
        UUID id = UUID.randomUUID();
        try {
            return execute("auth.bootstrap", null, id, () -> {
                validatePasswordPair(password, confirmation);
                String hash = hasher.hash(password);
                Admin admin = new Admin(id, name, email, true);
                return data.lifecycle().withExclusiveAccess(
                        () -> data.authentication().createInitialAdmin(admin, hash));
            });
        } finally { clear(password, confirmation); }
    }

    /** Public registration deliberately has no role parameter. */
    public AuthenticatedUser registerStudent(String name, String email, char[] password, char[] confirmation) {
        UUID id = UUID.randomUUID();
        try {
            return execute("auth.register", null, id, () -> {
                validatePasswordPair(password, confirmation);
                String hash = hasher.hash(password);
                Student student = new Student(id, name, email, true);
                data.authentication().createAccount(student, hash, false);
                return new AuthenticatedUser(student, false);
            });
        } finally { clear(password, confirmation); }
    }

    public AuthenticatedUser login(String email, char[] password) {
        AuthenticatedUser authenticated;
        try {
            authenticated = authenticate(email, password);
        } catch (RuntimeException failure) {
            log.record("auth.login", null, null, failure);
            throw failure;
        } finally { clear(password); }
        log.record("auth.login", null, authenticated.user().id(), null);
        return authenticated;
    }

    private AuthenticatedUser authenticate(String email, char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH
                || password.length > MAX_PASSWORD_LENGTH) { throw new AuthenticationException(); }
        final String normalizedEmail;
        try { normalizedEmail = Validation.email(email); }
        catch (IllegalArgumentException invalidEmail) { throw new AuthenticationException(); }
        StoredCredential credential = data.authentication().findCredentialByEmail(normalizedEmail)
                .orElseThrow(AuthenticationException::new);
        boolean matches = hasher.verify(credential.encodedHash(), password);
        if (!matches || !credential.user().isActive()) { throw new AuthenticationException(); }
        return new AuthenticatedUser(credential.user(), credential.mustChangePassword());
    }

    public AuthenticatedUser changePassword(UUID userId, char[] currentPassword, char[] newPassword,
                                            char[] confirmation) {
        try {
            return execute("auth.password.change", userId, userId,
                    () -> data.lifecycle().withExclusiveAccess(() -> {
                User user = data.users().findById(Objects.requireNonNull(userId, "userId"))
                        .filter(User::isActive).orElseThrow(AuthenticationException::new);
                StoredCredential credential = data.authentication().findCredentialByEmail(user.email())
                        .orElseThrow(AuthenticationException::new);
                if (currentPassword == null || !hasher.verify(credential.encodedHash(), currentPassword)) {
                    throw new AuthenticationException();
                }
                validatePasswordPair(newPassword, confirmation);
                data.authentication().updateCredential(userId, hasher.hash(newPassword), false);
                return new AuthenticatedUser(user, false);
            }));
        } finally { clear(currentPassword, newPassword, confirmation); }
    }

    public User provisionAccount(UUID adminId, Role role, String name, String email,
                                 char[] temporaryPassword) {
        UUID id = UUID.randomUUID();
        try {
            return execute("auth.account.provision", adminId, id, () -> {
                requireActiveAdmin(adminId);
                if (role == null) { throw new IllegalArgumentException("Select an account role"); }
                validatePassword(temporaryPassword);
                String hash = hasher.hash(temporaryPassword);
                User account = switch (role) {
                    case STUDENT -> new Student(id, name, email, true);
                    case TUTOR -> new Tutor(id, name, email, true);
                    case ADMIN -> new Admin(id, name, email, true);
                };
                return data.lifecycle().withExclusiveAccess(() -> {
                    requireActiveAdmin(adminId);
                    return data.authentication().createAccount(account, hash, true);
                });
            });
        } finally { clear(temporaryPassword); }
    }

    public void resetPassword(UUID adminId, UUID targetId, char[] temporaryPassword) {
        try {
            execute("auth.password.reset", adminId, targetId, () -> {
                requireActiveAdmin(adminId);
                validatePassword(temporaryPassword);
                String hash = hasher.hash(temporaryPassword);
                return data.lifecycle().withExclusiveAccess(() -> {
                    requireActiveAdmin(adminId);
                    data.users().findById(Objects.requireNonNull(targetId, "targetId"))
                            .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));
                    data.authentication().updateCredential(targetId, hash, true);
                    return null;
                });
            });
        } finally { clear(temporaryPassword); }
    }

    private void requireActiveAdmin(UUID adminId) {
        User admin = data.users().findById(Objects.requireNonNull(adminId, "adminId"))
                .filter(user -> user.role() == Role.ADMIN && user.isActive())
                .orElseThrow(() -> new SecurityException("An active admin account is required."));
        data.authentication().findCredentialByEmail(admin.email())
                .filter(credential -> !credential.mustChangePassword())
                .orElseThrow(() -> new SecurityException("An authenticated admin account is required."));
    }

    private static void validatePasswordPair(char[] password, char[] confirmation) {
        validatePassword(password);
        if (confirmation == null || !Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH
                || password.length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be between 8 and 128 characters");
        }
    }

    private static void clear(char[]... passwords) {
        for (char[] password : passwords) { if (password != null) { Arrays.fill(password, '\0'); } }
    }

    private <T> T execute(String operation, UUID actorId, UUID entityId, Supplier<T> action) {
        try {
            T result = action.get();
            log.record(operation, actorId, entityId, null);
            return result;
        } catch (RuntimeException failure) {
            log.record(operation, actorId, entityId, failure);
            throw failure;
        }
    }
}
