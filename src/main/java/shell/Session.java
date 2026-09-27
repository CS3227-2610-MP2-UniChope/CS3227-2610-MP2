package shell;

import authentication.AuthenticatedUser;
import data.repository.UserRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import model.user.User;

/** Verified session state; re-reads users so deactivation invalidates an open session. */
public final class Session {
    private final UserRepository users;
    private UUID userId;

    public Session(UserRepository users) { this.users = Objects.requireNonNull(users, "users"); }

    public User signIn(AuthenticatedUser authenticated) {
        signOut();
        Objects.requireNonNull(authenticated, "authenticated");
        if (authenticated.mustChangePassword()) {
            throw new IllegalArgumentException("Change the temporary password before opening a workspace");
        }
        User user = users.findById(authenticated.user().id()).filter(User::isActive)
                .filter(authenticated.user()::equals)
                .orElseThrow(() -> new IllegalArgumentException("Sign in again to continue"));
        userId = user.id();
        return user;
    }

    public Optional<User> currentUser() {
        return userId == null ? Optional.empty() : users.findById(userId).filter(User::isActive);
    }

    public void signOut() { userId = null; }
}
