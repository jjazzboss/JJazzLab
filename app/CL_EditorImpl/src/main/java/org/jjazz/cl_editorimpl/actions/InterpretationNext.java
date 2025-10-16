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
package org.jjazz.cl_editorimpl.actions;

import java.awt.event.ActionEvent;
import java.util.EnumSet;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;


/**
 * Change to the next accent.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.interpretationnext")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolInterpretation", position = 5, separatorAfter = 6),
            @ActionReference(path = "Shortcuts", name="P")
        })
public final class InterpretationNext extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("P");
    private CL_ContextActionSupport cap;
    private final String undoText = ResUtil.getString(getClass(), "CTL_InterpretationNext");

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_InterpretationNext"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION));        
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);


        for (CLI_ChordSymbol item : selection.getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = item.getData();
            ChordRenderingInfo newCri = next(ecs.getRenderingInfo());
            ExtChordSymbol newCs = ecs.getCopy(null, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
            item.getContainer().changeItem(item, newCs);
        }


        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        setEnabled(selection.isChordSymbolSelected());
    }


    // ====================================================================================================================
    // Private methods
    // ====================================================================================================================
    private ChordRenderingInfo next(ChordRenderingInfo cri)
    {
        var features = cri.getFeatures();

        if (!cri.hasOneFeature(Feature.HOLD, Feature.SHOT, Feature.ACCENT, Feature.ACCENT_STRONGER))
        {
            features.add(Feature.ACCENT);       // NORMAL => ACCENT                      

        } else if (cri.hasOneFeature(Feature.ACCENT))
        {
            if (!cri.hasOneFeature(Feature.HOLD, Feature.SHOT))
            {
                features.add(Feature.HOLD);               // ACCENT + HOLD 
            } else if (cri.hasOneFeature(Feature.HOLD))
            {
                features.remove(Feature.HOLD);
                features.add(Feature.SHOT);               // ACCENT + HOLD => ACCENT + SHOT
            } else
            {
                features.remove(Feature.SHOT);              // ACCENT + SHOT => NORMAL
                features.remove(Feature.ACCENT);
            }
        } else if (cri.hasOneFeature(Feature.ACCENT_STRONGER))
        {
            if (!cri.hasOneFeature(Feature.HOLD, Feature.SHOT))
            {
                features.add(Feature.HOLD);               // ACCENT_STRONGER + HOLD 
            } else if (cri.hasOneFeature(Feature.HOLD))
            {
                features.remove(Feature.HOLD);
                features.add(Feature.SHOT);               // ACCENT_STRONGER + HOLD => ACCENT_STRONGER + SHOT
            } else
            {
                features.remove(Feature.SHOT);              // ACCENT_STRONGER + SHOT => NORMAL
                features.remove(Feature.ACCENT_STRONGER);
            }
        } else if (cri.hasOneFeature(Feature.HOLD, Feature.SHOT))
        {
            features.remove(Feature.SHOT);                  // Should never be here since HOLD/SHOT should have ACCENT too, just for robustness
            features.remove(Feature.HOLD);
        }
        ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());

        return newCri;
    }


}
