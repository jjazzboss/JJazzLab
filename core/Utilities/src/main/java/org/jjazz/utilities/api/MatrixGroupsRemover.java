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

import com.google.common.base.Preconditions;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Determine the minimum sequence of "group" removals needed to clear a matrix.
 * <p>
 * A "group" is a set of cells that share the same non-null value and can span multiple rows or columns (adjacent or not) with these alignment rules:<br>
 * - If a group spans multiple rows, it must occupy the same columns in each row.<br>
 * - If a group spans multiple columns, it must occupy the same rows in each column.<br>
 * <p>
 * Input matrix example:</p>
 * <pre>
 * A B A
 * A A A
 * B B B
 *
 * Groups of min 2 cells: (row, col)
 * 1. (0,0), (0,2), (1,0), (1,2)  - 'A'
 * 2. (1,0), (1,1), (1,2)         - 'A' the middle line
 * 3. (2,0), (2,1), (2,2)         - 'B' the last line
 * 4. (0,1), (2,1)                - 'B'
 * </pre>
 * <p>
 * This class uses a greedy algorithm to find the minimum number of removals required to clear the matrix.
 *
 * @param <T> The type of the values in the matrix (e.g., Character, Integer, etc.).
 */
public class MatrixGroupsRemover<T>
{

    private final T[][] grid;
    private final int minGroupSize;
    private static final Logger LOGGER = Logger.getLogger(MatrixGroupsRemover.class.getSimpleName());

    /**
     *
     * @param grid         The matrix containing the values to process (not modified)
     * @param minGroupSize The minimum size of a group (&gt;=1)
     */
    public MatrixGroupsRemover(T[][] grid, int minGroupSize)
    {
        Objects.requireNonNull(grid);
        Preconditions.checkArgument(minGroupSize >= 1);
        this.grid = Utilities.clone2Darray(grid);
        this.minGroupSize = minGroupSize;
    }

    /**
     * Finds the minimum sequence of groups needed to remove all cells in the matrix.
     *
     * @return A list of Groups representing the removal sequence.
     */
    public List<Group<T>> findRemovalSequence()
    {
        List<Group<T>> sequence = new ArrayList<>();

        while (true)
        {
            // Identify all valid cell groups in the current state of the matrix
            List<Group<T>> groups = findAllGroups();

            // If no groups are left, the matrix is cleared
            if (groups.isEmpty())
            {
                break;
            }

            // Select the largest group (greedy algorithm)
            Group<T> largestGroup = groups.get(0);

            // Add the largest group to the removal sequence
            sequence.add(largestGroup);

            // Remove the group from the matrix
            removeGroup(largestGroup);
        }

        if (minGroupSize == 1)
        {
            // Add the remaining one-cell groups
            int rows = grid.length;
            int cols = grid[0].length;
            for (int r = 0; r < rows; r++)
            {
                for (int c = 0; c < cols; c++)
                {
                    T value = grid[r][c];
                    if (value != null)
                    {
                        Group g = new Group(value, List.of(new Cell(r, c)));
                        sequence.add(g);
                    }
                }
            }
        }

        return sequence;
    }

    /**
     * Utility method to pretty-print the result of findRemovalSequence().
     *
     * @param removalSequence The list of groups returned by the findRemovalSequence() method.
     * @param <T>             The type of values in the matrix (e.g., String, Integer, etc.).
     */
    public static <T> void printRemovalSequence(List<Group<T>> removalSequence)
    {
        LOGGER.info("Removal Sequence:");

        for (int step = 0; step < removalSequence.size(); step++)
        {
            Group<T> group = removalSequence.get(step);

            // Print the step number and group value
            LOGGER.log(Level.INFO, "Step {0}:", step + 1);
            LOGGER.log(Level.INFO, "  Value: {0}", group.value());
            LOGGER.log(Level.INFO, "  Nb cells: {0}", group.cells().size());

            // Print the coordinates of all cells in the group
            LOGGER.info("  Coordinates:");
            for (Cell cell : group.cells())
            {
                LOGGER.log(Level.INFO, "    ({0},{1})", new Object[]
                {
                    cell.row(), cell.col()
                });
            }

            // Add a separator between steps for readability
            LOGGER.info(" ");
        }

        if (removalSequence.isEmpty())
        {
            LOGGER.info("No group found in the matrix.");
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
        LOGGER.info("Matrix:");

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
            LOGGER.info(joiner.toString());
        }

        LOGGER.info(" ");
    }

