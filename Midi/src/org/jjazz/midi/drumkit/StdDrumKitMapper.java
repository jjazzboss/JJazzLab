package org.jjazz.midi.drumkit;

import org.openide.util.lookup.ServiceProvider;

/**
 * Note mapping between GSGM2/XG/GM DrumKits.
 */
@ServiceProvider(service = DrumKitMapper.class)
public class StdDrumKitMapper implements DrumKitMapper
{

    @Override
    public String getMapperId()
    {
        return "StdMapper";
    }

    @Override
    public boolean acceptSrcKit(DrumKit kit)
    {
        return kit == DrumKitXG.getInstance() || kit == DrumKitGSGM2.getInstance() || kit == DrumKitGM.getInstance();
    }

    @Override
    public boolean acceptDestKit(DrumKit kit)
    {
        return acceptSrcKit(kit);
    }

    @Override
    public int mapNote(DrumKit srcKit, int srcPitch, DrumKit destKit)
    {
        if (srcPitch < 0 || srcPitch > 127 || !acceptSrcKit(srcKit) || !acceptDestKit(destKit) | srcKit.getNoteName(srcPitch) == null)
        {
            throw new IllegalArgumentException("srcPitch=" + srcPitch + " srcKit=" + srcKit);
        }
        if (srcKit == destKit)
        {
            return srcPitch;
        }

        int destPitch = -1;
        if (srcKit == DrumKitGM.getInstance())
        {
            // Easy: GM is included in other DrumKits
            destPitch = srcPitch;
        } else if (srcKit == DrumKitGSGM2.getInstance())
        {
            if (destKit == DrumKitGM.getInstance())
            {
                // GSGM2 => GM
                switch (srcPitch)
                {
                    case 
                }
            } else
            {
                // GSGM2 => XG
            }
        } else
        {
            if (destKit == DrumKitGM.getInstance())
            {
                // XG => GM
            } else
            {
                // XG => GSGM2
            }
        }
        return destPitch;
    }

}
