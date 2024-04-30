# YamJJazz plugin

This plugin provides 2 [RhythmProviders](../../core/Rhythm/src/main/java/org/jjazz/rhythm/spi/RhythmProvider.java) instances:

- **YamJJazz standard styles**: generate backing track from a standard Yamaha style file (.prs, .sty, .sst, .bcs)
- **YamJJazz extended styles**: generate backing track from a "JJazzLab extended" Yamaha style file (.yjz + Yamaha style file)

See the [online documentation](https://jjazzlab.gitbook.io/user-guide/rhythm-engines/yamjjazz-rhythm-engine) for more information.

The plugin embeds about 20 default style files. 

This plugin is part of the [JJazzLab](https://www.jjazzlab.org) application.

## Use the plugin

The plugin can be used with the [JJazzLab Toolkit](https://github.com/jjazzboss/JJazzLabToolkit).

The plugin follows the same versioning than the JJazzLab application: a new version of the plugin is released when a new version of JJazzLab is released.

### With Maven
```
<dependency>
    <groupId>org.jjazzlab.plugins</groupId>
    <artifactId>yamjjazz</artifactId>
    <version>4.1.0</version>
</dependency>
```

### With Gradle
```
compile 'org.jjazzlab.plugins:yamjjazz:4.1.0'
```
