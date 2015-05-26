package jnetention.gui;

import javafx.embed.swing.SwingNode;
import javafx.scene.layout.BorderPane;
import jnetention.Self;
import nars.gui.NARControlPanel;


public class ChatLogPane extends BorderPane {

    public ChatLogPane(Self self) {
        super();


        SwingNode sn = new SwingNode();

        sn.setContent(new NARControlPanel(self.nar));
        setCenter(sn);

    }

}
