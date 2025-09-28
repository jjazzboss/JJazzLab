package org.jjazz.jjswing.drums.db;

import static com.google.common.base.Preconditions.checkArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jjazz.jjswing.api.DrumsStyle;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.utilities.api.FloatRange;

/**
 * A drums/percussion source phrase to be managed by a DrumsPhraseDatabase.
 * <p>
 * The drumsStyle and alternateId identify a DpSource instance.
 */
public class DpSource
{

    public enum Type
    {
        STD, FILL
    }
    private final SizedPhrase drumsPhrase;
    private final SizedPhrase percPhrase;
    private final Type type;
    private final int alternateId;
    private final DrumsStyle drumsStyle;
    private final List<String> tags;
    private DpSourceStats stats;
    private static final Logger LOGGER = Logger.getLogger(DpSource.class.getSimpleName());

    /**
     * Create a source drums/perc phrase.
     * <p>
     *
     * @param dStyle
     * @param type
     * @param altId   alternateId. First id=0, then 1 etc.
     * @param dPhrase Drums phrase. Can be empty if pPhrase not empty. Phrase might be modified. If type==FILL size must be 1.
     * @param pPhrase Percussion phrase. Can be empty if dPhrase not empty. Phrase might be modified. If type==FILL size must be 1.
     * @param tags
     */
    public DpSource(DrumsStyle dStyle, Type type, int altId, SizedPhrase dPhrase, SizedPhrase pPhrase, String... tags)
    {
        Objects.requireNonNull(dStyle);
        Objects.requireNonNull(type);
        Objects.requireNonNull(dPhrase);
        Objects.requireNonNull(pPhrase);
        checkArgument(!(dPhrase.isEmpty() && pPhrase.isEmpty())
                && dPhrase.getNotesBeatRange().from == 0 && dPhrase.getNotesBeatRange().equals(pPhrase.getNotesBeatRange()),
                "dPhrase=%s pPhrase=%s",
                dPhrase, pPhrase);
        checkArgument(type == Type.STD || dPhrase.getSizeInBars() == 1, "dPhrase=%s pPhrase=%s", dPhrase, pPhrase);
        checkArgument(altId >= 0, "altId=%s", altId);

        this.type = type;
        this.drumsStyle = dStyle;
        this.alternateId = altId;
        this.drumsPhrase = dPhrase;
        this.percPhrase = pPhrase;
        this.tags = new ArrayList<>();
        Stream.of(tags).forEach(t -> this.tags.add(t.toLowerCase()));


        Phrases.fixEndOfPhraseNotes(drumsPhrase);
        Phrases.fixEndOfPhraseNotes(percPhrase);
        // Velocities.normalizeBassVelocities(phrase, 0.5f);

    }

    /**
     * Relies only on drumsStyle + alternateId.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 53 * hash + this.alternateId;
        hash = 53 * hash + Objects.hashCode(this.drumsStyle);
        return hash;
    }

    /**
     * Relies only on drumsStyle + alternateId.
     *
     * @return
     */
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
        final DpSource other = (DpSource) obj;
        if (this.alternateId != other.alternateId)
        {
            return false;
        }
        return this.drumsStyle == other.drumsStyle;
    }


    public DpSourceStats getStats()
    {
        if (stats == null)
        {
            stats = DpSourceStats.of(this);
        }
        return stats;
    }

    public Type getType()
    {
        return type;
    }

    /**
     * @return Can't be null
     */
    public DrumsStyle getDrumsStyle()
    {
        return drumsStyle;
    }

    /**
     * Get a copy of the drums phrase (possibly trimmed) starting at targetBeatRange.from.
     *
     * @param targetBeatRange The size of the resulting phrase.
     * @return Can be empty.
     * @see #getPercPhrase()
     */
    public SizedPhrase getDrumsPhrase(FloatRange targetBeatRange)
    {
        Objects.requireNonNull(targetBeatRange);
        var sp = buildTargetPhrase(drumsPhrase, targetBeatRange);
        return sp;
    }

    /**
     * Get a copy of the perc phrase (possibly trimmed) starting at targetBeatRange.from.
     *
     * @param targetBeatRange The size of the resulting phrase.
     * @return Can be empty.
     * @see #getDrumsPhrase()
     */
    public SizedPhrase getPercPhrase(FloatRange targetBeatRange)
    {
        Objects.requireNonNull(targetBeatRange);
        var sp = buildTargetPhrase(percPhrase, targetBeatRange);
        return sp;
    }

    /**
     * True if this phrase has a non-empty drums phrase.
     * <p>
     * Note that both isDrums() and isPerc() can be true.
     *
     * @return
     */
    public boolean isDrums()
    {
        return !drumsPhrase.isEmpty();
    }

    /**
     * True if this phrase has a non-empty percussion phrase.
     * <p>
     * Note that both isDrums() and isPerc() can be true.
     *
     * @return
     */
    public boolean isPerc()
    {
        return !percPhrase.isEmpty();
    }

    /**
     *
     * @return Phrase size in bars
     */
    public int getSizeInBars()
    {
        return drumsPhrase.getSizeInBars();
    }

    /**
     *
     * @return &gt;=0
     */
    public int getAlternateId()
    {
        return alternateId;
    }

    public List<String> getTags()
    {
        return Collections.unmodifiableList(tags);
    }

    public boolean addTag(String tag)
    {
        boolean b = false;
        if (!tags.contains(tag))
        {
            tags.add(tag);
            b = true;
        }
        return b;
    }

    public boolean removeTag(String tag)
    {
        return tags.remove(tag);
    }


    public boolean hasTag(String tag)
    {
        return tags.contains(tag.toLowerCase());
    }


    @Override
    public String toString()
    {
        return "DPS[" + drumsStyle + "-" + type + "-" + alternateId + ", size=" + drumsPhrase.getSizeInBars() + "]";
    }


    // =================================================================================================================
    // Private methods
    // ================================================================================================================= 
    private SizedPhrase buildTargetPhrase(SizedPhrase srcPhrase, FloatRange targetBeatRange)
    {
        SizedPhrase res = new SizedPhrase(srcPhrase.getChannel(), targetBeatRange, srcPhrase.getTimeSignature(), srcPhrase.isDrums());
        for (var ne : srcPhrase)
        {
            var newPosInBeats = ne.getPositionInBeats() + targetBeatRange.from;
            if (targetBeatRange.contains(newPosInBeats, true))
            {
                var newDur = ne.getDurationInBeats();
                if (newPosInBeats + newDur >= targetBeatRange.to)
                {
                    newDur = targetBeatRange.to - 0.001f - newPosInBeats;
                }
                var newNe = ne.setAll(ne.getPitch(), newDur, ne.getVelocity(), newPosInBeats, null, false);
                res.add(newNe);
            }
        }
        return res;
    }
    // =================================================================================================================
    // Inner classes
    // =================================================================================================================    
}
