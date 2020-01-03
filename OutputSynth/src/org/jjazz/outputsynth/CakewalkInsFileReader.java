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
package org.jjazz.outputsynth;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.midi.AbstractInstrumentBank;
import org.jjazz.midi.DrumKit;
import org.jjazz.midi.GM1Instrument;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.MidiAddress;
import org.jjazz.midi.MidiAddress.BankSelectMethod;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.StdSynth;
import org.jjazz.midi.keymap.KeyMapGM;
import org.jjazz.midi.spi.KeyMapProvider;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.midi.spi.MidiSynthFileReader;

/**
 * A MidiSynth provider reading Cakewalk .ins instrument definition files.
 * <p>
 * Accept extensions to the .ins format for patch names to adjust the created Instrument with DrumKit or substitute GM1
 * instrument:<br>
 * - For drums instruments: 0=Live!DrumsStandardKit1 {{DrumKit=STANDARD, XG}}. Param1 is a DrumKit.Type value, param2 must be a
 * value corresponding to a DrumKit.KeyMap.getName(). <br>
 * - For voice instruments: 12=New Marimba {{SubGM1=12}}<br>
 */
@ServiceProvider(service = MidiSynthFileReader.class)
public class CakewalkInsFileReader implements MidiSynthFileReader
{

    public static final String NAME = "InsFileSynthProvider";
    private static final Logger LOGGER = Logger.getLogger(CakewalkInsFileReader.class.getSimpleName());
    private final FileNameExtensionFilter FILTER = new FileNameExtensionFilter("Cakewalk instrument files (.ins)", "ins");

    public CakewalkInsFileReader()
    {
    }

    @Override
    public String getId()
    {
        return NAME;
    }

    @Override
    public List<FileNameExtensionFilter> getSupportedFileTypes()
    {
        return Arrays.asList(FILTER);
    }

