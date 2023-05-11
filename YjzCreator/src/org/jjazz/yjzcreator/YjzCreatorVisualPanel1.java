package org.jjazz.yjzcreator;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import org.jjazz.util.api.ResUtil;

public final class YjzCreatorVisualPanel1 extends JPanel
{

    /**
     * Creates new form YjzCreatorVisualPanel1 
     */
    public YjzCreatorVisualPanel1()
    {
        initComponents();
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(),"START");
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane2 = new javax.swing.JScrollPane();
        editorPane_intro = new javax.swing.JEditorPane();
        editorPane_intro.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE); // To make setFont work

        jScrollPane2.setBackground(null);
        jScrollPane2.setBorder(null);

        editorPane_intro.setEditable(false);
        editorPane_intro.setBackground(null);
        editorPane_intro.setBorder(null);
        editorPane_intro.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        editorPane_intro.setText(org.openide.util.NbBundle.getMessage(YjzCreatorVisualPanel1.class, "YjzCreatorVisualPanel1.editorPane_intro.text")); // NOI18N
        editorPane_intro.addHyperlinkListener(new javax.swing.event.HyperlinkListener()
        {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt)
            {
                editorPane_introHyperlinkUpdate(evt);
            }
        });
        jScrollPane2.setViewportView(editorPane_intro);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 541, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void editorPane_introHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt)//GEN-FIRST:event_editorPane_introHyperlinkUpdate
    {//GEN-HEADEREND:event_editorPane_introHyperlinkUpdate
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            org.jjazz.util.api.Utilities.openInBrowser(evt.getURL(), false);
        }
    }//GEN-LAST:event_editorPane_introHyperlinkUpdate

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane editorPane_intro;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables
}
