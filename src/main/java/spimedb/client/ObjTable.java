package spimedb.client;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;
import spimedb.bag.ChangeBatcher;

/**
 * Created by me on 1/18/17.
 */
public class ObjTable {

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
                return render(n);
            }

            @Override
            public void update(Node[] added, Node[] removed) {
                for (Node h : removed)
                    content.removeChild(h);
                for (Node h : added)
                    content.appendChild(h);
            }

        };
    }

    protected Node render(NObj n) {

        HTMLElement link = doc.createElement("a").withText(n.name());
        HTMLElement d = doc.createElement("div");
        d.appendChild(link);
        return d;
    }

}
