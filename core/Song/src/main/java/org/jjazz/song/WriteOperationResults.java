package org.jjazz.song;

import com.google.common.base.Preconditions;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.song.api.SongPropertyChangeEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;


/**
 * Store a change event and the return value produced by a mutating operation.
 * <p>
 * All parameters can be null. Only clsChangeEvent or sgsChangeEvent or pChangeEvent can be non-null.
 */
public record WriteOperationResults<T>(ClsChangeEvent clsChangeEvent, SgsChangeEvent sgsChangeEvent, SongPropertyChangeEvent pChangeEvent, T returnValue)
        {

    public WriteOperationResults
    {
        Preconditions.checkArgument(clsChangeEvent == null
                || (sgsChangeEvent == null && pChangeEvent == null),
                "clsChangeEvent=%s sgsChangeEvent=%s pChangeEvent=%s", clsChangeEvent, sgsChangeEvent, pChangeEvent);
        Preconditions.checkArgument(sgsChangeEvent == null
                || (clsChangeEvent == null && pChangeEvent == null),
                "clsChangeEvent=%s sgsChangeEvent=%s pChangeEvent=%s", clsChangeEvent, sgsChangeEvent, pChangeEvent);
        Preconditions.checkArgument(pChangeEvent == null
                || (clsChangeEvent == null && sgsChangeEvent == null),
                "clsChangeEvent=%s sgsChangeEvent=%s pChangeEvent=%s", clsChangeEvent, sgsChangeEvent, pChangeEvent);
    }

    // ===================================================================================================================    
    // Helper constructors
    // ===================================================================================================================        
    static public <T> WriteOperationResults<T> of(T returnValue)
    {
        return new WriteOperationResults(null, null, null, returnValue);
    }

    static public <T> WriteOperationResults<T> of(ClsChangeEvent clsChangeEvent, T returnValue)
    {
        return new WriteOperationResults(clsChangeEvent, null, null, returnValue);
    }

    static public <T> WriteOperationResults<T> of(SgsChangeEvent sgsChangeEvent, T returnValue)
    {
        return new WriteOperationResults(null, sgsChangeEvent, null, returnValue);
    }

    static public <T> WriteOperationResults<T> of(SongPropertyChangeEvent pChangeEvent, T returnValue)
    {
        return new WriteOperationResults(null, null, pChangeEvent, returnValue);
    }
}
