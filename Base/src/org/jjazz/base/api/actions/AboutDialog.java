package org.jjazz.base.api.actions;

import java.awt.Image;
import java.io.File;
import java.util.Locale;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JTextArea;
import org.jjazz.base.api.AuthorizationManager;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.modules.Places;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

public class AboutDialog extends javax.swing.JDialog
{

    private static final String BRANDING_SPLASH_IMAGE = "org/netbeans/core/startup/splash.gif";
    private static final String LICENSE_FILENAME = "License.txt";
    private static final String LICENSE_RESOURCE = "resources/" + LICENSE_FILENAME;
    private static final Logger LOGGER = Logger.getLogger(AboutDialog.class.getSimpleName());

    /**
     * Creates new form AboutDialog
     */
    public AboutDialog()
    {
        super(WindowManager.getDefault().getMainWindow(), true);
        initComponents();


        // Reuse branding splash image if there is one
        Image splash = ImageUtilities.loadImage(BRANDING_SPLASH_IMAGE, true);      // true to get the branded version
        if (splash != null)
        {
            iconImage.setIcon(new ImageIcon(splash));
        }

        // Update the JJazzLab version
        String version = System.getProperty("jjazzlab.version");
        if (version == null)
        {
            version = "";
        }
        String msg = "JJazzLab version: " + version + "\n";


        // Authorization code
        var am = AuthorizationManager.getDefault();
        String authorizationCode = am != null ? am.getRegisteredCodeExpirationDateAsString() : null;
        msg += "Authorization code: " + (authorizationCode == null ? "No" : "Yes, expiration date=" + authorizationCode);
        ta_header.setText(msg);
        ta_header.setCaretPosition(0);


        // Update the system info
        updateSysInfo(ta_sysInfo);


        pack();
        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Fill textArea with system/product info
     *
     * @param textArea
     */
    private void updateSysInfo(JTextArea textArea)
    {
        var fdm = FileDirectoryManager.getInstance();
        String text = "";
        text += "OS: name=" + System.getProperty("os.name", "?") + ", version=" + System.getProperty("os.version", "?") + " arch=" + System.getProperty(
                "os.arch", "?") + ", vendor=" + System.getProperty("java.vm.vendor", "?") + "\n";
        text += "Java: version=" + System.getProperty("java.version", "?") + ", vm.name=" + System.getProperty("java.vm.name", "?") + ", vm.version=" + System.getProperty(
                "java.vm.version", "?") + ", runtime=" + System.getProperty("java.runtime.name", "?") + "\n";
        text += "Java: java.home=" + System.getProperty("java.home", "?") + ", jdk.home=" + System.getProperty("jdk.home", "?") + "\n";
        text += "Locale: " + getSystemLocale() + "\n";
        text += "File.separator: " + System.getProperty("file.separator", "?") + "\n";
        text += "User.name: " + System.getProperty("user.name", "?") + "\n";
        text += "User.home: " + System.getProperty("user.home", "?") + "\n";
        text += "User.dir: " + System.getProperty("user.dir", "?") + "\n";
        text += "User.country: " + System.getProperty("user.country", "?") + "\n";
        text += "User.language: " + System.getProperty("user.language", "?") + "\n";
        text += "File.encoding: " + System.getProperty("file.encoding", "?") + "\n";
        text += "Netbeans.buildnumber: " + System.getProperty("netbeans.buildnumber", "?") + "\n";
        text += "Netbeans.home: " + System.getProperty("netbeans.home", "?") + "\n";
        text += "Netbeans user dir: " + Places.getUserDirectory() + "\n";
        text += "Netbeans cache dir: " + Places.getCacheDirectory() + "\n";
        text += "Netbeans.default_userdir_root: " + System.getProperty("netbeans.default_userdir_root", "?") + "\n";
        text += "Nb.native.filechooser: " + System.getProperty("nb.native.filechooser", "?") + "\n";
        text += "JJazzLab user dir: " + fdm.getJJazzLabUserDirectory() + "\n";
        text += "JJazzLab rhythm user dir: " + fdm.getUserRhythmDirectory() + "\n";
        text += "JJazzLab app config dir: " + fdm.getAppConfigDirectory(null) + "\n";
        text += "JJazzLab rhythm mix dir: " + fdm.getRhythmMixDirectory() + "\n";

        textArea.setText(text);
        textArea.setCaretPosition(0);
    }

    public static String getSystemLocale()
    {
        String branding;
        return Locale.getDefault().toString() + ((branding = NbBundle.getBranding()) == null ? "" : (" (" + branding + ")")); // NOI18N
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane2 = new javax.swing.JScrollPane();
        ta_header = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        ta_sysInfo = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        btn_license = new javax.swing.JButton();
        pnl_image = new javax.swing.JPanel();
        iconImage = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(AboutDialog.class, "AboutDialog.title")); // NOI18N

        ta_header.setEditable(false);
        ta_header.setColumns(20);
        ta_header.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        ta_header.setRows(5);
        jScrollPane2.setViewportView(ta_header);

        ta_sysInfo.setEditable(false);
        ta_sysInfo.setColumns(20);
        ta_sysInfo.setFont(new java.awt.Font("Courier New", 0, 11)); // NOI18N
        ta_sysInfo.setRows(5);
        ta_sysInfo.setText("sys info"); // NOI18N
        jScrollPane1.setViewportView(ta_sysInfo);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(AboutDialog.class, "AboutDialog.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_license, org.openide.util.NbBundle.getMessage(AboutDialog.class, "AboutDialog.btn_license.text")); // NOI18N
        btn_license.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_licenseActionPerformed(evt);
            }
        });

        iconImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/base/api/actions/resources/DefaultAboutImage.png"))); // NOI18N
        iconImage.setFocusable(false);
        pnl_image.add(iconImage);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2)
                            .addComponent(jScrollPane1))
                        .addContainerGap())))
            .addComponent(pnl_image, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_license)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnl_image, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 16, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btn_license)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_licenseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_licenseActionPerformed
    {//GEN-HEADEREND:event_btn_licenseActionPerformed
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        File configDir = fdm.getAppConfigDirectory(null);
        File licenseFile = new File(configDir, LICENSE_FILENAME);
        if (!licenseFile.exists())
        {
            // Copy the resource file to the config directory
            if (!org.jjazz.utilities.api.Utilities.copyResource(getClass(), LICENSE_RESOURCE, licenseFile.toPath()))
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ResUtil.getString(getClass(), "ERR_CantReadLicenceFile", LICENSE_RESOURCE),
                        NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            }
        }

        Utilities.openFile(licenseFile, false);

    }//GEN-LAST:event_btn_licenseActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_license;
    private javax.swing.JLabel iconImage;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel pnl_image;
    private javax.swing.JTextArea ta_header;
    private javax.swing.JTextArea ta_sysInfo;
    // End of variables declaration//GEN-END:variables
}
