// Copyright 2012 Jeff Hodges and Jeff Smick. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package spimedb.util.bloom;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.math.IntMath;
import org.jetbrains.annotations.NotNull;

import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

/**
 * Un Bloom - concurrent inverse bloom filter
 * <p>
 * OoaBFilter is used to filter out duplicate elements from a given dataset or stream. It is
 * guaranteed to never return a false positive (that is, it will never say that an item has already
 * been seen by the filter when it has not) but may return a false negative.
 */
public class UnBloomFilter<X> {

    private static final HashFunction HASH_FUNC = Hashing.murmur3_32();

    private final Function<X, byte[]> asBytes;
    private final int sizeMask;
    final AtomicReferenceArray array;
    volatile long hit = 0, total = 0;

    /**
     * Constructs a OoaBFilter with an underlying array of the given size, rounded up to the next
     * power of two.
     * <p>
     * This rounding occurs because the hashing is much faster on an array the size of a power of two.
     *
     * @param size    The size of the underlying array.
     * @param asBytes
     */
    public UnBloomFilter(int size, Function<X, byte[]> asBytes) {
        final int MAX_SIZE = 1 << 30;
        if (size <= 0 || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "array size must be greater than 0 and array size may not be larger than 2**31-1, but will be rounded to larger. was " + size);
        }

        this.asBytes = asBytes;
        int poweredSize = IntMath.pow(2, IntMath.log2(size, RoundingMode.CEILING)); // round to the next largest power of two
        this.array = new AtomicReferenceArray(poweredSize);
        this.sizeMask = poweredSize - 1;

    }

    /**
     * Returns whether the given elemtn has been previously seen by this filter. That is, if a byte
     * buffer with the same bytes as elem has been passed to to this method before.
     * <p>
     * This method may return false when it has seen an element before. This occurs if the element passed in
     * hashes to the same index in the underlying array as another element previously checked. On the
     * flip side, this method will never return true incorrectly.
     *
     * @param element The byte array that may have been previously seen.
     * @return Whether the element is contained in the OoaBFilter.
     */
    public boolean containsOrAdd(@NotNull X element) {
        HashCode code = HASH_FUNC.hashBytes(asBytes.apply(element));

        Object prev = array.getAndSet(code.asInt() & sizeMask, element);

        total++;
        if ((!element.equals(prev))) {
            hit++;
            return false;
        }

        return true;

    }

    /** percent of containsAndAdd requests that returned true */
    public float hitrate(boolean reset) {
        long t = total;
        float r = t != 0 ? ((float) hit) / t : 0;
        if (reset) {
            hit = total = 0;
        }
        return r;
    }

}
