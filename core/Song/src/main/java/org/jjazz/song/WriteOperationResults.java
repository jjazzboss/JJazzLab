package org.jjazz.song;

import com.google.common.base.Preconditions;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;


/**
 * Store a change event and the return value produced by a mutating operation.
 * <p>
 * All parameters can be null. If clsChangeEvent is non-null then sgsChangeEvent must be null, and vice-versa.
 */
public record WriteOperationResults<T>(ClsChangeEvent clsChangeEvent, SgsChangeEvent sgsChangeEvent, T returnValue)
        {

    public WriteOperationResults
    {
        Preconditions.checkArgument(clsChangeEvent == null || sgsChangeEvent == null, "clsChangeEvent=%s sgsChangeEvent=%s", clsChangeEvent, sgsChangeEvent);
    }

    // ===================================================================================================================    
    // Helper constructors
    // ===================================================================================================================        
    static public <T> WriteOperationResults<T> of(T returnValue)
    {
        return new WriteOperationResults(null, null, returnValue);
    }

    static public <T> WriteOperationResults<T> of(ClsChangeEvent clsChangeEvent, T returnValue)
    {
        return new WriteOperationResults(clsChangeEvent, null, returnValue);
    }

    static public <T> WriteOperationResults<T> of(SgsChangeEvent sgsChangeEvent, T returnValue)
    {
        return new WriteOperationResults(null, sgsChangeEvent, returnValue);
    }
}
