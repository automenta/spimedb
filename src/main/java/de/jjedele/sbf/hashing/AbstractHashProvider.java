package de.jjedele.sbf.hashing;

/**
 * Created by jeff on 16/05/16.
 */
public abstract class AbstractHashProvider<E> implements HashProvider<E> {

    @Override
    public int hash1(E element) {
        return Murmur3Hash.hash(asBytes(element));
    }

    @Override
    public int hash2(E element) {
        return FNV1aHash.hash(asBytes(element));
    }

    public abstract byte[] asBytes(E element);

}
