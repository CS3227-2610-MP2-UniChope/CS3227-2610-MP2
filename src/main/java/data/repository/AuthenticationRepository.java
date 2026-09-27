package data.repository;

import java.util.Optional;
import java.util.UUID;
import model.user.Admin;
import model.user.User;

/** Persistence boundary for credential state; ordinary user reads do not expose hashes. */
public interface AuthenticationRepository {
    /** Finds a user with stored credentials, including inactive users for caller-side status checks. */
    Optional<StoredCredential> findCredentialByEmail(String email);

    /** Stores a new account and Argon2id-encoded hash atomically. */
    User createAccount(User user, String encodedHash, boolean mustChangePassword);

    /** Replaces an existing user's Argon2id-encoded hash and required-change state atomically. */
    void updateCredential(UUID userId, String encodedHash, boolean mustChangePassword);

    boolean bootstrapComplete();

    /** Creates the active initial Admin and completes bootstrap in one transaction. */
    Admin createInitialAdmin(Admin admin, String encodedHash);
}
