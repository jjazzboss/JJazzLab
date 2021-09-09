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
package org.jjazz.harmony.api;


import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.math.BigIntegerMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.util.api.Utilities;

/**
 * Find matching chord symbol(s) from individual notes.
 */
public class ChordFinder
{

    static public final int MAX_NOTES = 4;
    static private NoteNode root;
    private static final Logger LOGGER = Logger.getLogger(ChordFinder.class.getSimpleName());  //NOI18N

    public ChordFinder()
    {
        buildNoteNodeTree();
    }

    /**
     * Find the chord symbols which match the specified notes.
     *
     * @param pitches
     * @return
     */
    public List<ChordSymbol> find(List<Integer> pitches)
    {
        List<ChordSymbol> res = new ArrayList<>();
        if (pitches.size() < 3 || pitches.size() > MAX_NOTES)
        {
            return res;
        }

        // Compute all permutations
        int nbPerm = BigIntegerMath.factorial(pitches.size()).intValue();
        List<Integer[]> pitchPermutations = new ArrayList<>(nbPerm);
        Utilities.heapPermutation(pitches.toArray(new Integer[0]), pitches.size(), pitchPermutations);


        // Test each permutation (fast since tree contains all possibilites)
        for (Integer[] perm : pitchPermutations)
        {
            NoteNode nn = root.findNode(perm);
            if (nn != null)
            {
                res.add(nn.getChordSymbol());
            }
        }

        return res;
    }

    // =====================================================================================
    // Private methods
    // =====================================================================================

    /**
     * Compute a note tree for all the possible combinations: all keys, all chordtypes, all positions
     * <p>
     */
    static private void buildNoteNodeTree()
    {
        if (root != null)
        {
            return;
        }
        root = new NoteNode(-1, null);

        long nodeCount = 1;  // root node included

        // Each key
        for (int rootPitch = 0; rootPitch < 12; rootPitch++)
        {
            Note rootNote = new Note(rootPitch);


            // Each 3-4 note chord type
            for (ChordType ct : ChordTypeDatabase.getInstance().getChordTypes())
            {
                var chord = ct.getChord();
                if (chord.size() < 3 || chord.size() > MAX_NOTES)
                {
                    continue;
                }
                chord.transpose(rootPitch);
                ChordSymbol cs = new ChordSymbol(rootNote, ct);


                // Compute all possible positions for this chord symbol
                List<Integer> pitches = chord.getNotes().stream().map(n -> n.getRelativePitch()).collect(Collectors.toList());
                List<Integer[]> pitchPermutations = new ArrayList<>(BigIntegerMath.factorial(pitches.size()).intValue());
                Utilities.heapPermutation(pitches.toArray(new Integer[0]), pitches.size(), pitchPermutations);


                // Each position                
                for (Integer[] perm : pitchPermutations)
                {
                    NoteNode lastNode = root;

                    // Each note of the position
                    for (int i = 0; i < perm.length; i++)
                    {
                        int pitch = perm[i];
                        NoteNode node = new NoteNode(pitch, i == perm.length - 1 ? cs : null);      // Add chord symbol on last note only
                        lastNode.addChild(node);
                        lastNode = node;
                        nodeCount++;
                    }
                }


            }
        }

        System.out.println("buildNoteNodeTree() nodeCount=" + nodeCount);
    }

    // =====================================================================================
    // Private classes
    // =====================================================================================

    static private class NoteNode
    {

        private final int pitch;
        private final ChordSymbol chordSymbol;
        private NoteNode parent;
        private Set<NoteNode> children;

        public NoteNode(int p, ChordSymbol cs)
        {
            pitch = p % 12;
            chordSymbol = cs;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            if (children != null)
            {
                sb.append("[");
                for (NoteNode n : children)
                {
                    sb.append(n.pitch).append(",");
                }
                sb.append("]");
            }
            return "pitch=" + pitch + " chordSymbol=" + chordSymbol + " children=" + sb.toString();
        }

        /**
         * Find the node by searching a path where each pitch exists in the children.
         *
         * @param pitches
         * @return Null if not found
         */
        public NoteNode findNode(Integer[] pitches)
        {
            checkNotNull(pitches);
            System.out.println("findNode() -- this=" + this + " pitches=" + Arrays.asList(pitches));
            if (pitches.length == 0 || children == null)
            {
                System.out.println("  pitches empty or no children => exiting");
                return null;
            }

            NoteNode res = children.stream()
                    .filter(child -> child.pitch == pitches[0] % 12)
                    .findAny()
                    .orElse(null);
            System.out.println("  res=" + res);
            if (res != null && pitches.length > 1)
            {
                System.out.println("  ==> recursive call");
                res = res.findNode(Arrays.copyOfRange(pitches, 1, pitches.length));
            }
            System.out.println("  final res=" + res);
            return res;
        }

        public void addChild(NoteNode node)
        {
            if (children == null)
            {
                children = new HashSet<>();
            }
            children.add(node);
            node.parent = this;
        }

        public NoteNode getParent()
        {
            return parent;
        }

        public Set<NoteNode> getChildren()
        {
            return children;
        }

        public int getPitch()
        {
            return pitch;
        }

        public ChordSymbol getChordSymbol()
        {
            return chordSymbol;
        }

        /**
         * Use only pitch!
         *
         * @return
         */
        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 59 * hash + this.pitch;
            return hash;
        }

        /**
         * Use only pitch!
         *
         * @return
         */
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
            final NoteNode other = (NoteNode) obj;
            if (this.pitch != other.pitch)
            {
                return false;
            }
            return true;
        }

    }


}
