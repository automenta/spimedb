package de.jjedele.sbf.hashing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
/**
 * Created by jeff on 16/05/16.
 */
public class Murmur3Test {

    @Test
    public void whenInvoked_returnsCorrectHash() {
        byte[] data = "hello world".getBytes();

        int hash = Murmur3Hash.hash(data);

        assertEquals(hash, 0x5E928F0F);
    }

}
