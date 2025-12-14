/*
    Copyright (c) 2009, incava.org
    All rights reserved.

    Redistribution and use in source and binary forms, with or without modification,
    are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright notice,
        * this list of conditions and the following disclaimer.

        * Redistributions in binary form must reproduce the above copyright notice,
        * this list of conditions and the following disclaimer in the documentation
        * and/or other materials provided with the distribution.

        * Neither the name of incava.org nor the names of its contributors may be
        * used to endorse or promote products derived from this software without
        * specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
    ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
    ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
    LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
    ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jjazz.utilities.api;

import java.util.*;

/**
 * Compares two lists, returning a list of the additions, changes, and deletions between them.
 * <p>
 * A <code>Comparator</code> may be passed as an argument to the constructor, and will thus be used. If not provided, the initial value in the <code>a</code>
 * ("from") list will be looked at to see if it supports the <code>Comparable</code> interface. If so, its <code>equals</code> and <code>compareTo</code>
 * methods will be invoked on the instances in the "from" and "to" lists; otherwise, for speed, hash codes from the objects will be used instead for comparison.
 * <p>
 * JJazzLab changes: updated so that each CHANGED difference has the same number of objects in/out.
 *
 * @param <Type>
 */
@SuppressWarnings(
        {
            "unchecked", "rawtypes"
        })
public class Diff<Type>
{

    private final boolean DEBUG = false;
    /**
     * The source list, AKA the "from" values.
     */
    private List<Type> a;
    /**
     * The target list, AKA the "to" values.
     */
    private List<Type> b;
    /**
     * The list of differences, as <code>Difference</code> instances.
     */
    private final List<Difference> diffs = new ArrayList<>();
    /**
     * The pending, uncommitted difference.
     */
    private Difference pending;
    /**
     * The comparator used, if any.
     */
    private Comparator<Type> comparator;
    /**
     * The thresholds.
     */
    private TreeMap<Integer, Integer> thresh;


    /**
     * Constructs the Diff object for the two arrays, using the given comparator.
     */
    private Diff(Type[] a, Type[] b, Comparator<Type> comp)
    {
        this(Arrays.asList(a), Arrays.asList(b), comp);
    }

    /**
     * Constructs the Diff object for the two arrays, using the default comparison mechanism between the objects, such as <code>equals</code> and
     * <code>compareTo</code>.
     */
    private Diff(Type[] a, Type[] b)
    {
        this(a, b, null);
    }

    /**
     * Constructs the Diff object for the two lists, using the given comparator.
     */
    private Diff(List<Type> a, List<Type> b, Comparator<Type> comp)
    {
        this.a = a;
        this.b = b;
        this.comparator = comp;
        this.thresh = null;
    }

    /**
     * Constructs the Diff object for the two lists, using the default comparison mechanism between the objects, such as <code>equals</code> and
     * <code>compareTo</code>.
     */
    private Diff(List<Type> a, List<Type> b)
    {
        this(a, b, null);
    }

    /**
     * Compute differences for the two arrays, using the specified comparison mechanism.
     *
     * @param <T>
     * @param a
     * @param b
     * @param comp
     * @return
     */
    static public <T> List<Difference> diff(T[] a, T[] b, Comparator<T> comp)
    {
        Diff di = new Diff(a, b, comp);
        return di.diff();
    }

    /**
     * Compute differences for the two arrays, using the default comparison mechanism between the objects, such as <code>equals</code> and
     * <code>compareTo</code>.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    static public <T> List<Difference> diff(T[] a, T[] b)
    {
        Diff di = new Diff(a, b);
        return di.diff();
    }

    /**
     * Compute differences for the two lists, using the specified comparison mechanism.
     *
     * @param <T>
     * @param a
     * @param b
     * @param comp
     * @return
     */
    static public <T> List<Difference> diff(List<T> a, List<T> b, Comparator<T> comp)
    {
        Diff di = new Diff(a, b, comp);
        return di.diff();
    }

