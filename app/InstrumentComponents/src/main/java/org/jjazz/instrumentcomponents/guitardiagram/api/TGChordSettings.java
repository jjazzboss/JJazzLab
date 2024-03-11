package org.jjazz.instrumentcomponents.guitardiagram.api;

import org.jjazz.utilities.api.ResUtil;

/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 *  
 */
 /*
 * NOTE: code reused and modified from the TuxGuitar software (GNU Lesser GPL license,  author: Julián Gabriel Casadesús)
 */
public class TGChordSettings
{

    public enum ChordMode
    {
        MOST_COMMON(ResUtil.getString(TGChordSettings.class, "ModeMostCommonChords")),
        OPEN(ResUtil.getString(TGChordSettings.class, "ModeOpenVoicedChords")),
        CLOSE(ResUtil.getString(TGChordSettings.class, "ModeCloseVoicedChords")),
        INVERSIONS(ResUtil.getString(TGChordSettings.class, "ModeInversionChords"));

        private final String displayName;

        private ChordMode(String name)
        {
            displayName = name;
        }

        public ChordMode next()
        {
            int index = (ordinal() + 1) % values().length;
            return values()[index];
        }

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    private static TGChordSettings INSTANCE;

    private boolean emptyStringChords;
    private float bassGrade;
    private float fingeringGrade;
    private float subsequentGrade;
    private float requiredBasicsGrade;
    private float manyStringsGrade;
    private float goodChordSemanticsGrade;
    private int chordsToDisplay;
    private int howManyIncompleteChords;
    private ChordMode chordMode;
    private int findChordsMin;
    private int findChordsMax;

    private TGChordSettings()
    {
        this.emptyStringChords = false;
        this.bassGrade = 200.0f;
        this.fingeringGrade = 150.0f; // was:200
        this.subsequentGrade = 200.0f;
        this.requiredBasicsGrade = 150.0f;
        this.manyStringsGrade = 100.0f;
        this.goodChordSemanticsGrade = 200.0f;
        this.chordsToDisplay = 30;
        this.howManyIncompleteChords = 4;
        this.chordMode = ChordMode.MOST_COMMON;
        this.findChordsMin = 0;
        this.findChordsMax = 15;
    }

    public static TGChordSettings getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new TGChordSettings();
        }
        return INSTANCE;
    }

    public float getBassGrade()
    {
        return this.bassGrade;
    }

    public void setBassGrade(float bassGrade)
    {
        this.bassGrade = bassGrade;
    }

    public int getChordsToDisplay()
    {
        return this.chordsToDisplay;
    }

    public void setChordsToDisplay(int chordsToDisplay)
    {
        this.chordsToDisplay = chordsToDisplay;
    }

    public boolean isEmptyStringChords()
    {
        return this.emptyStringChords;
    }

    public void setEmptyStringChords(boolean emptyStringChords)
    {
        this.emptyStringChords = emptyStringChords;
    }

    public float getFingeringGrade()
    {
        return this.fingeringGrade;
    }

    public void setFingeringGrade(float fingeringGrade)
    {
        this.fingeringGrade = fingeringGrade;
    }

    public float getGoodChordSemanticsGrade()
    {
        return this.goodChordSemanticsGrade;
    }

    public void setGoodChordSemanticsGrade(float goodChordSemanticsGrade)
    {
        this.goodChordSemanticsGrade = goodChordSemanticsGrade;
    }

    public float getManyStringsGrade()
    {
        return this.manyStringsGrade;
    }

    public void setManyStringsGrade(float manyStringsGrade)
    {
        this.manyStringsGrade = manyStringsGrade;
    }

    public float getRequiredBasicsGrade()
    {
        return this.requiredBasicsGrade;
    }

    public void setRequiredBasicsGrade(float requiredBasicsGrade)
    {
        this.requiredBasicsGrade = requiredBasicsGrade;
    }

    public float getSubsequentGrade()
    {
        return this.subsequentGrade;
    }

    public void setSubsequentGrade(float subsequentGrade)
    {
        this.subsequentGrade = subsequentGrade;
    }

    public int getIncompleteChords()
    {
        return this.howManyIncompleteChords;
    }

    public void setIncompleteChords(int incomplete)
    {
        this.howManyIncompleteChords = incomplete;
    }

    public int getFindChordsMin()
    {
        return this.findChordsMin;
    }

    public void setFindChordsMin(int min)
    {
        this.findChordsMin = min;
    }

    public int getFindChordsMax()
    {
        return this.findChordsMax;
    }

    public void setFindChordsMax(int max)
    {
        this.findChordsMax = max;
    }

    public ChordMode getChordMode()
    {
        return this.chordMode;
    }

    public void setChordMode(ChordMode mode)
    {
        switch (mode)
        {
            case MOST_COMMON: // normal
                this.bassGrade = 200.0f;
                this.fingeringGrade = 150.0f;
                this.subsequentGrade = 200.0f;
                this.requiredBasicsGrade = 150.0f;
                this.manyStringsGrade = 100.0f;
                this.goodChordSemanticsGrade = 200.0f;
                break;
            case INVERSIONS: // inversions
                this.bassGrade = -100.0f;
                this.fingeringGrade = 150.0f;
                this.subsequentGrade = 200.0f;
                this.requiredBasicsGrade = 150.0f;
                this.manyStringsGrade = 50.0f;
                this.goodChordSemanticsGrade = 200.0f;
                break;
            case CLOSE: // close-voiced
                this.bassGrade = 50.0f;
                this.fingeringGrade = 200.0f;
                this.subsequentGrade = 350.0f;
                this.requiredBasicsGrade = 150.0f;
                this.manyStringsGrade = -100.0f;
                this.goodChordSemanticsGrade = 200.0f;
                break;
            case OPEN: // open-voiced
                this.bassGrade = 100.0f;
                this.fingeringGrade = 100.0f;
                this.subsequentGrade = -80.0f;
                this.requiredBasicsGrade = 100.0f;
                this.manyStringsGrade = -80.0f;
                this.goodChordSemanticsGrade = 200.0f;
                break;
        }
        this.chordMode = mode;
    }
}
