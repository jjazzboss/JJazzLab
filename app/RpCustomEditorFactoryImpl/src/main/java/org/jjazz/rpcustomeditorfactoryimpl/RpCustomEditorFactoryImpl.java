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
package org.jjazz.rpcustomeditorfactoryimpl;

import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorDialog;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_OverrideTracks;
import org.jjazz.ss_editor.rpviewer.api.RpCustomEditorDialog;
import org.jjazz.ss_editor.rpviewer.spi.RpCustomEditorFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 * A default factory for RpCustomEditors.
 * <p>
 */
@ServiceProvider(service = RpCustomEditorFactory.class)
public class RpCustomEditorFactoryImpl implements RpCustomEditorFactory
{

    @Override
    public boolean isSupported(RhythmParameter<?> rp)
    {
        boolean b = switch (rp)
        {
            case RP_SYS_CustomPhrase rps ->
                true;
            case RP_SYS_DrumsTransform rps ->
                true;
            case RP_SYS_OverrideTracks rps ->
                true;
            default ->
                false;
        };
        return b;
    }

    @Override
    public <E> RpCustomEditorDialog<E> getEditor(RhythmParameter<E> rp)
    {
        RpCustomEditorDialog res = switch (rp)
        {
            case RP_SYS_CustomPhrase rps ->
                new RP_SYS_CustomPhraseEditor(rps);
            case RP_SYS_DrumsTransform rps ->
                new RealTimeRpEditorDialog(new RP_SYS_DrumsTransformComp(rps));
            case RP_SYS_OverrideTracks rps ->
                new RealTimeRpEditorDialog(new RP_SYS_OverrideTracksComp(rps));
            default -> throw new IllegalArgumentException("rp=" + rp);
        };

        return res;
    }


}
