// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$48 {

    static final FunctionDescriptor fluid_event_get_pitch$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_event_get_pitch$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_get_pitch",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$48.fluid_event_get_pitch$FUNC, false
    );
    static final FunctionDescriptor fluid_event_get_scale$FUNC = FunctionDescriptor.of(C_DOUBLE,
        C_POINTER
    );
    static final MethodHandle fluid_event_get_scale$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_get_scale",
        "(Ljdk/incubator/foreign/MemoryAddress;)D",
        constants$48.fluid_event_get_scale$FUNC, false
    );
    static final FunctionDescriptor fluid_event_get_sfont_id$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_event_get_sfont_id$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_get_sfont_id",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$48.fluid_event_get_sfont_id$FUNC, false
    );
    static final FunctionDescriptor handle_midi_event_func_t$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle handle_midi_event_func_t$MH = RuntimeHelper.downcallHandle(
        "(Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$48.handle_midi_event_func_t$FUNC, false
    );
    static final FunctionDescriptor handle_midi_tick_func_t$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_INT
    );
}


