package authentication;

import java.util.Objects;
import model.user.User;

/** Verified identity and whether the first authenticated action must be a password change. */
public record AuthenticatedUser(User user, boolean mustChangePassword) {
    public AuthenticatedUser { Objects.requireNonNull(user, "user"); }
}
