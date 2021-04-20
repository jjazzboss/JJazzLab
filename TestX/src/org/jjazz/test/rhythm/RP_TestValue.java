package org.jjazz.test.rhythm;

import java.beans.PropertyChangeSupport;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 *
 */
public class RP_TestValue
{

    public static final String PROP_BASSDRUMOFFSET = "bassDrumOffset";
    public static final String PROP_SNAREOFFSET = "snareOffset";
    public static final String PROP_HIHATOFFSET = "hiHatOffset";

    private int bassDrumOffset;
    private int snareOffset;
    private int hiHatOffset;
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(RP_TestValue.class.getSimpleName());

    public RP_TestValue()
    {
        reset();
    }

    public void reset()
    {
        bassDrumOffset = 0;
        snareOffset = 0;
        hiHatOffset = 0;
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

    public String toDescriptionString()
    {
        return "BD=" + bassDrumOffset + " SN=" + snareOffset + " HH=" + hiHatOffset;
    }

    /**
     * Save the specified object state as a string.
     *
     * @return
     * @see loadFromString()
     */
    static public String saveAsString(RP_TestValue v)
    {
        return v.bassDrumOffset + "," + v.snareOffset + "," + v.hiHatOffset;
    }

    /**
     * Create an object from a string.
     *
     * @param s
     * @see saveAsString()
     */
    static public RP_TestValue loadFromString(String s)
    {
        String[] strs = s.split(",");
        RP_TestValue v = null;
        if (strs.length == 3)
        {
            try
            {
                v = new RP_TestValue();                
                v.bassDrumOffset = Integer.parseInt(strs[0]);
                v.snareOffset = Integer.parseInt(strs[1]);
                v.hiHatOffset = Integer.parseInt(strs[2]);
            } catch (NumberFormatException ex)
            {
                LOGGER.severe("loadFromString() ex="+ex.getMessage());
                v = null;
            }
        }
        return v;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 89 * hash + this.bassDrumOffset;
        hash = 89 * hash + this.snareOffset;
        hash = 89 * hash + this.hiHatOffset;
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
        final RP_TestValue other = (RP_TestValue) obj;
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
