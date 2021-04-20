/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3)
 *  as published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 *
 *  Contributor(s):
 */
package org.jjazz.songstructure;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.database.api.UnavailableRhythmException;
import org.jjazz.util.SmallMap;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.ResUtil;
import org.jjazz.rhythm.api.EnumerableParameter;

public class SongPartImpl implements SongPart, Serializable
{

    public static final String NO_NAME = "NoName";
    /**
     * The rhythm of this part.
     */
    private Rhythm rhythm;
    /**
     * Starts at this bar index.
     */
    private int startBarIndex;
    /**
     * The name of this song part.
     */
    private String name;
    /**
     * The length in bars.
     */
    private int nbBars;
    /**
     * Parent section.
     */
    private CLI_Section parentSection;
    /**
     * The value associated to each RhythmParameter.
     */
    private SmallMap<RhythmParameter<?>, Object> mapRpValue = new SmallMap<>();
    /**
     * Our container. Must be transient to avoid circular dependency at deserialization.
     */
    private transient SongStructure container;
    /**
     * The listeners for changes in this model.
     */
    private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongPartImpl.class.getSimpleName());

    /**
     * Create a SongPartImpl with default value for each of the rhythm's RhythmParameters.
     * <p>
     * Name is set to parentSection's name if parentSection not null.
     *
     * @param r
     * @param startBarIndex
     * @param nbBars
     * @param parentSection If not null must be part of sgs
     */
    public SongPartImpl(Rhythm r, int startBarIndex, int nbBars, CLI_Section parentSection)
    {
        if (r == null || startBarIndex < 0 || nbBars < 1)
        {
            throw new IllegalArgumentException( //NOI18N
                    "r=" + r + " startBarIndex=" + startBarIndex + " nbBars=" + nbBars + " parentSection=" + parentSection);
        }
        rhythm = r;
        this.startBarIndex = startBarIndex;
        this.nbBars = nbBars;
        name = parentSection == null ? NO_NAME : parentSection.getData().getName();
        this.parentSection = parentSection;


        // Associate a default value to each RhythmParameter                    
        for (RhythmParameter<?> rp : r.getRhythmParameters())
        {
            mapRpValue.putValue(rp, rp.getDefaultValue());
        }
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        if (name == null || name.length() == 0)
        {
            throw new IllegalArgumentException("name=" + name);   //NOI18N
        }
        String oldName = this.name;
        if (!name.equals(this.name))
        {
            // LOGGER.log(Level.FINE, "setName getName()=" + getName() + " name=" + name);
            this.name = name;
            pcs.firePropertyChange(PROPERTY_NAME, oldName, name);
        }
    }

    @Override
    public SongStructure getContainer()
    {
        return container;
    }

    public void setContainer(SongStructure sgs)
    {
        container = sgs;
    }

    /**
     * Get the value of a RhythmParameter at a specified barIndex.
     *
     * @param rp
     * @return the java.lang.Object
     */
    @Override
    public <T> T getRPValue(RhythmParameter<T> rp)
    {
        if (rp == null || !rhythm.getRhythmParameters().contains(rp))
        {
            throw new IllegalArgumentException("this=" + this + " rp=" + rp);   //NOI18N
        }
        @SuppressWarnings("unchecked")
        T value = (T) mapRpValue.getValue(rp);
        assert value != null : "rp=" + rp + " mapRpValueProfile=" + mapRpValue;   //NOI18N
        return value;
    }

    /**
     * Change the v for a given RhythmParameter. Fire a PropertyChangeEvent with OldValue=rp, NewValue=vp.
     *
     * @param rp
     * @param value Must be a valid value for rp
     */
    public <T> void setRPValue(RhythmParameter<T> rp, T value)
    {
        if (!rhythm.getRhythmParameters().contains(rp) || value == null || !rp.isValidValue(value))
        {
            throw new IllegalArgumentException("rp=" + rp + " value=" + value);   //NOI18N
        }
        @SuppressWarnings("unchecked")
        T oldValue = (T) mapRpValue.getValue(rp);
        assert oldValue != null : "rpValueProfileMap=" + mapRpValue + " rp=" + rp + " value=" + value;   //NOI18N
        if (oldValue != value)
        {
            mapRpValue.putValue(rp, value);
            pcs.firePropertyChange(PROPERTY_RP_VALUE, rp, value);
        }
    }

    @Override
    public Rhythm getRhythm()
    {
        return rhythm;
    }

    /**
     * Fire a propertyChangeEvent.
     *
     * @param barIndex
     */
    public void setStartBarIndex(int barIndex)
    {
        if (barIndex < 0)
        {
            throw new IllegalArgumentException("barIndex=" + barIndex);   //NOI18N
        }
        if (barIndex != startBarIndex)
        {
            int old = startBarIndex;
            startBarIndex = barIndex;
            pcs.firePropertyChange(SongPart.PROPERTY_START_BAR_INDEX, old, startBarIndex);
        }
    }

    @Override
    public int getStartBarIndex()
    {
        return startBarIndex;
    }

    @Override
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    public SongPart clone(Rhythm r, int newStartBarIndex, int newNbBars, CLI_Section cliSection)
    {
        if (newStartBarIndex < 0)
        {
            throw new IllegalArgumentException("newStartBarIndex=" + newStartBarIndex);   //NOI18N
        }
        Rhythm newRhythm = (r == null) ? getRhythm() : r;

        // Check that time signature match
        if (cliSection != null && !cliSection.getData().getTimeSignature().equals(newRhythm.getTimeSignature()))
        {
            throw new IllegalArgumentException("r=" + r + " newRhythm=" + newRhythm + " cliSection=" + cliSection);   //NOI18N
        }

        SongPartImpl newSpt = new SongPartImpl(newRhythm, newStartBarIndex, newNbBars, cliSection);
        newSpt.setContainer(container);
        newSpt.setName(name);

        // Update the values for compatible RhythmParameters
        for (RhythmParameter<?> newRp : newRhythm.getRhythmParameters())
        {
            RhythmParameter crp = findCompatibleRP(newRp);
            if (crp != null)
            {
                // rp can reuse existing crp value: try first using the RP string value, then use percentageValue 
                Object crpValue = mapRpValue.getValue(crp);
                Object newRpValue = null;
                String crpStringValue = crp.valueToString(crpValue);
                if (crpStringValue != null)
                {
                    newRpValue = newRp.stringToValue(crpStringValue);     // May return null
                }
                if (newRpValue == null && newRp instanceof EnumerableParameter && crp instanceof EnumerableParameter)
                {
                    double crpPercentageValue = ((EnumerableParameter) crp).calculatePercentage(crpValue);
                    newRpValue = ((EnumerableParameter) newRp).calculateValue(crpPercentageValue);
                }
                if (newRpValue != null)
                {
                    newSpt.mapRpValue.putValue(newRp, newRpValue);
                } else
                {
                    LOGGER.warning("clone() Can't transpose value crpValue=" + crpValue + " to newRp=" + newRp.getId() + " (newRhythm=" + newRhythm.getName()
                            + "), despite newRp being compatible with crp=" + crp.getId() + " (rhythm=" + rhythm.getName() + ")");
                }
            }
        }
        return newSpt;
    }

    /**
     * Fire a propertyChangeEvent.
     *
     * @param n
     */
    public void setNbBars(int n)
    {
        if (n < 1)
        {
            throw new IllegalArgumentException("n=" + n);   //NOI18N
        }
        if (nbBars != n)
        {
            int old = nbBars;
            nbBars = n;
            pcs.firePropertyChange(PROPERTY_NB_BARS, old, nbBars);
        }
    }

    @Override
    public int getNbBars()
    {
        return nbBars;
    }

    @Override
    public CLI_Section getParentSection()
    {
        return parentSection;
    }

    @Override
    public String toString()
    {
        return "[" + name + ", r=" + rhythm + ", startBarIndex=" + startBarIndex + ", nbBars=" + nbBars + "]";
    }

    public String toDumpString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(toString()).append("\n");
        for (RhythmParameter<?> rp : rhythm.getRhythmParameters())
        {
            sb.append("  " + rp + ":" + this.mapRpValue.getValue(rp) + "\n");
        }
        return sb.toString();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ------------------------------------------------------------------------------
    // Implementation of interface Transferable
    // ------------------------------------------------------------------------------
    private static DataFlavor flavor = DATA_FLAVOR;

    private static DataFlavor[] supportedFlavors =
    {
        DATA_FLAVOR,
        DataFlavor.stringFlavor
    };

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return supportedFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor fl)
    {
        if (fl.equals(DATA_FLAVOR) || fl.equals(DataFlavor.stringFlavor))
        {
            return true;
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor fl) throws UnsupportedFlavorException
    {
        if (fl.equals(DATA_FLAVOR))
        {
            return this;
        } else if (fl.equals(DataFlavor.stringFlavor))
        {
            return getName();
        } else
        {
            throw new UnsupportedFlavorException(fl);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------------------------
    /**
     * Find the first RhythmParameter compatible with rp in this object's Rhythm.
     * <p>
     * Compatible = same name ignoring case and value has same class.
     *
     * @param rp
     * @return Null if not found.
     */
    private RhythmParameter<?> findCompatibleRP(RhythmParameter<?> rp)
    {
        for (RhythmParameter<?> rpi : getRhythm().getRhythmParameters())
        {
            if (RhythmParameter.checkCompatibility(rp, rpi))
            {
                return rpi;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");

    }

    /**
     * Can not save RhythmParameters instances: they will change because the Rhythm are not serialized but recreated from the
     * local database.
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 99812309123112L;
        /**
         * To avoid multiple error messages when one rhythm is not available, store it here for the session.
         */
        private static transient List<String> saveUnavailableRhythmIds = new ArrayList<>();

        private final int spVERSION = 1;
        private String spRhythmId;
        private String spRhythmName;
        private TimeSignature spRhythmTs;
        private int spStartBarIndex;
        private String spName;
        private int spNbBars;
        private CLI_Section spParentSection;
        private SmallMap<String, String> spMapRpIdValue = new SmallMap<>();
        private SmallMap<String, String> spMapRpIdDisplayName = new SmallMap<>();        // Used to find a matching RP when rpId does not work
        private SmallMap<String, Double> spMapRpIdPercentageValue = new SmallMap<>();   // Used as backup when RP's valueToString() does not work

        @SuppressWarnings(
                {
                    "unchecked", "rawtypes"
                })
        private SerializationProxy(SongPartImpl spt)
        {
            spName = spt.getName();
            spStartBarIndex = spt.getStartBarIndex();
            spNbBars = spt.getNbBars();
            spParentSection = spt.getParentSection();
            spRhythmId = spt.getRhythm().getUniqueId();
            spRhythmName = spt.getRhythm().getName();
            spRhythmTs = spt.getRhythm().getTimeSignature();

            for (RhythmParameter rp : spt.getRhythm().getRhythmParameters())
            {
                Object value = spt.getRPValue(rp);
                double percentage = rp instanceof EnumerableParameter ? ((EnumerableParameter) rp).calculatePercentage(value) : -1;
                spMapRpIdPercentageValue.putValue(rp.getId(), percentage);
                spMapRpIdDisplayName.putValue(rp.getId(), rp.getDisplayName());
                String strValue = rp.valueToString(value);
                if (strValue != null)
                {
                    spMapRpIdValue.putValue(rp.getId(), strValue);
                }
            }
        }

        @SuppressWarnings(
                {
                    "unchecked", "rawtypes"
                })
        private Object readResolve() throws ObjectStreamException
        {
            // Restore the rhythm
            String errRhythm = null;
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            Rhythm r = null;
            try
            {
                r = rdb.getRhythmInstance(spRhythmId);
                saveUnavailableRhythmIds.remove(spRhythmId);    // Rhythm is now available
            } catch (UnavailableRhythmException ex1)
            {
                // Problem ! The saved rhythm does not exist on the system, need to find another one
                LOGGER.warning("readResolve() Can't get rhythm instance for rhythm id=" + spRhythmId + ". ex1=" + ex1.getMessage());   //NOI18N
                RhythmInfo ri = rdb.getDefaultRhythm(spRhythmTs);
                try
                {
                    r = rdb.getRhythmInstance(ri);
                } catch (UnavailableRhythmException ex2)
                {
                    LOGGER.warning("readResolve() Can't get rhythm instance for " + ri + ". ex2=" + ex2.getMessage());   //NOI18N
                    r = rdb.getDefaultStubRhythmInstance(spRhythmTs);   // Can't be null
                }
                errRhythm = ResUtil.getString(getClass(), "ERR_RhythmNotFound") + ": " + spRhythmName + ". " + ResUtil.getString(getClass(), "ERR_UsingReplacementRhythm") + ": " + r.getName();
            }
            assert r != null;   //NOI18N

            // Recreate a SongPart
            SongPartImpl newSpt = new SongPartImpl(r, spStartBarIndex, spNbBars, spParentSection);
            newSpt.setName(spName);

            // Update new rhythm parameters with saved values 
            for (String savedRpId : spMapRpIdPercentageValue.getKeys())
            {
                // Reuse the old rp value if there is a matchig rp in the destination rhythm
                String savedRpDisplayName = spMapRpIdDisplayName.getValue(savedRpId);
                RhythmParameter newRp = findMatchingRp(r.getRhythmParameters(), savedRpId, savedRpDisplayName);
                if (newRp != null)
                {
                    // Try to reuse the string value if available, otherwise use percentage value if RP is enumerable
                    Object newValue = null;
                    String savedRpStringValue = spMapRpIdValue.getValue(savedRpId);
                    if (savedRpStringValue != null)
                    {
                        newValue = newRp.stringToValue(savedRpStringValue);      // newValue can still be null after this
                    }
                    if (newValue == null && newRp instanceof EnumerableParameter)
                    {
                        double savedRpPercentageValue = spMapRpIdPercentageValue.getValue(savedRpId);
                        if (savedRpPercentageValue >= 0 && savedRpPercentageValue <= 1)
                        {
                            newValue = ((EnumerableParameter) newRp).calculateValue(savedRpPercentageValue);
                        }
                    }
                    if (newValue == null)
                    {
                        LOGGER.warning("readResolve() Could not restore value of rhythm parameter " + newRp.getId() + " from savedRpStringValue=" + savedRpStringValue);
                    }
                    newSpt.setRPValue(newRp, newValue);
                } else if (!saveUnavailableRhythmIds.contains(spRhythmId))
                {
                    String msg = "- '" + savedRpId + "' original rhythm parameter value can not be reused by new rhythm '" + r.getName() + "'";
                    LOGGER.log(Level.WARNING, msg);   //NOI18N
                }
            }

            if (errRhythm != null && !saveUnavailableRhythmIds.contains(spRhythmId))
            {
                LOGGER.warning(errRhythm);   //NOI18N
                NotifyDescriptor nd = new NotifyDescriptor.Message(errRhythm, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                saveUnavailableRhythmIds.add(spRhythmId);
            }

            return newSpt;
        }

        /**
         * Find a RP in rps who can match the specified parameters.
         * <p>
         * First try to match rpId, then rpDisplayName (ignoring case).
         *
         * @param rps
         * @param rpId
         * @param rpDisplayName
         * @return
         */
        private RhythmParameter<?> findMatchingRp(List<RhythmParameter<?>> rps, String rpId, String rpDisplayName)
        {
            for (RhythmParameter<?> rp : rps)
            {
                if (rp.getId().equals(rpId))
                {
                    return rp;
                }
            }
            for (RhythmParameter<?> rp : rps)
            {
                if (rp.getDisplayName().equalsIgnoreCase(rpDisplayName))
                {
                    return rp;
                }
            }
            return null;
        }
    }
}
