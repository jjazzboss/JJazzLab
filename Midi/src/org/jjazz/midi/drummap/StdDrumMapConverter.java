package org.jjazz.midi.drummap;

import org.jjazz.midi.DrumMap;
import org.openide.util.lookup.ServiceProvider;

/**
 * Note mapping between GSGM2/XG/GM DrumMaps.
 */
@ServiceProvider(service = DrumMapConverter.class)
public class StdDrumMapConverter implements DrumMapConverter
{

    @Override
    public String getConverterId()
    {
        return "StdMapper";
    }

    @Override
    public boolean acceptSrcDrumMap(DrumMap map)
    {
        return map == DrumMapXG.getInstance() || map == DrumMapGSGM2.getInstance() || map == DrumMapGM.getInstance();
    }

    @Override
    public boolean acceptDestDrumMap(DrumMap map)
    {
        return acceptSrcDrumMap(map);
    }

    @Override
    public int mapNote(DrumMap srcDrMap, int srcPitch, DrumMap destMap)
    {
        if (srcPitch < 0 || srcPitch > 127 || !acceptSrcDrumMap(srcDrMap) || !acceptDestDrumMap(destMap) | srcDrMap.getNoteName(srcPitch) == null)
        {
            throw new IllegalArgumentException("srcPitch=" + srcPitch + " srcDrMap=" + srcDrMap);
        }

        if (true)
        {
            throw new UnsupportedOperationException("To be implemented");
        }

        if (srcDrMap == destMap)
        {
            return srcPitch;
        }

        int destPitch = -1;
        if (srcDrMap == DrumMapGM.getInstance())
        {
            // Easy: GM is included in other DrumMaps
            destPitch = srcPitch;
        } else if (srcDrMap == DrumMapGSGM2.getInstance())
        {
            if (destMap == DrumMapGM.getInstance())
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
            if (destMap == DrumMapGM.getInstance())
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
