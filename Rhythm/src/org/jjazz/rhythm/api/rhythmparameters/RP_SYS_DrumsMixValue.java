package org.jjazz.rhythm.api.rhythmparameters;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.jjazz.midi.api.DrumKit;

/**
 *
 */
public class RP_SYS_DrumsMixValue
{

    private int bassDrumOffset;
    private int snareOffset;
    private int hiHatOffset;
    private int tomsOffset;
    private int crashOffset;
    private int cymbalsOffset;
    private int percOffset;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_DrumsMixValue.class.getSimpleName());

    public RP_SYS_DrumsMixValue()
    {
        this(0, 0, 0, 0, 0, 0, 0);
    }

    public RP_SYS_DrumsMixValue(RP_SYS_DrumsMixValue v)
    {
        this(v.bassDrumOffset, v.snareOffset, v.hiHatOffset, v.tomsOffset, v.crashOffset, v.cymbalsOffset, v.percOffset);
    }

    public RP_SYS_DrumsMixValue(int bassDrumOffset, int snareOffset, int hiHatOffset, int tomsOffset, int crashOffset, int cymbalsOffset, int percOffset)
    {
        this.bassDrumOffset = bassDrumOffset;
        this.snareOffset = snareOffset;
        this.hiHatOffset = hiHatOffset;
        this.tomsOffset = tomsOffset;
        this.crashOffset = crashOffset;
        this.cymbalsOffset = cymbalsOffset;
        this.percOffset = percOffset;
    }


    /**
     * @return the bassDrumOffset
     */
    public int getBassDrumOffset()
    {
        return bassDrumOffset;
    }


    /**
     * @return the snareOffset
     */
    public int getSnareOffset()
    {
        return snareOffset;
    }


    /**
     * @return the hiHatOffset
     */
    public int getHiHatOffset()
    {
        return hiHatOffset;
    }


    /**
     * @return the tomsOffset
     */
    public int getTomsOffset()
    {
        return tomsOffset;
    }


    /**
     * @return the crashOffset
     */
    public int getCrashOffset()
    {
        return crashOffset;
    }


    /**
     * @return the cymbalsOffset
     */
    public int getCymbalsOffset()
    {
        return cymbalsOffset;
    }

    /**
     * @return the percussionOffset
     */
    public int getPercOffset()
    {
        return percOffset;
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
        if (percOffset != 0)
        {
            res.put(DrumKit.Subset.PERCUSSION, percOffset);
        }
        return res;
    }

    public String toDescriptionString()
    {
        return "BD=" + bassDrumOffset + " SN=" + snareOffset + " HH=" + hiHatOffset
                + " TO=" + tomsOffset + " CY=" + cymbalsOffset + " CR=" + crashOffset + " PC=" + percOffset;
    }

    /**
     * Save the specified object state as a string.
     *
     * @param v
     * @return
     * @see loadFromString()
     */
    static public String saveAsString(RP_SYS_DrumsMixValue v)
    {
        return v.bassDrumOffset + "," + v.snareOffset + "," + v.hiHatOffset + "," + v.tomsOffset + "," + v.crashOffset + "," + v.cymbalsOffset + "," + v.percOffset;
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
        RP_SYS_DrumsMixValue res = new RP_SYS_DrumsMixValue();
        if (strs.length == 7)
        {
            try
            {
                res = new RP_SYS_DrumsMixValue(
                        Integer.parseInt(strs[0]),
                        Integer.parseInt(strs[1]),
                        Integer.parseInt(strs[2]),
                        Integer.parseInt(strs[3]),
                        Integer.parseInt(strs[4]),
                        Integer.parseInt(strs[5]),
                        Integer.parseInt(strs[6])
                );
            } catch (NumberFormatException ex)
            {
                LOGGER.severe("loadFromString() ex=" + ex.getMessage());
            }
        }
        return res;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 13 * hash + this.bassDrumOffset;
        hash = 13 * hash + this.snareOffset;
        hash = 13 * hash + this.hiHatOffset;
        hash = 13 * hash + this.tomsOffset;
        hash = 13 * hash + this.crashOffset;
        hash = 13 * hash + this.cymbalsOffset;
        hash = 13 * hash + this.percOffset;
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
        if (this.percOffset != other.percOffset)
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
