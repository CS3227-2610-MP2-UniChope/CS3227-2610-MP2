package tutor;

import data.repository.Repositories;
import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import model.module.Module;
import model.module.TutorModule;
import model.user.Tutor;
import util.OperationLog;

final class TutorFixture {
    final Instant now = Instant.parse("2026-09-24T00:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    final Repositories data;
    final Tutor tutor = new Tutor(UUID.randomUUID(), "Ada", "ada@example.edu", true);
    final Module module = new Module(UUID.randomUUID(), "CS3227", "Software Engineering", true);
    final List<OperationLog.Event> events = new ArrayList<>();
    final TutorService service;

    TutorFixture(Path database) {
        data = SqliteRepositories.open(database);
        data.users().save(tutor);
        data.modules().save(module);
        data.modules().assign(new TutorModule(tutor.id(), module.id()));
        service = new TutorService(data, tutor.id(), clock, new OperationLog(clock, events::add));
    }

    TutorService service(UUID actorId) {
        return new TutorService(data, actorId, clock, new OperationLog(clock, events::add));
    }
}