    /**
     * Compute differences for the two lists, using the default comparison mechanism between the objects, such as <code>equals</code> and
     * <code>compareTo</code>.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    static public <T> List<Difference> diff(List<T> a, List<T> b)
    {
        Diff di = new Diff(a, b);
        return di.diff();
    }

    /**
     * Runs diff and returns the results.
     */
    private List<Difference> diff()
    {
        if (a == null || b == null)
        {
            throw new IllegalStateException("a=" + a + " b=" + b);
        }
        if (DEBUG)
        {
            System.out.println("Diff.diff()");
            System.out.println("  a=" + a);
            System.out.println("  b=" + b);
        }

        // Perform the diff
        traverseSequences();

        // add the last difference, if pending:
        if (pending != null)
        {
            diffs.add(pending);
        }

        List<Difference> diffResults = new ArrayList<>();

        for (Difference d : diffs)
        {
            // Update the CHANGED differences so that they have the same number of in/out lines
            if (d.getType() == Difference.ResultType.CHANGED && d.getFromRange() != d.getToRange())
            {
                if (d.getFromRange() > d.getToRange())
                {
                    // More changes in From than in To: transform into 1 changed + 1 deleted
                    Difference cdr = new Difference(d.getDeletedStart(), d.getDeletedStart() + d.getToRange() - 1, d.getAddedStart(), d.getAddedEnd());
                    Difference ddr = new Difference(d.getDeletedStart() + d.getToRange(), d.getDeletedEnd(), d.getDeletedEnd() + 1,
                            Difference.NONE);
                    diffResults.add(cdr);
                    diffResults.add(ddr);
                } else
                {
                    // More changes in To than in From: transform into 1 changed + 1 added
                    Difference cdr = new Difference(d.getDeletedStart(), d.getDeletedEnd(), d.getAddedStart(), d.getAddedStart() + d.getFromRange() - 1);
                    Difference adr = new Difference(d.getDeletedEnd() + 1, Difference.NONE, d.getAddedStart() + d.getFromRange(), d.getAddedEnd());
                    diffResults.add(cdr);
                    diffResults.add(adr);
                }
            } else
            {
                // Don't do anything for Add or Del only differences.
                diffResults.add(d);
            }
        }

        if (DEBUG)
        {
            System.out.println(" Diff Results1: " + diffResults);
            System.out.println(" Diff results2:");
            for (Difference dr : diffResults)
            {
                if (dr.getType() == Difference.ResultType.DELETED)
                {
                    System.out.println(" DEL" + a.subList(dr.getDeletedStart(), dr.getDeletedEnd() + 1));
                } else if (dr.getType() == Difference.ResultType.ADDED)
                {
                    System.out.println(" ADD" + b.subList(dr.getAddedStart(), dr.getAddedEnd() + 1));
                } else
                {
                    System.out.println(" CHG" + a.subList(dr.getDeletedStart(), dr.getDeletedEnd() + 1)
                            + b.subList(dr.getAddedStart(), dr.getAddedEnd() + 1));
                }
            }
        }

        return diffResults;
    }

