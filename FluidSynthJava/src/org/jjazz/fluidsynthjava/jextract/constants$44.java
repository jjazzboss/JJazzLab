// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$44 {

    static final FunctionDescriptor fluid_event_modulation$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_event_modulation$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_modulation",
        "(Ljdk/incubator/foreign/MemoryAddress;II)V",
        constants$44.fluid_event_modulation$FUNC, false
    );
    static final FunctionDescriptor fluid_event_sustain$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_event_sustain$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_sustain",
        "(Ljdk/incubator/foreign/MemoryAddress;II)V",
        constants$44.fluid_event_sustain$FUNC, false
    );
    static final FunctionDescriptor fluid_event_pan$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_event_pan$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_pan",
        "(Ljdk/incubator/foreign/MemoryAddress;II)V",
        constants$44.fluid_event_pan$FUNC, false
    );
    static final FunctionDescriptor fluid_event_volume$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_event_volume$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_volume",
        "(Ljdk/incubator/foreign/MemoryAddress;II)V",
        constants$44.fluid_event_volume$FUNC, false
    );
    static final FunctionDescriptor fluid_event_reverb_send$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_event_reverb_send$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_reverb_send",
        "(Ljdk/incubator/foreign/MemoryAddress;II)V",
        constants$44.fluid_event_reverb_send$FUNC, false
    );
    static final FunctionDescriptor fluid_event_chorus_send$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_event_chorus_send$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_event_chorus_send",
        "(Ljdk/incubator/foreign/MemoryAddress;II)V",
        constants$44.fluid_event_chorus_send$FUNC, false
    );
}


