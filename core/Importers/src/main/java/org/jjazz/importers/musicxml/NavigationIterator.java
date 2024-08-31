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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.importers.musicxml.NavigationMark;
import org.jjazz.utilities.api.StringProperties;

/**
 * Run through the navigation elements stored in the ChordLeadSheet.
 */
public class NavigationIterator implements Iterator<ChordLeadSheetItem>
{

    private static final String PROP_TIME = "PropTime";

    private final ChordLeadSheet cls;
    private ChordLeadSheetItem previousElement;
    private ChordLeadSheetItem currentElement;
    private ChordLeadSheetItem nextElement;
    private CLI_Repeat currentRepeatStart;
    private String goingAlCoda;     // If non null it's the coda value we're aiming for
    private boolean goingAlFine;
    private HashMap<ChordLeadSheetItem, StringProperties> mapCliProps = new HashMap<>();
    private static final Predicate<ChordLeadSheetItem> IS_NAV_ITEM = cli -> cli instanceof CLI_Repeat
            || cli instanceof CLI_NavigationItem
            || cli instanceof CLI_Ending
            || cli instanceof CLI_Section;
    private static final Logger LOGGER = Logger.getLogger(NavigationIterator.class.getSimpleName());

    public NavigationIterator(ChordLeadSheet cls)
    {
        Objects.requireNonNull(cls);
        this.cls = cls;

        nextElement = nextImpl();
        LOGGER.log(Level.SEVERE, "NavigationIterator() curElt={0}    nextElt={1}", new Object[]
        {
            currentElement, nextElement
        });
    }


