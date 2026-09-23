package data.memory;

import data.repository.Repository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Shared storage mechanics for test/development fakes; not durable persistence. */
abstract class InMemoryRepository<T> implements Repository<T> {
    private final Map<UUID, T> entities = new LinkedHashMap<>();
    private final Function<T, UUID> idOf;

    InMemoryRepository(Function<T, UUID> idOf) { this.idOf = idOf; }

    @Override
    public synchronized T save(T entity) {
        Objects.requireNonNull(entity, "entity");
        entities.put(idOf.apply(entity), entity);
        return entity;
    }

    @Override
    public synchronized Optional<T> findById(UUID id) {
        return Optional.ofNullable(entities.get(Objects.requireNonNull(id, "id")));
    }

    @Override
    public synchronized List<T> findAll() { return List.copyOf(entities.values()); }
}
