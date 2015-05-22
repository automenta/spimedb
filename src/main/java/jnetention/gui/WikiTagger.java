/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention.gui;

import javafx.application.Platform;
import jnetention.NObject;
import jnetention.Self;

import java.util.Collection;

/**
 *
 * @author me
 */
public class WikiTagger extends WikiBrowser {

    
    
    public WikiTagger(Self core, String startPage) {
        super(core, startPage);
        
        
    }

    
    @Override
    public void onTagClicked(final String id) {
        setBottom(new OperatorTagPane(core, id, this) {
            
            @Override
            public void onFinished(boolean save, final NObject _subject, final Collection<String> tags) {
                if (save && tags!=null) {
                
                    NObject subject;
                    if (_subject == null)
                        subject = core.getMyself();
                    else
                        subject = _subject;
                    
        
                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            //TODO create Nobject

                            NObject n = core.newObject(subject.name() + ": " + id + "=" + tags);

                            n.put("S",subject.id());

                            for (String t : tags) {
                                n.put(t, id);
                            }

                            core.publish(n);


                        }
                    });


                }                    
                    
                
                WikiTagger.this.setBottom(null);
            }
         
        });
    }
    
    
}