    @Override
    public List<MidiSynth> readSynthsFromStream(InputStream in, File f) throws IOException
    {
        ArrayList<MidiSynth> synths = new ArrayList<>();
        String fileName = (f != null) ? f.getAbsolutePath() : "<stream>";
        // state=0: unknown or ignored section
        // state=1: within .Patch Names section
        // state=2: within .Instrument Definitions section
        int state = 0;
        InsBank currentBank = null;
        MidiSynth currentSynth = null;
        BankSelectMethod currentBsm = BankSelectMethod.MSB_LSB;
        // The BankName and the InstrumentBank
        HashMap<String, InsBank> mapNameBank = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
        {
            int lineCount = 0;
            String line;
            Pattern pBank1 = Pattern.compile("^\\s*\\[(.+)\\]\\s*");  // [Bank PRE1]
            Pattern pPatch1 = Pattern.compile("^\\s*(\\d+)=(.*)");   // 1=Grand Piano
            Pattern pPatchDrums = Pattern.compile("\\{\\{\\s*DrumKit\\s*=(.*),(.*)\\}\\}");   // {{DrumKit=STANDARD, KitKeyMap}}
            Pattern pPatchSubstitute = Pattern.compile("\\{\\{\\s*SubGM1\\s*=\\s*(\\d+)\\s*\\}\\}");   // {{SubGM1=12}}          
            Pattern pSynth2 = Pattern.compile("^\\s*\\[(.+)\\]\\s*");  // [Motif XS]
            Pattern pBsm2 = Pattern.compile("^\\s*BankSelMethod=([0-9])\\s*");  // BankSelMethod=2
            Pattern pPatch2 = Pattern.compile("^\\s*Patch\\[(\\d+)\\]=(.*)");  // Patch[1232]=PRE 1
            Pattern p2Patch2 = Pattern.compile("^\\s*Patch\\[\\*\\]=([^.]*)\\s*");  // Patch[*]=PRE 1, but not Patch[*]=1...128                                        

            // 
            // MAIN LOOP
            //
            while ((line = reader.readLine()) != null)
            {
                lineCount++;
                String l = line.replaceFirst(";.*", "");  // remove comments
                LOGGER.log(Level.FINE, "readFile() l={0} state={1} : {2}", new Object[]
                {
                    lineCount, state, l
                });

                if (l.matches("^\\s*\\..*")) // ".something..."
                {
                    // It's a section change
                    if (l.contains("Patch Names"))
                    {
                        // Entering new section : bank + patch names
                        state = 1;

                    } else if (l.contains("Instrument Definitions"))
                    {
                        // Entering new section : LSB/MSB
                        state = 2;
                    } else
                    {
                        // Ignore other sections
                        state = 0;
                    }
                    currentBank = null;
                    continue;
                }

                // We are within a section
                if (state == 1)
                {
                    // We are in the Patch Names section
                    Matcher mBank = pBank1.matcher(l);
                    Matcher mPatch = pPatch1.matcher(l);
                    if (mBank.matches())
                    {
                        // Create a new bank 
                        String bankName = mBank.group(1).trim();
                        currentBank = new InsBank(bankName);
                        // And store it
                        mapNameBank.put(bankName, currentBank);
                        continue;
                    } else if (mPatch.matches())
                    {
                        // Add instrument to the current bank
                        if (currentBank == null)
                        {
                            LOGGER.warning("readSynthsFromStream() Patch name found in file " + fileName + " at line " + lineCount + " but no bank set.");
                            continue;
                        }
                        String spc = mPatch.group(1);
                        int pc = 0;
                        try
                        {
                            pc = Integer.valueOf(spc);
                        } catch (NumberFormatException e)
                        {
                            // Leave pc=0
                            LOGGER.warning("readSynthsFromStream() Can't read program change value in file " + fileName + " at line " + lineCount);
                        }
                        DrumKit kit = null;
                        GM1Instrument insGM1 = null;
                        String patchName = mPatch.group(2);
                        if (patchName == null || patchName.trim().isEmpty())
                        {
                            LOGGER.warning("readSynthsFromStream() Can't read a valid patch name in file " + fileName + " at line " + lineCount);
                            patchName = "not set";
                        } else
                        {
                            // Got the patchName right

                            // Check if a drummap is defined
                            Matcher mPatchDrumMap = pPatchDrums.matcher(patchName);
                            if (mPatchDrumMap.find())
                            {
                                // Yes, get the KitType and KeyMap
                                String keyMapName = mPatchDrumMap.group(2).trim();
                                String typeName = mPatchDrumMap.group(1).trim();
                                assert typeName != null : "patchName=" + patchName;
                                DrumKit.KeyMap kitKeyMap = KeyMapProvider.Util.getKeyMap(keyMapName);
                                if (kitKeyMap == null)
                                {
                                    LOGGER.warning("readSynthsFromStream() Unknown KeyMap " + keyMapName + " for instrument" + patchName + " in file " + fileName + " at line " + lineCount + ". Using the GM drum map instead.");
                                    kitKeyMap = KeyMapGM.getInstance();
                                }
                                DrumKit.Type kitType = DrumKit.Type.valueOf(typeName);
                                if (kitType == null)
                                {
                                    LOGGER.warning("readSynthsFromStream() Unknown DrumKit type " + typeName + " for instrument" + patchName + " in file " + fileName + " at line " + lineCount + ". Using the STANDARD DrumKit type instead.");
                                    kitType = DrumKit.Type.STANDARD;
                                }
                                kit = new DrumKit(kitType, kitKeyMap);
                            }

                            // Check if a substitute is defined
                            Matcher mPatchSubstitute = pPatchSubstitute.matcher(patchName);
                            if (mPatchSubstitute.find())
                            {
                                // Yes, get the GM1 ProgramChange
                                String pcGM1str = mPatchSubstitute.group(1);
                                assert pcGM1str != null : "patchName=" + patchName;
                                int pcGM1 = -1;
                                try
                                {
                                    pcGM1 = Integer.valueOf(pcGM1str);
                                } catch (NumberFormatException e)
                                {
                                }
                                if (pcGM1 < 0 || pcGM1 > 127)
                                {
                                    LOGGER.warning("readSynthsFromStream() Invalid SubGM1 program change " + pcGM1str + " for instrument" + patchName
                                            + " in file " + fileName + " at line " + lineCount + ". Using value SubGM1 PC=0 instead.");
                                    pcGM1 = 0;
                                }
                                insGM1 = StdSynth.getGM1Bank().getInstrument(pcGM1);
                            }

                            // Get rid of the extensions if any
                            if (kit != null || insGM1 != null)
                            {
                                patchName = patchName.substring(0, patchName.indexOf("{{"));
                            }
                        }

                        // Build the instrument with the optional parameters
                        Instrument ins = new Instrument(patchName, null, new MidiAddress(pc, -1, -1, null), kit, insGM1);
                        currentBank.addInstrument(ins);     // This will set the LSB/MSB/BSM of the Instrument with the bank values
                        continue;
                    } else if (mPatch.matches())
                    {
                        // Add instrument to the current bank
                        if (currentBank == null)
                        {
                            LOGGER.warning("readSynthsFromStream() Patch name found in file " + fileName + " at line " + lineCount + " but no bank set.");
                            continue;
                        }
                        String spc = mPatch.group(1);
                        int pc = 0;
                        try
                        {
                            pc = Integer.valueOf(spc);
                        } catch (NumberFormatException e)
                        {
                            // Leave pc=0
                            LOGGER.warning("readSynthsFromStream() Can't read program change value in file " + fileName + " at line " + lineCount);
                        }
                        String patchName = mPatch.group(2);
                        if (patchName == null || patchName.trim().isEmpty())
                        {
                            LOGGER.warning("readSynthsFromStream() Can't read a valid patch name in file " + fileName + " at line " + lineCount);
                            patchName = "not set";
                        }
                        Instrument ins = new Instrument(pc, patchName);
                        currentBank.addInstrument(ins);
                        continue;
                    }
                } else if (state == 2)
                {
                    // We are in the Instrument Definition section
                    Matcher mSynth = pSynth2.matcher(l);
                    Matcher mBsm = pBsm2.matcher(l);
                    Matcher mPatch = pPatch2.matcher(l);
                    Matcher m2Patch = p2Patch2.matcher(l);
                    if (mSynth.matches())
                    {
                        // It's a new synth, create it
                        String synthName = mSynth.group(1);
                        currentSynth = new MidiSynth(synthName, "");
                        synths.add(currentSynth);
                    } else if (mBsm.matches())
                    {
                        // It's a BankSelectMethod specification
                        // IMPORTANT: it will impact only lines after this line ! So should be the first line after Synth name
                        if (currentSynth == null)
                        {
                            LOGGER.warning("readSynthsFromStream() BankSelectMethod found in file " + fileName + " at line " + lineCount + " but no instrument set.");
                            continue;
                        }
                        String s = mBsm.group(1);
                        int bsm = 0;
                        try
                        {
                            bsm = Integer.valueOf(s);
                        } catch (NumberFormatException e)
                        {
                            // Leave defaultBsm=0
                            LOGGER.warning("readSynthsFromStream() Can't read BankSelectMethod in file " + fileName + " at line " + lineCount);
                        }
                        currentBsm = getBsm(bsm);
                        continue;
                    } else if (m2Patch.matches())
                    {
                        // It's a "patch[*]=SomeBankName"
                        // Find the bank by its name, set the special BankSelectMethod, attach to the current MidiSynth
                        if (currentSynth == null)
                        {
                            LOGGER.warning("readSynthsFromStream() Patch[*] found in file " + fileName + " at line " + lineCount + " but no instrument set.");
                            continue;
                        }
                        String bankName = m2Patch.group(1).trim();
                        InsBank bank = mapNameBank.get(bankName);
                        if (bank == null)
                        {
                            LOGGER.warning("readSynthsFromStream() Can't find bank " + bankName + " in file " + fileName + " at line " + lineCount);
                        } else if (bank.getMidiSynth() != null)
                        {
                            LOGGER.warning("readSynthsFromStream() Bank " + bankName + " already assigned to a synth in file " + fileName + " at line " + lineCount);
                        } else
                        {
                            // Update the bank 
                            bank.setBankSelectMethod(BankSelectMethod.PC_ONLY);
                            // Add it to the current Synth
                            if (!currentSynth.getBanks().contains(bank))
                            {
                                currentSynth.addBank(bank);
                            }
                        }
                    } else if (mPatch.matches())
                    {
                        // It's a patch[]=SomeBankName
                        // Find the bank by its name, set the MSB and LSB, attach to the current MidiSynth
                        if (currentSynth == null)
                        {
                            LOGGER.warning("readSynthsFromStream() Patch[] found in file " + fileName + " at line " + lineCount + " but no instrument set.");
                            continue;
                        }
                        String s = mPatch.group(1);
                        int value = 0;
                        try
                        {
                            value = Integer.valueOf(s);
                        } catch (NumberFormatException e)
                        {
                            // Leave i=0
                            LOGGER.warning("readSynthsFromStream() Can't read Patch[] integer value in file " + fileName + " at line " + lineCount);
                        }
                        int msb = value / 128;
                        int lsb = value - (msb * 128);
                        String bankName = mPatch.group(2).trim();
                        InsBank bank = mapNameBank.get(bankName);
                        if (bank == null)
                        {
                            LOGGER.warning("readSynthsFromStream() Can't find bank " + bankName + " in file " + fileName + " at line " + lineCount);
                        } else if (bank.getMidiSynth() != null)
                        {
                            LOGGER.warning("readSynthsFromStream() Bank " + bankName + " already assigned to a synth in file " + fileName + " at line " + lineCount);
                        } else
                        {
                            // Update the bank 
                            bank.setBankSelectMethod(currentBsm);
                            bank.setBankSelectLsb(lsb);
                            bank.setBankSelectMsb(msb);
                            // Add it to the current Synth
                            if (!currentSynth.getBanks().contains(bank))
                            {
                                currentSynth.addBank(bank);
                            }
                        }
                    }
                }
            }
            // We reached end of file
        }

        // In some cases we may have synths with no banks, don't keep them
        // Example of such ins file : 
        // .Instrument Definitions
        // [Giga]
        // Patch[*]=Giga      
        // [MOXF]
        // Patch[239]=PRE 1
        // etc.
        // Set the file for the others
        for (MidiSynth synth : synths.toArray(new MidiSynth[0]))
        {
            if (synth.getBanks().isEmpty())
            {
                synths.remove(synth);
            } else
            {
                synth.setFile(f);
            }
        }
        LOGGER.fine("readSynthsFromStream() EXIT synths.size()=" + synths.size());
        if (synths.isEmpty())
        {
            LOGGER.warning("No synth found in file " + f.getAbsolutePath());
        }

        return synths;
    }

