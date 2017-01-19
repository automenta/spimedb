package de.jjedele.sbf.hashing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jeff on 16/05/16.
 */
public class Murmur3Hash {

    private static final int SEED = 0x0;
    private static final int AVALANCHING_MULTIPLIER1 = 0xcc9e2d51;
    private static final int AVALANCHING_MULTIPLIER2 = 0x1b873593;
    private static final int BLOCK_OFFSET = 0xe6546b64;
    private static final int FINAL_AVALANCHING_MULTIPLIER1 = 0x85ebca6b;
    private static final int FINAL_AVALANCHING_MULTIPLIER2 = 0xc2b2ae35;

    public static int hash(byte[] data) {
        ByteBuffer buffer = ByteBuffer
                .wrap(data)
                .order(ByteOrder.LITTLE_ENDIAN); // produce same output than reference implementation

        // initialisation
        int length = data.length;
        int hash = SEED;

        // full blocks
        int numberOfBlocks = length / 4;
        for (int i = 0; i < numberOfBlocks * 4; i += 4) {
            int block = buffer.getInt(i);

            block *= AVALANCHING_MULTIPLIER1;
            block = Integer.rotateLeft(block, 15);
            block *= AVALANCHING_MULTIPLIER2;

            hash ^= block;
            hash = Integer.rotateLeft(hash, 13);
            hash = 5 * hash + BLOCK_OFFSET;
        }

        // fractioned end-block
        int leftOverLength = length % 4;
        int block = 0;
        switch (leftOverLength) {
            case 3:
                block ^= buffer.get(length - 1) << 16;
            case 2:
                block ^= buffer.get(length - 2) << 8;
            case 1:
                block ^= buffer.get(length - 3);
                block *= AVALANCHING_MULTIPLIER1;
                block = Integer.rotateLeft(block, 15);
                block *= AVALANCHING_MULTIPLIER2;
                hash ^= block;
        }

        // finalisation
        hash ^= length;
        hash ^= hash >>> 16;
        hash *= FINAL_AVALANCHING_MULTIPLIER1;
        hash ^= hash >>> 13;
        hash *= FINAL_AVALANCHING_MULTIPLIER2;
        hash ^= hash >>> 16;

        return hash;
    }

}
