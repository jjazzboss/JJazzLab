/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3)
 *  as published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 *  Contributor(s):
 */
package org.jjazz.songstructure;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.utilities.api.SmallMap;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.StringProperties;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;


/**
 * SongPart implementation.
 * <p>
 * This is a mutable class. SongPart mutating methods can only be called via the enclosing SongStructure methods which manage synchronization. For read
 * operations the implementation reuses the ExecutionManager lock.
 */
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
    private final StringProperties clientProperties;
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
    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongPartImpl.class.getSimpleName());


    /**
     * Constructor for deserialization only.
     * <p>
     * Container will be set by enclosing SongStructure deserialization.
     *
     * @param r             Cannot be null
     * @param startBarIndex
     * @param nbBars
     * @param parentSection Cannot be null
     */
    private SongPartImpl(Rhythm r, int startBarIndex, int nbBars, CLI_Section parentSection)
    {
        Objects.requireNonNull(r);
        Objects.requireNonNull(parentSection);
        Preconditions.checkArgument(startBarIndex >= 0 && nbBars >= 1, "startBarIndex=%s nbBars=%s", startBarIndex, nbBars);

        this.rhythm = r;
        this.startBarIndex = startBarIndex;
        this.nbBars = nbBars;
        this.parentSection = parentSection;
        this.clientProperties = new StringProperties(this);
    }

    /**
     * Create a SongPartImpl with default value for each of the rhythm's RhythmParameters.
     * <p>
     * Name is set to parentSection's name.
     *
     * @param container     Cannot be null
     * @param r             Cannot be null
     * @param startBarIndex
     * @param nbBars
     * @param parentSection Cannot be null
     */
    public SongPartImpl(SongStructure container, Rhythm r, int startBarIndex, int nbBars, CLI_Section parentSection)
    {
        Objects.requireNonNull(container);
        Objects.requireNonNull(r);
        Objects.requireNonNull(parentSection);
        Preconditions.checkArgument(startBarIndex >= 0 && nbBars >= 1, "startBarIndex=%s nbBars=%s", startBarIndex, nbBars);

        this.container = container;
        this.rhythm = r;
        this.startBarIndex = startBarIndex;
        this.nbBars = nbBars;
        this.name = parentSection.getData().getName();
        this.parentSection = parentSection;
        this.clientProperties = new StringProperties(this);

        resetRPvalues();
    }


    @Override
    public int hashCode()
    {
        int res = performReadAPImethod(() -> 
        {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.rhythm);
            hash = 97 * hash + this.startBarIndex;
            hash = 97 * hash + Objects.hashCode(this.name);
            hash = 97 * hash + this.nbBars;
            hash = 97 * hash + Objects.hashCode(this.parentSection);
            hash = 97 * hash + Objects.hashCode(this.mapRpValue);
            hash = 97 * hash + Objects.hashCode(this.container);
            return hash;
        });
        return res;
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
        final SongPartImpl other = (SongPartImpl) obj;
        record TmpRecord(int start, int nbBars, String name, Rhythm rhythm, CLI_Section section, SmallMap<RhythmParameter<?>, Object> map,
                SongStructure container)
                {

        }
        TmpRecord tmp = performReadAPImethod(() -> 
        {
            return new TmpRecord(this.startBarIndex, this.nbBars, this.name, this.rhythm, this.parentSection, this.mapRpValue, this.container);
        });
        TmpRecord tmpOther = other.performReadAPImethod(() -> 
        {
            return new TmpRecord(other.startBarIndex, other.nbBars, other.name, other.rhythm, other.parentSection, other.mapRpValue, other.container);
        });
        return tmp.equals(tmpOther);
    }


    public void firePropertyChangeEvent(PropertyChangeEvent event)
    {
        pcs.firePropertyChange(event);
    }

    @Override
    public String getName()
    {
        return performReadAPImethod(() -> name);
    }

    /**
     * Can only be called by SongStructure.
     *
     * @param name
     * @return
     */
    public PropertyChangeEvent setName(String name)
    {
        Objects.requireNonNull(name);
        Preconditions.checkArgument(!name.isBlank());

        String oldName = this.name;
        this.name = name;

        return new PropertyChangeEvent(this, PROP_NAME, oldName, name);
    }

    @Override
    public SongStructure getContainer()
    {
        return container;
    }

    /**
     * Can only be called by SongStructure.
     *
     * @param sgs
     */
    public void setContainer(SongStructure sgs)
    {
        Objects.requireNonNull(sgs);
        container = sgs;
    }

    @Override
    public <T> T getRPValue(RhythmParameter<T> rp)
    {
        Objects.requireNonNull(rp);

        return performReadAPImethod(() -> 
        {
            Preconditions.checkArgument(rhythm.getRhythmParameters().contains(rp), "this=%s rhythm=%s rp=%s", this, rhythm, rp);
            @SuppressWarnings("unchecked")
            T value = (T) mapRpValue.getValue(rp);
            assert value != null : "rp=" + rp + " mapRpValue=" + mapRpValue;
            return value;
        });
    }

    /**
     * Change the value for a given RhythmParameter.
     * <p>
     * Fire a PROP_RP_VALUE with OldValue=rp, NewValue=vp.
     * <p>
     * Can only be called by SongStructure.
     *
     * @param <T>
     * @param rp
     * @param newValue Must be a valid value for rp
     * @return
     */
    public <T> PropertyChangeEvent setRPValue(RhythmParameter<T> rp, T newValue)
    {
        Objects.requireNonNull(rp);
        Objects.requireNonNull(newValue);
        Preconditions.checkArgument(rhythm.getRhythmParameters().contains(rp), "rhythm=%s rp=%s", rhythm, rp);
        Preconditions.checkArgument(rp.isValidValue(newValue), "rp=%s newValue=%s", rp, newValue);


        @SuppressWarnings("unchecked")
        T oldValue = (T) mapRpValue.getValue(rp);
        assert oldValue != null : "rpValueProfileMap=" + mapRpValue + " rp=" + rp + " value=" + newValue;
        if (oldValue.equals(newValue))
        {
            return getVoidPropertyChangeEvent();
        }

        mapRpValue.putValue(rp, newValue);
        return new PropertyChangeEvent(this, PROP_RP_VALUE, rp, newValue);
    }

    @Override
    public Rhythm getRhythm()
    {
        return performReadAPImethod(() -> rhythm);
    }

    /**
     * Set the rhythm and the parent section.
     * <p>
     * Can only be called by SongStructure.
     *
     * @param newRhythm        If null rhythm is unchanged.
     * @param newParentSection If null parent section is unchanged.
     * @return An event with oldValue=newRhythm, newValue=newParentSection
     */
    public PropertyChangeEvent setRhythm(Rhythm newRhythm, CLI_Section newParentSection)
    {
        Preconditions.checkArgument(newParentSection == null
                || newParentSection.getContainer() != null,
                "newParentSection=%s", newParentSection);
        Preconditions.checkArgument(newParentSection == null
                || nbBars == newParentSection.getContainer().getBarRange(newParentSection).size(),
                "this=%s newParentSection=%s", this, newParentSection);
        Preconditions.checkArgument(
                newParentSection == null
                || newRhythm == null
                || newParentSection.getData().getTimeSignature() == newRhythm.getTimeSignature(),
                "newRhythm=%s newParentSection=%s", newRhythm, newParentSection);

        if ((newRhythm == null || rhythm == newRhythm) && (newParentSection == null || newParentSection == parentSection))
        {
            return getVoidPropertyChangeEvent();
        }

        var oldRhythm = rhythm;
        var saveMapRvValues = mapRpValue.clone();


        rhythm = newRhythm == null ? rhythm : newRhythm;;
        parentSection = newParentSection == null ? parentSection : newParentSection;


        resetRPvalues();


        // Update the values for compatible RhythmParameters
        for (RhythmParameter<?> newRp : rhythm.getRhythmParameters())
        {
            RhythmParameter crp = RhythmParameter.findFirstCompatibleRp(oldRhythm.getRhythmParameters(), newRp);
            if (crp != null)
            {
                Object crpValue = saveMapRvValues.getValue(crp);
                Object newRpValue = newRp.convertValue(crp, crpValue);
                if (newRpValue != null)
                {
                    mapRpValue.putValue(newRp, newRpValue);
                } else
                {
                    LOGGER.log(Level.WARNING,
                            "clone() Can''t transpose value crpValue={0} to newRp={1} (newRhythm={2}), despite newRp being compatible with crp={3} from oldRhythm={4}",
                            new Object[]
                            {
                                crpValue,
                                newRp.getId(), rhythm.getName(), crp.getId(), oldRhythm.getName()
                            });
                }
            }
        }

        return new PropertyChangeEvent(this, PROP_RHYTHM_PARENT_SECTION, rhythm, newParentSection);
    }

    /**
     * Can only be called by SongStructure.
     *
     * @param barIndex
     * @return
     */
    public PropertyChangeEvent setStartBarIndex(int barIndex)
    {
        Preconditions.checkArgument(barIndex >= 0, "barIndex=%s", barIndex);

        int old = startBarIndex;
        startBarIndex = barIndex;

        return new PropertyChangeEvent(this, PROP_START_BAR_INDEX, old, barIndex);
    }

    @Override
    public int getStartBarIndex()
    {
        return performReadAPImethod(() -> startBarIndex);
    }

    /**
     * Can only be called by SongStructure.
     *
     * @param n
     * @return
     */
    public PropertyChangeEvent setNbBars(int n)
    {
        Preconditions.checkArgument(n >= 1, "n=%s", n);

        int old;
        old = nbBars;
        nbBars = n;

        return new PropertyChangeEvent(this, PROP_NB_BARS, old, n);
    }

    @Override
    public int getNbBars()
    {
        return performReadAPImethod(() -> nbBars);
    }

    @Override
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    public SongPart getCopy(Rhythm r, int newStartBarIndex, int newNbBars, CLI_Section cliSection)
    {
        Preconditions.checkArgument(newStartBarIndex >= 0, "newStartBarIndex=%s", newStartBarIndex);
        Preconditions.checkArgument(newNbBars > 0, "newNbBars=%s", newNbBars);

        Rhythm newRhythm = r == null ? getRhythm() : r;
        CLI_Section newCliSection = cliSection == null ? getParentSection() : cliSection;
        Preconditions.checkArgument(newCliSection.getData().getTimeSignature().equals(newRhythm.getTimeSignature()),
                "newRhythm=%s cliSection=%s", newRhythm, cliSection);


        final SongPartImpl newSpt = new SongPartImpl(container, newRhythm, newStartBarIndex, newNbBars, newCliSection);


        performReadAPImethod(() -> 
        {
            newSpt.setName(name);
            newSpt.setContainer(container);

            if (newRhythm == getRhythm())
            {
                newSpt.mapRpValue = mapRpValue.clone();

            } else
            {
                // Update the values for compatible RhythmParameters
                for (RhythmParameter<?> newRp : newRhythm.getRhythmParameters())
                {
                    RhythmParameter crp = RhythmParameter.findFirstCompatibleRp(getRhythm().getRhythmParameters(), newRp);
                    if (crp != null)
                    {
                        Object crpValue = getRPValue(crp);
                        Object newRpValue = newRp.convertValue(crp, crpValue);
                        if (newRpValue != null)
                        {
                            newSpt.mapRpValue.putValue(newRp, newRpValue);
                        } else
                        {
                            LOGGER.log(Level.WARNING,
                                    "clone() Can''t transpose value crpValue={0} to newRp={1} (newRhythm={2}), despite newRp being compatible with crp={3} (rhythm={4})",
                                    new Object[]
                                    {
                                        crpValue,
                                        newRp.getId(), newRhythm.getName(), crp.getId(), rhythm.getName()
                                    });
                        }
                    }
                }
            }
            return null;
        });


        newSpt.getClientProperties().set(clientProperties);

        return newSpt;
    }


    @Override
    public CLI_Section getParentSection()
    {
        return performReadAPImethod(() -> parentSection);
    }

    @Override
    public StringProperties getClientProperties()
    {
        return clientProperties;
    }

    @Override
    public IntRange getBarRange()
    {
        return performReadAPImethod(() -> new IntRange(startBarIndex, startBarIndex + nbBars - 1));
    }

    @Override
    public String toString()
    {
        return performReadAPImethod(() -> name + getBarRange() + "-" + rhythm.getName());
    }

    @Override
    public String toShortString()
    {
        return performReadAPImethod(() -> "[" + name + ", " + startBarIndex + "]");
    }

    public String toDumpString()
    {
        return performReadAPImethod(() -> 
        {
            StringBuilder sb = new StringBuilder();
            sb.append(toString()).append("\n");
            for (RhythmParameter<?> rp : rhythm.getRhythmParameters())
            {
                sb.append("  ").append(rp).append(":").append(mapRpValue.getValue(rp)).append("\n");
            }
            return sb.toString();
        });
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
    private static final DataFlavor flavor = DATA_FLAVOR;

    private static final DataFlavor[] supportedFlavors =
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
        return fl.equals(DATA_FLAVOR) || fl.equals(DataFlavor.stringFlavor);
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
    private <T> T performReadAPImethod(Supplier<T> operation)
    {
        return ((SongStructureImpl) container).getExecutionManager().executeReadOperation(operation);
    }

    /**
     * Reset all current rhythm parameters values to default.
     */
    private void resetRPvalues()
    {
        mapRpValue.clear();
        for (RhythmParameter<?> rp : rhythm.getRhythmParameters())
        {
            var rpValue = rp.getDefaultValue();
            mapRpValue.putValue(rp, rpValue);
        }
    }

    /**
     * Get an event which will never be fired.
     *
     * @return
     */
    private PropertyChangeEvent getVoidPropertyChangeEvent()
    {
        return new PropertyChangeEvent(this, "a", true, true);
    }

    // -------------------------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------------------------

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files
                    xstream.alias("SongPartImpl", SongPartImpl.class);
                    xstream.alias("SongPartImplSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spName");
                    xstream.useAttributeFor(SerializationProxy.class, "spStartBarIndex");
                    xstream.useAttributeFor(SerializationProxy.class, "spNbBars");
                    xstream.useAttributeFor(SerializationProxy.class, "spRhythmId");
                    xstream.useAttributeFor(SerializationProxy.class, "spRhythmTs");

                }

                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
                }
                default -> throw new AssertionError(instanceId.name());
            }
        }
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
     * Can not save RhythmParameters instances: they will change because the Rhythm are not serialized but recreated from the local database.
     * <p>
     * spVERSION 2 changes saved fields.<br>
     * spVERSION 3 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded full class names (XStreamConfig class introduction), discards the
     * spMapRpIdDisplayName and spMapRpIdPercentageValue maps, replace legacy SmallMap by HashMap<br>
     * <p>
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 99812309123112L;
        /**
         * To avoid multiple error messages when one rhythm is not available, store it here for the session.
         */
        private static final transient Set<String> saveUnavailableRhythmIds = new HashSet<>();

        private int spVERSION = 3;  // Do not make final!
        private String spRhythmId;
        private String spRhythmName;
        private TimeSignature spRhythmTs;
        private int spStartBarIndex;
        private String spName;
        private int spNbBars;
        private CLI_Section spParentSection;
        // from spVERSION 3 replaced by spHashMapRpIdValue
        private SmallMap<String, String> spMapRpIdValue;
        // Discarded (made transient) from spVERSION 3
        private transient SmallMap<String, String> spMapRpIdDisplayName = new SmallMap<>();       // Used to find a matching RP when rpId does not work.
        // Discarded (made transient) from spVERSION 3        
        private transient SmallMap<String, Double> spMapRpIdPercentageValue = new SmallMap<>();   // Used as backup when RP's valueToString() does not work. 
        private StringProperties spClientProperties;      // From spVERSION 2
        private HashMap<String, String> spHashMapRpIdValue = new HashMap<>();  // From spVERSION 3

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
                String strValue = rp.saveAsString(value);
                if (strValue == null)
                {
                    Object defValue = rp.getDefaultValue();
                    LOGGER.log(Level.WARNING, "SerializationProxy() Invalid value {0} for rp={1}, using default value {2} instead", new Object[]
                    {
                        value, rp,
                        defValue
                    });
                    strValue = rp.saveAsString(defValue);
                }
                spHashMapRpIdValue.put(rp.getId(), strValue);
            }

            // From spVERSION 2
            spClientProperties = spt.getClientProperties();
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
            Rhythm r;
            int rhythmIsUnavailable; // 0=available, 1=first time unavailable, 2=was already marked unavailable

            try
            {
                r = rdb.getRhythmInstance(spRhythmId);      // throws UnavailableRhythmException
                rhythmIsUnavailable = 0;
                saveUnavailableRhythmIds.remove(spRhythmId);    // Rhythm is now available

            } catch (UnavailableRhythmException ex1)
            {
                // Problem ! The saved rhythm does not exist on the system, need to find another one

                rhythmIsUnavailable = saveUnavailableRhythmIds.add(spRhythmId) ? 1 : 2;

                if (rhythmIsUnavailable == 1)
                {
                    LOGGER.log(Level.WARNING, "readResolve() spt={0}[{1}] Could not get rhythm instance for rhythm id={2}. ex1={3}", new Object[]
                    {
                        spName,
                        spStartBarIndex,
                        spRhythmId,
                        ex1.getMessage()
                    });
                }

                // Try to find a similar rhythm 
                var ri = rdb.findRhythm(spRhythmName, rii -> rii.timeSignature() == spRhythmTs);
                if (ri == null)
                {
                    // Also try via RhythmFeatures
                    var rf = RhythmFeatures.guessFeatures(spRhythmName, -1);
                    ri = rdb.findRhythm(rf, rii -> rii.timeSignature() == spRhythmTs);
                }
                if (ri == null)
                {
                    // Just use default
                    ri = rdb.getDefaultRhythm(spRhythmTs);
                }
                try
                {
                    LOGGER.log(Level.INFO, "readResolve() Using {0} as replacement", ri.name());
                    r = rdb.getRhythmInstance(ri);
                } catch (UnavailableRhythmException ex2)
                {
                    LOGGER.log(Level.WARNING, "readResolve() Could not get rhythm instance for {0}. ex2={1}", new Object[]
                    {
                        ri, ex2.getMessage()
                    });
                    r = rdb.getDefaultStubRhythmInstance(spRhythmTs);    // Can't be null
                }
                errRhythm = ResUtil.getString(getClass(), "ERR_RhythmNotFound") + ": " + spRhythmName + ". "
                        + ResUtil.getString(getClass(), "ERR_UsingReplacementRhythm") + ": " + r.getName();
            }
            assert r != null;


            // Recreate a SongPart
            SongPartImpl newSpt = new SongPartImpl(r, spStartBarIndex, spNbBars, spParentSection);
            newSpt.setName(spName);


            if (spVERSION < 3)
            {
                // Need to transfer the old SmallMap spMapRpIdValue content                
                assert spMapRpIdValue != null;
                spHashMapRpIdValue = new HashMap<>();
                for (var k : spMapRpIdValue.getKeys())
                {
                    spHashMapRpIdValue.put(k, spMapRpIdValue.getValue(k));
                }
            }


            // Update new rhythm parameters with saved values 
            for (String rpId : spHashMapRpIdValue.keySet())
            {
                RhythmParameter newRp = r.getRhythmParameters().stream()
                        .filter(rp -> rp.getId().equals(rpId))
                        .findAny()
                        .orElse(null);

                if (newRp != null)
                {
                    Object newValue = null;
                    String savedRpStringValue = spHashMapRpIdValue.get(rpId);
                    if (savedRpStringValue != null)
                    {
                        newValue = newRp.loadFromString(savedRpStringValue);      // newValue can still be null after this
                    }

                    if (newValue == null)
                    {
                        if (rhythmIsUnavailable == 1)
                        {
                            LOGGER.log(Level.WARNING,
                                    "readResolve() Could not restore value of rhythm parameter {0} from savedRpStringValue=''{1}''. Using default value instead.",
                                    new Object[]
                                    {
                                        newRp.getId(),
                                        savedRpStringValue
                                    });
                        }
                        newValue = newRp.getDefaultValue();
                    }

                    // Assign the value
                    newSpt.setRPValue(newRp, newValue);

                } else if (rhythmIsUnavailable == 1)
                {
                    String msg = "readResolve() Saved rhythm parameter " + rpId + " not found in rhythm " + r.getName();
                    LOGGER.log(Level.WARNING, msg);
                }
            }


            // From spVERSION 2
            if (spVERSION >= 2)
            {
                if (spClientProperties != null)
                {
                    newSpt.getClientProperties().set(spClientProperties);
                } else
                {
                    LOGGER.log(Level.WARNING, "readResolve() Unexpected null value for spClientProperties. spName={0}", spName);
                }
            }


            if (rhythmIsUnavailable == 1)
            {
                LOGGER.warning(errRhythm);
                LOGGER.log(Level.WARNING, "Warnings for missing {0} are turned off from now on", spRhythmName);
                NotifyDescriptor nd = new NotifyDescriptor.Message(errRhythm, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                saveUnavailableRhythmIds.add(spRhythmId);
            }

            return newSpt;
        }
    }
}
