/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention.gui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;
import jnetention.NObject;

import java.util.Collection;

/**
 * Combobox for selecting subjects (ex: users)
 * @author me
 */
public class SubjectSelect extends ComboBox<NObject> {

    static final Tooltip whoTooltip = new Tooltip("Who?");

    public SubjectSelect(final Collection<NObject> subjects) {
        super();
        
        setConverter(new StringConverter<NObject>() {
            
            @Override public String toString(NObject object) {
                if (object == null) return "?";
                
                if (object.name()!=null)
                    return object.name();
                return object.id();
            }

            @Override public NObject fromString(String string) {
                NObject first = null;
                for (NObject n : subjects) {
                    if (first == null) first = n;
                    if (n.name().equals(string))
                        return n;
                }
                return first;
            }
        });
        
        getItems().addAll(subjects);        
        
        setTooltip(whoTooltip);
        
        setEditable(true);
        

        
    }
    
    
}
