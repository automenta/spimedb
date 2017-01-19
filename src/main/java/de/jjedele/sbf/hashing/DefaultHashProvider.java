package de.jjedele.sbf.hashing;

import java.nio.ByteBuffer;

/**
 * Created by jeff on 14/05/16.
 */
public class DefaultHashProvider<E> extends AbstractHashProvider<E> {

    @Override
    public byte[] asBytes(Object element) {
        ByteBuffer buffer = ByteBuffer
                .allocate(4)
                .putInt(element.hashCode());
        return buffer.array();
    }

}
