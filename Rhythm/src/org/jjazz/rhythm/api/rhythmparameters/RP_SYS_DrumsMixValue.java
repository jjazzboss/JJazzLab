package org.jjazz.rhythm.api.rhythmparameters;

import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.midi.api.DrumKit;

/**
 *
 */
public class RP_SYS_DrumsMixValue
{

    public static final String PROP_BASSDRUMOFFSET = "bassDrumOffset";
    public static final String PROP_SNAREOFFSET = "snareOffset";
    public static final String PROP_HIHATOFFSET = "hiHatOffset";
    public static final String PROP_TOMSOFFSET = "tomsOffset";
    public static final String PROP_CRASHOFFSET = "crashOffset";
    public static final String PROP_CYMBALSOFFSET = "cymbalsOffset";

    private int bassDrumOffset;
    private int snareOffset;
    private int hiHatOffset;
    private int tomsOffset;
    private int crashOffset;
    private int cymbalsOffset;
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_DrumsMixValue.class.getSimpleName());

    public RP_SYS_DrumsMixValue()
    {
        reset();
    }

    public RP_SYS_DrumsMixValue(RP_SYS_DrumsMixValue v)
    {
        bassDrumOffset = v.bassDrumOffset;
        snareOffset = v.snareOffset;
        hiHatOffset = v.hiHatOffset;
        crashOffset = v.crashOffset;
        tomsOffset = v.tomsOffset;
        cymbalsOffset = v.cymbalsOffset;
    }

    public RP_SYS_DrumsMixValue(int bassDrumOffset, int snareOffset, int hiHatOffset, int tomsOffset, int crashOffset, int cymbalsOffset)
    {
        this.bassDrumOffset = bassDrumOffset;
        this.snareOffset = snareOffset;
        this.hiHatOffset = hiHatOffset;
        this.tomsOffset = tomsOffset;
        this.crashOffset = crashOffset;
        this.cymbalsOffset = cymbalsOffset;
    }


    public void reset()
    {
        bassDrumOffset = 0;
        snareOffset = 0;
        hiHatOffset = 0;
        crashOffset = 0;
        tomsOffset = 0;
        cymbalsOffset = 0;
    }

    /**
     * @return the bassDrumOffset
     */
    public int getBassDrumOffset()
    {
        return bassDrumOffset;
    }

    /**
     * @param bassDrumOffset the bassDrumOffset to set
     */
    public void setBassDrumOffset(int bassDrumOffset)
    {
        int oldBassDrumOffset = this.bassDrumOffset;
        this.bassDrumOffset = bassDrumOffset;
        pcs.firePropertyChange(PROP_BASSDRUMOFFSET, oldBassDrumOffset, bassDrumOffset);
    }

    /**
     * @return the snareOffset
     */
    public int getSnareOffset()
    {
        return snareOffset;
    }

    /**
     * @param snareOffset the snareOffset to set
     */
    public void setSnareOffset(int snareOffset)
    {
        int oldSnareOffset = this.snareOffset;
        this.snareOffset = snareOffset;
        pcs.firePropertyChange(PROP_SNAREOFFSET, oldSnareOffset, snareOffset);
    }

    /**
     * @return the hiHatOffset
     */
    public int getHiHatOffset()
    {
        return hiHatOffset;
    }

    /**
     * @param hiHatOffset the hiHatOffset to set
     */
    public void setHiHatOffset(int hiHatOffset)
    {
        int oldHiHatOffset = this.hiHatOffset;
        this.hiHatOffset = hiHatOffset;
        pcs.firePropertyChange(PROP_HIHATOFFSET, oldHiHatOffset, hiHatOffset);
    }

    /**
     * @return the tomsOffset
     */
    public int getTomsOffset()
    {
        return tomsOffset;
    }

    /**
     * @param tomsOffset the tomsOffset to set
     */
    public void setTomsOffset(int tomsOffset)
    {
        int oldTomsOffset = this.tomsOffset;
        this.tomsOffset = tomsOffset;
        pcs.firePropertyChange(PROP_TOMSOFFSET, oldTomsOffset, tomsOffset);
    }

    /**
     * @return the crashOffset
     */
    public int getCrashOffset()
    {
        return crashOffset;
    }

    /**
     * @param crashOffset the crashOffset to set
     */
    public void setCrashOffset(int crashOffset)
    {
        int oldCrashOffset = this.crashOffset;
        this.crashOffset = crashOffset;
        pcs.firePropertyChange(PROP_CRASHOFFSET, oldCrashOffset, crashOffset);
    }

    /**
     * @return the cymbalsOffset
     */
    public int getCymbalsOffset()
    {
        return cymbalsOffset;
    }

    /**
     * @param cymbalsOffset the cymbalsOffset to set
     */
    public void setCymbalsOffset(int cymbalsOffset)
    {
        int oldCymbalsOffset = this.cymbalsOffset;
        this.cymbalsOffset = cymbalsOffset;
        pcs.firePropertyChange(PROP_CYMBALSOFFSET, oldCymbalsOffset, cymbalsOffset);
    }

    /**
     * Get the velocity offsets for each DrumKit.Subset.
     * <p>
     * Only the offsets != 0 are returned.
     *
     * @return
     */
    public Map<DrumKit.Subset, Integer> getMapSubsetOffset()
    {
        var res = new HashMap<DrumKit.Subset, Integer>();
        if (bassDrumOffset != 0)
        {
            res.put(DrumKit.Subset.BASS, bassDrumOffset);
        }
        if (snareOffset != 0)
        {
            res.put(DrumKit.Subset.SNARE, snareOffset);
        }
        if (hiHatOffset != 0)
        {
            res.put(DrumKit.Subset.HI_HAT, hiHatOffset);
        }
        if (tomsOffset != 0)
        {
            res.put(DrumKit.Subset.TOM, tomsOffset);
        }
        if (cymbalsOffset != 0)
        {
            res.put(DrumKit.Subset.CYMBAL, cymbalsOffset);
        }
        if (crashOffset != 0)
        {
            res.put(DrumKit.Subset.CRASH, crashOffset);
        }
        return res;
    }

    public String toDescriptionString()
    {
        return "BD=" + bassDrumOffset + " SN=" + snareOffset + " HH=" + hiHatOffset
                + " TO=" + tomsOffset + " CY=" + cymbalsOffset + " CR=" + crashOffset;
    }

    /**
     * Save the specified object state as a string.
     *
     * @return
     * @see loadFromString()
     */
    static public String saveAsString(RP_SYS_DrumsMixValue v)
    {
        return v.bassDrumOffset + "," + v.snareOffset + "," + v.hiHatOffset + "," + v.tomsOffset + "," + v.crashOffset + "," + v.cymbalsOffset;
    }

    /**
     * Create an object from a string.
     *
     * @param s
     * @return
     * @see saveAsString()
     */
    static public RP_SYS_DrumsMixValue loadFromString(String s)
    {
        String[] strs = s.split(",");
        RP_SYS_DrumsMixValue res = null;
        if (strs.length == 6)
        {
            try
            {
                res = new RP_SYS_DrumsMixValue();
                res.bassDrumOffset = Integer.parseInt(strs[0]);
                res.snareOffset = Integer.parseInt(strs[1]);
                res.hiHatOffset = Integer.parseInt(strs[2]);
                res.tomsOffset = Integer.parseInt(strs[3]);
                res.crashOffset = Integer.parseInt(strs[4]);
                res.cymbalsOffset = Integer.parseInt(strs[5]);
            } catch (NumberFormatException ex)
            {
                LOGGER.severe("loadFromString() ex=" + ex.getMessage());
                res = null;
            }
        }
        return res;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 41 * hash + this.bassDrumOffset;
        hash = 41 * hash + this.snareOffset;
        hash = 41 * hash + this.hiHatOffset;
        hash = 41 * hash + this.tomsOffset;
        hash = 41 * hash + this.crashOffset;
        hash = 41 * hash + this.cymbalsOffset;
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final RP_SYS_DrumsMixValue other = (RP_SYS_DrumsMixValue) obj;
        if (this.bassDrumOffset != other.bassDrumOffset)
        {
            return false;
        }
        if (this.snareOffset != other.snareOffset)
        {
            return false;
        }
        if (this.hiHatOffset != other.hiHatOffset)
        {
            return false;
        }
        if (this.tomsOffset != other.tomsOffset)
        {
            return false;
        }
        if (this.crashOffset != other.crashOffset)
        {
            return false;
        }
        if (this.cymbalsOffset != other.cymbalsOffset)
        {
            return false;
        }
        if (!Objects.equals(this.pcs, other.pcs))
        {
            return false;
        }
        return true;
    }


    @Override
    public String toString()
    {
        return toDescriptionString();
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================    
}
