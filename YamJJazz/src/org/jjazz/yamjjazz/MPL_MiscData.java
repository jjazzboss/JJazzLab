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
package org.jjazz.yamjjazz;

import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.parser.MidiParserListenerAdapter;
import org.jjazz.rhythm.api.Feel;

/**
 * MidiParserListener to retrieve header and SFF1 sections data in a standard Yamaha style file: sequence name, tempo, time signature.
 * <p>
 * Also quickly analyze the Notes to infer the style's Rhythm.Feel.
 */
public class MPL_MiscData extends MidiParserListenerAdapter
{

   private enum Section
   {
      INITIAL, SFF1, SFF2, OTHER
   };
   private Section currentSection = Section.INITIAL;
   private final Style style;
   private int halfTimeNotes = 0;
   private int offBeatNotes = 0;
   private float sectionStartPosInBeats;
   private static final Logger LOGGER = Logger.getLogger(MPL_MiscData.class.getSimpleName());

   public MPL_MiscData(Style style)
   {
      this.style = style;
   }

   @Override
   public void onMarkerParsed(String string, float posInBeats)
   {
      if (string.equals("SFF1"))
      {
         currentSection = Section.SFF1;
         style.sffType = Style.SFFtype.SFF1;
      } else if (string.equals("SFF2"))
      {
         currentSection = Section.SFF2;
         style.sffType = Style.SFFtype.SFF2;
      } else
      {
         currentSection = Section.OTHER;
      }
      sectionStartPosInBeats = posInBeats;
   }

   @Override
   public void onTrackNameParsed(String name, float posInBeats)
   {
      style.name = name;
   }

   @Override
   public void onTimeSignatureParsed(byte numerator, byte powerOfTwo, float positionInBeats)
   {
      if (currentSection.equals(Section.INITIAL))
      {
         style.timeSignature = TimeSignature.get(numerator, (int) Math.pow(2, powerOfTwo));
      }
   }

   @Override
   public void onTempoChanged(int tempo, float positionInBeats)
   {
      if (currentSection.equals(Section.INITIAL))
      {
         style.tempo = tempo;
      }
   }

   @Override
   public void onNoteParsed(Note note, float posInBeats)
   {
      // Count the nb of "half-beats" notes and nb of "off-beats" notes
      float relPosInBeats = posInBeats - sectionStartPosInBeats;
      float inBeatPos = relPosInBeats - (float) Math.floor(relPosInBeats);
      if (inBeatPos > 0.4f && inBeatPos < 0.6f)
      {
         halfTimeNotes++;
         offBeatNotes++;
      } else if (inBeatPos > 0.15f && inBeatPos < 0.85f)
      {
         offBeatNotes++;
      }
   }

   @Override
   public void onMetaEndEvent(float posInBeats)
   {
      // Update the style Feel
      style.feel = Feel.BINARY; // By default
      if (offBeatNotes > 0)
      {
         float ratio = (float) halfTimeNotes / offBeatNotes;
         if (ratio < 0.25)
         {
            // In ternary half-beats should be rare compared to notes on the .33/.66 sub-beats.
            style.feel = Feel.TERNARY;
         }
      }
   }

}
