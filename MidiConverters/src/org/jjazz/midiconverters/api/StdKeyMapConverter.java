package org.jjazz.midiconverters.api;

import org.jjazz.midi.keymap.KeyMapGSGM2;
import org.jjazz.midi.keymap.KeyMapXG_Std;
import org.jjazz.midi.keymap.KeyMapGM;
import org.jjazz.midiconverters.spi.KeyMapConverter;
import org.jjazz.midi.DrumKit;

/**
 * Note mapping between GSGM2/XG/GM DrumMaps.
 */
public class StdKeyMapConverter implements KeyMapConverter
{

    private static StdKeyMapConverter INSTANCE;

    public static StdKeyMapConverter getInstance()
    {
        synchronized (StdKeyMapConverter.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new StdKeyMapConverter();
            }
        }
        return INSTANCE;
    }

    private StdKeyMapConverter()
    {
    }

    @Override
    public String getConverterId()
    {
        return "StdMapper";
    }

    @Override
    public int convertKey(DrumKit srcKit, int srcPitch, DrumKit destKit)
    {
        if (srcPitch < 0 || srcPitch > 127 || srcKit.getKeyMap().getKeyName(srcPitch) == null)
        {
            throw new IllegalArgumentException("srcKit=" + srcKit + " srcPitch=" + srcPitch + " destKit=" + destKit);
        }

        if (!isStandardKeyMap(srcKit) || !isStandardKeyMap(destKit))
        {
            return -1;
        }

        if (true)
        {
            throw new UnsupportedOperationException("To be implemented");
        }

        if (srcKit.equals(destKit))
        {
            return srcPitch;
        }

        DrumKit.KeyMap srcMap = srcKit.getKeyMap();
        DrumKit.KeyMap destMap = destKit.getKeyMap();
        int destPitch = -1;
        if (srcMap == KeyMapGM.getInstance())
        {
            // Easy: GM is included in other DrumMaps
            destPitch = srcPitch;
        } else if (srcMap == KeyMapGSGM2.getInstance())
        {
            if (destMap == KeyMapGM.getInstance())
            {
                // GSGM2 => GM
                switch (srcPitch)
                {

                }
            } else
            {
                // GSGM2 => XG
            }
        } else
        {
            if (destMap == KeyMapGM.getInstance())
            {
                // XG => GM
            } else
            {
                // XG => GSGM2
            }
        }
        return destPitch;
    }

    public boolean isStandardKeyMap(DrumKit kit)
    {
        if (kit == null)
        {
            throw new NullPointerException("kit");
        }
        return kit.getKeyMap() == KeyMapXG_Std.getInstance() || kit.getKeyMap() == KeyMapGSGM2.getInstance() || kit.getKeyMap() == KeyMapGM.getInstance();
    }

}
