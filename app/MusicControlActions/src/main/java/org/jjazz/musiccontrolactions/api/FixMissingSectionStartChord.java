/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2025 Jerome Lelasseux. All rights reserved.
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
package org.jjazz.musiccontrolactions.api;

import java.text.ParseException;
import java.util.Objects;
import java.util.prefs.Preferences;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

/**
 * Helper class to try to auto-fix missing chords at section start.
 */
public class FixMissingSectionStartChord
{


    private static final String PREF_AUTO_FIX_MISSING_SECTION_START_CHORD_ENABLED = "PrefAutoFixMissingSectionStartChordEnabled";
    private static final String PREF_WARNING_SHOWN = "PrefAutoFixWarningShown";
    private final SongContext songContext;
    private static final Preferences prefs = NbPreferences.forModule(FixMissingSectionStartChord.class);

    public FixMissingSectionStartChord(SongContext context)
    {
        Objects.requireNonNull(context);
        this.songContext = context;
    }

    static public boolean isEnabled()
    {
        return prefs.getBoolean(PREF_AUTO_FIX_MISSING_SECTION_START_CHORD_ENABLED, true);
    }

    static public void setEnabled(boolean b)
    {
        prefs.putBoolean(PREF_AUTO_FIX_MISSING_SECTION_START_CHORD_ENABLED, b);
    }

    public void autofix()
    {
        if (!isEnabled())
        {
            return;
        }

        final String ACTION_NAME = ResUtil.getString(FixMissingSectionStartChord.class, "FixMissingSectionStartChord");

        var cls = songContext.getSong().getChordLeadSheet();
        var um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(ACTION_NAME);


        for (var cliSection : songContext.getUniqueSections())
        {
            var sectionPos = cliSection.getPosition();
            var chords = cls.getItems(cliSection, CLI_ChordSymbol.class);
            if (chords.isEmpty() || !chords.getFirst().getPosition().equals(sectionPos))
            {
                var moveChord = getMoveChordFix(cliSection);
                if (moveChord != null)
                {
                    // Just move it
                    showWarningIfRequired(cliSection);
                    cls.moveItem(moveChord, sectionPos);
                } else
                {
                    // Search for a chord to copy
                    var newChord = getCopyChordFix(cliSection);
                    if (newChord != null)
                    {
                        showWarningIfRequired(cliSection);
                        cls.addItem(newChord);
                    }
                }
            }
        }


        um.endCEdit(ACTION_NAME);
    }

    /**
     * Check if we can just fix the issue by moving a close chord on same bar.
     *
     * @param cliSection
     * @return
     */
    private CLI_ChordSymbol getMoveChordFix(CLI_Section cliSection)
    {
        CLI_ChordSymbol res = null;
        var cls = cliSection.getContainer();
        int bar = cliSection.getPosition().getBar();
        var chords = cls.getItems(cliSection, CLI_ChordSymbol.class);
        if (!chords.isEmpty())
        {
            var chordFirst = chords.getFirst();
            var pos = chordFirst.getPosition();
            if (pos.getBar() == bar && pos.getBeat() <= 1f)
            {
                res = chordFirst;
            }
        }
        return res;
    }


    /**
     * Get the chord to add at cliSection's start.
     *
     * @param cliSection
     * @return Can be null.
     */
    private CLI_ChordSymbol getCopyChordFix(CLI_Section cliSection)
    {
        CLI_ChordSymbol res = null;
        int bar = cliSection.getPosition().getBar();
        var cls = cliSection.getContainer();

        // We don't take into account the song structure to keep things simple for user

        if (bar > 0)
        {
            // Case of an anticipated chord just before our section
            var ts = cliSection.getData().getTimeSignature();
            var anticipatedChord = cls.getBarLastItem(bar - 1, CLI_ChordSymbol.class, cliCs -> cliCs.getPosition().isLastBarBeat(ts));
            if (anticipatedChord != null)
            {
                res = getCopy(anticipatedChord, bar, false);
            }
        }

        if (res == null)
        {
            // Use the nearest chord symbol using a simplified position comparison
            var allChords = cls.getItems(CLI_ChordSymbol.class);
            CLI_ChordSymbol nearest = null;
            float nearestSimplifiedPos = Float.MAX_VALUE;
            for (var chord : allChords)
            {
                var pos = chord.getPosition();
                float simplifiedBeatPos = pos.getBar() * 4 + pos.getBeat();         // we ignore time signature and song structure

                if (nearest == null)
                {
                    nearest = chord;
                    nearestSimplifiedPos = simplifiedBeatPos;
                } else
                {
                    if (Math.abs(simplifiedBeatPos - 4 * bar) < Math.abs(nearestSimplifiedPos - 4 * bar))
                    {
                        nearest = chord;
                        nearestSimplifiedPos = simplifiedBeatPos;
                    }
                }
            }

            if (nearest != null)
            {
                res = getCopy(nearest, bar, nearest.getPosition().getBar() == bar);
            }
        }

        return res;
    }

    private CLI_ChordSymbol getCopy(CLI_ChordSymbol srcChord, int destBar, boolean copyRenderingInfo)
    {
        CLI_ChordSymbol res = null;
        var newPos = new Position(destBar);

        if (copyRenderingInfo)
        {
            res = (CLI_ChordSymbol) srcChord.getCopy(null, newPos);
        } else
        {
            try
            {
                var newEcs = ExtChordSymbol.get(srcChord.getData().getOriginalName(), new ChordRenderingInfo(), null, null);
                res = CLI_Factory.getDefault().createChordSymbol(newEcs, newPos);
            } catch (ParseException ex)
            {
                // Should never happen : we don't change name
                Exceptions.printStackTrace(ex);
            }
        }
        return res;
    }

    private void showWarningIfRequired(CLI_Section cliSection)
    {
        if (prefs.getBoolean(PREF_WARNING_SHOWN, false))
        {
            return;
        }

        prefs.putBoolean(PREF_WARNING_SHOWN, true);
        String msg = ResUtil.getString(getClass(), "FixMissingSectionStartChordWarning", cliSection.getData().getName());
        NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
        DialogDisplayer.getDefault().notify(nd);

    }
}
