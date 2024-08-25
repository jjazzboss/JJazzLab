/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.importers.musicxml;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.chordleadsheet.api.item.WritableItem;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.utilities.api.StringProperties;
import org.openide.util.Exceptions;

/**
 * Perform a conversion from musicXml file to Song.
 */
public class SongBuilder implements MusicXmlParserListener
{

    private enum EndingType
    {
        START, STOP, DISCONTINUE
    };
    private int nbMeasures = 0;
    private int sectionNumber = 1;
    private TimeSignature timeSignature = TimeSignature.FOUR_FOUR;
    private Position firstChordPos = null;
    private int tempo = 120;
    private final ChordLeadSheet clsWork;
    private Song song = null;
    private String musicalStyle;
    private static final Logger LOGGER = Logger.getLogger(SongBuilder.class.getSimpleName());

    public SongBuilder()
    {
        // clsWork is just used to store all parsed items, actual song creation will be done after
        clsWork = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet(getSectionUniqueName(), TimeSignature.FOUR_FOUR, ChordLeadSheet.MAX_SIZE, null);
    }

    /**
     * Get the resulting song.
     *
     * @return Might be null if parsing is not started yet, or if problem occured.
     */
    public Song getSong()
    {
        return song;
    }

    @Override
    public void beforeParsingStarts()
    {
        // Nothing
    }

    @Override
    public void afterParsingFinished()
    {
        song = buildSong();
    }

    @Override
    public void onTempoChanged(int tempoBPM, int barIndex)
    {
        LOGGER.log(Level.FINE, "onTempoChanged() tempoBPM={0} barIndex={1}", new Object[]
        {
            tempoBPM, barIndex
        });
        if (barIndex == 0)
        {
            tempo = Math.max(TempoRange.TEMPO_MIN, tempoBPM);
            tempo = Math.min(TempoRange.TEMPO_MAX, tempo);
        } else
        {
            LOGGER.log(Level.WARNING, "onTempoChanged() Tempo changed to {0} at barIndex={1}: ignored", new Object[]
            {
                tempoBPM, barIndex
            });
        }
    }

    @Override
    public void onTimeSignatureParsed(TimeSignature ts, int barIndex)
    {
        LOGGER.log(Level.FINE, "onTimeSignatureParsed() ts={0} barIndex={1}", new Object[]
        {
            ts, barIndex
        });
        timeSignature = ts;
        CLI_Section cliSection = clsWork.getSection(barIndex);
        Section section = cliSection.getData();
        if (barIndex == 0)
        {
            // Special case
            try
            {
                clsWork.setSectionTimeSignature(cliSection, ts);
            } catch (UnsupportedEditException ex)
            {
                LOGGER.log(Level.WARNING, "onTimeSignatureParsed() Can''t change time signature to {0} at bar {1} because: {2}",
                        new Object[]
                        {
                            ts,
                            barIndex, ex
                        });
                return;
            }
        } else if (!section.getTimeSignature().equals(ts))
        {
            // Need to introduce a new section
            cliSection = CLI_Factory.getDefault().createSection(getSectionUniqueName(), ts, barIndex, null);
            try
            {
                clsWork.addSection(cliSection);
            } catch (UnsupportedEditException ex)
            {
                LOGGER.log(Level.WARNING, "onTimeSignatureParsed() Can''t change time signature to {0} at bar {1} because: {2}",
                        new Object[]
                        {
                            ts,
                            barIndex, ex
                        });
            }
        }
    }

    @Override
    public void onBarLineParsed(String id, int barIndex)
    {
        nbMeasures = barIndex + 1;
    }

    @Override
    public void onLyricParsed(String lyric, Position pos)
    {
        // Nothing
    }

    @Override
    public void onNoteParsed(Note note, Position pos)
    {
        // Nothing
    }

