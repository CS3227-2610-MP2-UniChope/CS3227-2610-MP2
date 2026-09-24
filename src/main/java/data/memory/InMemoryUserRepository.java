package data.memory;

import data.repository.UserRepository;
import java.util.Optional;
import model.Validation;
import model.user.User;

public final class InMemoryUserRepository extends InMemoryRepository<User> implements UserRepository {
    public InMemoryUserRepository() { super(User::id); }

    @Override
    public synchronized User save(User user) {
        findByEmail(user.email()).filter(existing -> !existing.id().equals(user.id()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Email already exists"); });
        findById(user.id()).filter(existing -> existing.role() != user.role())
                .ifPresent(existing -> { throw new IllegalArgumentException("Cannot change a user's role"); });
        return super.save(user);
    }

    @Override
    public synchronized Optional<User> findByEmail(String email) {
        String normalized = Validation.text(email, "email");
        return findAll().stream().filter(user -> user.email().equalsIgnoreCase(normalized)).findFirst();
    }
}
