# FluidSynthEmbeddedSynth plugin

This plugin provides an [EmbeddedSynthProvider](../../core/EmbeddedSynth/src/main/java/org/jjazz/embeddedsynth/spi/EmbeddedSynthProvider.java) instance which
lets JJazzLab use a FluidSynth native instance as output synth.

This plugin is part of the [JJazzLab](https://www.jjazzlab.org) application.

## Use the plugin

The plugin can be used with the [JJazzLab Toolkit](https://github.com/jjazzboss/JJazzLabToolkit).

The plugin follows the same versioning than the JJazzLab application: a new version of the plugin is released when a new version of JJazzLab is released.

### With Maven
```
<dependency>
    <groupId>org.jjazzlab.plugins</groupId>
    <artifactId>fluidsynthembeddedsynth</artifactId>
    <version>4.1.0</version>
</dependency>
```

### With Gradle
```
compile 'org.jjazzlab.plugins:fluidsynthembeddedsynth:4.1.0'
```
