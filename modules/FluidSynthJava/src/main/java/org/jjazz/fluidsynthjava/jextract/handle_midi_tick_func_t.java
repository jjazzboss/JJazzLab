// Generated by jextract

package org.jjazz.fluidsynthjava.jextract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
public interface handle_midi_tick_func_t {

    int apply(jdk.incubator.foreign.MemoryAddress x0, int x1);
    static MemoryAddress allocate(handle_midi_tick_func_t fi) {
        return RuntimeHelper.upcallStub(handle_midi_tick_func_t.class, fi, constants$48.handle_midi_tick_func_t$FUNC, "(Ljdk/incubator/foreign/MemoryAddress;I)I");
    }
    static MemoryAddress allocate(handle_midi_tick_func_t fi, ResourceScope scope) {
        return RuntimeHelper.upcallStub(handle_midi_tick_func_t.class, fi, constants$48.handle_midi_tick_func_t$FUNC, "(Ljdk/incubator/foreign/MemoryAddress;I)I", scope);
    }
    static handle_midi_tick_func_t ofAddress(MemoryAddress addr) {
        return (jdk.incubator.foreign.MemoryAddress x0, int x1) -> {
            try {
                return (int)constants$49.handle_midi_tick_func_t$MH.invokeExact((Addressable)addr, x0, x1);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


