package shell;

import data.repository.Repositories;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import model.module.Module;

/** Named NUS CS curriculum requirements for local demo data. */
public final class DemoData {
    // Named requirements in the NUS BComp (CS) AY2026/27 curriculum.
    // https://www.comp.nus.edu.sg/cug/per-cohort/cs/cs-26-27/
    private static final List<Course> CS_COURSES = List.of(
            new Course("CS1101S", "Programming Methodology"),
            new Course("ES2660", "Communicating in the Information Age"),
            new Course("IS1108", "Digital and AI Ethics"),
            new Course("CS1231S", "Discrete Structures"),
            new Course("CS2030S", "Programming Methodology II"),
            new Course("CS2040S", "Data Structures and Algorithms"),
            new Course("CS2100", "Computer Organisation"),
            new Course("CS2101", "Effective Communication for Computing Professionals"),
            new Course("CS2103T", "Software Engineering"),
            new Course("CS2106", "Introduction to Operating Systems"),
            new Course("CS2109S", "Introduction to AI and Machine Learning"),
            new Course("CS3230", "Design and Analysis of Algorithms"),
            new Course("MA1521", "Calculus for Computing"),
            new Course("MA1522", "Linear Algebra for Computing"),
            new Course("ST2334", "Probability and Statistics"));

    private DemoData() { }

    public static void seed(Repositories repositories) {
        repositories.lifecycle().withExclusiveAccess(() -> {
            Set<String> codes = repositories.modules().findAll().stream()
                    .map(module -> module.code().toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
            for (Course course : CS_COURSES) {
                UUID id = UUID.nameUUIDFromBytes(("nus-cs-2026-27:" + course.code())
                        .getBytes(StandardCharsets.UTF_8));
                if (!codes.contains(course.code()) && repositories.modules().findById(id).isEmpty()) {
                    repositories.modules().save(new Module(id, course.code(), course.name(), true));
                }
            }
            return null;
        });
    }

    private record Course(String code, String name) { }
}
