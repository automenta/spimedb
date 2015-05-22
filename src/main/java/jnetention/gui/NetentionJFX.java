package jnetention.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jnetention.NObject;
import jnetention.Self;
import org.controlsfx.control.PropertySheet;
import org.jewelsea.willow.util.ResourceUtil;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author me
 */
abstract public class NetentionJFX extends Application {


    String appName = "Netention";

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

        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage image = null;
            try {
                image = ResourceUtil.getImageAWT("canvas.jpg");
            } catch (IOException e) {
                e.printStackTrace();
            }
            PopupMenu popup = new PopupMenu();
            MenuItem item = new MenuItem("Exit");
            item.setFont(new Font("Monospace", Font.PLAIN, 16));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    // TODO Auto-generated method stub
                    System.exit(0);
                }
            });

            popup.add(item);

            TrayIcon trayIcon = new TrayIcon(image, appName, popup);
            trayIcon.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent event) {

                    if (!primaryStage.isIconified()) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                primaryStage.setIconified(true);
                            }
                        });
                    }
                    else {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                primaryStage.setIconified(false);
                            }
                        });
                    }
                }
            });

            try {
                tray.add(trayIcon);

            } catch (Exception e) {
                System.err.println("Can't add to tray");
            }
        } else {
            System.err.println("Tray unavailable");
        }
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

        PropertySheet propertySheet = new PropertySheet();
        propertySheet.getItems().add(new PropertySheet.Item() {

            @Override
            public Class<?> getType() {
                return NObject.class;
            }

            @Override
            public String getCategory() {
                return "Object";
            }

            @Override
            public String getName() {
                return n.name;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public Object getValue() {
                return n;
            }

            @Override
            public void setValue(Object o) {
                System.out.println("set: " + o);
            }
        });
        root.setTop(propertySheet);

        st.setTitle(n.id);
        st.setScene(new Scene(root));
        st.show();
    }

}
