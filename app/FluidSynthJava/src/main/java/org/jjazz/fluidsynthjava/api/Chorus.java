package org.jjazz.fluidsynthjava.api;

import com.google.common.base.Preconditions;

/**
 * FluidSynth chorus setting.
 * <p>
 * name can be null.
 */
public record Chorus(String name, int nr, float speed, float depth, int type, float level)
        {
// PrefChorusPresets=Zero;3;0.1;0.0;0;0.0##JJazzLab;3;0.3;12.0;0;0.78##SlowJJazzLab;2;0.1;11.0;0;0.78##Thick;7;0.3;40.0;0;0.7
    public static final Chorus ZERO_CHORUS = new Chorus("Zero", 0, 0.1f, 0, 0, 0);
    public static final Chorus NORMAL_CHORUS = new Chorus("Normal", 3, 0.3f, 12f, 0, 0.8f);    
    public static final Chorus SLOW_CHORUS = new Chorus("Slow", 3, 0.1f, 12f, 0, 0.8f);    
    public static final Chorus THICK_CHORUS = new Chorus("Thick", 7, 0.3f, 35f, 0, 0.7f);    
    public static final Chorus FLUID_CHORUS = new Chorus("Fluid", 3, 0.3f, 8f, 0, 2f);
    public static final Chorus DOSBOX_X_CHORUS = new Chorus("DosBox_X", 3, 0.3f, 8f, 0, 1.2f);


    public Chorus()
    {
        this(null, NORMAL_CHORUS.nr, NORMAL_CHORUS.speed, NORMAL_CHORUS.depth, NORMAL_CHORUS.type, NORMAL_CHORUS.level);
    }

    public Chorus     
    {
        if (nr < 0 || nr > 99
                || speed < 0.1d || speed > 5
                || depth < 0 || depth > 256
                || type < 0 || type > 1 // 0 = Sine, 1 = Triangle
                || level < 0 || level > 10)
        {
            throw new IllegalArgumentException("nr=" + nr + " speed=" + speed + " depth=" + depth + " type=" + type + " level=" + level);
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
        return String.join(";", name, Integer.toString(nr), Float.toString(speed), Float.toString(depth), Integer.toString(type), Float.toString(level));
    }

    /**
     * Get a Chorus instance from the specified string.
     *
     * @param s
     * @return
     * @see #saveAsString()
     */
    static public Chorus loadFromString(String s) throws NumberFormatException
    {
        Preconditions.checkNotNull(s);
        String strs[] = s.split("\\s*;\\s*");
        if (strs.length == 6)
        {
            String n = strs[0];
            int nr = Integer.parseInt(strs[1]);
            float speed = Float.parseFloat(strs[2]);
            float depth = Float.parseFloat(strs[3]);
            int type = Integer.parseInt(strs[4]);
            float level = Float.parseFloat(strs[5]);
            return new Chorus(n, nr, speed, depth, type, level);
        } else
        {
            throw new NumberFormatException("Invalid Chorus string s=" + s);
        }
    }

}
