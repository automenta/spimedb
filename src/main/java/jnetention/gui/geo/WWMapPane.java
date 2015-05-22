package jnetention.gui.geo;

import javafx.embed.swing.SwingNode;
import javafx.scene.layout.BorderPane;

/**
 * Created by me on 5/22/15.
 */
public class WWMapPane extends BorderPane {

    public WWMapPane() {
        super();


        SwingNode sn = new SwingNode();

        sn.setContent(new WWMap());
        setCenter(sn);

    }

}
