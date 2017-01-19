package de.jjedele.sbf;

/**
 * In contrast to a {@link BloomFilter}, elements can also be removed from {@link CountingBloomFilter}s.
 */
public interface CountingBloomFilter<E> extends BloomFilter<E> {

    /**
     * Remove an element from the filter.
     * @param element Must have been added to the filter before. If not, the method wont fail but unpredictable
     *                side-effects might occur.
     */
    void remove(E element);

}
