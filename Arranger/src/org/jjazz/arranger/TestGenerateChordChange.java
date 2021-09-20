package org.jjazz.arranger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.arranger.api.ArrangerTopComponent;
import org.jjazz.harmony.api.ChordSymbol;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;


@ActionID(
        category = "Edit",
        id = "org.jjazz.arranger.TestGenerateChordChange"
)
@ActionRegistration(
        displayName = "#CTL_TestGenerateChordChange"
)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = -100),
            @ActionReference(path = "Shortcuts", name = "O-N")
        })
@Messages("CTL_TestGenerateChordChange=Generate chord change")
public final class TestGenerateChordChange implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(TestGenerateChordChange.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        ArrangerPanel ap = ArrangerTopComponent.getInstance().getArrangerPanel();
        ChordSymbol cs = ChordSymbol.getRandom();
        while (cs.getChord().size() > 5)
        {
            cs = ChordSymbol.getRandom();
        }
        LOGGER.info("actionPerformed() calling ArrangerPanel.processIncomingChord() with notes for " + cs);
        ap.processIncomingChord(cs.getChord().getNotes());
    }
}
