// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$58 {

    static final FunctionDescriptor fluid_player_get_total_ticks$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_player_get_total_ticks$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_player_get_total_ticks",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$58.fluid_player_get_total_ticks$FUNC, false
    );
    static final FunctionDescriptor fluid_player_get_bpm$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_player_get_bpm$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_player_get_bpm",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$58.fluid_player_get_bpm$FUNC, false
    );
    static final FunctionDescriptor fluid_player_get_midi_tempo$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_player_get_midi_tempo$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_player_get_midi_tempo",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$58.fluid_player_get_midi_tempo$FUNC, false
    );
    static final FunctionDescriptor fluid_player_seek$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_INT
    );
    static final MethodHandle fluid_player_seek$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_player_seek",
        "(Ljdk/incubator/foreign/MemoryAddress;I)I",
        constants$58.fluid_player_seek$FUNC, false
    );
    static final FunctionDescriptor fluid_event_callback_t$FUNC = FunctionDescriptor.ofVoid(
        C_INT,
        C_POINTER,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle fluid_event_callback_t$MH = RuntimeHelper.downcallHandle(
        "(ILjdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$58.fluid_event_callback_t$FUNC, false
    );
}

