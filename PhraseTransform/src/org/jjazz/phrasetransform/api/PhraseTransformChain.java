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
package org.jjazz.phrasetransform.api;

import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.StringJoiner;
import org.jjazz.midi.api.Instrument;
import org.jjazz.phrase.api.SizedPhrase;

/**
 * A chain of PhraseTransforms.
 */
public class PhraseTransformChain extends ArrayList<PhraseTransform>
{

    public PhraseTransformChain()
    {

    }

    public PhraseTransformChain(PhraseTransformChain chain)
    {
        super(chain);
    }

    /**
     * Run all the transforms in the chain.
     *
     * @param inPhrase
     * @param ins
     * @return
     */
    public SizedPhrase transform(SizedPhrase inPhrase, Instrument ins)
    {
        SizedPhrase sp = inPhrase;
        for (var pt : this)
        {
            sp = pt.transform(sp, ins);

            if (!sp.getBeatRange().equals(inPhrase.getBeatRange()))
            {
                throw new IllegalStateException("Invalid beatRange modification by pt=" + pt + ", inPhrase=" + inPhrase + ", sp=" + sp);
            }
        }
        return sp;
    }

    /**
     * Save the chain as a string.
     * <p>
     * Example: "[uniqueId1#prop1=value1,prop2=value2|uniqueId2#|uniqueId3#prop1=value1]"
     *
     * @param chain
     * @return
     * @see PhraseTransformChain#loadFromString(java.lang.String)
     */
    static public String saveAsString(PhraseTransformChain chain)
    {
        StringJoiner joiner = new StringJoiner("|", "[", "]");
        chain.forEach(pt -> joiner.add(PhraseTransform.saveAsString(pt)));
        return joiner.toString();
    }

    /**
     * Create a PhraseTransformChain instance from a save string.
     *
     * @param s
     * @return
     * @throws ParseException
     * @see PhraseTransformChain#saveAsString(org.jjazz.phrasetransform.api.PhraseTransformChain)
     */
    static public PhraseTransformChain loadFromString(String s) throws ParseException
    {
        checkNotNull(s);
        s = s.trim();
        if (s.length() < 2 || s.charAt(0) != '[' || s.charAt(s.length() - 1) != ']')
        {
            throw new ParseException("Invalid PhraseTransformChain string s=" + s, 0);
        }
        s = s.substring(1, s.length() - 1);

        PhraseTransformChain res = new PhraseTransformChain();
        if (s.isBlank())
        {
            return res;
        }
        String strs[] = s.split("\\|");

        for (String str : strs)
        {
            PhraseTransform pt = PhraseTransform.loadFromString(str.trim());
            res.add(pt);
        }

        return res;
    }

}
