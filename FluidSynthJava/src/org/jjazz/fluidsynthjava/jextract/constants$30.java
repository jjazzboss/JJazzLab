// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$30 {

    static final FunctionDescriptor new_fluid_defsfloader$FUNC = FunctionDescriptor.of(C_POINTER,
        C_POINTER
    );
    static final MethodHandle new_fluid_defsfloader$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "new_fluid_defsfloader",
        "(Ljdk/incubator/foreign/MemoryAddress;)Ljdk/incubator/foreign/MemoryAddress;",
        constants$30.new_fluid_defsfloader$FUNC, false
    );
    static final FunctionDescriptor fluid_sfloader_callback_open_t$FUNC = FunctionDescriptor.of(C_POINTER,
        C_POINTER
    );
    static final MethodHandle fluid_sfloader_callback_open_t$MH = RuntimeHelper.downcallHandle(
        "(Ljdk/incubator/foreign/MemoryAddress;)Ljdk/incubator/foreign/MemoryAddress;",
        constants$30.fluid_sfloader_callback_open_t$FUNC, false
    );
    static final FunctionDescriptor fluid_sfloader_callback_read_t$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_LONG_LONG,
        C_POINTER
    );
    static final MethodHandle fluid_sfloader_callback_read_t$MH = RuntimeHelper.downcallHandle(
        "(Ljdk/incubator/foreign/MemoryAddress;JLjdk/incubator/foreign/MemoryAddress;)I",
        constants$30.fluid_sfloader_callback_read_t$FUNC, false
    );
    static final FunctionDescriptor fluid_sfloader_callback_seek_t$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_LONG_LONG,
        C_INT
    );
}


