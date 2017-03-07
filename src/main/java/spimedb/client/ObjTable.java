package spimedb.client;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.Node;
import spimedb.util.bag.ChangeBatcher;

import java.util.function.BiConsumer;

/**
 * Created by me on 1/18/17.
 */
public class ObjTable {

    final static String eleID = "_ot";

    private final ChangeBatcher<NObj, Node> updater;
    private final HTMLDocument doc;

    public ObjTable(Client client, HTMLBodyElement target) {

        doc = target.getOwnerDocument();

        HTMLElement content = doc.createElement("div");
        content.setAttribute("class", "sidebar");
        target.appendChild(content);

        updater = new ChangeBatcher<NObj,Node>(100, Node[]::new, client.obj) {

            @Nullable
            @Override
            public Node build(NObj n) {
                return ObjTable.this.build(n);
            }

            @Override
            public void update(Node[] added, Node[] removed) {

                for (Node h : removed)
                    h.delete();

                for (Node h : added) {

                    Node parent = content; //default

                    String pid = ((Element) h).getAttribute("P");
                    if (pid!=null) {

                        parent = content.getOwnerDocument().getElementById(eleID + pid);
                        if (parent == null)
                            parent = content;
                    }

                    parent.appendChild(h);
                }
            }

        };
    }

    public void forEach(BiConsumer<NObj, Node> each) {
        updater.forEach(each);
    }

    protected Node build(NObj n) {

        if (n.isLeaf())
            return null;

        String[] in = n.inh(true);
        String[] out = n.inh(false);
        if (in == null && out == null) {
            //leaf node, hide
            return null;
        }

        HTMLElement d = doc.createElement("div");

        HTMLElement link = doc.createElement("a").withText(n.name());

        if (in!=null)
            d.appendChild( doc.createElement("a").withText("<-- (" + in.length + ") ") ) ;
                //Arrays.toString(in)

        d.appendChild(link);

        if (out!=null)
            d.appendChild( doc.createElement("a").withText(" (" + out.length  + ") -->") );

        d.setAttribute("id", eleID + n.id);
        String[] tags = n.tags();
        if (tags!=null) {
            d.setAttribute("P", tags[0]);
        }

        return d;
    }


}
