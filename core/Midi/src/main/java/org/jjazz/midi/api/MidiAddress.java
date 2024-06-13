/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midi.api;

import java.util.Objects;

/**
 * The data to select an Instrument via Midi.
 * <p>
 * This is an immutable class.
 */
public class MidiAddress
{

    public enum BankSelectMethod
    {
        MSB_LSB, MSB_ONLY, LSB_ONLY, PC_ONLY
    };
    private int bankMSB;
    private int bankLSB;
    private int progChange;
    private BankSelectMethod bsMethod;

    /**
     * Create a MidiAddress.
     *
     * @param progChange [0-127]
     * @param bankMSB    [0-127] or -1 if undefined
     * @param bankLSB    [0-127] or -1 if undefined
     * @param bsMethod   null if undefined
     */
    public MidiAddress(int progChange, int bankMSB, int bankLSB, BankSelectMethod bsMethod)
    {
        if (bankMSB < -1 || bankMSB > 127 || bankLSB < -1 || bankLSB > 127 || progChange < 0 || progChange > 127)
        {
            throw new IllegalArgumentException("bankMSB=" + bankMSB + " bankLSB=" + bankLSB + " progChange=" + progChange);   
        }
        this.bankMSB = bankMSB;
        this.bankLSB = bankLSB;
        this.progChange = progChange;
        this.bsMethod = bsMethod;
    }

    /**
     * @return Null if undefined.
     */
    public BankSelectMethod getBankSelectMethod()
    {
        return bsMethod;
    }

    /**
     * The BankSelect MSB byte (Midi CC control #0).
     *
     * @return the bankMSB [0;127] or -1 if bankMSB is undefined.
     */
    public int getBankMSB()
    {
        return bankMSB;
    }

    /**
     * The BankSelect LSB byte (Midi CC control #32).
     *
     * @return the bankLSB [0;-127] or -1 if bankLSB is undefined.
     */
    public int getBankLSB()
    {
        return bankLSB;
    }

    /**
     * @return the progChange [0-127]
     */
    public int getProgramChange()
    {
        return progChange;
    }

    /**
     * Return true if bankMSB, bankLSB and bankSelectMethod are defined consistently.
     * <p>
     *
     * @return
     */
    public boolean isFullyDefined()
    {
        if (bsMethod == null)
        {
            return false;
        }
        switch (bsMethod)
        {
            case MSB_LSB:
                return bankLSB > -1 && bankMSB > -1;
            case MSB_ONLY:
                return bankMSB > -1;
            case LSB_ONLY:
                return bankLSB > -1;
            case PC_ONLY:
                return true;
            default:
                throw new IllegalStateException("bsMethod=" + bsMethod);   
        }
    }

    @Override
    public String toString()
    {
        return "[pc=" + this.progChange + ", bankMSB=" + this.bankMSB + ", bankLSB=" + this.bankLSB + ", bankSelectMethod=" + this.bsMethod + "]";
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        if (this.bsMethod == null)
        {
            hash = 23 * hash + 11;
        } else
        {
            switch (this.bsMethod)
            {
                case MSB_LSB:
                    hash = 23 * hash + this.bankMSB;
                    hash = 23 * hash + this.bankLSB;
                    break;
                case MSB_ONLY:
                    hash = 23 * hash + this.bankMSB;
                    break;
                case LSB_ONLY:
                    hash = 23 * hash + this.bankLSB;
                    break;
                case PC_ONLY:
                    // Nothing
                    break;
                default:
                    throw new IllegalStateException("bsMethod=" + bsMethod);   
            }
        }
        hash = 23 * hash + this.progChange;
        hash = 23 * hash + Objects.hashCode(this.bsMethod);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final MidiAddress other = (MidiAddress) obj;
        if (this.progChange != other.progChange)
        {
            return false;
        }
        if (this.bsMethod != other.bsMethod)
        {
            return false;
        }
        if (this.bsMethod == null)
        {
            return true;
        }
        switch (this.bsMethod)
        {
            case MSB_LSB:
                return (this.bankMSB == other.bankMSB) && (this.bankLSB == other.bankLSB);
            case MSB_ONLY:
                return (this.bankMSB == other.bankMSB);
            case LSB_ONLY:
                return (this.bankLSB == other.bankLSB);
            case PC_ONLY:
                return true;
            default:
                throw new IllegalArgumentException("bsMethod=" + this.bsMethod);   
        }
    }
}
