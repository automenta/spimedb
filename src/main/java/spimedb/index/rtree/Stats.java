package spimedb.index.rtree;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.PrintStream;
import java.util.Arrays;

/**
 * Created by jcovert on 5/20/15.
 */
public class Stats {

    private final static int MAX_DEPTH = 64;
    private final int[] entriesAtDepth = new int[MAX_DEPTH];
    private final int[] branchesAtDepth = new int[MAX_DEPTH];
    private final int[] leavesAtDepth = new int[MAX_DEPTH];
    private RTree.Split type;
    private int maxFill;
    private int minFill;
    private int maxDepth;
    private int branchCount;
    private int leafCount;
    private int entryCount;

    public void print(PrintStream out) {
        out.println("[" + type + "] m=" + minFill + " M=" + maxFill);
        out.println("   Branches (" + branchCount + " total)");
        out.print("      ");
        for (int i = 0; i <= maxDepth; i++) {
            out.print(i + ": " + branchesAtDepth[i] + "  ");
        }
        out.println("\n   Leaves (" + leafCount + " total)");
        out.print("      ");
        for (int i = 0; i <= maxDepth; i++) {
            out.print(i + ": " + leavesAtDepth[i] + "  ");
        }
        out.println("\n   Entries (" + entryCount + " total)");
        out.print("      ");
        for (int i = 0; i <= maxDepth; i++) {
            out.print(i + ": " + entriesAtDepth[i] + "  ");
        }
        out.printf("\n   Leaf Fill Percentage: %.2f%%\n", getLeafFillPercentage());
        out.printf("   Entries per Leaf: %.2f\n", getEntriesPerLeaf());
        out.println("   Max Depth: " + maxDepth);
        out.println();
    }

    public float getEntriesPerLeaf() {
        return ((entryCount * 1.0f) / leafCount);
    }

    private float getLeafFillPercentage() {
        return (getEntriesPerLeaf() * 100) / maxFill;
    }

    public RTree.Split getType() {
        return type;
    }

    public void setType(RTree.Split type) {
        this.type = type;
    }

    public void setMaxFill(int maxFill) {
        this.maxFill = maxFill;
    }

    public void setMinFill(int minFill) {
        this.minFill = minFill;
    }

    public int getBranchCount() {
        return branchCount;
    }

    public int getLeafCount() {
        return leafCount;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void countEntriesAtDepth(int entries, int depth) {
        entryCount += entries;
        entriesAtDepth[depth] += entries;
    }

    public void countLeafAtDepth(int depth) {
        leafCount++;
        leavesAtDepth[depth]++;
    }

    public void countBranchAtDepth(int depth) {
        branchCount++;
        branchesAtDepth[depth]++;
    }

    @Override
    public String toString() {
        return "Stats{" +
                "type=" + type +
                ", maxFill=" + maxFill +
                ", minFill=" + minFill +
                ", maxDepth=" + maxDepth +
                ", branchCount=" + branchCount +
                ", leafCount=" + leafCount +
                ", entryCount=" + entryCount +
                ", entriesAtDepth=" + Arrays.toString(entriesAtDepth) +
                ", branchesAtDepth=" + Arrays.toString(branchesAtDepth) +
                ", leavesAtDepth=" + Arrays.toString(leavesAtDepth) +
                '}';
    }
}