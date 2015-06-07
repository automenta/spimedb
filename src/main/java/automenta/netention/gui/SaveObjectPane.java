

package automenta.netention.gui;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import automenta.netention.Self;
import automenta.netention.NObject;
import automenta.netention.Scope;

/**
 *
 * @author me
 */
abstract public class SaveObjectPane extends BorderPane {

    public SaveObjectPane(Self core) {
                
        SubjectSelect s = new SubjectSelect(core.getUsers());
        s.setTooltip(new Tooltip("Re:"));
        s.getSelectionModel().select(core.getMyself());
        

        ScopeSelect scope = new ScopeSelect();
        
        Button cancelButton = GlyphsDude.createIconButton(FontAwesomeIcon.UNDO);
        cancelButton.setTooltip(new Tooltip("Cancel"));
        
        Button saveButton = GlyphsDude.createIconButton(FontAwesomeIcon.SAVE);
        saveButton.setTooltip(new Tooltip("Save"));
        
        saveButton.setDefaultButton(true);
        saveButton.setOnAction(new EventHandler() {
            @Override public void handle(javafx.event.Event event) {                         
                onSave(scope.getScope(), s.getSelectionModel().getSelectedItem()); 
            }
        });
        cancelButton.setOnAction(new EventHandler() {
            @Override public void handle(javafx.event.Event event) { onCancel(); }
        });
        
        setLeft(s);
        
        FlowPane fp = new FlowPane(scope, cancelButton, saveButton);
        fp.setAlignment(Pos.BOTTOM_RIGHT);
        
        setCenter(fp);
        
    }
    
    public abstract void onCancel();
    public abstract void onSave(Scope scope, NObject subject);
}
