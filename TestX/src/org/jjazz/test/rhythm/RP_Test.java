package org.jjazz.test.rhythm;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RpEditorDialogProvider;
import org.jjazz.rhythm.api.RpEnumerable;
import org.jjazz.ui.rpviewer.api.RpRenderer;
import org.jjazz.ui.rpviewer.api.RpViewer;
import org.jjazz.ui.rpviewer.spi.DefaultRpRendererFactory;
import org.jjazz.ui.rpviewer.spi.RpRendererFactory;
import org.jjazz.ui.rpviewer.spi.RpViewerSettings;

/**
 *
 * @author Administrateur
 */
public class RP_Test implements RhythmParameter<RP_TestValue>, RpEnumerable<RP_TestValue>, RpEditorDialogProvider<RP_TestValue>, RpRendererFactory
{

    private RhythmVoice rhythmVoice;
    private RP_TestValue value = new RP_TestValue();
    private RpRenderer rendererDelegate = DefaultRpRendererFactory.getDefault().getRpRenderer(DefaultRpRendererFactory.Type.METER, RpViewerSettings.getDefault());
    private static final Logger LOGGER = Logger.getLogger(RP_Test.class.getSimpleName());

    public RP_Test(RhythmVoice rv)
    {
        if (rv == null)
        {
            throw new NullPointerException("rv");
        }
        rhythmVoice = rv;
    }

    /**
     * @return The RhythmVoices impacted by this
     */
    public RhythmVoice getRhythmVoice()
    {
        return rhythmVoice;
    }

    @Override
    public String getId()
    {
        return "RP_TestId";
    }

    @Override
    public String getDisplayName()
    {
        return "RP_Test";
    }

    @Override
    public String getDescription()
    {
        return "RP_Test-desc";
    }

    @Override
    public String getValueDescription(RP_TestValue value)
    {
        return rhythmVoice.getName() + ": " + value.toDescriptionString();
    }

    @Override
    public RP_TestValue getDefaultValue()
    {
        return new RP_TestValue();
    }

    @Override
    public String valueToString(RP_TestValue v)
    {
        return RP_TestValue.saveAsString(v);
    }

    @Override
    public RP_TestValue stringToValue(String s)
    {
        return RP_TestValue.loadFromString(s);
    }

    @Override
    public boolean isValidValue(RP_TestValue value)
    {
        return value instanceof RP_TestValue;
    }

    // ======================================================================================
    // RpEditorDialogProvider
    // ======================================================================================    
    @Override
    public RP_TestValue editValueWithCustomDialog(RP_TestValue initValue)
    {
        var dlg = RP_TestEditorDialog.getInstance();
        dlg.preset(initValue);
        dlg.setVisible(true);
        return dlg.getValue();
    }

    // ======================================================================================
    // RpEnumerable interface
    // ======================================================================================  
    @Override
    public RP_TestValue getMaxValue()
    {
        return new RP_TestValue(50, 50, 50);
    }

    @Override
    public RP_TestValue getMinValue()
    {
        return new RP_TestValue(-50, -50, -50);
    }

    @Override
    public double calculatePercentage(RP_TestValue value)
    {
        return (value.getBassDrumOffset() + 64) / 127d;
    }

    @Override
    public RP_TestValue calculateValue(double percentage)
    {
        return new RP_TestValue((int) percentage - 50, 0, 0);
    }

    @Override
    public RP_TestValue getNextValue(RP_TestValue value)
    {
        int bd = value.getBassDrumOffset() + 1;
        if (bd > 63)
        {
            bd = -63;
        }
        return new RP_TestValue(bd, value.getSnareOffset(), value.getHiHatOffset());
    }

    @Override
    public RP_TestValue getPreviousValue(RP_TestValue value)
    {
        int bd = value.getBassDrumOffset() - 1;
        if (bd < -63)
        {
            bd = 63;
        }
        return new RP_TestValue(bd, value.getSnareOffset(), value.getHiHatOffset());
    }

    @Override
    public List<RP_TestValue> getPossibleValues()
    {
        return Arrays.asList(new RP_TestValue(-10, -10, -10), new RP_TestValue(-5, -5, -5), new RP_TestValue(00, 0, 0), new RP_TestValue(10, 10, 10));
    }

    // ======================================================================================
    // RpRendererFactory interface
    // ======================================================================================  
    @Override
    public RpRenderer getRpRenderer(RhythmParameter<?> rp, RpViewerSettings settings)
    {
        if (rp != this)
        {
            throw new IllegalArgumentException("rp=" + rp + " is different than this=" + this);
        }

        RpRenderer r = new RpRenderer()
        {
            @Override
            public void setRpViewer(RpViewer rpv)
            {
                rendererDelegate.setRpViewer(rpv);
            }

            @Override
            public RpViewer getRpViewer()
            {
                return rendererDelegate.getRpViewer();
            }

            @Override
            public Dimension getPreferredSize()
            {
                return rendererDelegate.getPreferredSize();
            }

            @Override
            public void paintComponent(Graphics g)
            {
                rendererDelegate.paintComponent(g);
            }

            @Override
            public void addChangeListener(ChangeListener l)
            {
                rendererDelegate.addChangeListener(l);
            }

            @Override
            public void removeChangeListener(ChangeListener l)
            {
                rendererDelegate.removeChangeListener(l);
            }
        };

        return r;
    }

}
