package jnetention.run;

import static javafx.application.Application.launch;
import javafx.stage.Stage;
import jnetention.Self;
import jnetention.gui.NetentionJFX;

/**
 * Runs in-memory only (no disk saving)
 */
public class RunMemory extends NetentionJFX {

    @Override
    protected Self newCore(Parameters p) {
        return new Self();
    }

    @Override
    public void start(Stage primaryStage) {
        super.start(primaryStage); //To change body of generated methods, choose Tools | Templates.
        
        //NodeControlPane.popupObjectEdit(core, core.newObject(""));
    }
    
    
    
    public static void main(String[] args) {
        launch(args);
    }
    
}
