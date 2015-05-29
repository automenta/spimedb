/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention.gui;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import jnetention.NObject;
import jnetention.Self;
import jnetention.gui.geo.WWMapPane;
import jnetention.run.WebBrowser;
import nars.gui.NARControlPanel;
import nars.gui.output.ConceptLogPanel;

/**
 *
 * @author me
 */
public class NodeControlPane extends BorderPane {
    private final Self self;

    public NodeControlPane(Self core) {
        super();
        
        this.self = core;
        
       
        TabPane tab = new TabPane();
        tab.getTabs().add(newChatLogTab());
        tab.getTabs().add(newIndexTab());
        tab.getTabs().add(newWikiTab());
        tab.getTabs().add(newSpacetimeTab());
        //tab.getTabs().add(newSpaceTab());
        tab.getTabs().add(newTimeTab());
        tab.getTabs().add(newOptionsTab());
        tab.getTabs().add(newReasonerTab());

        tab.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tab.autosize();
        
        setCenter(tab);
        
        FlowPane menu = new FlowPane();
        menu.getChildren().add(newAddButton());
        menu.getChildren().add(newBrowserButton());
        
        setBottom(menu);
    }
    
    public Button newBrowserButton() {
        Button b = new Button(/**/);

        GlyphsDude.setIcon(b, FontAwesomeIcon.LINK);
        b.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                NetentionJFX.popup(self, new WebBrowser(self));
            }

        });
        return b;
        
    }
    
    public Button newAddButton() {
        Button b = new Button("+");
        b.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                popupObjectEdit(self, new NObject.HashNObject(""));
            }

        });
        return b;
    }
    
    
    public Tab newWikiTab() {
        final Tab t = new Tab(/*"Wiki"*/);
        GlyphsDude.setIcon(t, FontAwesomeIcon.TAGS);
        
        t.selectedProperty().addListener(new ChangeListener<Boolean>() {
            boolean firstvisible = true;

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean b, Boolean t1) {
                if (firstvisible) {                
                    t.setContent(new WikiTagger(self, "Self"));
                    firstvisible = false;
                }
            }
            
        });
        
        return t;
    }    
//
//    public Tab newSpaceTab() {
//        Tab t = new Tab(/*"Space"*/);
//        GlyphsDude.setIcon(t, FontAwesomeIcon.MAP_MARKER);
//
//
//        SwingNode swingMap = new SwingNode();
//        t.selectedProperty().addListener(new ChangeListener<Boolean>() {
//            boolean firstvisible = true;
//            @Override
//            public void changed(ObservableValue<? extends Boolean> o, Boolean a, Boolean b) {
//                if (swingMap.isVisible() && firstvisible) {
//                    swingMap.setContent(new SwingMap( new GeoPosition(40.00, -80.00)));
//                    t.setContent(swingMap);
//                    firstvisible = false;
//                }
//            }
//        });
//        return t;
//    }
    public Tab newTimeTab() {
        Tab tab = new Tab(/*"Time"*/);
        GlyphsDude.setIcon(tab, FontAwesomeIcon.CLOCK_ALT);
        tab.setContent(new TimePanel());
                
        return tab;
    }    
    public Tab newSpacetimeTab() {
        Tab t = new Tab(/*"Spacetime"*/);
        GlyphsDude.setIcon(t, FontAwesomeIcon.CUBES);
        t.setContent(new WWMapPane());
        return t;
    }    
    public Tab newOptionsTab() {
        Tab t = new Tab(/*"Options"*/);
        GlyphsDude.setIcon(t, FontAwesomeIcon.COGS);
        
        Accordion a =new Accordion();
        
        a.getPanes().addAll(new TitledPane("Identity", newIdentityPanel()), new TitledPane("Network", newNetworkPanel()), /*new TitledPane("Logic", newLogicPanel()),*/ new TitledPane("Database", newDatabasePanel()));
        
        for (TitledPane tp : a.getPanes())
            tp.setAnimated(false);
        
        t.setContent(a);
        return t;
    }    
    public Tab newIndexTab() {
        Tab t = new Tab(/*"Index"*/);
        GlyphsDude.setIcon(t, FontAwesomeIcon.LIST);
        t.setContent(new IndexTreePane(self, new TaggerPane.TagReceiver() {

            @Override
            public void onTagSelected(String s) {
                //NetentionJFX.popupObjectView(core, core.data.get(s));
            }
        }));
        return t;
    }
    public Tab newReasonerTab() {
        Tab t = new Tab();
        GlyphsDude.setIcon(t, FontAwesomeIcon.EDIT);
        t.setContent(new SwingPane(new NARControlPanel(self.nar)));
        return t;
    }
    public Tab newChatLogTab() {
        Tab t = new Tab();
        GlyphsDude.setIcon(t, FontAwesomeIcon.EDIT);

        TextField iff = new TextField();

        SplitPane sp = new SplitPane(new SwingPane(new ConceptLogPanel(self.nar)), iff);

        t.setContent(sp);
        return t;
    }

    protected Node newNetworkPanel() {
        Pane p = new Pane();
        
        TextArea ta = new TextArea();
//        if (core.net!=null) {
//            ta.setText(core.net.getClass().getSimpleName());
//        }
//        else {
//            ta.setText("Offline");
//        }
        p.getChildren().add(ta);        
        
        return p;
    }
    protected Node newLogicPanel() {        

        
        
        final SwingNode s = new SwingNode();
        

        ScrollPane x = new ScrollPane(s);
        
        
        BorderPane flow = new BorderPane(x);
        flow.setPadding(new Insets(5, 5, 5, 5));
        flow.autosize();
        
        
        return flow;
    }
    protected Node newIdentityPanel() {
        Pane p = new Pane();
        
        if (self.getMyself()!=null)
            p.getChildren().add(new ObjectCard(self.getMyself()));
                    
        return p;
    }
    protected Node newDatabasePanel() {
        Pane p = new Pane();
        
        TextArea ta = new TextArea();
        ta.setText(self.db.toString() + "\n" + self.db.toString() );
        p.getChildren().add(ta);
        
        //core.data.sizeLong()
        
        return p;
    }
    
    public static void popupObjectEdit(Self core, NObject n) {
        Stage st = new Stage();

        st.setTitle(n.id());
        st.setScene(new Scene(new ObjectEditPane(core, n), 600, 400));
        
        st.show();
    }
    
}