    //========================================================================================
    // Private methods
    //========================================================================================
    /**
     * Translate the Cakewalk number for BankSelectMethod into our data type.
     */
    private BankSelectMethod getBsm(int bsm)
    {
        switch (bsm)
        {
            case 1:
                return BankSelectMethod.MSB_ONLY;
            case 2:
                return BankSelectMethod.LSB_ONLY;
            case 3:
                return BankSelectMethod.PC_ONLY;
            default:
                return BankSelectMethod.MSB_LSB;
        }
    }

}

/**
 * Our simplified InstrumentBank with convenience methods.
 */
class InsBank extends AbstractInstrumentBank<Instrument>
{

    public InsBank(String name)
    {
        super(name, null, 0, 0);
    }    

    public void setBankSelectMethod(BankSelectMethod m)
    {
        defaultBsm = m;
    }

    public void setBankSelectLsb(int lsb)
    {
        if (lsb < 0 || lsb > 127)
        {
            throw new IllegalArgumentException("lsb=" + lsb);
        }
        aze
        this.defaultLsb = lsb;
    }

    public void setBankSelectMsb(int msb)
    {
        if (msb < 0 || msb > 127)
        {
            throw new IllegalArgumentException("msb=" + msb);
        }
        this.defaultMsb = msb;
    }
}
