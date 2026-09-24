package data.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal storage contract. save inserts or replaces by ID; reads return immutable
 * values and unmodifiable snapshots. Missing IDs return Optional.empty().
 * Repositories are not authorization or business-service boundaries. Cross-repository
 * validation and atomic booking/slot transitions belong in the application service
 * and transaction layer, which must be added before implementing booking workflows.
 * There is intentionally no hard-delete operation for historical entities.
 */
public interface Repository<T> {
    T save(T entity);
    Optional<T> findById(UUID id);
    List<T> findAll();
}
