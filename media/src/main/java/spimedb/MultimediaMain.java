package spimedb;

import spimedb.media.Multimedia;

/**
 * entrypoint for Multimedia enablement
 */
public class MultimediaMain {

    public static void main(String[] args) throws Exception {
        Spime.start(new Class[] { Multimedia.class }, args);
    }
}
