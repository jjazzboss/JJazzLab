package org.jjazz.fluidsynthjava.api;

import com.google.common.base.Preconditions;


/**
 * FluidSynth reverb setting.
 * <p>
 * Name can be null.
 */
public record Reverb(String name, float room, float damp, float width, float level)
        {
    public static final Reverb ZERO_REVERB = new Reverb("Zero", 0, 0, 0, 0.9f);
    public static final Reverb SMALL_ROOM_REVERB = new Reverb("Small room", 0.2f, 0, 0.5f, 0.9f);
    public static final Reverb ROOM_REVERB = new Reverb("Room", 0.42f, 0.3f, 0.5f, 0.8f);
    public static final Reverb HALL_REVERB = new Reverb("Hall", 0.65f, 0.36f, 0.5f, 0.73f);
    public static final Reverb LARGE_HALL_REVERB = new Reverb("Large hall", 0.8f, 0.44f, 0.5f, 0.65f);
    public static final Reverb FLUID_REVERB = new Reverb("Fluid", 0.2f, 0f, 0.5f, 0.9f);
    public static final Reverb DOSBOX_X_REVERB = new Reverb("DosBox_X", 0.61f, 0.23f, 0.76f, 0.57f);
    public static final Reverb JJAZZLAB_REVERB = new Reverb("JJazzLab", 0.61f, 0.6f, 0.76f, 0.57f);

    public Reverb()
    {
        // Values from https://dosbox-x.com/wiki/Guide%3ASetting-up-MIDI-in-DOSBox%E2%80%90X  "carefully optimized for game music, much better than FluidSynth defaults..."
        this(null, JJAZZLAB_REVERB.room, JJAZZLAB_REVERB.damp, JJAZZLAB_REVERB.width, JJAZZLAB_REVERB.level);
    }

    public Reverb    
    {
        if (room < 0 || room > 1
                || damp < 0 || damp > 1
                || width < 0 || width > 100
                || level < 0 || level > 1)
        {
            throw new IllegalArgumentException("room=" + room + " damp=" + damp + " width=" + width + " level=" + level);
        }
    }


    @Override
    public String toString()
    {
        return name();
    }

    /**
     * Save the object as a string.
     *
     * @return
     * @see #loadFromString(java.lang.String)
     */
    public String saveAsString()
    {
        return String.join(";", name, Float.toString(room), Float.toString(damp), Float.toString(width), Float.toString(level));
    }

    /**
     * Get a Reverb instance from the specified string.
     *
     * @param s
     * @return
     * @see #saveAsString()
     */
    static public Reverb loadFromString(String s) throws NumberFormatException
    {
        Preconditions.checkNotNull(s);
        String strs[] = s.split("\\s*;\\s*");
        if (strs.length == 5)
        {
            String n = strs[0];
            float room = Float.parseFloat(strs[1]);
            float damp = Float.parseFloat(strs[2]);
            float width = Float.parseFloat(strs[3]);
            float level = Float.parseFloat(strs[4]);
            return new Reverb(n, room, damp, width, level);
        } else
        {
            throw new NumberFormatException("Invalid Reverb string s=" + s);
        }
    }
}
