
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
package org.jjazz.phrasetransform;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.swing.JDialog;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransforms;
import org.jjazz.phrasetransform.api.PtProperties;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.ui.utilities.api.Utilities;
import org.openide.windows.WindowManager;

/**
 * A generic transform for drums or melodic phrases, with one Strength property.
 * <p>
 */
public class GenericTransform implements PhraseTransform
{

    public interface NoteMapper
    {

        /**
         * Transform a note into another.
         * <p>
         * Any attribute of the NoteEvent can be changed, position, duration, pitch, etc. but the note position must remain within
         * the source phrase beat range.
         *
         * @param ne
         * @param strength A 0-127 value representing the "strength" of the transformation, default is 64.
         * @return
         */
        NoteEvent apply(NoteEvent ne, int strength);
    }

    public static final String PROP_STRENGTH = "Strength";

    private final PtProperties properties;
    private final Predicate<NoteEvent> tester;
    private final NoteMapper noteMapper;
    private final boolean drumsTransform;
    private final String strengthDisplayName;
    private GenericSettingsPanel propertyEditorPanel;
    private final Info info;
    private static final Logger LOGGER = Logger.getLogger(GenericTransform.class.getSimpleName());


    /**
     * Create a transform which map some notes to other notes.
     *
     * @param info
     * @param drumsTransform If true, getFitScore() will return 100 for drums phrases, 0 otherwise. If false do the oppposite.
     * @param strengthDisplayName The display name used for the strength property
     * @param tester Notes which match the tester will be mapped using the mapper, otherwise they are just reused as is.
     * @param noteMapper Convert a note into another one, everything can be changed (pitch, position, duration, etc.). The new
     * note must remain in the range of the source phrase.
     */
    public GenericTransform(PhraseTransform.Info info,
            boolean drumsTransform,
            String strengthDisplayName,
            Predicate<NoteEvent> tester,
            NoteMapper noteMapper)
    {
        checkNotNull(tester);
        checkNotNull(noteMapper);
        checkNotNull(strengthDisplayName);

        this.drumsTransform = drumsTransform;
        this.info = info;
        this.tester = tester;
        this.noteMapper = noteMapper;
        this.strengthDisplayName = strengthDisplayName;

        Properties defaults = new Properties();
        defaults.setProperty(PROP_STRENGTH, Integer.toString(30));
        properties = new PtProperties(defaults);


    }

    @Override
    public Info getInfo()
    {
        return info;
    }


    @Override
    public SizedPhrase transform(SizedPhrase inPhrase, SongPartContext context)
    {
        SizedPhrase res = new SizedPhrase(inPhrase.getChannel(), inPhrase.getBeatRange(), inPhrase.getTimeSignature());
        int strength = getStrength();
        for (var ne : inPhrase)
        {
            NoteEvent nne = ne;
            if (tester.test(ne))
            {
                nne = noteMapper.apply(ne, strength);
            }
            res.addOrdered(nne);
        }

        return res;
    }

    @Override
    public int getFitScore(SizedPhrase inPhrase, SongPartContext context)
    {
        if (drumsTransform)
        {
            return PhraseTransforms.getRhythmVoice(inPhrase, context).isDrums() ? 100 : 0;
        } else
        {
            return PhraseTransforms.getRhythmVoice(inPhrase, context).isDrums() ? 0 : 100;
        }
    }


    @Override
    public PtProperties getProperties()
    {
        return properties;
    }

    public void setStrength(int offset)
    {
        properties.setProperty(PROP_STRENGTH, Integer.toString(offset));
    }

    public int getStrength()
    {
        return properties.getPropertyAsInteger(PROP_STRENGTH);
    }

    public void setEditorPanel(GenericSettingsPanel panel)
    {
        propertyEditorPanel = panel;
    }

    @Override
    public void showUserSettingsDialog(Component anchor)
    {
        JDialog dlg = new JDialog(WindowManager.getDefault().getMainWindow(), true);
        if (propertyEditorPanel == null)
        {
            propertyEditorPanel = new GenericSettingsPanel(PROP_STRENGTH, strengthDisplayName, false);
        }
        propertyEditorPanel.preset(properties);
        dlg.add(propertyEditorPanel);
        dlg.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseExited(MouseEvent me)
            {
                dlg.setVisible(false);
            }
        });
        dlg.pack();
        Utilities.setDialogLocationRelativeTo(dlg, anchor, 10, 0.5, 1);
        dlg.setVisible(true);
        dlg.dispose();
    }

    @Override
    public boolean hasUserSettings()
    {
        return true;
    }

    @Override
    public int hashCode()
    {
        return PhraseTransform.hashCode(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        return PhraseTransform.equals(this, obj);
    }


    @Override
    public GenericTransform getCopy()
    {
        var res = new GenericTransform(info, drumsTransform, strengthDisplayName, tester, noteMapper);
        res.setEditorPanel(propertyEditorPanel);
        return res;
    }


}