    /**
     * Traverses the sequences, seeking the longest common subsequences, invoking the methods <code>finishedA</code>, <code>finishedB</code>,
     * <code>onANotB</code>, and <code>onBNotA</code>.
     */
    private void traverseSequences()
    {
        Integer[] matches = getLongestCommonSubsequences();

        int lastA = a.size() - 1;
        int lastB = b.size() - 1;
        int bi = 0;
        int ai;

        int lastMatch = matches.length - 1;

        for (ai = 0; ai <= lastMatch; ++ai)
        {
            Integer bLine = matches[ai];

            if (bLine == null)
            {
                onANotB(ai, bi);
            } else
            {
                while (bi < bLine)
                {
                    onBNotA(ai, bi++);
                }

                onMatch(ai, bi++);
            }
        }

        boolean calledFinishA = false;
        boolean calledFinishB = false;

        while (ai <= lastA || bi <= lastB)
        {

            // last A?
            if (ai == lastA + 1 && bi <= lastB)
            {
                if (!calledFinishA && callFinishedA())
                {
                    finishedA(lastA);
                    calledFinishA = true;
                } else
                {
                    while (bi <= lastB)
                    {
                        onBNotA(ai, bi++);
                    }
                }
            }

            // last B?
            if (bi == lastB + 1 && ai <= lastA)
            {
                if (!calledFinishB && callFinishedB())
                {
                    finishedB(lastB);
                    calledFinishB = true;
                } else
                {
                    while (ai <= lastA)
                    {
                        onANotB(ai++, bi);
                    }
                }
            }

            if (ai <= lastA)
            {
                onANotB(ai++, bi);
            }

            if (bi <= lastB)
            {
                onBNotA(ai, bi++);
            }
        }
    }

    /**
     * Override and return true in order to have <code>finishedA</code> invoked at the last element in the <code>a</code> array.
     */
    private boolean callFinishedA()
    {
        return false;
    }

    /**
     * Override and return true in order to have <code>finishedB</code> invoked at the last element in the <code>b</code> array.
     */
    private boolean callFinishedB()
    {
        return false;
    }

    /**
     * Invoked at the last element in <code>a</code>, if <code>callFinishedA</code> returns true.
     */
    private void finishedA(int lastA)
    {
    }

    /**
     * Invoked at the last element in <code>b</code>, if <code>callFinishedB</code> returns true.
     */
    private void finishedB(int lastB)
    {
    }

    /**
     * Invoked for elements in <code>a</code> and not in <code>b</code>.
     */
    private void onANotB(int ai, int bi)
    {
        if (pending == null)
        {
            pending = new Difference(ai, ai, bi, -1);
        } else
        {
            pending.setDeleted(ai);
        }
    }

    /**
     * Invoked for elements in <code>b</code> and not in <code>a</code>.
     */
    private void onBNotA(int ai, int bi)
    {
        if (pending == null)
        {
            pending = new Difference(ai, -1, bi, bi);
        } else
        {
            pending.setAdded(bi);
        }
    }

    /**
     * Invoked for elements matching in <code>a</code> and <code>b</code>.
     */
    private void onMatch(int ai, int bi)
    {
        if (pending == null)
        {
            // no current pending
        } else
        {
            diffs.add(pending);
            pending = null;
        }
    }

    /**
     * Compares the two objects, using the comparator provided with the constructor, if any.
     */
    private boolean equals(Type x, Type y)
    {
        return comparator == null ? x.equals(y) : comparator.compare(x, y) == 0;
    }

