package data.repository;

import java.util.Optional;
import model.user.User;

public interface UserRepository extends Repository<User> {
    /** Rejects duplicate emails ignoring case and changes to an existing ID's role. */
    @Override
    User save(User user);

    /** Case-insensitive, trimmed email lookup, including inactive users. */
    Optional<User> findByEmail(String email);
}
