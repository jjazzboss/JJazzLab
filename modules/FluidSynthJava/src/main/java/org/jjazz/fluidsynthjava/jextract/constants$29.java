// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$29 {

    static final FunctionDescriptor fluid_sfloader_load_t$FUNC = FunctionDescriptor.of(C_POINTER,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle fluid_sfloader_load_t$MH = RuntimeHelper.downcallHandle(
        "(Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)Ljdk/incubator/foreign/MemoryAddress;",
        constants$29.fluid_sfloader_load_t$FUNC, false
    );
    static final FunctionDescriptor fluid_sfloader_free_t$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER
    );
    static final MethodHandle fluid_sfloader_free_t$MH = RuntimeHelper.downcallHandle(
        "(Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$29.fluid_sfloader_free_t$FUNC, false
    );
    static final FunctionDescriptor new_fluid_sfloader$FUNC = FunctionDescriptor.of(C_POINTER,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle new_fluid_sfloader$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "new_fluid_sfloader",
        "(Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)Ljdk/incubator/foreign/MemoryAddress;",
        constants$29.new_fluid_sfloader$FUNC, false
    );
    static final FunctionDescriptor delete_fluid_sfloader$FUNC = FunctionDescriptor.ofVoid(
        C_POINTER
    );
    static final MethodHandle delete_fluid_sfloader$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "delete_fluid_sfloader",
        "(Ljdk/incubator/foreign/MemoryAddress;)V",
        constants$29.delete_fluid_sfloader$FUNC, false
    );
}


