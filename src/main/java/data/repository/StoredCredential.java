package data.repository;

import java.util.Objects;
import model.user.User;

/** Credential material returned only by {@link AuthenticationRepository}. */
public record StoredCredential(User user, String encodedHash, boolean mustChangePassword) {
    public StoredCredential {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(encodedHash, "encodedHash");
    }

    @Override
    public String toString() {
        return "StoredCredential[user=" + user + ", mustChangePassword=" + mustChangePassword + "]";
    }
}
