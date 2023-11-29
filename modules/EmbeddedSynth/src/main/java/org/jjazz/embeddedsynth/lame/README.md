https://github.com/nwaldispuehl/java-lame


# Java LAME
This java port of LAME 3.98.4 was created by Ken Händel for his 'jump3r - Java Unofficial MP3 EncodeR' project:
http://sourceforge.net/projects/jsidplay2/

Original sources by the authors of LAME: http://www.sourceforge.net/projects/lame

The code is - as the original - licensed under the LGPL (see LICENSE).

## How to build 

To create a JAR file, you may start the gradle build process with the included gradle wrapper:

    $ ./gradlew jar
    
The resulting library is then to be found in the following directory:

    ./build/libs/
    
You can find an already built JAR file in the releases: https://github.com/nwaldispuehl/java-lame/releases

## How to publish artifact to local Maven repository for the use in Maven projects

To store the artifact in the local Maven repository (e.g. `~/.m2/repository/`) use this task:

    $ ./gradlew publishToMavenLocal

It is then present at the expected location, e.g.:

    $ ls ~/.m2/repository/net/sourceforge/lame/lame/3.98.4    
    lame-3.98.4.jar  lame-3.98.4.module  lame-3.98.4.pom

and can be used in local Maven projects with this signature:

    <dependency>
        <groupId>net.sourceforge.lame</groupId>
        <artifactId>lame</artifactId>
        <version>3.98.4</version>
    </dependency>

## How to publish artifact to Maven Central

If you intend to publish the artifact to Maven Central (https://search.maven.org/) please first adapt the `groupId` in the `build.gradle` file to your own domain / projectname.
Consult https://docs.gradle.org/current/userguide/publishing_maven.html for how to publish to a Maven repository.

## How to run

After having created a JAR file, you certainly can run it as a command line application:

    $ cd /build/libs
    $ java -jar net.sourceforge.lame-3.98.4.jar

## How to run the test

To see the creation of a MP3 file in action one can run the test class `LameEncoderTest.java`:

    $ ./gradlew check
    
It takes the `src/test/resources/test.wav` file as input and writes the converted data into `build/test.mp3`.


## How to use Java LAME in a project?

### WAV/PCM to MP3

To convert a WAV/PCM byte array to an MP3 byte array, you may use Ken Händels ```LameEncoder``` which offers the 
following convenience method for converting chunks of such byte buffer:

```
LameEncoder#encodeBuffer(final byte[] pcm, final int pcmOffset, final int pcmLength, final byte[] encoded)
```

A sample of its use can be found in the `LameEncoderTest.java`:
https://github.com/nwaldispuehl/java-lame/blob/master/src/test/java/net/sourceforge/lame/lowlevel/LameEncoderTest.java

### MP3 to WAV/PCM

Analog for decoding; the following method allows for easy conversion of a MP3 file into a PCM byte array:

```
LameDecoder#decode(final ByteBuffer sampleBuffer)
```

A sample of its use can be found in the `LameDecoderTest.java`:
https://github.com/nwaldispuehl/java-lame/blob/master/src/test/java/net/sourceforge/lame/lowlevel/LameDecoderTest.java


### Credits

Test sound 'Jingle004' (CC BY-NC 3.0) by 'cydon': https://freesound.org/people/cydon/sounds/133054/


