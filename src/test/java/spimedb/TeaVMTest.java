package spimedb;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.junit.Test;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import spimedb.bag.BudgetMerge;
import spimedb.bag.PriBag;
import spimedb.util.js.JavaToJavascript;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 1/13/17.
 */
public class TeaVMTest {


    public static class Client {

        final static PriBag p = new PriBag(1, BudgetMerge.add, new UnifiedMap());

        public static void main(String[] args) {
            HTMLDocument document = HTMLDocument.current();
            HTMLElement div = document.createElement("div");
            div.appendChild(document.createTextNode("TeaVM generated element"));
            document.getBody().appendChild(div);

            p.put("x", 0.5f);
        }
    }

    @Test
    public void test1() {

        String s = JavaToJavascript.build().compileMain(Client.class).toString();
        assertTrue(s.length() > 1000);

    }


}
