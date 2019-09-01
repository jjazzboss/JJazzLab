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
package org.jjazz.base.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.SwingPropertyChangeSupport;
import static org.jjazz.base.actions.Bundle.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Actions;
import org.openide.modules.OnStop;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.actions.Presenter;

/**
 * Work in conjunction with a RecentFilesProvider to get the opened and closed files, and the open action associated to each file
 * (used in the menu item).
 * <p>
 * If no RecentFilesProvider implementation is found in the global lookup, the class does nothing.
 */
@ActionID(category = "File", id = "org.jjazz.base.actions.OpenRecentFile")
@ActionRegistration(displayName = "#CTL_OpenRecentFile", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 7)
        })
@Messages(
        {
            "CTL_OpenRecentFile=Open recent file"
        })

public final class OpenRecentFile extends AbstractAction implements Presenter.Menu, Presenter.Popup, PropertyChangeListener
{

    public static OpenRecentFile INSTANCE;
    private static final String PREF_RECENT_FILES = "RecentFiles";
    private static final String PREF_MAX_NB_FILES = "NbMaxRecentFiles";
    /**
     * Used as the Preference id and property change.
     */
    public static final String PREF_OPEN_RECENT_FILE_UPON_STARTUP = "OpenRecentFileUponStartup";
    private RecentFilesProvider rfProvider;
    private JMenu subMenu;
    /**
     * Reference to the first menu item.
     */
    private JMenuItem firstMenuItem;
    private ArrayList<File> recentFiles;
    private ArrayList<File> openFiles;
    private static Preferences prefs = NbPreferences.forModule(OpenRecentFile.class);
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(OpenRecentFile.class.getSimpleName());

    public OpenRecentFile()
    {
        if (INSTANCE != null)
        {
            throw new IllegalStateException("INSTANCE=" + INSTANCE);
        }
        INSTANCE = this;
        rfProvider = RecentFilesProvider.getDefault();
        if (rfProvider == null)
        {
            LOGGER.log(Level.WARNING, "No RecentFilesProvider found. Recent files list will be disabled.");
            putValue(Action.NAME, "Disabled");
            setEnabled(false);
            return;
        }
        rfProvider.addPropertyChangeListener(this);

        // Initialize the file list
        recentFiles = new ArrayList<>();
        openFiles = new ArrayList<>();
        String s = prefs.get(PREF_RECENT_FILES, "").trim();
        if (!s.isEmpty())
        {
            List<String> strFiles = Arrays.asList(s.split("\\s*,\\s*"));
            int max = Math.min(strFiles.size(), getNbMaxFiles());
            for (int i = 0; i < max; i++)
            {
                recentFiles.add(new File(strFiles.get(i)));
            }
        }

        // Build the menu
        subMenu = new JMenu();
        subMenu.setText(CTL_OpenRecentFile());
        updateMenuItems();

        // Open last recent file or create a new file if no recent file
        if (isOpenRecentFileUponStartup() && firstMenuItem != null)
        {
            Runnable doRun = new Runnable()
            {
                @Override
                public void run()
                {
                    firstMenuItem.doClick();
                }
            };
            SwingUtilities.invokeLater(doRun);
        } else
        {
            // Do nothing: faster startup, better for the Getting Started tutorial
//            String cat = "File";
//            String id = "org.jjazz.songeditormanager.NewSong";
//            final Action a = Actions.forID(cat, id);
//            if (a != null)
//            {
//                Runnable doRun = new Runnable()
//                {
//                    @Override
//                    public void run()
//                    {
//                        a.actionPerformed(null);
//                    }
//                };
//                SwingUtilities.invokeLater(doRun);
//            } else
//            {
//                LOGGER.warning("OpenRecentFile() no action found for category=" + cat + ", id=" + id);
//            }
        }
    }

    public void setOpenRecentFileUponStartup(boolean b)
    {
        if (b != isOpenRecentFileUponStartup())
        {
            prefs.putBoolean(PREF_OPEN_RECENT_FILE_UPON_STARTUP, b);
            pcs.firePropertyChange(PREF_OPEN_RECENT_FILE_UPON_STARTUP, !b, b);
        }
    }

    public boolean isOpenRecentFileUponStartup()
    {
        return prefs.getBoolean(PREF_OPEN_RECENT_FILE_UPON_STARTUP, true);
    }

