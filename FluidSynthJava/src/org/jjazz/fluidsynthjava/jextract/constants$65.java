// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$65 {

    static final FunctionDescriptor fluid_mod_get_source2$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_mod_get_source2$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_mod_get_source2",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$65.fluid_mod_get_source2$FUNC, false
    );
    static final FunctionDescriptor fluid_mod_get_flags2$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_mod_get_flags2$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_mod_get_flags2",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$65.fluid_mod_get_flags2$FUNC, false
    );
    static final FunctionDescriptor fluid_mod_get_dest$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER
    );
    static final MethodHandle fluid_mod_get_dest$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_mod_get_dest",
        "(Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$65.fluid_mod_get_dest$FUNC, false
    );
    static final FunctionDescriptor fluid_mod_get_amount$FUNC = FunctionDescriptor.of(C_DOUBLE,
        C_POINTER
    );
    static final MethodHandle fluid_mod_get_amount$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_mod_get_amount",
        "(Ljdk/incubator/foreign/MemoryAddress;)D",
        constants$65.fluid_mod_get_amount$FUNC, false
    );
    static final FunctionDescriptor fluid_mod_test_identity$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_POINTER
    );
    static final MethodHandle fluid_mod_test_identity$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_mod_test_identity",
        "(Ljdk/incubator/foreign/MemoryAddress;Ljdk/incubator/foreign/MemoryAddress;)I",
        constants$65.fluid_mod_test_identity$FUNC, false
    );
    static final FunctionDescriptor fluid_mod_has_source$FUNC = FunctionDescriptor.of(C_INT,
        C_POINTER,
        C_INT,
        C_INT
    );
    static final MethodHandle fluid_mod_has_source$MH = RuntimeHelper.downcallHandle(
        fluidsynth_h.LIBRARIES, "fluid_mod_has_source",
        "(Ljdk/incubator/foreign/MemoryAddress;II)I",
        constants$65.fluid_mod_has_source$FUNC, false
    );
}


