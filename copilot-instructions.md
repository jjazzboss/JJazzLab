## Project Overview

JJazzLab is a musical application which generates MIDI backing tracks. It's a java cross-platform desktop application based on the NetBeans platform. It uses maven.

## Folder Structure

- `/app`: Contains application-level modules (UI, session management, etc.).
- `/core`: Contains the core modules (data model, generic music generation algorithms, playback control, etc.).
- `/plugins`: Contains the plugins modules (rhythm generation engines, fluidsynth integration).

## Libraries

- Guava
- Xstream for serialization

## Best practices

- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible. Use collections from `List.of()`/`Map.of()` for fixed data. Use `Stream.toList()` to create immutable lists.
- **Records**: For classes primarily intended to store data, **Java Records should be used instead of traditional classes**.
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expression to simplify conditional logic and type casting.
- **Type Inference**: Use `var` for local variable declarations to improve readability, but only when the type is explicitly clear from the right-hand side of the expression.
- **javadoc**: required except for trivial cases such as a short method with a self-explanatory name.
- **Comment**: use comments in the code

### Naming Conventions

- `UpperCamelCase` for class and interface names.
- `lowerCamelCase` for method and variable names.
- `UPPER_SNAKE_CASE` for constants.
- `lowercase` for package names.
- Use nouns for classes (`UserService`) and verbs for methods (`getUserById`).

### Common Bug Patterns

- Resource management: Always close resources (files, sockets, streams). Use try-with-resources where possible so resources are closed automatically.
- Equality checks: Compare object equality with `.equals()` or `Objects.equals(...)` rather than `==` for non-primitives; this avoids reference-equality bugs. Use `==` for enum type comparison.
- Redundant casts: Remove unnecessary casts; prefer correct generic typing and let the compiler infer types where possible.


### Common Code Smells

- Parameter count: Keep method parameter lists reasonably short. If a method needs many params, consider grouping into a value object or using the builder pattern.
- Method size: Keep methods focused. Extract helper methods to improve readability and testability.
- Cognitive complexity: Reduce nested conditionals and heavy branching by extracting methods, using polymorphism, or applying the Strategy pattern.
- Duplicated literals: Extract repeated strings and numbers into named constants or enums to reduce errors and ease changes.
- Dead code: Remove unused variables and assignments. They confuse readers and can hide bugs.
- Magic numbers: Replace numeric literals with named constants that explain intent (e.g., MAX_RETRIES).
