# JJazzLab-X

JJazzLab-X is a Midi-based framework dedicated to backing tracks generation -some people talk about "play-along songs" or “auto-accompaniment applications”. You type in chord symbols, select a rhythm (style), then the application generates a complete backing track with drums, bass, guitar, piano, strings, etc. 

The objective is to develop a jam buddy able to quickly generate intelligent and interesting backing tracks: realistic and non-boring backing tracks which you can easily adjust to a specific song.

*The JJazzLab-X UI:* 
![JJazzLab-X screenshot](https://github.com/jjazzboss/JJazzLab-X/blob/master/Graphics/JJazzLab3.0Full.png)

## Example

To see the JJazzLab-X capabilities download and try the JJazzLab application at [www.jjazzlab.com](https://www.jjazzlab.com), it's a JJazzLab-X distribution which bundles the YamJJazz music generation engine based on Yamaha style files. You can also check out the demo videos: [JJazzLab YouTube channel](https://www.youtube.com/channel/UC0L3SwjY6bhTj6jsbOYzzAw).

## Architecture

![JJazzLab-X architecture](https://github.com/jjazzboss/JJazzLab-X/blob/master/Graphics/JJazzLab-X-architecture.jpg)

## Develop your own music generation engine without hassle

Thanks to JJazzLab-X developers can save a huge amount of work by only focusing on their music generation engine. Out of the box, the JJazzLab-X framework provides all the infrastructure, all the “plumbing” that, before, every developer had to write themselves. 

JJazzLab-X can host any number of music generation engines as plugins. What happens when you load a song file and press the Play button?

1. The framework shows the song in the editors
2. The framework sends Midi messages to initialize the connected Midi sound device
3. When user press Play, the framework sends the song data to the music generation engine
4. The music engine uses the song data to generate the Midi data for the backing tracks
5. The framework retrieves the Midi data and plays it

## Easily add new features

JJazzLab-X is based on the [Netbeans Platform](https://netbeans.org/features/platform/features.html) (now hosted by the Apache foundation). It provides a reliable and extensible application architecture.

The Netbeans Platform turns JJazzLab-X into a pluggable application where plugins can be installed or deactivated at runtime. Plugins can easily add/alter features and insert UI elements such as menu items.

For example suppose that you work on reharmonization algorithms (e.g. replace | A7 | D7M | by | Em7 A7 | D7M). It's easy to add a feature which propose possible reharmonizations when user selects multiple chord symbols. You'll just create a new action class which implements the algorithm on the current chord symbols selection, and "connect" (via annotations, no code required) this action to a new menu item in the Chord Symbol popup menu.

## Installation

If you're looking for a binary distribution visit [www.jjazzlab.com](https://www.jjazzlab.com).

To build from source code see the Developer's guide below.

## License

Lesser GPL v3 (LGPL v3), see LICENCE file.

## User guide

[https://jjazzlab.gitbook.io/user-guide/](https://jjazzlab.gitbook.io/user-guide)

## Developer's guide 

[https://jjazzlab.gitbook.io/developer-guide/](https://jjazzlab.gitbook.io/developer-guide/)

## Contact

For more information use the Contact page at [www.jjazzlab.com](https://www.jjazzlab.com)

