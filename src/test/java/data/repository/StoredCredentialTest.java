package data.repository;

import java.util.UUID;
import model.user.Student;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class StoredCredentialTest {
    @Test
    void toString_credentialRecord_doesNotExposeEncodedHash() {
        StoredCredential credential = new StoredCredential(
                new Student(UUID.randomUUID(), "Alice", "alice@example.edu", true),
                "private-encoded-hash", false);

        assertFalse(credential.toString().contains("private-encoded-hash"));
    }
}
