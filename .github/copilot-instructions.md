## Project Overview

JJazzLab is a musical application which generates MIDI backing tracks. It's a java cross-platform desktop application based on the NetBeans platform. It uses maven.

## Folder Structure

- `/app`: Contains application-level modules (UI, session management, etc.).
- `/model`: Contains the core data models Song, SongStructure, ChordLeadSheet, MidiMix, Phrase, Harmony.
- `/core`: Contains the core services (generic music generation algorithms, playback control, etc.).
- `/plugins`: Contains the plugins modules (rhythm generation engines, fluidsynth integration).
- Maven executable path is `D:\Progs\netbeans\java\maven\bin\mvn.cmd`


## Best practices

- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible. Use collections from `List.of()`/`Map.of()` for fixed data. Use `Stream.toList()` to create immutable lists.
- **Records**: For classes primarily intended to store data, **Java Records should be used instead of traditional classes**.
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expression to simplify conditional logic and type casting.
- **Type Inference**: Use `var` for local variable declarations to improve readability, but only when the type is explicitly clear from the right-hand side of the expression.
- **Javadoc**: required except for trivial cases such as a short method with a self-explanatory name. Keep it simple. Limit the use of html tags to <p> and <br>.
- **Comment**: use comments in the code
- **Preconditions**: use Objects.requireNonNull(var) and Guava Preconditions methods for other cases.
