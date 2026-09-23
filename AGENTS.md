# Project memory

Read PROJECT_REQUIREMENTS.md before planning or implementing work. It preserves the full assignment requirements supplied by the user and is the source of truth for this project.

## Mandatory constraints
- New Java desktop app; default Java SE 25. Do not reuse MP1.
- Team of 2 or 3 students; one distinct user role per student, with simple, separate role interfaces. Each student owns all features for their role and a substantial share of team work. Expected individual workload: 1.5 times MP1.
- Production-level reliability, CI/CD, automated testing, and monitoring. Shared components should follow SRP and DRY.
- Focus on basic Agentic SE: customize a single AI agent and verify its skills for implementation and testing. Codex and optionally Claude are allowed.
- Keep accurate summaries of development prompts and AI interactions under logs/ for user verification. Record concrete skill examples and reflection evidence as work proceeds; do not invent experiences.

## Submission requirements
- Public repository: CS3227-2610-MP2, in organization CS3227-2610-MP2-[your-project-name].
- Source code under src/.
- Formal GitHub production release with a build-tool-generated JAR, JavaFX dependencies included, compatible across operating systems.
- docs/UserGuide.md: current features and setup/testing instructions that precisely match the product.
- docs/DeveloperGuide.md: current design, SE process, and acknowledgements of reused ideas/code/documentation.
- GitHub Pages product website.
- docs/Reflections.md: Agentic SE reflections with at least 3 interesting skills explained in detail; see original requirements for guiding questions.
- logs/: verified summaries of prompts and AI interactions.
- Keep master current for grading. Deadline: 29 Sep (Tue), 2pm SGT (2026 in this assignment context); no extensions. Repository must remain accessible with no further changes after submission.
- One member submits organization name and all team members' GitHub usernames via Canvas quiz by 4 Sep (Fri), 2pm SGT.

## Tentative grading
Features 20%; code quality 25%; documentation 10%; SE practices 20%; Agentic SE reflections 25%.

## Current project plan
Read PROJECT_PLAN.md for the user's current plan: a University Consultation Booking System with three roles (Student, Tutor, Admin), assigned to Persons A, B, and C respectively. Build the shared domain model, repository interfaces and in-memory fakes, and JavaFX login/routing shell first. Keep role packages separate.
- Project name: UniChope. Repository: https://github.com/CS3227-2610-MP2-UniChope/CS3227-2610-MP2.
- Follow merged PR #1 naming: packages under src/main/java/ are student, tutor, admin, shell, util, model.user, model.module, model.consultation, data.repository, data.memory, and data.sqlite. Do not add the previously planned com.app prefix. Keep declarations, imports, future tests, and documentation consistent with these names.
- PR #1 is merged; only the directory scaffold exists at that milestone. The remote default branch is main, while the assignment requires master for grading. Resolve that discrepancy before submission.
- Shared foundation is implemented on feature branch codex/shared-foundation, based on origin/main. Read docs/Foundation.md before extending it: immutable UUID-based records, Instant timestamps, four repositories with in-memory implementations, assignment/note APIs in the module/booking repositories, and a shared repository bundle injected into role views. JavaFX login currently selects demo accounts; real authentication is not implemented. Gradle/JUnit setup and 12 passing tests (11 unit tests plus one three-role UI smoke test) verify the foundation on Windows. Role workflows, cross-repository transactions, persistence, CI/CD, and release packaging remain future work. Check GitHub for the current PR/merge status before starting follow-up work.
- Person A: student features, student tests, booking/cancellation end-to-end test, and GitHub Actions CI/CD.
- Person B: tutor features, tutor tests, and real persistence implementing the shared repository interfaces.
- Person C: admin features, admin tests, JUnit setup, and shared logging/monitoring.
- Split documentation and AI interaction logs by person. Follow the detailed role features and ownership in PROJECT_PLAN.md.
- This is a plan, not evidence of implemented features. The assignment requirements remain authoritative.

## Decisions still open
Actual team-member names, persistence choice (JDBC/SQLite or file-based), architecture overview/acknowledgements owner, and GitHub Pages/release owner are not yet specified.
