package jnetention.gui;

import automenta.vivisect.Video;
import javafx.embed.swing.SwingNode;

import javax.swing.*;


public class SwingPane extends SwingNode {

    static {
        Video.themeInvert();
    }

    public SwingPane(JComponent swingComponent) {
        super();

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                setContent(swingComponent);
            }
        });

    }

}
