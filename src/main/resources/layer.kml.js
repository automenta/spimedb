class KMLLayer extends GeoLayer {
    constructor(name, url) {
        super(name, new WorldWind.RenderableLayer(name));
        this.url = url;
    }

    start(f) {
        var kmlFilePromise = new WorldWind.KmlFile(this.url/*, [new WorldWind.KmlTreeVisibility('treeControls', wwd)]*/);
        kmlFilePromise.then((kmlFile)=>  {
            // this.layer.currentTimeInterval = [
            //     new Date("Mon Aug 09 2015 12:10:10 GMT+0200 (Střední Evropa (letní čas))").valueOf(),
            //     new Date("Mon Aug 11 2015 12:10:10 GMT+0200 (Střední Evropa (letní čas))").valueOf()
            // ];
            this.layer.addRenderable(kmlFile);
        });
        super.start(f);
    }

    stop(focus) {
        super.stop(focus);
        this.layer.removeAllRenderables();
    }

}