    @Override
    public void onChordSymbolParsed(String strChord, Position pos)
    {
        LOGGER.log(Level.FINE, "onChordSymbolParsed() strChord={0} pos={1}", new Object[]
        {
            strChord, pos
        });

        ExtChordSymbol ecs;
        try
        {
            ecs = ExtChordSymbol.get(strChord);
        } catch (ParseException ex)
        {
            LOGGER.log(Level.WARNING, "onChordSymbolParsed() Invalid chord string={0}({1}), can''t insert chord at pos={2}",
                    new Object[]
                    {
                        strChord,
                        ex.getMessage(), pos
                    });
            return;
        }
        CLI_ChordSymbol cliCs = CLI_Factory.getDefault().createChordSymbol(ecs, pos);
        clsWork.addItem(cliCs);
        if (firstChordPos == null)
        {
            firstChordPos = pos;
        }
    }

    @Override
    public void onRepeatParsed(int barIndex, boolean startOrEnd, int times)
    {
        LOGGER.log(Level.FINE, "onRepeatParsed() barIndex={0} repeatStart={1} times={2}", new Object[]
        {
            barIndex, startOrEnd, times
        });
        Repeat data = new Repeat(startOrEnd, times);
        float beat = startOrEnd ? 0 : timeSignature.getNbNaturalBeats() - 0.01f;
        CLI_Repeat cliItem = new CLI_Repeat(new Position(barIndex, beat), data);
        clsWork.addItem(cliItem);
    }

    @Override
    public void onRehearsalParsed(int barIndex, String value)
    {
        LOGGER.log(Level.FINE, "onRehearsalParsed() barIndex={0} value={1}", new Object[]
        {
            barIndex, value
        });
        var cliSection = clsWork.getSection(barIndex);
        if (cliSection.getPosition().getBar() < barIndex)
        {
            // Add a section
            var newSection = CLI_Factory.getDefault().createSection(value, timeSignature, barIndex, clsWork);
            try
            {
                clsWork.addSection(newSection);
            } catch (UnsupportedEditException ex)
            {
                Exceptions.printStackTrace(ex);     // Should never happen, we don't change the time signature
            }
        } else
        {
            // Change section letter
            clsWork.setSectionName(cliSection, value);
        }
    }

    @Override
    public void onOtherPlayParsed(int barIndex, String value, String type)
    {
        LOGGER.log(Level.FINE, "onOtherPlayParsed() barIndex={0} value={1} type={2}", new Object[]
        {
            barIndex, value, type
        });
        if (type.equals("groove"))
        {
            musicalStyle = value;
        }
    }

    @Override
    public void onEndingParsed(int barIndex, List<Integer> numbers, int type)
    {
        LOGGER.log(Level.FINE, "onEndingParsed() barIndex={0} numbers={1} type={2}", new Object[]
        {
            barIndex, numbers, type
        });


        EndingType endType = switch (type)
        {
            case 0 ->
                EndingType.START;
            case 1 ->
                EndingType.STOP;
            default ->
                EndingType.DISCONTINUE;
        };
        Ending data = new Ending(endType, numbers);
        float beat = endType.equals(EndingType.STOP) ? 0 : timeSignature.getNbNaturalBeats() - 0.01f;
        CLI_Ending cliItem = new CLI_Ending(new Position(barIndex, beat), data);
        clsWork.addItem(cliItem);
    }

    @Override
    public void onNavigationMarkParsed(int barIndex, NavigationMark marker, String value, List<Integer> timeOnly)
    {
        LOGGER.log(Level.FINE, "onStructureMarkerParsed() barIndex={0} marker={1} value={2}, timeOnly={3}", new Object[]
        {
            barIndex, marker, value, timeOnly
        });
        NavigationItem data = new NavigationItem(marker, value, timeOnly);
        CLI_NavigationItem cliItem = new CLI_NavigationItem(new Position(barIndex), data);
        clsWork.addItem(cliItem);
    }

    // ==========================================================================================
    // Private methods
    // ==========================================================================================

    private String getSectionUniqueName()
    {
        return "TMP**" + sectionNumber++;
    }

