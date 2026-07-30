# Backend Engineering Guidelines

These rules are mandatory for all generated backend code.

## Code Quality

- Write clean, readable, and maintainable code.
- Follow Clean Code, SOLID, and Separation of Concerns.
- Prefer simplicity over unnecessary abstraction or clever solutions.
- Use meaningful and intention-revealing names for classes, methods, variables, and packages.
- Keep classes and methods focused on a single responsibility.
- Avoid duplicated logic (DRY), but do not over-engineer.

## Architecture

- Respect the existing project architecture and coding conventions.
- Keep Controller, Service, Repository, DTO, Entity, and Configuration responsibilities clearly separated.
- Place business logic only in the Service layer.
- Repository should only handle data access.
- Reuse existing components and patterns whenever possible instead of creating new ones.

## Extensibility

- Never hardcode business values, URLs, credentials, timeout values, file paths, or magic numbers.
- Use configuration files, constants, enums, or dedicated abstractions instead.
- Design solutions that are easy to extend and require minimal modification when new requirements are added.

## Performance & Security

- Avoid unnecessary database queries, duplicated computations, and inefficient algorithms.
- Validate all external input.
- Never expose sensitive information in code or logs.

## Code Changes

When modifying existing code:

- Explain why the change is necessary.
- Minimize the scope of changes.
- Preserve existing behavior unless the requirement explicitly changes it.
- Reuse existing code before creating new implementations.

## Engineering Mindset

- Think like a senior backend engineer, not a code generator.
- Choose the simplest solution that satisfies the current requirements.
- Do not introduce unnecessary design patterns or abstractions.
- Do not invent business rules or assumptions. Ask for clarification when requirements are ambiguous.
- Before implementing new code, understand and follow the existing implementation style of the project.

## Teaching Mode

After every implementation, explain:

1. What was implemented.
2. Why this approach was chosen.
3. Which design principles or design patterns were applied (if any).
4. The trade-offs compared with other possible approaches.
5. How the solution can be extended or maintained in the future.
6. Any assumptions made during implementation.

Focus on explaining the engineering reasoning behind the solution, not just describing what the code does.

## Self Review

Before returning any code, verify that it is:

- Readable
- Maintainable
- Extensible
- Testable
- Free of duplicated logic
- Free of hardcoded values
- Consistent with the existing project architecture