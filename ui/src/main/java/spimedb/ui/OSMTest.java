package spimedb.ui;

import jcog.User;
import jcog.exe.Exe;
import org.xml.sax.SAXException;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.meta.OmniBox;
import spacegraph.space2d.widget.OsmSurface;
import spacegraph.util.geo.osm.Osm;

import java.io.IOException;

import static jcog.Util.round;

public enum OSMTest {
	;

	enum OSMGraphTest {
		;

		public static void main(String[] args) throws SAXException, IOException {

            IRL i = new IRL(User.the());

            Osm o = new Osm().load("/home/me/m/test.osm.bz2");

            SpaceGraph.window(new OsmSurface(i.index).go(o).widget(), 800, 800);

            i.index.stats().print(System.out);
        }
    }

    public static void main(String[] args) {
//        i.load(-80.65, 28.58, -80.60, 28.63);

//        SpaceGraphPhys3D sg = new SpaceGraphPhys3D(new OsmSpace(i.osm).volume());
//        sg.io.show(800, 800);

//        sg.addAt(new SubOrtho(WidgetTest.widgetDemo()).posWindow(0, 0, 0.3f, 1f));
        SpaceGraph.window(osmTest(), 800, 800);
    }

    public static Surface osmTest() {
        IRL i = new IRL(User.the());


        OsmSurface o = new OsmSurface(i.index);

        Surface s =
            new Splitting(
                o.widget()
            ,0.1f,
                //new TextEdit(16)
                    new OmniBox(new OmniBox.JShellModel())
            )
        ;

        go(o, i,
                //-80.65f, 28.58f
                -73.9712488f, 40.7830603f
                //-73.9712488f, 40.7830603f
                //-73.993449f, 40.751029f
                , 0.001f*16, 0.001f*12);

        return s;
        //i.index.stats().print(System.out);
    }

    @Deprecated
    public static void go(OsmSurface o, IRL i, float lon, float lat, float lonRange, float latRange) {
        float x0 = lon - lonRange/2, x1 = lon + lonRange/2;
        float y0 = lat - latRange/2, y1 = lat + latRange/2;

        float cellResolution = 0.002f;
        x0 = round(x0 - cellResolution/2, cellResolution); x1 = round(x1 + cellResolution/2, cellResolution);
        y0 = round(y0 - cellResolution/2, cellResolution); y1 = round(y1 + cellResolution/2, cellResolution);


        o.projection.center(lon, lat);

        for (float X = x0; X <= x1; X+=cellResolution) {
            float finalX = X;
            for (float Y = y0; Y <= y1; Y += cellResolution) {
                //System.out.println(X + " " + Y);

                float finalY =Y;
                Exe.run(() ->
                    o.load(i.load(
                            finalX, finalY,
                        finalX + cellResolution, finalY + cellResolution
                )));
            }
        }
    }
}