package data.repository;

import java.util.Objects;
import java.util.regex.Pattern;

/** Validates the encoded format required by the credential persistence contract. */
public final class CredentialHash {
    private static final Pattern ARGON2ID = Pattern.compile(
            "\\$argon2id\\$v=\\d+\\$m=\\d+,t=\\d+,p=\\d+\\$[A-Za-z0-9+/]+={0,2}\\$[A-Za-z0-9+/]+={0,2}");

    private CredentialHash() { }

    public static String requireArgon2id(String encodedHash) {
        String hash = Objects.requireNonNull(encodedHash, "encodedHash");
        if (!ARGON2ID.matcher(hash).matches()) {
            throw new IllegalArgumentException("encodedHash must be an Argon2id encoded hash");
        }
        return hash;
    }
}
