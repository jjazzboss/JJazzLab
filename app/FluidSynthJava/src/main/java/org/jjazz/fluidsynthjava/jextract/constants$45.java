// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$45 {

    static final FunctionDescriptor fluid_event_key_pressure$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_INT,
        C_SHORT,
        C_INT
    );
    static final MethodHandle fluid_event_key_pressure$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_key_pressure",
        "(Ljdk/incubator/foreign/MemoryAddress;ISI)V",
        constants$45.fluid_event_key_pressure$FUNC, false
    );
    static final FunctionDescriptor fluid_event_channel_pressure$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_event_channel_pressure$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_channel_pressure",
        "(Ljdk/incubator/foreign/MemoryAddress;II)V",
        constants$45.fluid_event_channel_pressure$FUNC, false
    );
    static final FunctionDescriptor fluid_event_system_reset$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER
    );
    static final MethodHandle fluid_event_system_reset$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_system_reset",
        "(Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$45.fluid_event_system_reset$FUNC, false
    );
    static final FunctionDescriptor fluid_event_unregistering$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER
    );
    static final MethodHandle fluid_event_unregistering$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_unregistering",
        "(Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$45.fluid_event_unregistering$FUNC, false
    );
    static final FunctionDescriptor fluid_event_scale$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_DOUBLE
    );
    static final MethodHandle fluid_event_scale$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_scale",
        "(Ljdk/incubator/foreign/MemoryAddress;D)V",
        constants$45.fluid_event_scale$FUNC, false
    );
    static final FunctionDescriptor fluid_event_from_midi_event$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle fluid_event_from_midi_event$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_from_midi_event",
        "(Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$45.fluid_event_from_midi_event$FUNC, false
    );
}