    /**
     * Build the final song from parsed data collected into clsWork.
     *
     * @return
     */
    private Song buildSong()
    {
        // Set minimum size
        if (nbMeasures == 0)
        {
            nbMeasures = 4;
        }


        var sg = SongFactory.getInstance().createEmptySong("songBuilder song");
        var cls = sg.getChordLeadSheet();
        var sgs = sg.getSongStructure();

        LOGGER.severe("buildSong() musicalStyle=" + musicalStyle);
        LOGGER.severe("buildSong() clsWork=" + clsWork.toDebugString());


        // Remove useless bars (BIAB export seems to systematically insert 2 bars at the beginning)                            
        Position firstPos = firstChordPos;
//        if (firstPos != null && firstPos.isFirstBarBeat() && firstPos.getBar() > 0)
//        {
//            try
//            {
//                cls.deleteBars(0, firstPos.getBar() - 1);
//            } catch (UnsupportedEditException ex)
//            {
//                // Should never happen
//                Exceptions.printStackTrace(ex);
//            }
//        }


        if (firstChordPos == null)
        {
            LOGGER.warning("afterParsingFinished() No chord symbols found, importing an empty song.");
        }

        return sg;
    }

    // ============================================================================
    // Private classes
    // ============================================================================
    static private record Ending(EndingType type, List<Integer> numbers)
            {

    }

    static private record Repeat(boolean startOrEnd, int times)
            {

    }

    static private record NavigationItem(NavigationMark mark, String value, List<Integer> timeOnly)
            {

    }

    static private class CLI_Ending implements ChordLeadSheetItem<Ending>, WritableItem<Ending>
    {

        private ChordLeadSheet container;
        private Position position;
        private Ending data;

        public CLI_Ending(Position position, Ending data)
        {
            this.position = position;
            this.data = data;
        }

        @Override
        public ChordLeadSheet getContainer()
        {
            return container;
        }

        @Override
        public Ending getData()
        {
            return data;
        }

        @Override
        public Position getPosition()
        {
            return position;
        }

        @Override
        public ChordLeadSheetItem<Ending> getCopy(Position newPos)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isBarSingleItem()
        {
            return true;
        }

        @Override
        public StringProperties getClientProperties()
        {
            return new StringProperties();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void setPosition(Position pos)
        {
            this.position = pos;
        }

        @Override
        public void setData(Ending data)
        {
            this.data = data;
        }

        @Override
        public void setContainer(ChordLeadSheet cls)
        {
            this.container = container;
        }

    }


    static private class CLI_Repeat implements ChordLeadSheetItem<Repeat>, WritableItem<Repeat>
    {

        private ChordLeadSheet container;
        private Position position;
        private Repeat data;

        public CLI_Repeat(Position position, Repeat data)
        {
            this.position = position;
            this.data = data;
        }

        @Override
        public ChordLeadSheet getContainer()
        {
            return container;
        }

        @Override
        public Repeat getData()
        {
            return data;
        }

        @Override
        public Position getPosition()
        {
            return position;
        }

        @Override
        public ChordLeadSheetItem<Repeat> getCopy(Position newPos)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isBarSingleItem()
        {
            return true;
        }

        @Override
        public StringProperties getClientProperties()
        {
            return new StringProperties();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void setPosition(Position pos)
        {
            this.position = pos;
        }

        @Override
        public void setData(Repeat data)
        {
            this.data = data;
        }

        @Override
        public void setContainer(ChordLeadSheet cls)
        {
            this.container = container;
        }

    }

    static private class CLI_NavigationItem implements ChordLeadSheetItem<NavigationItem>, WritableItem<NavigationItem>
    {

        private ChordLeadSheet container;
        private Position position;
        private NavigationItem data;

        public CLI_NavigationItem(Position position, NavigationItem data)
        {
            this.position = position;
            this.data = data;
        }

        @Override
        public ChordLeadSheet getContainer()
        {
            return container;
        }

        @Override
        public NavigationItem getData()
        {
            return data;
        }

        @Override
        public Position getPosition()
        {
            return position;
        }

        @Override
        public ChordLeadSheetItem<NavigationItem> getCopy(Position newPos)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isBarSingleItem()
        {
            return true;
        }

        @Override
        public StringProperties getClientProperties()
        {
            return new StringProperties();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void setPosition(Position pos)
        {
            this.position = pos;
        }

        @Override
        public void setData(NavigationItem data)
        {
            this.data = data;
        }

        @Override
        public void setContainer(ChordLeadSheet cls)
        {
            this.container = container;
        }
    }

}
