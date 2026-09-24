package util;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Structured diagnostics with no user input, exception messages, or note content. */
public final class OperationLog {
    public enum Outcome { SUCCESS, FAILURE }
    public record Event(Instant time, String operation, UUID actorId, UUID entityId,
                        Outcome outcome, String errorType) { }
    public record Metrics(long successes, long failures, long deliveryFailures) { }

    private static final OperationLog APPLICATION = new OperationLog(Clock.systemUTC(), event -> {
        Logger.getLogger("unichope.operations").log(
                event.outcome() == Outcome.SUCCESS ? Level.INFO : Level.WARNING,
                "time={0} operation={1} actorId={2} entityId={3} outcome={4} errorType={5}",
                new Object[] {event.time(), event.operation(), event.actorId(), event.entityId(),
                        event.outcome(), event.errorType()});
    });

    private final Clock clock;
    private final Consumer<Event> sink;
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong deliveryFailures = new AtomicLong();

    public OperationLog(Clock clock, Consumer<Event> sink) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /** Uses existing JDK logging handlers; installs no duplicate/global handlers. */
    public static OperationLog application() { return APPLICATION; }

    public void record(String operation, UUID actorId, UUID entityId, RuntimeException failure) {
        if (operation == null || !operation.matches("[a-z][a-z0-9_.]{0,63}")) {
            throw new IllegalArgumentException("Use a fixed operation identifier");
        }
        Outcome outcome = failure == null ? Outcome.SUCCESS : Outcome.FAILURE;
        if (failure == null) { successes.incrementAndGet(); } else { failures.incrementAndGet(); }
        try {
            sink.accept(new Event(clock.instant(), operation, actorId, entityId, outcome,
                    failure == null ? "" : failure.getClass().getSimpleName()));
        } catch (RuntimeException loggingFailure) {
            // Diagnostics must not turn a successful write into an apparent failure.
            deliveryFailures.incrementAndGet();
        }
    }

    public Metrics metrics() {
        return new Metrics(successes.get(), failures.get(), deliveryFailures.get());
    }
}
