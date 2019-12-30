package org.jjazz.midiconverters;

import org.jjazz.midi.keymap.KeyMapGSGM2;
import org.jjazz.midi.keymap.KeyMapXG;
import org.jjazz.midi.keymap.KeyMapGM;
import org.jjazz.midiconverters.spi.KeyMapConverter;
import org.jjazz.midi.DrumKit;
import org.openide.util.lookup.ServiceProvider;

/**
 * Note mapping between GSGM2/XG/GM DrumMaps.
 */
@ServiceProvider(service = KeyMapConverter.class)
public class StdKeyMapConverter implements KeyMapConverter
{

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

        if (!isStandardDrumKit(srcKit) || !isStandardDrumKit(destKit))
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

    private boolean isStandardDrumKit(DrumKit kit)
    {
        if (kit == null)
        {
            throw new NullPointerException("kit");
        }
        return kit.getKeyMap() == KeyMapXG.getInstance() || kit.getKeyMap() == KeyMapGSGM2.getInstance() || kit.getKeyMap() == KeyMapGM.getInstance();
    }

}
