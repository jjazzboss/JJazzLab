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
package org.jjazz.utilities.api;

import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jerome
 */
public class MatrixGroupsRemoverTest
{

    public MatrixGroupsRemoverTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testFindRemovalSequence1()
    {
        System.out.println("testFindRemovalSequence1() ----------------");
        // Define the 3x3 test matrix
        String[][] matrix =
        {
            {
                "A", "B", "A"
            },
            {
                "A", "A", "A"
            },
            {
                "B", "B", "B"
            }
        };

        // Define the minimum group size
        int minGroupSize = 2;

        // Create the MatrixGroupsRemover instance
        MatrixGroupsRemover<String> groupRemover = new MatrixGroupsRemover<>(matrix, minGroupSize);

        // Find the removal sequence
        List<MatrixGroupsRemover.Group<String>> removalSequence = groupRemover.findRemovalSequence();

        // Validate the removal sequence size (2 groups)
        assertEquals("Expected removal sequence size is 2", 2, removalSequence.size());

        // Validate the groups in the sequence
        // Group 1: (0,0), (0,2), (1,0), (1,2)
        MatrixGroupsRemover.Group<String> group1 = removalSequence.get(0);
        assertEquals(4, group1.cells().size());
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(0, 0)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(0, 2)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(1, 0)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(1, 2)));
        assertEquals("A", group1.value());

        // Group 2: (2,0), (2,1), (2,2)
        MatrixGroupsRemover.Group<String> group2 = removalSequence.get(1);
        assertEquals(3, group2.cells().size());
        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 0)));
        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 1)));
        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 2)));
        assertEquals("B", group2.value());
    }

    @Test
    public void testFindRemovalSequenceMinGroupSize1()
    {
        System.out.println("testFindRemovalSequenceMinGroupSize1() ----------------");
        // Define the 3x3 test matrix
        String[][] matrix =
        {
            {
                "A", "B", "A"
            },
            {
                "A", "A", "A"
            },
            {
                "B", "B", "B"
            }
        };

        printMatrix(matrix);

        // Define the minimum group size
        int minGroupSize = 1;

        // Create the MatrixGroupsRemover instance
        MatrixGroupsRemover<String> groupRemover = new MatrixGroupsRemover<>(matrix, minGroupSize);

        // Find the removal sequence
        List<MatrixGroupsRemover.Group<String>> removalSequence = groupRemover.findRemovalSequence();

        // Validate the removal sequence size (2 groups)
        assertEquals("Expected removal sequence size is 4", 4, removalSequence.size());

        // Validate the groups in the sequence
        // Group 1: (0,0), (0,2), (1,0), (1,2)
        MatrixGroupsRemover.Group<String> group1 = removalSequence.get(0);
        assertEquals(4, group1.cells().size());
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(0, 0)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(0, 2)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(1, 0)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(1, 2)));
        assertEquals("A", group1.value());

        // Group 2: (2,0), (2,1), (2,2)
        MatrixGroupsRemover.Group<String> group2 = removalSequence.get(1);
        assertEquals(3, group2.cells().size());
        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 0)));
        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 1)));
        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 2)));
        assertEquals("B", group2.value());

        // Group 3
        MatrixGroupsRemover.Group<String> group3 = removalSequence.get(2);
        assertEquals(1, group3.cells().size());
        assertTrue(group3.cells().contains(new MatrixGroupsRemover.Cell(0, 1)));

        // Group 4
        MatrixGroupsRemover.Group<String> group4 = removalSequence.get(3);
        assertEquals(1, group4.cells().size());
        assertTrue(group4.cells().contains(new MatrixGroupsRemover.Cell(1, 1)));
    }

    @Test
    public void testFindRemovalSequence3()
    {
        System.out.println("testFindRemovalSequence3() ----------------");
        // Define the 3x3 test matrix
        String[][] matrix =
        {
            {
                "C", "B", "B", "C"
            },
            {
                "C", "A", "A", "B"
            },
            {
                "C", "B", "B", "C"
            }
        };

        printMatrix(matrix);

        // Define the minimum group size
        int minGroupSize = 1;

        // Create the MatrixGroupsRemover instance
        MatrixGroupsRemover<String> groupRemover = new MatrixGroupsRemover<>(matrix, minGroupSize);

        // Find the removal sequence
        List<MatrixGroupsRemover.Group<String>> removalSequence = groupRemover.findRemovalSequence();

        printRemovalSequence(removalSequence);

        // Validate the removal sequence size (2 groups)
        assertEquals("Expected removal sequence size is 5", 5, removalSequence.size());

        // Validate the groups in the sequence
        MatrixGroupsRemover.Group<String> group1 = removalSequence.get(0);
        assertEquals(4, group1.cells().size());
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(0, 0)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(0, 3)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(2, 0)));
        assertTrue(group1.cells().contains(new MatrixGroupsRemover.Cell(2, 3)));
        assertEquals("C", group1.value());
