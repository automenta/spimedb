package de.jjedele.sbf.hashing;

/**
 * Created by jeff on 16/05/16.
 */
public class FNV1aHash {

    private static final int OFFSET_BASIS = 0x811C9DC5;
    private static final int PRIME = 0x01000193;

    public static int hash(byte[] data) {
        int hash = OFFSET_BASIS;

        for (byte octet : data) {
            hash ^= octet;
            hash *= PRIME;
        }

        return hash;
    }

}
