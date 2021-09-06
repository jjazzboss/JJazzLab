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
package org.jjazz.rpcustomeditorfactoryimpl;


import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.awt.datatransfer.*;

/**
 * Demonstration of the top-level {@code TransferHandler} support on {@code JFrame}.
 *
 * @author Shannon Hickey
 */
public class Test extends JFrame
{

    private static boolean DEMO = false;

    private JDesktopPane dp = new JDesktopPane();
    private DefaultListModel listModel = new DefaultListModel();
    private JList list = new JList(listModel);
    private static int left;
    private static int top;
    private JCheckBoxMenuItem copyItem;
    private JCheckBoxMenuItem nullItem;
    private JCheckBoxMenuItem thItem;

    private class Doc extends InternalFrameAdapter implements ActionListener
    {

        String name;
        JInternalFrame frame;
        TransferHandler th;
        JTextArea area;

        public Doc(File file)
        {
            this.name = file.getName();
            try
            {
                init(file.toURI().toURL());
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
        }

        public Doc(String name)
        {
            this.name = name;
            init(getClass().getResource(name));
        }

        private void init(URL url)
        {
            frame = new JInternalFrame(name);
            frame.addInternalFrameListener(this);
            listModel.add(listModel.size(), this);

            area = new JTextArea();
            area.setMargin(new Insets(5, 5, 5, 5));

            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String in;
                while ((in = reader.readLine()) != null)
                {
                    area.append(in);
                    area.append("\n");
                }
                reader.close();
            } catch (Exception e)
            {
                e.printStackTrace();
                return;
            }

            th = area.getTransferHandler();
            area.setFont(new Font("monospaced", Font.PLAIN, 12));
            area.setCaretPosition(0);
            area.setDragEnabled(true);
            area.setDropMode(DropMode.INSERT);
            frame.getContentPane().add(new JScrollPane(area));
            dp.add(frame);
            frame.show();
            if (DEMO)
            {
                frame.setSize(300, 200);
            } else
            {
                frame.setSize(400, 300);
            }
            frame.setResizable(true);
            frame.setClosable(true);
            frame.setIconifiable(true);
            frame.setMaximizable(true);
            frame.setLocation(left, top);
            incr();
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    select();
                }
            });
            nullItem.addActionListener(this);
            setNullTH();
        }

        public void internalFrameClosing(InternalFrameEvent event)
        {
            listModel.removeElement(this);
            nullItem.removeActionListener(this);
        }

        public void internalFrameOpened(InternalFrameEvent event)
        {
            int index = listModel.indexOf(this);
            list.getSelectionModel().setSelectionInterval(index, index);
        }

        public void internalFrameActivated(InternalFrameEvent event)
        {
            int index = listModel.indexOf(this);
            list.getSelectionModel().setSelectionInterval(index, index);
        }

        public String toString()
        {
            return name;
        }

        public void select()
        {
            try
            {
                frame.toFront();
                frame.setSelected(true);
            } catch (java.beans.PropertyVetoException e)
            {
            }
        }

        public void actionPerformed(ActionEvent ae)
        {
            setNullTH();
        }

        public void setNullTH()
        {
            if (nullItem.isSelected())
            {
                area.setTransferHandler(null);
            } else
            {
                area.setTransferHandler(th);
            }
        }
    }

    private TransferHandler handler = new TransferHandler()
    {
        public boolean canImport(TransferHandler.TransferSupport support)
        {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
            {
                return false;
            }

            if (copyItem.isSelected())
            {
                boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;

                if (!copySupported)
                {
                    return false;
                }

                support.setDropAction(COPY);
            }

            return true;
        }

        public boolean importData(TransferHandler.TransferSupport support)
        {
            if (!canImport(support))
            {
                return false;
            }

            Transferable t = support.getTransferable();

            try
            {
                java.util.List<File> l
                        = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

                for (File f : l)
                {
                    new Doc(f);
                }
            } catch (UnsupportedFlavorException e)
            {
                return false;
            } catch (IOException e)
            {
                return false;
            }

            return true;
        }
    };

    private static void incr()
    {
        left += 30;
        top += 30;
        if (top == 150)
        {
            top = 0;
        }
    }

    public Test()
    {
        super("TopLevelTransferHandlerDemo");
        setJMenuBar(createDummyMenuBar());
        getContentPane().add(createDummyToolBar(), BorderLayout.NORTH);

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, list, dp);
        sp.setDividerLocation(120);
        getContentPane().add(sp);
        //new Doc("sample.txt");
        //new Doc("sample.txt");
        //new Doc("sample.txt");

        list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                if (e.getValueIsAdjusting())
                {
                    return;
                }

                Doc val = (Doc) list.getSelectedValue();
                if (val != null)
                {
                    val.select();
                }
            }
        });

        final TransferHandler th = list.getTransferHandler();

        nullItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                if (nullItem.isSelected())
                {
                    list.setTransferHandler(null);
                } else
                {
                    list.setTransferHandler(th);
                }
            }
        });
        thItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                if (thItem.isSelected())
                {
                    setTransferHandler(handler);
                } else
                {
                    setTransferHandler(null);
                }
            }
        });
        dp.setTransferHandler(handler);
    }

    private static void createAndShowGUI(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e)
        {
        }

        Test test = new Test();
        test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (DEMO)
        {
            test.setSize(493, 307);
        } else
        {
            test.setSize(800, 600);
        }
        test.setLocationRelativeTo(null);
        test.setVisible(true);
        test.list.requestFocus();
    }

    public static void main(final String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI(args);
            }
        });
    }

    private JToolBar createDummyToolBar()
    {
        JToolBar tb = new JToolBar();
        JButton b;
        b = new JButton("New");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        b = new JButton("Open");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        b = new JButton("Save");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        b = new JButton("Print");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        b = new JButton("Preview");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        tb.setFloatable(false);
        return tb;
    }

    private JMenuBar createDummyMenuBar()
    {
        JMenuBar mb = new JMenuBar();
        mb.add(createDummyMenu("File"));
        mb.add(createDummyMenu("Edit"));
        mb.add(createDummyMenu("Search"));
        mb.add(createDummyMenu("View"));
        mb.add(createDummyMenu("Tools"));
        mb.add(createDummyMenu("Help"));

        JMenu demo = new JMenu("Demo");
        demo.setMnemonic(KeyEvent.VK_D);
        mb.add(demo);

        thItem = new JCheckBoxMenuItem("Use Top-Level TransferHandler");
        thItem.setMnemonic(KeyEvent.VK_T);
        demo.add(thItem);

        nullItem = new JCheckBoxMenuItem("Remove TransferHandler from List and Text");
        nullItem.setMnemonic(KeyEvent.VK_R);
        demo.add(nullItem);

        copyItem = new JCheckBoxMenuItem("Use COPY Action");
        copyItem.setMnemonic(KeyEvent.VK_C);
        demo.add(copyItem);

        return mb;
    }

    private JMenu createDummyMenu(String str)
    {
        JMenu menu = new JMenu(str);
        JMenuItem item = new JMenuItem("[Empty]");
        item.setEnabled(false);
        menu.add(item);
        return menu;
    }
}
