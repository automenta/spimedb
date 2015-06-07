package automenta.netention.gui;

import automenta.vivisect.Video;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.Tab;

import javax.swing.*;


abstract public class SwingPane extends SwingNode {

    static private boolean themed = false;
    static synchronized void ensureThemed() {
        if (!themed) {
            Video.themeInvert();
            themed = true;
        }
    }

    public SwingPane(Tab container) {
        super();

        container.selectedProperty().addListener(new ChangeListener<Boolean>() {
            boolean firstvisible = true;

            @Override
            public void changed(ObservableValue<? extends Boolean> o, Boolean a, Boolean b) {
                if (isVisible() && firstvisible) {
                    firstvisible = false;

                    //construct the component in a new thread, then setContent in the swing thread
                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            ensureThemed();


                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    JComponent c = newComponent();

                                    setContent(c);
                                }
                            });
                        }
                    }).start();

                }
            }
        });


    }


    abstract public JComponent newComponent();
}
