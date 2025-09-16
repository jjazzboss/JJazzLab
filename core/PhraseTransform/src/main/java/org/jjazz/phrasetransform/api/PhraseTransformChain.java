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
package org.jjazz.phrasetransform.api;

import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.songcontext.api.SongPartContext;

/**
 * A chain of PhraseTransforms.
 */
public class PhraseTransformChain extends ArrayList<PhraseTransform>
{

    private static final Logger LOGGER = Logger.getLogger(PhraseTransformChain.class.getSimpleName());

    public PhraseTransformChain()
    {

    }

    /**
     * Create a chain which directly reuse the specified PhraseTransforms.
     *
     * @param pts The PhraseTransforms to be added in this chain (no copy is done).
     * @see #deepClone() 
     */
    public PhraseTransformChain(List<PhraseTransform> pts)
    {
        addAll(pts);
    }

    /**
     * Perform a deep clone of this chain : PhraseTransforms are cloned too.
     *
     * @return
     */
    public PhraseTransformChain deepClone()
    {
        var res = new PhraseTransformChain();
        for (var pt : this)
        {
            res.add(pt.getCopy());
        }
        return res;
    }

    /**
     * Run all the transforms in the chain.
     *
     *
     * @param inPhrase
     * @param context
     * @return
     * @throws IllegalStateException If a transformed phrase has a different beat range than inPhrase.
     */
    public SizedPhrase transform(SizedPhrase inPhrase, SongPartContext context)
    {
        SizedPhrase sp = new SizedPhrase(inPhrase);
        for (var pt : this)
        {
            sp = pt.transform(sp, context);

            if (!sp.getNotesBeatRange().equals(inPhrase.getNotesBeatRange()))
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
            if (pt == null)
            {
                LOGGER.log(Level.WARNING, "loadFromString() No PhraseTransform found for str={0} (s={1})", new Object[]{str, s});
            } else
            {
                res.add(pt);
            }
        }

        return res;
    }

}