    /**
     * We should use a Runnable, but then Preferences are not saved ! (probably too late ?) Use a Callable<> as workaround: if
     * another Callable vetoes the shutdown, we'll have updated the recent list anyway...
     */
    @OnStop
    public static class Shutdown implements Callable<Boolean>
    {

        @Override
        public Boolean call() throws Exception
        {
            INSTANCE.shutdown();
            return Boolean.TRUE;
        }
    }

    public void shutdown()
    {
        if (rfProvider != null)
        {
            LOGGER.log(Level.FINE, "shutdown() openFiles=" + openFiles + " recentFiles=" + recentFiles);
            rfProvider.removePropertyChangeListener(this);
            for (File f : openFiles)
            {
                if (recentFiles.size() >= getNbMaxFiles())
                {
                    break;
                }
                if (!recentFiles.contains(f))
                {
                    recentFiles.add(0, f);
                }
            }
            updatePreferences();
        }
    }

    /**
     * File was open, must be removed from the recent files list.
     *
     * @param f
     */
    private void fileOpened(File f)
    {
        if (!openFiles.contains(f))
        {
            openFiles.add(f);
        }
        recentFiles.remove(f);
        updatePreferences();
        updateMenuItems();
    }

    /**
     * file was closed, must be added to the recent files list. Latest first.
     *
     * @param f
     */
    private void fileClosed(File f)
    {
        if (recentFiles.contains(f))
        {
            recentFiles.remove(f);
        }
        if (recentFiles.size() >= getNbMaxFiles())
        {
            // Max size will be exceeded, remove the last file
            recentFiles.remove(recentFiles.size() - 1);
        }
        recentFiles.add(0, f);
        openFiles.remove(f);
        updatePreferences();
        updateMenuItems();
    }

    private void updateMenuItems()
    {
        subMenu.removeAll();
        firstMenuItem = null;
        for (final File f : recentFiles)
        {
            JMenuItem mi = createMenuItem(f);
            subMenu.add(mi);
            if (firstMenuItem == null)
            {
                firstMenuItem = mi;
            }
        }
        subMenu.setEnabled(recentFiles.size() > 0);
    }

    private void updatePreferences()
    {
        LOGGER.log(Level.FINE, "updatePreferences() recentFiles=" + recentFiles);
        if (recentFiles.size() == 0)
        {
            prefs.put(PREF_RECENT_FILES, "");
            return;
        }
        StringBuilder sb = new StringBuilder(recentFiles.get(0).getAbsolutePath());
        for (int i = 1; i < recentFiles.size(); i++)
        {
            sb.append(",").append(recentFiles.get(i).getAbsolutePath());
        }
        prefs.put(PREF_RECENT_FILES, sb.toString());
    }

    private JMenuItem createMenuItem(final File f)
    {
        JMenuItem mi = new JMenuItem(f.getName());
        mi.setToolTipText(f.getAbsolutePath());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (!rfProvider.open(f))
                {
                    // There was a problem opening this file, remove it from the recent list
                    fileOpened(f);
                }
            }
        });
        return mi;
    }

    private int getNbMaxFiles()
    {
        return Math.max(Math.min(20, prefs.getInt(PREF_MAX_NB_FILES, 10)), 1);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Useless
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
        return getMenuPresenter();
    }

    @Override
    public JMenuItem getMenuPresenter()
    {
        return subMenu;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName() == RecentFilesProvider.PROP_FILE_OPENED)
        {
            LOGGER.log(Level.FINE, "propertyChange() FILE_OPENED");
            File f = (File) evt.getNewValue();
            if (f == null)
            {
                LOGGER.log(Level.WARNING, "propertyChange() FILE_OPENED but newValue is null. Recent Files will not be updated.");
            } else
            {
                fileOpened(f);
            }
        } else if (evt.getPropertyName() == RecentFilesProvider.PROP_FILE_CLOSED)
        {
            LOGGER.log(Level.FINE, "propertyChange() FILE_CLOSED");
            File f = (File) evt.getNewValue();
            if (f == null)
            {
                LOGGER.log(Level.WARNING, "propertyChange() FILE_CLOSED but newValue is null. Recent Files will not be updated.");
            } else
            {
                fileClosed(f);
            }
        }
    }

    public void addPropertyListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

}
