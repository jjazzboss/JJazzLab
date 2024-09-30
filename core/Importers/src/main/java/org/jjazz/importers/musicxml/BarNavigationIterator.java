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

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import static org.jjazz.importers.musicxml.NavigationMark.DACAPO;
import static org.jjazz.importers.musicxml.NavigationMark.DACAPO_ALCODA;
import static org.jjazz.importers.musicxml.NavigationMark.DACAPO_ALFINE;
import static org.jjazz.importers.musicxml.NavigationMark.DALSEGNO;
import static org.jjazz.importers.musicxml.NavigationMark.DALSEGNO_ALCODA;
import static org.jjazz.importers.musicxml.NavigationMark.DALSEGNO_ALFINE;
import org.jjazz.utilities.api.StringProperties;

/**
 * Run through the bars stored in the ChordLeadSheet following the navigation elements (repeats, endings, coda, DS/DC, fine).
 */
public class BarNavigationIterator implements Iterator<Integer>
{

    private static final String PROP_TIME = "PropTime";

    private final ChordLeadSheet cls;
    private CLI_Repeat currentRepeatStart;  // Not null if we're inside a repeat 
    private int nbRestarts;         // When song is restarted from head (dacapo) or from segno (dal segno).
    private String goingAlCoda;     // If non null it's the coda value we're aiming for
    private boolean goingAlFine;
    private HashMap<ChordLeadSheetItem, StringProperties> mapClirepeatProps = new HashMap<>();
    private int previousBar = -1, currentBar = -1, nextBar = -1;
    private static final Predicate<ChordLeadSheetItem> IS_NAV_ITEM = cli -> cli instanceof CLI_Repeat
            || cli instanceof CLI_NavigationItem
            || cli instanceof CLI_Ending
            || cli instanceof CLI_Section;
    private static final Logger LOGGER = Logger.getLogger(BarNavigationIterator.class.getSimpleName());

    public BarNavigationIterator(ChordLeadSheet cls)
    {
        Objects.requireNonNull(cls);
        this.cls = cls;
        nbRestarts = 1;
        nextBar = nextImpl();
        LOGGER.log(Level.FINE, "NavigationIterator() currentBar={0}    nextBar={1}", new Object[]
        {
            currentBar, nextBar
        });
    }