    /**
     * Returns an array of the longest common subsequences.
     */
    private Integer[] getLongestCommonSubsequences()
    {
        int aStart = 0;
        int aEnd = a.size() - 1;

        int bStart = 0;
        int bEnd = b.size() - 1;

        TreeMap<Integer, Integer> matches = new TreeMap<>();

        while (aStart <= aEnd && bStart <= bEnd && equals(a.get(aStart), b.get(bStart)))
        {
            matches.put(aStart++, bStart++);
        }

        while (aStart <= aEnd && bStart <= bEnd && equals(a.get(aEnd), b.get(bEnd)))
        {
            matches.put(aEnd--, bEnd--);
        }

        Map<Type, List<Integer>> bMatches = null;
        if (comparator == null)
        {
            if (a.size() > 0 && a.get(0) instanceof Comparable)
            {
                // this uses the Comparable interface
                bMatches = new TreeMap<>();
            } else
            {
                // this just uses hashCode()
                bMatches = new HashMap<>();
            }
        } else
        {
            // we don't really want them sorted, but this is the only Map
            // implementation (as of JDK 1.4) that takes a comparator.
            bMatches = new TreeMap<>(comparator);
        }

        for (int bi = bStart; bi <= bEnd; ++bi)
        {
            Type element = b.get(bi);
            Type key = element;
            List<Integer> positions = bMatches.get(key);

            if (positions == null)
            {
                positions = new ArrayList<>();
                bMatches.put(key, positions);
            }

            positions.add(bi);
        }

        thresh = new TreeMap<>();
        Map<Integer, Object[]> links = new HashMap<>();

        for (int i = aStart; i <= aEnd; ++i)
        {
            Type aElement = a.get(i);
            List<Integer> positions = bMatches.get(aElement);

            if (positions != null)
            {
                Integer k = 0;
                ListIterator<Integer> pit = positions.listIterator(positions.size());
                while (pit.hasPrevious())
                {
                    Integer j = pit.previous();

                    k = insert(j, k);

                    if (k == null)
                    {
                        // nothing
                    } else
                    {
                        Object value = k > 0 ? links.get(k - 1) : null;
                        links.put(k, new Object[]
                        {
                            value, i, j
                        });
                    }
                }
            }
        }

        if (thresh.size() > 0)
        {
            Integer ti = thresh.lastKey();
            Object[] link = links.get(ti);
            while (link != null)
            {
                Integer x = (Integer) link[1];
                Integer y = (Integer) link[2];
                matches.put(x, y);
                link = (Object[]) link[0];
            }
        }

        int size = matches.size() == 0 ? 0 : 1 + matches.lastKey();
        Integer[] ary = new Integer[size];
        for (Integer idx : matches.keySet())
        {
            Integer val = matches.get(idx);
            ary[idx] = val;
        }
        return ary;
    }

    /**
     * Returns whether the integer is not zero (including if it is not null).
     */
    private static boolean isNonzero(Integer i)
    {
        return i != null && i != 0;
    }

    /**
     * Returns whether the value in the map for the given index is greater than the given value.
     */
    private boolean isGreaterThan(Integer index, Integer val)
    {
        Integer lhs = thresh.get(index);
        return lhs != null && val != null && lhs.compareTo(val) > 0;
    }

    /**
     * Returns whether the value in the map for the given index is less than the given value.
     */
    private boolean isLessThan(Integer index, Integer val)
    {
        Integer lhs = thresh.get(index);
        return lhs != null && (val == null || lhs.compareTo(val) < 0);
    }

    /**
     * Returns the value for the greatest key in the map.
     */
    private Integer getLastValue()
    {
        return thresh.get(thresh.lastKey());
    }

    /**
     * Adds the given value to the "end" of the threshold map, that is, with the greatest index/key.
     */
    private void append(Integer value)
    {
        Integer addIdx = null;
        if (thresh.size() == 0)
        {
            addIdx = 0;
        } else
        {
            Integer lastKey = thresh.lastKey();
            addIdx = lastKey + 1;
        }
        thresh.put(addIdx, value);
    }

