package shell;

import data.repository.UserRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import model.user.User;

/** Demo session state; re-reads users so deactivation invalidates an open session. */
public final class Session {
    private final UserRepository users;
    private UUID userId;

    public Session(UserRepository users) { this.users = Objects.requireNonNull(users, "users"); }

    public User signIn(UUID id) {
        signOut();
        User user = users.findById(id).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Select an active account"));
        userId = user.id();
        return user;
    }

    public Optional<User> currentUser() {
        return userId == null ? Optional.empty() : users.findById(userId).filter(User::isActive);
    }

    public void signOut() { userId = null; }
}