//
//        // Group 2: (2,0), (2,1), (2,2)
//        MatrixGroupsRemover.Group<String> group2 = removalSequence.get(1);
//        assertEquals(3, group2.cells().size());
//        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 0)));
//        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 1)));
//        assertTrue(group2.cells().contains(new MatrixGroupsRemover.Cell(2, 2)));
//        assertEquals("B", group2.value());
    }

    /**
     * Test with no valid groups.
     */
    @Test
    public void testEmptyGrid()
    {
        System.out.println("testEmptyGrid() ----------------");
        // Define an empty 2x2 test matrix (no values = no valid groups)
        String[][] matrix =
        {
            {
                null, null
            },
            {
                null, null
            }
        };

        // Define the minimum group size
        int minGroupSize = 2;

        // Create the MatrixGroupsRemover instance
        MatrixGroupsRemover<String> groupRemover = new MatrixGroupsRemover<>(matrix, minGroupSize);

        // Find the removal sequence
        List<MatrixGroupsRemover.Group<String>> removalSequence = groupRemover.findRemovalSequence();

        // Validate that there are no groups
        assertTrue("Expected no groups in the removal sequence", removalSequence.isEmpty());
    }

    /**
     * Test groups smaller than the minimum group size.
     */
    @Test
    public void testGroupSmallerThanMinSize()
    {
        System.out.println("testGroupSmallerThanMinSize() ----------------");
        // Define a 2x2 matrix where min group size isn't met
        String[][] matrix =
        {
            {
                "A", null
            },
            {
                null, "A"
            }
        };

        // Set min group size to 3
        int minGroupSize = 3;

        // Create the MatrixGroupsRemover instance
        MatrixGroupsRemover<String> groupRemover = new MatrixGroupsRemover<>(matrix, minGroupSize);

        // Find the removal sequence
        List<MatrixGroupsRemover.Group<String>> removalSequence = groupRemover.findRemovalSequence();

        // Validate that there are no groups
        assertTrue("Expected no groups as no group meets the minimum size", removalSequence.isEmpty());
    }

    /**
     * Utility method to pretty-print the result of findRemovalSequence().
     *
     * @param removalSequence The list of groups returned by the findRemovalSequence() method.
     * @param <T>             The type of values in the matrix (e.g., String, Integer, etc.).
     */
    public static <T> void printRemovalSequence(List<MatrixGroupsRemover.Group<T>> removalSequence)
    {
        System.out.println("Removal Sequence:");
        System.out.println("");
        for (int step = 0; step < removalSequence.size(); step++)
        {
            MatrixGroupsRemover.Group<T> group = removalSequence.get(step);

            // Print the step number and group value
            System.out.println("Step: " + (step + 1));
            System.out.println("  Value: " + group.value());
            System.out.println("  Nb cells: " + group.cells().size());

            // Print the coordinates of all cells in the group
            System.out.println("  Coordinates:");
            for (MatrixGroupsRemover.Cell cell : group.cells())
            {
                System.out.println("    (" + cell.row() + "," + cell.col() + ")");
            }

            // Add a separator between steps for readability
            System.out.println(" ");
        }

        if (removalSequence.isEmpty())
        {
            System.out.println("No group found in the matrix.");
        }
    }

    /**
     * Utility method to pretty-print the input matrix.
     *
     * @param matrix The matrix to be printed.
     * @param <T>    The type of values in the matrix (e.g., String, Integer, etc.).
     */
    public static <T> void printMatrix(T[][] matrix)
    {
        System.out.println("Matrix:");

        // Iterate over each row of the matrix
        for (T[] row : matrix)
        {
            // Print each cell in the row
            StringJoiner joiner = new StringJoiner(" . ");
            for (T cell : row)
            {
                if (cell == null)
                {
                    joiner.add("_");
                } else
                {
                    // Print the actual value
                    joiner.add(cell.toString());
                }
            }
            System.out.println(joiner.toString());
        }

        System.out.println(" ");
    }

}