    /**
     * Get the next navigation-related element.
     * <p>
     * Next element depends on current element's type.
     *
     * @return Can't be null
     * @throws NoSuchElementException
     */
    @Override
    public ChordLeadSheetItem next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException("cls=" + cls);
        }
        previousElement = currentElement;
        currentElement = nextElement;
        nextElement = nextImpl();
        LOGGER.log(Level.SEVERE, "next() curElt={0}    nextElt={1}", new Object[]
        {
            currentElement, nextElement
        });

        return currentElement;
    }

    @Override
    public boolean hasNext()
    {
        return nextElement != null;

    }

    public ChordLeadSheetItem getPrevious()
    {
        return previousElement;
    }


    // ======================================================================================================================
    // Private methods
    // ======================================================================================================================
    /**
     * Get next item, or null if no more element.
     *
     * @return
     */
    private ChordLeadSheetItem nextImpl()
    {
        if (currentElement == null)
        {
            return cls.getFirstItemAfter(new Position(0), true, ChordLeadSheetItem.class, IS_NAV_ITEM);
        }

        ChordLeadSheetItem res = null;


        if (currentElement instanceof CLI_Repeat cliRepeat)
        {
            res = nextRepeat(cliRepeat);
        } else if (currentElement instanceof CLI_Ending cliEnding)
        {
            res = nextEnding(cliEnding);
        } else if (currentElement instanceof CLI_NavigationItem cliNavItem)
        {
            res = nextNavigationItem(cliNavItem);
        } else if (currentElement instanceof CLI_Section cliSection)
        {
            res = nextSection(cliSection);
        } else
        {
            throw new IllegalStateException("currentElement=" + currentElement);
        }

        return res;
    }

    private ChordLeadSheetItem nextNavigationItem(CLI_NavigationItem cliNavItem) throws AssertionError
    {
        ChordLeadSheetItem res = null;
        Position currentPos = cliNavItem.getPosition();
        var navItem = cliNavItem.getData();

        switch (navItem.mark())
        {
            case CODA ->
            {
                if (navItem.value().equals(goingAlCoda))
                {
                    // We reached the right coda, continue
                    res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                } else
                {
                    // We arrived to the end of the song, do like we reached a DACAPO_ALCODA
                    res = new CLI_NavigationItem(new Position(currentPos.getBar() - 1), new NavItem(NavigationMark.DACAPO_ALCODA, "coda", List.of(1)));
                }
            }
            case TOCODA ->
            {
                int time = getProps(cliNavItem).getInt(PROP_TIME, 0);
                if (navItem.timeOnly().contains(time + 1))
                {
                    // Jump to coda
                    res = cls.getFirstItemAfter(new Position(0), true, CLI_NavigationItem.class,
                            cli -> cli.getData().mark().equals(NavigationMark.CODA) && cli.getData().value().equals(navItem.value()));
                    if (res == null)
                    {
                        // Something's wrong
                        LOGGER.log(Level.WARNING, "nextNavigationItem() Can't find destination coda {0} from tocoda at bar {1}.", new Object[]
                        {
                            navItem.value(), currentPos.getBar()
                        });
                        res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                    }
                } else
                {
                    // Continue
                    res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                }

                // Update state
                getProps(cliNavItem).shiftInt(PROP_TIME, 1, 0);

            }
            case SEGNO ->
            {
                res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
            }
            case DALSEGNO, DALSEGNO_ALCODA ->
            {
                int time = getProps(cliNavItem).getInt(PROP_TIME, 0);
                if (navItem.timeOnly().contains(time + 1))
                {
                    // Update state
                    goingAlCoda = "coda";
                    goingAlFine = false;
                    res = cls.getFirstItemAfter(new Position(0), true, CLI_NavigationItem.class,
                            cli -> cli.getData().mark().equals(NavigationMark.SEGNO) && cli.getData().value().equals(navItem.value()));
                    clearRepeats();
                    if (res == null)
                    {
                        // Something's wrong
                        LOGGER.log(Level.WARNING, "nextNavigationItem() Can''t find destination segno {0} from dalsegno at bar {1}.", new Object[]
                        {
                            navItem.value(), currentPos.getBar()
                        });
                        res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                    }
                } else
                {
                    // Continue
                    res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                }
                // Update state
                getProps(cliNavItem).shiftInt(PROP_TIME, 1, 0);
            }
            case DALSEGNO_ALFINE ->
            {
                int time = getProps(cliNavItem).getInt(PROP_TIME, 0);
                if (navItem.timeOnly().contains(time + 1))
                {
                    // Update state
                    goingAlFine = true;
                    goingAlCoda = null;
                    res = cls.getFirstItemAfter(new Position(0), true, CLI_NavigationItem.class,
                            cli -> cli.getData().mark().equals(NavigationMark.SEGNO) && cli.getData().value().equals(navItem.value()));
                    clearRepeats();
                    if (res == null)
                    {
                        // Something's wrong
                        LOGGER.log(Level.WARNING, "nextNavigationItem() Can't find destination segno {0} from dalsegno at bar {1}.", new Object[]
                        {
                            navItem.value(), currentPos.getBar()
                        });
                        res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                    }
                } else
                {
                    // Continue
                    res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                }
                // Update state
                getProps(cliNavItem).shiftInt(PROP_TIME, 1, 0);
            }
            case DACAPO, DACAPO_ALCODA ->
            {
                int time = getProps(cliNavItem).getInt(PROP_TIME, 0);
                if (navItem.timeOnly().contains(time + 1))
                {
                    // Jump to start
                    goingAlCoda = "coda";
                    goingAlFine = false;
                    res = cls.getFirstItemAfter(new Position(0), true, ChordLeadSheetItem.class, IS_NAV_ITEM);
                    clearRepeats();
                } else
                {
                    // Continue
                    res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                }
                // Update state
                getProps(cliNavItem).shiftInt(PROP_TIME, 1, 0);
            }
            case DACAPO_ALFINE ->
            {
                int time = getProps(cliNavItem).getInt(PROP_TIME, 0);
                if (navItem.timeOnly().contains(time + 1))
                {
                    // Jump to start
                    goingAlFine = true;
                    goingAlCoda = null;
                    res = cls.getFirstItemAfter(new Position(0), true, ChordLeadSheetItem.class, IS_NAV_ITEM);
                    clearRepeats();
                } else
                {
                    // Continue
                    res = cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
                }
                // Update state
                getProps(cliNavItem).shiftInt(PROP_TIME, 1, 0);
            }
            case FINE ->
            {
                res = goingAlFine ? null : cls.getFirstItemAfter(cliNavItem, ChordLeadSheetItem.class, IS_NAV_ITEM);
            }
            default -> throw new AssertionError(cliNavItem.getData().mark().name());
        }


        return res;
    }

    private ChordLeadSheetItem nextEnding(CLI_Ending cliEnding)
    {
        ChordLeadSheetItem res = null;
        Position currentPos = cliEnding.getPosition();

        if (cliEnding.isStartType())
        {
            // Ending-start
            if (currentRepeatStart == null)
            {
                // Something's wrong
                LOGGER.log(Level.WARNING, "nextEnding() Ending-start at bar {0} without a start repeat bar.", currentPos.getBar());
            }

            int time = getProps(cliEnding).getInt(PROP_TIME, 0);
            if (cliEnding.getData().numbers().contains(time + 1))
            {
                // Continue
                res = cls.getFirstItemAfter(cliEnding, ChordLeadSheetItem.class, IS_NAV_ITEM);
//                LOGGER.severe("nextEnding() start - continuing on next bar");
            } else
            {
                // Need to find ending start for time+1, or a ending discontinue
                res = cls.getFirstItemAfter(cliEnding, ChordLeadSheetItem.class,
                        cli -> cli instanceof CLI_Ending cliE
                        && ((cliE.isStartType() && cliE.getData().numbers().contains(time + 1))
                        || cliE.getData().type().equals(EndingType.DISCONTINUE))
                );
//                LOGGER.log(Level.SEVERE, "nextEnding() start jumping to next start({0}) => res={1}", new Object[]
//                {
//                    (time + 1), res
//                });
            }
            if (res == null)
            {
                LOGGER.log(Level.WARNING, "nextEnding() Can''t find a next item for {0}  time+1={1}", new Object[]
                {
                    cliEnding, (time + 1)
                });
            }

            // Update state
            getProps(cliEnding).putInt(PROP_TIME, time + 1);


        } else if (cliEnding.isStopType())
        {
            // Ending-stop : jump back, like for a repeat-end
            if (currentRepeatStart == null)
            {
                // Something's wrong
                LOGGER.log(Level.WARNING, "nextEnding() Ending-stop bar {0} without a start repeat bar.", currentPos.getBar());

                res = cls.getFirstItemAfter(cliEnding, ChordLeadSheetItem.class, IS_NAV_ITEM);
            } else
            {
                // Go back to repeat start
                res = currentRepeatStart;
                // Update state
                getProps(cliEnding).shiftInt(PROP_TIME, 1, 0);
            }


        } else
        {
            // Ending-discontinue
            res = cls.getFirstItemAfter(cliEnding, ChordLeadSheetItem.class, IS_NAV_ITEM);
        }


        return res;
    }

    private ChordLeadSheetItem nextRepeat(CLI_Repeat cliRepeat)
    {
        ChordLeadSheetItem res = null;

        Position currentPos = cliRepeat.getPosition();

        if (cliRepeat.getData().startOrEnd())
        {
            // Repeat start
            if (currentRepeatStart != null && currentRepeatStart != cliRepeat)
            {
                // Something's wrong
                LOGGER.log(Level.WARNING, "nextRepeat() Successive repeat-start bar at bar {0}, currentRepeatStart={1}", new Object[]
                {
                    currentPos.getBar(), currentRepeatStart
                });

            }

            // Update state
            currentRepeatStart = cliRepeat;
            getProps(cliRepeat).shiftInt(PROP_TIME, 1, 0);

            res = cls.getFirstItemAfter(cliRepeat, ChordLeadSheetItem.class, IS_NAV_ITEM);

        } else
        {
            // Repeat end (not part of an ending-stop, since ending-stop would have been processed before))
            if (currentRepeatStart == null)
            {
                // Something's wrong
                LOGGER.log(Level.WARNING, "Repeat-end bar {0} without a start repeat bar.", currentPos.getBar());

                res = cls.getFirstItemAfter(cliRepeat, ChordLeadSheetItem.class, IS_NAV_ITEM);
            } else
            {
                // Go back to repeat start
                int time = getProps(cliRepeat).getInt(PROP_TIME, 0);
                if (time + 1 < cliRepeat.getData().times())
                {
                    // Jump backwards
                    res = currentRepeatStart;
                } else
                {
                    // Continue
                    res = cls.getFirstItemAfter(cliRepeat, ChordLeadSheetItem.class, IS_NAV_ITEM);
                    currentRepeatStart = null;
                }
                // Update state
                getProps(cliRepeat).shiftInt(PROP_TIME, 1, 0);
            }
        }

        return res;
    }

    private ChordLeadSheetItem nextSection(CLI_Section cliSection)
    {
        ChordLeadSheetItem res = cls.getFirstItemAfter(cliSection, ChordLeadSheetItem.class, IS_NAV_ITEM);
        return res;
    }


    private void clearRepeats()
    {
        var it = mapCliProps.keySet().iterator();
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
        var res = mapCliProps.get(cli);
        if (res == null)
        {
            res = new StringProperties();
            mapCliProps.put(cli, res);
        }
        return res;
    }

}