    /**
     * Get the next bar index in cls, taking into account repeats, ending, codas etc.
     * <p>
     * Updates needSection() return value.
     *
     * @return
     * @throws NoSuchElementException
     * @see #needSection
     */
    @Override
    public Integer next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException("cls=" + cls);
        }
        previousBar = currentBar;
        currentBar = nextBar;
        nextBar = nextImpl();
        LOGGER.log(Level.FINE, "next() currentBar={0}    nextBar={1}", new Object[]
        {
            currentBar, nextBar
        });

        return currentBar;
    }


    @Override
    public boolean hasNext()
    {
        return nextBar != -1;

    }

    public int getPrevious()
    {
        return previousBar;
    }


    // ======================================================================================================================
    // Private methods
    // ======================================================================================================================
    /**
     * Get next bar, or -1 if no more element.
     * <p>
     * Also sets needSection.
     *
     * @return
     */
    private int nextImpl()
    {
        if (currentBar == -1)
        {
            // First call
            return 0;
        }

        int res = currentBar;
        var navItems = cls.getItems(currentBar, currentBar, ChordLeadSheetItem.class, IS_NAV_ITEM);
        for (var cli : navItems)
        {
            if (cli instanceof CLI_Repeat cliRepeat)
            {
                res = nextRepeat(cliRepeat);
            } else if (cli instanceof CLI_Ending cliEnding)
            {
                res = nextEnding(cliEnding);
            } else if (cli instanceof CLI_NavigationItem cliNavItem)
            {
                res = nextNavigationItem(cliNavItem);
            } else if (cli instanceof CLI_Section)
            {
                res = currentBar;
            } else
            {
                throw new IllegalStateException("currentBar=" + currentBar);
            }

            if (res != currentBar)
            {
                // There is a jump
                break;
            }
        }

        if (res == currentBar)
        {
            // The bar navigation items did not trigger a jump (eg a section, a repeat start, a segno before meeting DS alsegno, ...)
            // Just go to next bar, if possible
            res = (currentBar + 1) >= cls.getSizeInBars() ? -1 : currentBar + 1;
        }

        if (res == currentBar + 1)
        {
            // Special check on next bar : is there an Ending-start which does not match the repeat time ? If yes need to change next bar
            var cliEndingStart = cls.getFirstItemAfter(new Position(currentBar + 1),
                    true,
                    CLI_Ending.class,
                    cli -> cli.isStartType());
            if (cliEndingStart != null && cliEndingStart.getPosition().getBar() == currentBar + 1)
            {
                // Update state
                int time = getProps(cliEndingStart).getInt(PROP_TIME, 0) + 1;
                getProps(cliEndingStart).putInt(PROP_TIME, time);
                var numbers = cliEndingStart.getData().numbers();
                if (!numbers.contains(time))
                {
                    // Need to find the next Ending-start 
                    var cliEndingStart2 = cls.getFirstItemAfter(cliEndingStart,
                            CLI_Ending.class,
                            cle -> cle.isStartType() && cle.getData().numbers().contains(time));
                    if (cliEndingStart2 == null)
                    {
                        // Something's wrong
                        LOGGER.log(Level.WARNING, "nextImpl() Cant find matching Ending-start after {0} for time={1}. Continuing after next ending-stop",
                                new Object[]
                                {
                                    cliEndingStart, time
                                });
                        var cliEndingStop = cls.getFirstItemAfter(cliEndingStart, CLI_Ending.class, cle -> cle.isStopType());
                        if (cliEndingStop == null)
                        {
                            LOGGER.log(Level.WARNING, "nextImpl() Cant find Ending-stop after {0}", cliEndingStart);
                        } else
                        {
                            res = cliEndingStop.getPosition().getBar() + 1;
                        }
                    } else
                    {
                        // Change next bar
                        res = cliEndingStart2.getPosition().getBar();   // The only jump which doesn't need a section

                        // Reset currentRepeatStart if there is no more repeat planned
                        var numbers2 = cliEndingStart2.getData().numbers();
                        if (!numbers.contains(time + 1) && !numbers2.contains(time + 1))
                        {
                            currentRepeatStart = null;
                        }
                    }
                }
            }


            // Special check on next bar : is there a Coda on next bar ? If yes do like a DC AL CODA
            var cliCoda = cls.getFirstItemAfter(new Position(currentBar + 1),
                    true,
                    CLI_NavigationItem.class,
                    cli -> cli.getData().mark().equals(NavigationMark.CODA));
            if (cliCoda != null && cliCoda.getPosition().getBar() == currentBar + 1)
            {
                if (!cliCoda.getData().value().equals(goingAlCoda))
                {
                    // We arrived to the coda without a DACAPO, simulate a DACAPO_ALCODA
                    prepareRestart(false);
                    res = 0;
                }
            }
        }

        return res;
    }


    /**
     * Returns the destination bar after the specified item.
     *
     * @param cliNavItem
     * @return If bar is unchanged, it means we progress linearly. Otherwise it means a navigation jump has occured.
     */
    private int nextNavigationItem(CLI_NavigationItem cliNavItem)
    {
        Position pos = cliNavItem.getPosition();
        var navItem = cliNavItem.getData();
        int res = pos.getBar();     // By default no jump

        switch (navItem.mark())
        {
            case CODA ->
            {
                // Processed by nextImpl()
            }
            case TOCODA ->
            {
                if (navItem.timeOnly().contains(nbRestarts))
                {
                    // Jump to coda
                    var cliCoda = cls.getFirstItemAfter(new Position(0), true, CLI_NavigationItem.class,
                            cli -> cli.getData().mark().equals(NavigationMark.CODA) && cli.getData().value().equals(navItem.value()));
                    if (cliCoda != null)
                    {
                        res = cliCoda.getPosition().getBar();
                        currentRepeatStart = null;      // TOCODA is sometimes in the middle of a repeat
                    } else
                    {
                        // Something's wrong
                        LOGGER.log(Level.WARNING, "nextNavigationItem() Can''t find destination coda {0} from tocoda at bar {1}. Continuing on next bar.",
                                new Object[]
                                {
                                    navItem.value(), pos.getBar()
                                });
                    }
                }
            }
            case SEGNO ->
            {
                // Nothing
            }
            case DALSEGNO, DALSEGNO_ALCODA, DALSEGNO_ALFINE ->
            {
                if (navItem.timeOnly().contains(nbRestarts))
                {
                    var cliSegno = cls.getFirstItemAfter(new Position(0), true, CLI_NavigationItem.class,
                            cli -> cli.getData().mark().equals(NavigationMark.SEGNO) && cli.getData().value().equals(navItem.value()));

                    if (cliSegno != null)
                    {
                        res = cliSegno.getPosition().getBar();
                        boolean b = switch (navItem.mark())
                        {
                            case DALSEGNO ->
                                // We don't know if it's alCoda or alFine, try to find 
                                !cls.getItems(CLI_NavigationItem.class, cli -> cli.getData().mark().equals(NavigationMark.FINE)).isEmpty();
                            case DALSEGNO_ALCODA ->
                                false;
                            case DALSEGNO_ALFINE ->
                                true;
                            default -> throw new IllegalStateException("navItem.mark()=" + navItem.mark());
                        };
                        prepareRestart(b);

                    } else
                    {
                        // Something's wrong
                        LOGGER.log(Level.WARNING, "nextNavigationItem() Can''t find destination segno {0} from dalsegno at bar {1}. Continuing on next bar.",
                                new Object[]
                                {
                                    navItem.value(), pos.getBar()
                                });
                    }
                }
            }

            case DACAPO, DACAPO_ALCODA, DACAPO_ALFINE ->
            {
                if (navItem.timeOnly().contains(nbRestarts))
                {
                    // Jump to start
                    res = 0;
                    boolean b = switch (navItem.mark())
                    {
                        case DACAPO ->
                            // We don't know if it's alCoda or alFine, try to find 
                            !cls.getItems(CLI_NavigationItem.class, cli -> cli.getData().mark().equals(NavigationMark.FINE)).isEmpty();
                        case DACAPO_ALCODA ->
                            false;
                        case DACAPO_ALFINE ->
                            true;
                        default -> throw new IllegalStateException("navItem.mark()=" + navItem.mark());
                    };
                    prepareRestart(b);
                }
            }
            case FINE ->
            {
                if (goingAlFine)
                {
                    res = -1;
                }
            }
            default -> throw new AssertionError(cliNavItem.getData().mark().name());
        }


        return res;
    }

    /**
     * Returns the destination bar after the specified item.
     *
     * @param cliEnding
     * @return If bar is unchanged, it means we progress linearly. Otherwise it means a navigation jump has occured.
     */
    private int nextEnding(CLI_Ending cliEnding)
    {

        Position pos = cliEnding.getPosition();
        int res = pos.getBar();     // By default no jump        

        if (cliEnding.isStartType())
        {
            // Ending-start is processed in nextImpl()

        } else if (cliEnding.isStopType())
        {
            // Ending-stop : jump back, like for a repeat-end
            if (currentRepeatStart == null)
            {
                // Something's wrong
                LOGGER.log(Level.WARNING, "nextEnding() Ending-stop bar {0} without a start repeat bar.", pos.getBar());
            } else
            {
                // Go back to repeat start
                res = currentRepeatStart.getPosition().getBar();
            }


        } else
        {
            // Ending-discontinue : nothing
        }


        return res;
    }

    /**
     * Returns the destination bar after the specified item.
     *
     * @param cliRepeat
     * @return If bar is unchanged, it means we progress linearly. Otherwise it means a navigation jump has occured.
     */
    private int nextRepeat(CLI_Repeat cliRepeat)
    {
        Position pos = cliRepeat.getPosition();
        int res = pos.getBar();     // By default

        if (cliRepeat.getData().startOrEnd())
        {
            // Repeat start
            if (currentRepeatStart != null && currentRepeatStart != cliRepeat)
            {
                // Something's wrong
                LOGGER.log(Level.WARNING, "nextRepeat() Successive repeat-start bar at bar {0}, currentRepeatStart={1}", new Object[]
                {
                    pos.getBar(), currentRepeatStart
                });

            }

            currentRepeatStart = cliRepeat;

        } else
        {
            // Repeat end (not part of an ending-stop, since ending-stop would have been processed before))
            if (currentRepeatStart == null)
            {
                // Something's wrong
                LOGGER.log(Level.WARNING, "Repeat-end bar {0} without a start repeat bar, ignored.", pos.getBar());

            } else
            {
                // Go back to repeat start
                int time = getProps(cliRepeat).getInt(PROP_TIME, 0);
                if (time + 1 < cliRepeat.getData().times())
                {
                    // Jump backwards
                    res = currentRepeatStart.getPosition().getBar();
                } else
                {
                    // Continue
                    currentRepeatStart = null;

                }
                // Update state
                getProps(cliRepeat).shiftInt(PROP_TIME, 1, 0);
            }
        }

        return res;
    }


    /**
     * We restart the song from head or from segno, after a dacapo or dalsegno.
     *
     * @param alFine True if we aim at a Fine event, otherwise we aim at the coda.
     */
    private void prepareRestart(boolean alFine)
    {
        goingAlFine = alFine;
        goingAlCoda = goingAlFine ? null : "coda";
        nbRestarts++;

        // Clear the repeats times
        currentRepeatStart = null;
        var it = mapClirepeatProps.keySet().iterator();
        while (it.hasNext())
        {
            var cli = it.next();
            if (cli instanceof CLI_Repeat || cli instanceof CLI_Ending)
            {
                it.remove();
            }
        }
    }

    private StringProperties getProps(ChordLeadSheetItem cli)
    {
        var res = mapClirepeatProps.get(cli);
        if (res == null)
        {
            res = new StringProperties();
            mapClirepeatProps.put(cli, res);
        }
        return res;
    }

}
