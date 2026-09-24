package data.memory;

import data.repository.Repositories;

public final class InMemoryRepositories {
    private InMemoryRepositories() { }

    /** Every call creates isolated, empty storage. */
    public static Repositories create() {
        Object lock = new Object();
        InMemoryUserRepository users = new InMemoryUserRepository(lock);
        InMemoryModuleRepository modules = new InMemoryModuleRepository(lock);
        InMemorySlotRepository slots = new InMemorySlotRepository(lock);
        InMemoryBookingRepository bookings = new InMemoryBookingRepository(lock);
        return new Repositories(users, modules, slots, bookings,
                new InMemoryConsultationLifecycle(slots, bookings, lock));
    }
}
