package automenta.netention;


import automenta.netention.geo.SpimeBase;
import automenta.netention.geo.ImportKML;


public class TestKMZUnfucking {

    public static void main(String[] args) throws Exception {

        SpimeBase es = SpimeBase.memory();

        new ImportKML(es).url("main",
                "file:///tmp/kml/EOL-Field-Projects-CV3D.kmz"
                //"file:///tmp/kml/GVPWorldVolcanoes-List.kmz"
        ).run();


        es.forEach( x -> System.out.println(x) );

    }
}
