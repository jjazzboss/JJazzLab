// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$41 {

    static final FunctionDescriptor fluid_file_renderer_process_block$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_file_renderer_process_block$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_file_renderer_process_block",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$41.fluid_file_renderer_process_block$FUNC, false
    );
    static final FunctionDescriptor fluid_file_set_encoding_quality$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_DOUBLE
    );
    static final MethodHandle fluid_file_set_encoding_quality$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_file_set_encoding_quality",
        "(Ljdk/incubator/foreign/MemoryAddress;D)I",
        constants$41.fluid_file_set_encoding_quality$FUNC, false
    );
    static final FunctionDescriptor new_fluid_event$FUNC = FunctionDescriptor.of(C_POINTER);
    static final MethodHandle new_fluid_event$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "new_fluid_event",
        "()Ljdk/incubator/foreign/MemoryAddress;",
        constants$41.new_fluid_event$FUNC, false
    );
    static final FunctionDescriptor delete_fluid_event$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER
    );
    static final MethodHandle delete_fluid_event$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "delete_fluid_event",
        "(Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$41.delete_fluid_event$FUNC, false
    );
    static final FunctionDescriptor fluid_event_set_source$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_SHORT
    );
    static final MethodHandle fluid_event_set_source$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_set_source",
        "(Ljdk/incubator/foreign/MemoryAddress;S)V",
        constants$41.fluid_event_set_source$FUNC, false
    );
    static final FunctionDescriptor fluid_event_set_dest$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_SHORT
    );
    static final MethodHandle fluid_event_set_dest$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_set_dest",
        "(Ljdk/incubator/foreign/MemoryAddress;S)V",
        constants$41.fluid_event_set_dest$FUNC, false
    );
}


