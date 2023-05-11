# jextract.exe used from Panama project early access JDK 17 binaries    https://jdk.java.net/panama/17/
JEXTRACT="../../jdk-17-panama-for-jextract/bin/jextract.exe"

# FluidSynth header files
FLUID_HEADERS="fluidsynth-headers"

# standard C library headers (need only a small subset for fluidsynth)
C_HEADERS="x86_64w64-mingw32-headers"

# Run jextract on the fluidsynth header files
# --source : create java files, not class files
# -d <destination directory>
# -I <header files path>
# -t <org.my.package>   Package of the created java files
# -dump-includes <file> Dump all symbols into file, then file can be edited and reused as input using @file on the command line
$JEXTRACT @symbols.txt --source -d ../src -t org.javafluidsynth.jextract  -I "$C_HEADERS" "$FLUID_HEADERS"/fluidsynth.h 