    /**
     * Inserts the given values into the threshold map.
     */
    private Integer insert(Integer j, Integer k)
    {
        if (isNonzero(k) && isGreaterThan(k, j) && isLessThan(k - 1, j))
        {
            thresh.put(k, j);
        } else
        {
            int high = -1;

            if (isNonzero(k))
            {
                high = k;
            } else if (thresh.size() > 0)
            {
                high = thresh.lastKey();
            }

            // off the end?
            if (high == -1 || j.compareTo(getLastValue()) > 0)
            {
                append(j);
                k = high + 1;
            } else
            {
                // binary search for insertion point:
                int low = 0;

                while (low <= high)
                {
                    int index = (high + low) / 2;
                    Integer val = thresh.get(index);
                    int cmp = j.compareTo(val);

                    if (cmp == 0)
                    {
                        return null;
                    } else if (cmp > 0)
                    {
                        low = index + 1;
                    } else
                    {
                        high = index - 1;
                    }
                }

                thresh.put(low, j);
                k = low;
            }
        }

        return k;
    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================

    /**
     * Represents a difference.
     * <p>
     * A difference consists of two pairs of starting and ending points, each pair representing either the "from" or the "to" collection passed to
     * <code>Diff</code>. If an ending point is -1, then the difference was either a deletion or an addition. For example, if <code>getDeletedEnd()</code>
     * returns -1, then the difference represents an addition.
     * <p>
     * JJazzLab changes: minor usability changes.
     */
    static public class Difference
    {

        public static final int NONE = -1;

        public enum ResultType
        {
            CHANGED, ADDED, DELETED
        };

        /**
         * The type of this Difference.
         */
        private ResultType type;

        /**
         * The point at which the deletion starts.
         */
        private int fromStart = NONE;
        /**
         * The point at which the deletion ends.
         */
        private int fromEnd = NONE;
        /**
         * The nb of deleted chars.
         */
        private int fromRange;
        /**
         * The point at which the addition starts.
         */
        private int toStart = NONE;
        /**
         * The point at which the addition ends.
         */
        private int toEnd = NONE;
        /**
         * The nb of added chars.
         */
        private int toRange;

        /**
         * Creates the difference for the given start and end points for the deletion and addition.
         */
        public Difference(int delStart, int delEnd, int addStart, int addEnd)
        {
            this.fromStart = delStart;
            this.fromEnd = delEnd;
            this.toStart = addStart;
            this.toEnd = addEnd;
            updateCalculatedValues();
        }

        public int getFromRange()
        {
            return fromRange;
        }

        public int getToRange()
        {
            return toRange;
        }

        public ResultType getType()
        {
            return type;
        }

        /**
         * The point at which the deletion starts, if any. A value equal to <code>NONE</code> means this is an addition.
         */
        public int getDeletedStart()
        {
            return fromStart;
        }

        /**
         * The point at which the deletion ends, if any. A value equal to <code>NONE</code> means this is an addition.
         */
        public int getDeletedEnd()
        {
            return fromEnd;
        }

        /**
         * The point at which the addition starts, if any. A value equal to <code>NONE</code> means this must be an addition.
         */
        public int getAddedStart()
        {
            return toStart;
        }

        /**
         * The point at which the addition ends, if any. A value equal to <code>NONE</code> means this must be an addition.
         */
        public int getAddedEnd()
        {
            return toEnd;
        }

        /**
         * Sets the point as deleted. The start and end points will be modified to include the given line.
         */
        public void setDeleted(int line)
        {
            fromStart = Math.min(line, fromStart);
            fromEnd = Math.max(line, fromEnd);
            updateCalculatedValues();
        }

        /**
         * Sets the point as added. The start and end points will be modified to include the given line.
         */
        public void setAdded(int line)
        {
            toStart = Math.min(line, toStart);
            toEnd = Math.max(line, toEnd);
            updateCalculatedValues();
        }

        private void updateCalculatedValues()
        {
            if (fromEnd != NONE && toEnd != NONE)
            {
                type = ResultType.CHANGED;
            } else if (fromEnd == NONE)
            {
                type = ResultType.ADDED;
            } else
            {
                type = ResultType.DELETED;
            }
            fromRange = fromEnd - fromStart + 1;
            toRange = toEnd - toStart + 1;
        }

        /**
         * Compares this object to the other for equality. Both objects must be of type Difference, with the same starting and ending points.
         */
        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Difference)
            {
                Difference other = (Difference) obj;

                return (fromStart == other.fromStart
                        && fromEnd == other.fromEnd
                        && toStart == other.toStart
                        && toEnd == other.toEnd);
            } else
            {
                return false;
            }
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 71 * hash + this.fromStart;
            hash = 71 * hash + this.fromEnd;
            hash = 71 * hash + this.toStart;
            hash = 71 * hash + this.toEnd;
            return hash;
        }

        /**
         * Returns a string representation of this difference.
         */
        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append("[del:" + fromStart + "," + fromEnd);
            buf.append(" ");
            buf.append("add:" + toStart + "," + toEnd + "]");
            return buf.toString();
        }
    }


}
