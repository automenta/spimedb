package de.jjedele.sbf;

import de.jjedele.sbf.hashing.HashProvider;

import java.util.Random;

/**
 * Created by jeff on 14/05/16.
 */
public class StableBloomFilter<E> implements CountingBloomFilter<E> {

    private final HashProvider<E> hashProvider;
    private final byte[] cells;
    private final int numberOfCells;
    private final int numberOfHashes;
    private final float unlearnRate;
    private final Random rng = new Random();

    public StableBloomFilter(int numberOfCells,
                             int numberOfHashes,
                             float unlearnRate,
                             HashProvider<E> hashProvider) {
        this.numberOfCells = numberOfCells;
        this.numberOfHashes = numberOfHashes;
        this.cells = new byte[numberOfCells];
        this.unlearnRate = unlearnRate;
        this.hashProvider = hashProvider;
    }

    /** if the element isnt contained, add it. return the value from the contains test.*/
    public boolean addIfMissing(E element) {
        int[] hash = hash(element);
        boolean c = contains(hash);
        if (!c) {
            add(hash);
        }
        return c;
    }

    public void add(E element) {
        add(hash(element));
    }

    public boolean contains(E element) {
        return contains(hash(element));
    }

    public void add(int[] indices) {
        for (int i = 0; i < numberOfHashes; i++) {
            increment(indices[i]);
        }

        unlearn();
    }

    public boolean contains(int[] indices) {
        boolean mightContain = true;
        for (int i = 0; i < numberOfHashes; i++) {
            mightContain &= cells[indices[i]] > 0;
        }

        return mightContain;
    }


    public void remove(E element) {
        int[] indices = hash(element);

        remove(indices);
    }

    public void remove(int[] indices) {
        for (int i = 0; i < numberOfHashes; i++) {
            decrement(indices[i]);
        }
    }

    private void unlearn() {
        int unlearnedCells = Math.round(numberOfCells * unlearnRate);
        for (int i = 0; i < unlearnedCells; i++) {
            int index = rng.nextInt(numberOfCells);
            decrement(index);
        }
    }

    public int[] hash(E element) {
        int[] hashes = new int[numberOfHashes];

        long h1 = hashProvider.hash1(element);
        long h2 = hashProvider.hash2(element);
        for (int i = 0; i < numberOfHashes; i++) {
            hashes[i] = Math.abs((int) ((h1 + i * h2) % numberOfCells));
        }

        return hashes;
    }

    private void decrement(int idx) {
        if (cells[idx] > 0) {
            cells[idx] -= 1;
        }
    }

    private void increment(int idx) {
        if (cells[idx] < Byte.MAX_VALUE) {
            cells[idx] += 1;
        }
    }

}
