package de.jjedele.sbf.hashing;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Created by jeff on 16/05/16.
 */
public class FNV1aTest {

    @Test
    public void whenInvoked_returnsCorrectHash() {
        byte[] data = "hello world".getBytes();

        int hash = FNV1aHash.hash(data);

        assertEquals(hash, 0xd58b3fa7);
    }

}
