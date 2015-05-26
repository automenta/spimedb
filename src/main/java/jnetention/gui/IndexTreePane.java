/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention.gui;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import jnetention.NObject;
import jnetention.NTag;
import jnetention.Self;
import jnetention.Self.NetworkUpdateEvent;
import jnetention.Self.SaveEvent;
import jnetention.gui.TaggerPane.TagReceiver;
import nars.util.event.Reaction;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author me
 */
public class IndexTreePane extends BorderPane implements Reaction<Class>  {
    private final TreeView<NObject> tv;
    //http://docs.oracle.com/javafx/2/ui_controls/tree-view.htm
    private final Self self;
    private final TreeItem root;
    private final TagReceiver tagger;
    private final SubjectSelect subjectSelect;

    public IndexTreePane(Self core, TagReceiver tagger) {
        super();
        
        this.self = core;
        this.tagger = tagger;
        
        root = new TreeItem();        
                
        tv = new TreeView(root);
        
        tv.setCellFactory(new Callback<TreeView<NObject>,TreeCell<NObject>>(){
            @Override
            public TreeCell<NObject> call(TreeView<NObject> p) {

                return new TextFieldTreeCellImpl();
            }
        });
        
        tv.setShowRoot(false);
        tv.setEditable(false);

        tv.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override public void handle(MouseEvent mouseEvent)    {            
                if(mouseEvent.getClickCount() == 2) {
                    TreeItem<NObject> selected = tv.getSelectionModel().getSelectedItem();
                    if (selected!=null) {
                        NObject item = selected.getValue();
                        onDoubleClick(item);
                    }
                }
            }
        });
        
        visibleProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (isVisible()) {
                    addHandlers();
                    update();
                }
                else {
                    //core.off(SaveEvent.class, IndexTreePane.this);
                    //core.off(NetworkUpdateEvent.class, IndexTreePane.this);
                }
            }
        });
    

         
        
        
        FlowPane menup = new FlowPane();
        subjectSelect = new SubjectSelect(core.getUsers());
        subjectSelect.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override public void changed(ObservableValue ov, Object t, Object t1) {
                update();
            }
        });
        
        menup.getChildren().add(subjectSelect);
        setTop(menup);
        
        
        ScrollPane sp = new ScrollPane(tv);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);        
        setCenter(sp);
        
        addHandlers();
        update();
        
    }

    protected void addHandlers() {
        self.on(SaveEvent.class, IndexTreePane.this);
        self.on(NetworkUpdateEvent.class, IndexTreePane.this);
    }


    protected void update() {
                
        List<TreeItem> c = new ArrayList();
        for (NObject t : self.getTagRoots()) {
            c.add(newTagTree((NTag) t));
        }
        root.getChildren().clear();
        root.getChildren().addAll(c);
    }
    
    protected TreeItem newTagTree(final NTag t) {
        NObject subjectFilter = subjectSelect.getSelectionModel().getSelectedItem();

        TreeItem<NObject> i = new TreeItem(t);
                              
        //add instances of the tag        
        Iterators.addAll(i.getChildren(), Iterators.transform(self.tagged(t.id, subjectFilter != null ? subjectFilter.get("A").toString() : null), new Function<NObject, TreeItem<NObject>>() {
            @Override
            public TreeItem apply(final NObject f) {
                return newInstanceItem(f);
            }
        }));
        
        return i;        
    }
    
    protected TreeItem<NObject> newInstanceItem(NObject f) {
        TreeItem<NObject> t = new TreeItem<NObject>(f);        
        return t;
    }
    
    protected void onDoubleClick(NObject item) {
        if (tagger!=null) {
            tagger.onTagSelected(item.id());
        }        
    }

    @Override
    public void event(Class aClass, Object... objects) {
        Platform.runLater(this::update);
    }


    private final class TextFieldTreeCellImpl extends TreeCell<NObject> {
 
 
        public TextFieldTreeCellImpl() {
            super();
        }
 
 
        @Override
        public void updateItem(NObject item, boolean empty) {
            super.updateItem(item, empty);

            setItem(item);
            
            if (item!=null) {
                setText(null);
                
                BorderPane g = new BorderPane();
                                
                Label tl = new Label(item.toString());
                Hyperlink tb = new Hyperlink("[+]");
                tb.setOnMouseClicked(e -> {
                    NodeControlPane.popupObjectEdit(self, item);
                });
                g.setTop(new FlowPane(tl, tb));
                
                BorderPane content = new BorderPane();
                /*
                tb.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {                        
                        if (g.getBottom()==null) {

                            Slider slider = new Slider();
                            slider.setMin(0);
                            slider.setMax(100);
                            slider.setValue(40);
                            slider.setShowTickLabels(true);
                            slider.setShowTickMarks(true);
                            slider.setMajorTickUnit(50);
                            slider.setMinorTickCount(5);
                            slider.setBlockIncrement(10);

                            content.setCenter(slider);
                            g.setBottom(content);
                        }
                        else {
                            g.setBottom(null);
                        }
                    }
                });
                */
                
                setGraphic(g);
            }
            

        }
 
 
        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }
     
}