    // =======================================================================================================
    // Private methods
    // =======================================================================================================    
    /**
     * Finds all valid cell groups in the current state of the matrix.
     *
     * @return A list of all valid groups in the matrix.
     */
    private List<Group<T>> findAllGroups()
    {
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        Set<String> uniqueGroupHashes = new HashSet<>();
        List<Group<T>> allGroups = new ArrayList<>();

        // Scan the entire matrix for valid groups
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                if (!visited[r][c] && grid[r][c] != null)
                {
                    // Find row-based and column-based groups starting from this cell
                    Group<T> rowGroup = findRowGroup(r, c, visited);
                    Group<T> colGroup = findColGroup(r, c, visited);

                    // Add valid groups if they are unique
                    if (rowGroup.cells().size() >= Math.max(2, minGroupSize))
                    {
                        if (uniqueGroupHashes.add(rowGroup.generateHash()))
                        {
                            allGroups.add(rowGroup);
                        }
                    }

                    if (colGroup.cells().size() >= Math.max(2, minGroupSize))
                    {
                        if (uniqueGroupHashes.add(colGroup.generateHash()))
                        {
                            allGroups.add(colGroup);
                        }
                    }
                }
            }
        }

        // Sort groups by size in descending order to maximize removal progress
        allGroups.sort(Comparator.comparingInt((Group<T> g) -> g.cells().size()).reversed());
        return allGroups;
    }

    private Group<T> findRowGroup(int row, int col, boolean[][] visited)
    {
        T value = grid[row][col];
        List<Cell> cells = new ArrayList<>();
        int cols = grid[0].length;

        // Determine columns matching the pattern in the current row
        boolean[] columnMatch = new boolean[cols];
        for (int c = 0; c < cols; c++)
        {
            columnMatch[c] = Objects.equals(grid[row][c], value);
        }

        // Check all rows for matching column patterns
        for (int r = 0; r < grid.length; r++)
        {
            boolean matches = true;
            for (int c = 0; c < cols; c++)
            {
                if (columnMatch[c] && !Objects.equals(grid[r][c], value))
                {
                    matches = false;
                    break;
                }
            }

            // Add all cells in the matching row
            if (matches)
            {
                for (int c = 0; c < cols; c++)
                {
                    if (columnMatch[c])
                    {
                        cells.add(new Cell(r, c));
                        visited[r][c] = true;
                    }
                }
            }
        }

        return new Group<>(value, cells);
    }

    private Group<T> findColGroup(int row, int col, boolean[][] visited)
    {
        T value = grid[row][col];
        List<Cell> cells = new ArrayList<>();
        int rows = grid.length;

        // Determine rows matching the pattern in the current column
        boolean[] rowMatch = new boolean[rows];
        for (int r = 0; r < rows; r++)
        {
            rowMatch[r] = Objects.equals(grid[r][col], value);
        }

        // Check all columns for matching row patterns
        for (int c = 0; c < grid[0].length; c++)
        {
            boolean matches = true;
            for (int r = 0; r < rows; r++)
            {
                if (rowMatch[r] && !Objects.equals(grid[r][c], value))
                {
                    matches = false;
                    break;
                }
            }

            // Add all cells in the matching column
            if (matches)
            {
                for (int r = 0; r < rows; r++)
                {
                    if (rowMatch[r])
                    {
                        cells.add(new Cell(r, c));
                        visited[r][c] = true;
                    }
                }
            }
        }

        return new Group<>(value, cells);
    }

    private void removeGroup(Group<T> group)
    {
        for (Cell cell : group.cells())
        {
            grid[cell.row()][cell.col()] = null;
        }
    }

  

    // =========================================================================================
    // Inner classes
    // =========================================================================================

    /**
     * Represents a group of cells with the same value.
     *
     * @param value The shared value of all cells in the group.
     * @param cells A list of Cells in the group.
     * @param <T>   The type of the value (e.g., Character, Integer, etc.).
     */
    public record Group<T>(T value, List<Cell> cells)
            {

        public String generateHash()
        {
            cells.sort(Comparator.comparingInt((Cell c) -> c.row()).thenComparingInt(Cell::col));
            StringBuilder sb = new StringBuilder();
            for (Cell cell : cells)
            {
                sb.append(cell.row()).append(",").append(cell.col()).append(";");
            }
            return sb.toString();
        }
    }

    /**
     * Represents the coordinates of a single cell in the matrix.
     * <p>
     */
    public record Cell(int row, int col)
            {

    }
}
