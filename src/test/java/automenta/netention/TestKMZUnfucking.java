package automenta.netention;


import automenta.netention.geo.ImportKML;


public class TestKMZUnfucking {

    public static void main(String[] args) throws Exception {


        ImportKML kml = new ImportKML(null,
                TestKMZUnfucking.class.getSimpleName().toString());

        kml.task("file:///tmp/kmz/OneFolder.kmz").run();



    }
}
