package authentication;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/** Argon2id hashing using the approved 19 MiB, two-iteration baseline. */
final class Argon2PasswordHasher {
    private static final int ITERATIONS = 2;
    private static final int MEMORY_KIB = 19 * 1024;
    private static final int PARALLELISM = 1;
    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    String hash(char[] password) {
        return argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, password);
    }

    boolean verify(String encodedHash, char[] password) {
        return argon2.verify(encodedHash, password);
    }
}
