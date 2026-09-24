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
    protected final Object lock;

    InMemoryRepository(Function<T, UUID> idOf, Object lock) {
        this.idOf = idOf;
        this.lock = Objects.requireNonNull(lock, "lock");
    }

    @Override
    public T save(T entity) {
        synchronized (lock) {
            Objects.requireNonNull(entity, "entity");
            entities.put(idOf.apply(entity), entity);
            return entity;
        }
    }

    @Override
    public Optional<T> findById(UUID id) {
        synchronized (lock) {
            return Optional.ofNullable(entities.get(Objects.requireNonNull(id, "id")));
        }
    }

    @Override
    public List<T> findAll() {
        synchronized (lock) {
            return List.copyOf(entities.values());
        }
    }
}
