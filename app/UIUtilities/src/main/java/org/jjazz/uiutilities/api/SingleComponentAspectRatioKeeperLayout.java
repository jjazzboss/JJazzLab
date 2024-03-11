package org.jjazz.uiutilities.api;


import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/**
 * A Swing Layout that will shrink or enlarge to keep the content of a container while keeping it's aspect ratio.
 * <p>
 * The caveat is that only a single component is supported or an exception will be thrown. This is the component's
 * {@link Component#getPreferredSize()} method that must return the correct ratio. The preferredSize will not be preserved but the
 * ratio will.
 *
 * @author @francoismarot
 * @see https://gist.github.com/fmarot/f04346d0e989baef1f56ffd83bbf764d
 */
public class SingleComponentAspectRatioKeeperLayout implements LayoutManager
{

    /**
     * Will be used for calculus in case no real component is in the parent
     */
    private static final Component fakeComponent = new JPanel();

    public SingleComponentAspectRatioKeeperLayout()
    {
        fakeComponent.setPreferredSize(new Dimension(0, 0));
    }

    @Override
    public void addLayoutComponent(String arg0, Component arg1)
    {
    }

    @Override
    public void layoutContainer(Container parent)
    {
        Component component = getSingleComponent(parent);
        Insets insets = parent.getInsets();
        int maxWidth = parent.getWidth() - (insets.left + insets.right);
        int maxHeight = parent.getHeight() - (insets.top + insets.bottom);

        Dimension prefferedSize = component.getPreferredSize();
        Dimension targetDim = getScaledDimension(prefferedSize, new Dimension(maxWidth, maxHeight));

        double targetWidth = targetDim.getWidth();
        double targetHeight = targetDim.getHeight();

        double hgap = (maxWidth - targetWidth) / 2;
        double vgap = (maxHeight - targetHeight) / 2;

        // Set the single component's size and position.
        component.setBounds((int) hgap, (int) vgap, (int) targetWidth, (int) targetHeight);
    }

    private Component getSingleComponent(Container parent)
    {
        int parentComponentCount = parent.getComponentCount();
        if (parentComponentCount > 1)
        {
            throw new IllegalArgumentException(this.getClass().getSimpleName()   
                    + " can not handle more than one component");
        }
        Component comp = (parentComponentCount == 1) ? parent.getComponent(0) : fakeComponent;
        return comp;
    }

    private Dimension getScaledDimension(Dimension imageSize, Dimension boundary)
    {
        double widthRatio = boundary.getWidth() / imageSize.getWidth();
        double heightRatio = boundary.getHeight() / imageSize.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);
        return new Dimension((int) (imageSize.width * ratio), (int) (imageSize.height * ratio));
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        return preferredLayoutSize(parent);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        return getSingleComponent(parent).getPreferredSize();
    }

    @Override
    public void removeLayoutComponent(Component parent)
    {
    }

}
