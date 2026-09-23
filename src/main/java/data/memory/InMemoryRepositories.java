package data.memory;

import data.repository.Repositories;

public final class InMemoryRepositories {
    private InMemoryRepositories() { }

    /** Every call creates isolated, empty storage. */
    public static Repositories create() {
        return new Repositories(new InMemoryUserRepository(), new InMemoryModuleRepository(),
                new InMemorySlotRepository(), new InMemoryBookingRepository());
    }
}
