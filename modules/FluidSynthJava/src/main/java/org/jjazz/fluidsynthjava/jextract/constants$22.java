// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$22 {

    static final FunctionDescriptor fluid_synth_tuning_iteration_start$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER
    );
    static final MethodHandle fluid_synth_tuning_iteration_start$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_tuning_iteration_start",
        "(Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$22.fluid_synth_tuning_iteration_start$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_tuning_iteration_next$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle fluid_synth_tuning_iteration_next$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_tuning_iteration_next",
        "(Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$22.fluid_synth_tuning_iteration_next$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_tuning_dump$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_INT,
        C_INT,
        C_POINTER,
        C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_synth_tuning_dump$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_tuning_dump",
        "(Ljdk/incubator/foreign/MemoryAddress;IILjdk/incubator/foreign/MemoryAddress;ILjdk/incubator/foreign/MemoryAddress;)I",
        constants$22.fluid_synth_tuning_dump$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_write_s16$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_INT,
        C_POINTER,
        C_INT,
        C_INT,
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_synth_write_s16$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_write_s16",
        "(Ljdk/incubator/foreign/MemoryAddress;ILjdk/incubator/foreign/MemoryAddress;IILjdk/incubator/foreign/MemoryAddress;II)I",
        constants$22.fluid_synth_write_s16$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_write_float$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_INT,
        C_POINTER,
        C_INT,
        C_INT,
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_synth_write_float$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_write_float",
        "(Ljdk/incubator/foreign/MemoryAddress;ILjdk/incubator/foreign/MemoryAddress;IILjdk/incubator/foreign/MemoryAddress;II)I",
        constants$22.fluid_synth_write_float$FUNC, false
    );
    static final FunctionDescriptor fluid_synth_nwrite_float$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_INT,
        C_POINTER,
        C_POINTER,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle fluid_synth_nwrite_float$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_synth_nwrite_float",
        "(Ljdk/incubator/foreign/MemoryAddress;ILjdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$22.fluid_synth_nwrite_float$FUNC, false
    );
}


