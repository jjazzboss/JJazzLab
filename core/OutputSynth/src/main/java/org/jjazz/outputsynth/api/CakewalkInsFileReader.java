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
 */
package org.jjazz.outputsynth.api;

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
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.synths.GM1Bank;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.spi.KeyMapProvider;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.midi.api.synths.GSDrumsInstrument;
import org.jjazz.midi.api.synths.GSInstrument;

/**
 * A MidiSynth provider reading Cakewalk .ins instrument definition files.
 * <p>
 * Several JJazzLab-specific extensions marked with {{ ... }} to the .ins format are supported, as described below.
 * <p>
 * Patch name extensions to adjust the created Instrument with DrumKit, or to define a substitute GM1 instrument:<br>
 * - For drums instruments: "0=Live!DrumsStandardKit1 {{DrumKit=STANDARD, XG}}". Param1 is a DrumKit.Type value, param2 must be a
 * value corresponding to a DrumKit.KeyMap.getName(). <br>
 * - For voice instruments: "12=New Marimba {{SubGM1=0}}" defines GM instrument 0 (piano) as the GM substitute for the New Marimba
 * patch.<p>
 * Bank name extensions :<br>
 * - "Patch[10371]=Roland JV-1080 GM {{ GM_BANK }}" : this tells JJazzLab that the "current synth is GM compatible", and it
 * indicates the location of the GM bank. You can similarly use GM2_BANK or XG_BANK or GS_BANK extension on any bank to tell
 * JJazzLab that the synth is GM2/XG/GS compatible.
 * <br>
 * - "Patch[128]=Bank 1 {{ UseGsInstruments }}" : force the use of GSInstrument.java instances for this bank, to enable
 * drums/melodic - "Patch[*]=Drums {{ UseGsDrumsInstruments }}" : force the use of GSDumrsInstrument.java instances for this bank,
 * to enable drums/melodic GS voice switch<br>
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
        ArrayList<Instrument> currentBankInstruments = null;
        MidiSynth currentSynth = null;
        BankSelectMethod currentBsm = BankSelectMethod.MSB_LSB;
        HashMap<String, List<Instrument>> mapBankNameInstruments = new HashMap<>();
        try ( BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
        {
            int lineCount = 0;
            String line;
            Pattern pBank1 = Pattern.compile("^\\s*\\[(.+)\\]\\s*");  // [Bank PRE1]
            Pattern pPatch1 = Pattern.compile("^\\s*(\\d+)=(.*)");   // 1=Grand Piano
            Pattern pPatchDrums = Pattern.compile("\\{\\{\\s*DrumKit\\s*=\\s*(\\w+)\\s*,\\s*(\\w+)\\s*\\}\\}");   // {{DrumKit=STANDARD, KitKeyMap}}
            Pattern pPatchSubstitute = Pattern.compile("\\{\\{\\s*SubGM1\\s*=\\s*(\\d+)\\s*\\}\\}");   // {{SubGM1=12}}          
            Pattern pSynth2 = Pattern.compile("^\\s*\\[(.+)\\]\\s*");  // [Motif XS]
            Pattern pBsm2 = Pattern.compile("^\\s*BankSelMethod=([0-9])\\s*");  // BankSelMethod=2
            Pattern pPatch2 = Pattern.compile("^\\s*Patch\\[(\\d+)\\]=(.*)");  // Patch[1232]=PRE 1
            Pattern pPatch2Gs = Pattern.compile("\\{\\{\\s*UseGsInstruments\\s*\\}\\}");   // {{UseGsInstruments}}            
            Pattern pPatch2GsDrums = Pattern.compile("\\{\\{\\s*UseGsDrumsInstruments\\s*\\}\\}");   // {{UseGsDrumsInstruments}}            
            Pattern pPatch2XxBank0 = Pattern.compile("\\{\\{\\s*(GM|GM2|XG|GS)_BANK\\s*\\}\\}");   // {{ GM_BANK }}   or GM2_BANK, XG_BANK, GS_BANK
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
                    currentBankInstruments = null;
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
                        currentBankInstruments = new ArrayList<>();
                        // And store it
                        mapBankNameInstruments.put(bankName, currentBankInstruments);
                        continue;
                    } else if (mPatch.matches())
                    {
                        // Add instrument to the current bank
                        if (currentBankInstruments == null)
                        {
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Patch name found in file {0} at line {1} but no bank set.", new Object[]{fileName,
                                lineCount});   
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
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid program change value in file {0} at line {1}", new Object[]{fileName,
                                lineCount});   
                        }
                        DrumKit kit = null;
                        GM1Instrument gmSubstitute = null;
                        String patchName = mPatch.group(2);
                        if (patchName == null || patchName.trim().isEmpty())
                        {
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid patch name in file {0} at line {1}", new Object[]{fileName,
                                lineCount});   
                            patchName = "ERROR";
                        } else
                        {
                            // Got the patchName right, now check the Meta info {{ }}
                            patchName = patchName.trim();

                            // Check if a DrumKit is defined
                            Matcher mPatchDrumKit = pPatchDrums.matcher(patchName);
                            if (mPatchDrumKit.find())
                            {
                                // Yes, get the KitType and KeyMap
                                String keyMapName = mPatchDrumKit.group(2).trim();
                                String typeName = mPatchDrumKit.group(1).trim();
                                assert typeName != null : "patchName=" + patchName;   
                                DrumKit.KeyMap kitKeyMap = KeyMapProvider.Util.getKeyMap(keyMapName);
                                if (kitKeyMap == null)
                                {
                                    LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid DrumKit KeyMap {0} for instrument{1} in file {2} at line {3}. Using the GM drum map instead.", new Object[]{keyMapName,
                                        patchName, fileName, lineCount});   
                                    kitKeyMap = KeyMapGM.getInstance();
                                }
                                DrumKit.Type kitType = DrumKit.Type.STANDARD;
                                try
                                {
                                    kitType = DrumKit.Type.valueOf(typeName);
                                } catch (IllegalArgumentException e)
                                {
                                    LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid DrumKit type {0} for instrument{1} in file {2} at line {3}. Using the STANDARD DrumKit type instead.", new Object[]{typeName,
                                        patchName, fileName, lineCount});   
                                }
                                kit = new DrumKit(kitType, kitKeyMap);
                            }

                            // Check if a substitute is defined
                            Matcher mPatchSubstitute = pPatchSubstitute.matcher(patchName);
                            if (kit == null && mPatchSubstitute.find())
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
                                    LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid SubGM1 program change {0} for instrument{1} in file {2} at line {3}. Using value SubGM1 PC=0 instead.", new Object[]{pcGM1str,
                                        patchName, fileName, lineCount});
                                    pcGM1 = 0;
                                }
                                gmSubstitute = GMSynth.getInstance().getGM1Bank().getInstrument(pcGM1);
                            }

                            // Get rid of the extensions if meta info was provided
                            int index = patchName.indexOf("{{");
                            if (index != -1)
                            {
                                patchName = patchName.substring(0, index).trim();
                            }

                            // No DrumKit meta info found, try to guess if it's a drums from patchName
                            if (kit == null && gmSubstitute == null && MidiUtilities.guessIsPatchNameDrums(patchName))
                            {
                                kit = new DrumKit(DrumKit.Type.STANDARD, KeyMapGM.getInstance());
                            }

                            // No meta info found but it's probably not a drums, try to guess the substitute
                            if (kit == null && gmSubstitute == null)
                            {
                                gmSubstitute = GMSynth.getInstance().getGM1Bank().guessInstrument(patchName);
                            }
                        }

                        // Build the instrument with a NOT fully defined MidiAddress
                        Instrument ins;
                        ins = new Instrument(patchName, null, new MidiAddress(pc, -1, -1, null), kit, gmSubstitute);
                        currentBankInstruments.add(ins);
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
                        String synthName = mSynth.group(1).trim();
                        currentSynth = new MidiSynth(synthName, "");
                        synths.add(currentSynth);
                        currentBsm = BankSelectMethod.MSB_LSB;
                    } else if (mBsm.matches())
                    {
                        // It's a BankSelectMethod specification
                        // IMPORTANT: it will impact only lines after this line ! So should be the first line after Synth name
                        if (currentSynth == null)
                        {
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() BankSelectMethod found in file {0} at line {1} but no instrument set.", new Object[]{fileName,
                                lineCount});   
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
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid BankSelectMethod in file {0} at line {1}", new Object[]{fileName,
                                lineCount});   
                        }
                        currentBsm = getBsm(bsm);
                        continue;
                    } else if (m2Patch.matches())
                    {
                        // It's a "patch[*]=SomeBankName"
                        // Find the bank by its name, set the special BankSelectMethod, attach to the current MidiSynth
                        if (currentSynth == null)
                        {
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Patch[*] found in file {0} at line {1} but no instrument set.", new Object[]{fileName,
                                lineCount});   
                            continue;
                        }
                        String bankName = m2Patch.group(1).trim();

                        // Check if there is a Meta info {{ }}
                        Matcher mGsInstrument = pPatch2Gs.matcher(bankName);
                        Matcher mGsDrumsInstrument = pPatch2GsDrums.matcher(bankName);
                        boolean useGsIns = mGsInstrument.find();
                        boolean useGsDrumsIns = mGsDrumsInstrument.find();


                        // Remove possible meta info
                        int index = bankName.indexOf("{{");
                        if (index != -1)
                        {
                            bankName = bankName.substring(0, index).trim();
                        }

                        if (currentSynth.getBank(bankName) != null)
                        {
                            // The bank has already been created for this synth via a Patch[123]=SomeBankName                        
                            continue;
                        }
                        List<Instrument> bankInstruments = mapBankNameInstruments.get(bankName);
                        if (bankInstruments == null)
                        {
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid bank {0} in file {1} at line {2}", new Object[]{bankName,
                                fileName, lineCount});   
                        } else
                        {
                            // Create the actual bank with BankSelectMethod.PC_ONLY
                            InstrumentBank<Instrument> bank = new InstrumentBank<>(bankName, 0, 0, BankSelectMethod.PC_ONLY);
                            currentSynth.addBank(bank);
                            // Add the instruments
                            for (Instrument ins : bankInstruments)
                            {
                                // Create a copy of the instrument because some .ins can reuse one bank for several synths
                                Instrument insCopy;
                                MidiAddress adr = ins.getMidiAddress();
                                if (useGsIns)
                                {
                                    MidiAddress newMa = new MidiAddress(adr.getProgramChange(), adr.getBankMSB(), adr.getBankLSB(), MidiAddress.BankSelectMethod.MSB_ONLY);
                                    insCopy = new GSInstrument(ins.getPatchName(), ins.getBank(), newMa, ins.getDrumKit(), ins.getSubstitute());
                                } else if (useGsDrumsIns)
                                {
                                    MidiAddress newMa = new MidiAddress(adr.getProgramChange(), adr.getBankMSB(), adr.getBankLSB(), MidiAddress.BankSelectMethod.PC_ONLY);
                                    insCopy = new GSDrumsInstrument(ins.getPatchName(), ins.getBank(), newMa, ins.getDrumKit(), ins.getSubstitute());
                                } else
                                {
                                    insCopy = ins.getCopy();
                                }
                                try
                                {
                                    bank.addInstrument(insCopy);    // Instrument will inherit the MSB/LSB/BSM from the bank
                                } catch (IllegalArgumentException ex)
                                {
                                    LOGGER.log(Level.WARNING, "readSynthsFromStream() Can''t add instrument {0} in file {1} at line {2}. Exception={3}", new Object[]{insCopy.getPatchName(),
                                        fileName, lineCount, ex.getMessage()});   
                                }
                            }
                        }
                    } else if (mPatch.matches())
                    {
                        // It's a "patch[123]=SomeBankName"
                        // Find the bank by its name, set the MSB and LSB, attach to the current MidiSynth
                        if (currentSynth == null)
                        {
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Patch[] found in file {0} at line {1} but no instrument set.", new Object[]{fileName,
                                lineCount});   
                            continue;
                        }
                        String s = mPatch.group(1);
                        int value = 0;
                        try
                        {
                            value = Integer.parseInt(s);
                        } catch (NumberFormatException e)
                        {
                            // Leave i=0
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid Patch[] integer value in file {0} at line {1}", new Object[]{fileName,
                                lineCount});   
                        }
                        int msb = value / 128;
                        int lsb = value - (msb * 128);
                        String bankName = mPatch.group(2).trim();

                        // Check if there is a Meta info {{ }}
                        Matcher mGsInstrument = pPatch2Gs.matcher(bankName);
                        Matcher mGsDrumsInstrument = pPatch2GsDrums.matcher(bankName);
                        Matcher mXxBank0 = pPatch2XxBank0.matcher(bankName);
                        boolean useGsIns = mGsInstrument.find();
                        boolean useGsDrumsIns = mGsDrumsInstrument.find();
                        boolean useXxBank0 = mXxBank0.find();   // GM_BANK or GM2_BANK or XG_BANK or GS_BANK


                        // Remove possible meta info
                        int index = bankName.indexOf("{{");
                        if (index != -1)
                        {
                            bankName = bankName.substring(0, index).trim();
                        }

                        List<Instrument> bankInstruments = mapBankNameInstruments.get(bankName);
                        if (bankInstruments == null)
                        {
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Invalid bank {0} in file {1} at line {2}", new Object[]{bankName,
                                fileName, lineCount});   
                        } else if (bankInstruments.isEmpty())
                        {
                            LOGGER.log(Level.WARNING, "readSynthsFromStream() Empty bank {0} in file {1} at line {2}", new Object[]{bankName,
                                fileName, lineCount});   

                        } else
                        {
                            // Create the actual bank with current parameters
                            InstrumentBank<Instrument> bank = new InstrumentBank<>(bankName, msb, lsb, currentBsm);
                            currentSynth.addBank(bank);


                            // Update the synth if a GM_BANK or equivalent was used
                            if (useXxBank0)
                            {
                                String bankType = mXxBank0.group(1);        // GM, GM2, XG, GS
                                switch (bankType)
                                {
                                    case "GM":
                                        currentSynth.setCompatibility(true, null, null, null);
                                        currentSynth.setGM1BankBaseMidiAddress(new MidiAddress(0, msb, lsb, currentBsm));
                                        LOGGER.log(Level.FINE, "readSynthsFromStream() marking {0} as GM compatible, GM1 bank base address={1}", new Object[]{currentSynth.getName(),
                                            currentSynth.getGM1BankBaseMidiAddress()});
                                        break;
                                    case "GM2":
                                        currentSynth.setCompatibility(null, true, null, null);
                                        LOGGER.log(Level.FINE, "readSynthsFromStream() marking currentSynth={0} as GM2 compatible", currentSynth.getName());
                                        break;
                                    case "XG":
                                        currentSynth.setCompatibility(null, null, true, null);
                                        LOGGER.log(Level.FINE, "readSynthsFromStream() marking currentSynth={0} as XG compatible", currentSynth.getName());
                                        break;
                                    case "GS":
                                        currentSynth.setCompatibility(null, null, null, true);
                                        LOGGER.log(Level.FINE, "readSynthsFromStream() marking currentSynth={0} as GS compatible", currentSynth.getName());
                                        break;
                                    default:
                                        LOGGER.log(Level.WARNING, "readSynthsFromStream() unexpected bankType value{0} in file {1} at line {2}", new Object[]{bankType,
                                            fileName, lineCount});   
                                }
                            }


                            // Add the instruments
                            for (Instrument ins : bankInstruments)
                            {
                                // Create a copy of the instrument because some .ins can reuse one bank for several synths
                                Instrument insCopy;
                                MidiAddress adr = ins.getMidiAddress();
                                if (useGsIns)
                                {
                                    MidiAddress newMa = new MidiAddress(adr.getProgramChange(), adr.getBankMSB(), adr.getBankLSB(), MidiAddress.BankSelectMethod.MSB_ONLY);
                                    insCopy = new GSInstrument(ins.getPatchName(), ins.getBank(), newMa, ins.getDrumKit(), ins.getSubstitute());
                                } else if (useGsDrumsIns)
                                {
                                    MidiAddress newMa = new MidiAddress(adr.getProgramChange(), adr.getBankMSB(), adr.getBankLSB(), MidiAddress.BankSelectMethod.PC_ONLY);
                                    insCopy = new GSDrumsInstrument(ins.getPatchName(), ins.getBank(), newMa, ins.getDrumKit(), ins.getSubstitute());
                                } else
                                {
                                    insCopy = ins.getCopy();
                                }
                                try
                                {
                                    bank.addInstrument(insCopy);    // Instrument will inherit the MSB/LSB/BSM from the bank
                                } catch (IllegalArgumentException ex)
                                {
                                    LOGGER.log(Level.WARNING, "readSynthsFromStream() Can''t add instrument {0} in file {1} at line {2}. Exception={3}", new Object[]{insCopy.getPatchName(),
                                        fileName, lineCount, ex.getMessage()});   
                                }
                            }
                        }
                    }
                }
            }
            // We reached end of file
        }

        // In some cases we may have synths with no banks or 0 instruments, don't keep them
        // Example of such ins file : 
        // .Instrument Definitions
        // [Giga]
        // Patch[*]=Giga      
        // [MOXF]
        // Patch[239]=PRE 1
        // etc.
        synths.removeIf(ms -> ms.getNbInstruments() == 0);

        LOGGER.log(Level.FINE, "readSynthsFromStream() EXIT synths.size()={0}", synths.size());   
        if (synths.isEmpty())
        {
            LOGGER.log(Level.WARNING, "No synth found {0}", f == null ? "" : "in file " + f.getAbsolutePath());   
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
