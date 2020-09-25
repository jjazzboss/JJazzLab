# JJazzLab-X

JJazzLab-X is a Midi-based framework dedicated to backing tracks generation -some people talk about "play-along songs" or “auto-accompaniment applications”. You type in chord symbols, select a rhythm, then the application generates a complete backing track with drums, bass, guitar, piano, strings, etc. 

The ultimate objective is to develop a jam buddy able to quickly generate intelligent and interesting backing tracks: realistic and non-boring backing tracks which you can easily adjust to song specificities.

*The JJazzLab-X UI:* 
![JJazzLab-X screenshot](https://github.com/jjazzboss/JJazzLab-X/blob/master/Graphics/JJazzLabScreenShot.png)

## Example

To see the JJazzLab-X capabilities download and try the JJazzLab application at [www.jjazzlab.com](https://www.jjazzlab.com), it's built upon JJazzLab-X. You can also check out the demo videos: [JJazzLab YouTube channel](https://www.youtube.com/channel/UC0L3SwjY6bhTj6jsbOYzzAw).

## Develop your own music generation engine without hassle

Thanks to JJazzLab-X developers can save a huge amount of work by only focusing on their music generation engine. Out of the box, the JJazzLab-X framework provides all the infrastructure, all the “plumbing” that, before, every developer had to write themselves. 

JJazzLab-X can host any number of music generation engines as plugins. What happens when you load a song file and press the Play button?

1. The framework shows the song in the editors
2. The framework sends Midi messages to initialize the connected Midi sound device
3. When user press Play, the framework sends the song data to the music generation engine
4. The music engine uses the song data to generate the Midi data for the backing tracks
5. The framework retrieves the Midi data and plays it

## Architecture

![JJazzLab-X architecture](https://github.com/jjazzboss/JJazzLab-X/blob/master/Graphics/JJazzLab-X-architecture.jpg)

## Easily add new features

JJazzLab-X is based on the [Netbeans Platform](https://netbeans.org/features/platform/features.html) (now hosted by the Apache foundation). It provides a reliable and extensible application architecture.

The Netbeans Platform turns JJazzLab-X into a pluggable application where plugins can be installed or deactivated at runtime. Plugins can easily add/alter features and insert UI elements such as menu items.

For example suppose that you work on reharmonization algorithms (e.g. replace | A7 | D7M | by | Em7 A7 | D7M). It's easy to add a feature which propose possible reharmonizations when user selects multiple chord symbols. You'll just create a new action class which implements the algorithm on the current chord symbols selection, and "connect" (via annotations, no code required) this action to a new menu item in the Chord Symbol popup menu.

## Installation

If you're looking for a binary distribution visit [www.jjazzlab.com](https://www.jjazzlab.com).

The current version is an Ant-based Netbeans IDE project (Netbeans 12, JDK 11).

From Netbeans IDE:
- menu Team/Git/Clone, enter repository address: https://github.com/jjazzboss/JJazzLab-X.git
- let Netbeans create a new project from the cloned files
- select the created project then right-click Build

**Note**: JJazzLab-X only embeds a very basic music generation for debugging purpose. See [www.jjazzlab.com](https://www.jjazzlab.com) for an example with a more powerful rhythm generation engine.

## License

Lesser GPL v3 (LGPL v3), see LICENCE file.

## Developer's documentation 
See the project's [GitHub Wiki](https://github.com/jjazzboss/JJazzLab-X/wiki).

## Contact

For more information use the Contact page at [www.jjazzlab.com](https://www.jjazzlab.com)

