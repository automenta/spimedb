package jnetention.gui;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jnetention.Self;
import jnetention.NObject;

/**
 *
 * @author me
 */
abstract public class NetentionJFX extends Application {


    public Self core;
    private NodeControlPane root;

    
    abstract protected Self newCore(Parameters p);
    
    @Override
    public void start(Stage primaryStage) {
        
        core = newCore(getParameters());
        
        root = new NodeControlPane(core);
        
        
        Scene scene = new Scene(root, 350, 650);
        
        primaryStage.setTitle(core.getMyself().id);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
    }
    
   static void popup(Self core, Parent n) {
        Stage st = new Stage();

        st.setScene(new Scene(n));
        st.show();
    }
   static void popup(Self core, Application a) {
        Stage st = new Stage();

        BorderPane root = new BorderPane();
        st.setScene(new Scene(root));
        try {
            a.start(st);
        } catch (Exception ex) {
            Logger.getLogger(NetentionJFX.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        st.show();
    }

    static void popupObjectView(Self core, NObject n) {
        Stage st = new Stage();

        BorderPane root = new BorderPane();
        
        WebView v = new WebView();
        v.getEngine().loadContent(ObjectEditPane.toHTML(n));
        
        root.setCenter(v);

        st.setTitle(n.id);
        st.setScene(new Scene(root));
        st.show();
    }
    
}
