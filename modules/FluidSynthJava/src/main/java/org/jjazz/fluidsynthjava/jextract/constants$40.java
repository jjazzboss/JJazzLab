// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$40 {

    static final FunctionDescriptor new_fluid_audio_driver$FUNC = FunctionDescriptor.of(C_POINTER,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle new_fluid_audio_driver$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "new_fluid_audio_driver",
        "(Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)Ljdk/incubator/foreign/MemoryAddress;",
        constants$40.new_fluid_audio_driver$FUNC, false
    );
    static final FunctionDescriptor new_fluid_audio_driver2$FUNC = FunctionDescriptor.of(C_POINTER,
        C_POINTER,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle new_fluid_audio_driver2$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "new_fluid_audio_driver2",
        "(Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)Ljdk/incubator/foreign/MemoryAddress;",
        constants$40.new_fluid_audio_driver2$FUNC, false
    );
    static final FunctionDescriptor delete_fluid_audio_driver$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER
    );
    static final MethodHandle delete_fluid_audio_driver$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "delete_fluid_audio_driver",
        "(Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$40.delete_fluid_audio_driver$FUNC, false
    );
    static final FunctionDescriptor fluid_audio_driver_register$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_audio_driver_register$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_audio_driver_register",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$40.fluid_audio_driver_register$FUNC, false
    );
    static final FunctionDescriptor new_fluid_file_renderer$FUNC = FunctionDescriptor.of(C_POINTER,
        C_POINTER
    );
    static final MethodHandle new_fluid_file_renderer$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "new_fluid_file_renderer",
        "(Ljdk/incubator/foreign/MemoryAddress;)Ljdk/incubator/foreign/MemoryAddress;",
        constants$40.new_fluid_file_renderer$FUNC, false
    );
    static final FunctionDescriptor delete_fluid_file_renderer$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER
    );
    static final MethodHandle delete_fluid_file_renderer$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "delete_fluid_file_renderer",
        "(Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$40.delete_fluid_file_renderer$FUNC, false
    );
}


