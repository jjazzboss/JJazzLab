// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$16 {

    static final FunctionDescriptor fluid_synth_set_chorus_type$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_INT
    );
    static final MethodHandle fluid_synth_set_chorus_type$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_set_chorus_type",
        "(Ljdk/incubator/foreign/MemoryAddress;I)I",
        constants$16.fluid_synth_set_chorus_type$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_get_chorus_nr$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_synth_get_chorus_nr$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_get_chorus_nr",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$16.fluid_synth_get_chorus_nr$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_get_chorus_level$FUNC = FunctionDescriptor.of(C_DOUBLE,
        C_POINTER
    );
    static final MethodHandle fluid_synth_get_chorus_level$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_get_chorus_level",
        "(Ljdk/incubator/foreign/MemoryAddress;)D",
        constants$16.fluid_synth_get_chorus_level$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_get_chorus_speed$FUNC = FunctionDescriptor.of(C_DOUBLE,
        C_POINTER
    );
    static final MethodHandle fluid_synth_get_chorus_speed$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_get_chorus_speed",
        "(Ljdk/incubator/foreign/MemoryAddress;)D",
        constants$16.fluid_synth_get_chorus_speed$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_get_chorus_depth$FUNC = FunctionDescriptor.of(C_DOUBLE,
        C_POINTER
    );
    static final MethodHandle fluid_synth_get_chorus_depth$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_get_chorus_depth",
        "(Ljdk/incubator/foreign/MemoryAddress;)D",
        constants$16.fluid_synth_get_chorus_depth$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_get_chorus_type$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_synth_get_chorus_type$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_get_chorus_type",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$16.fluid_synth_get_chorus_type$FUNC, false
    );
}


