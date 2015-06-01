package jnetention.gui;

import automenta.vivisect.Video;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.Tab;

import javax.swing.*;


abstract public class SwingPane extends SwingNode {

    static {
        Video.themeInvert();
    }

    public SwingPane(Tab container) {
        super();

        container.selectedProperty().addListener(new ChangeListener<Boolean>() {
            boolean firstvisible = true;

            @Override
            public void changed(ObservableValue<? extends Boolean> o, Boolean a, Boolean b) {
                if (isVisible() && firstvisible) {
                    SwingUtilities.invokeLater(new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       setContent(newComponent());
                                                   }
                                               }
                    );
                    firstvisible = false;
                }
            }
        });


    }

    abstract public JComponent newComponent();
}
