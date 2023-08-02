class ShapefileLayer extends GeoLayer {
    constructor(name, url) {
        super(name, new WorldWind.RenderableLayer(name));
        this.url = url;
    }

    start(f) {
        const shapeFileOutlineLoader = (attributes, x) => {
            const cfg = {};
            cfg.name = attributes.values.name || attributes.values.Name || attributes.values.NAME;

            if (x.isPointType()) { // Configure point-based features (cities, in this example)
                cfg.name = attributes.values.name || attributes.values.Name || attributes.values.NAME;
                cfg.attributes = new WorldWind.PlacemarkAttributes(null);
                cfg.attributes.imageSource = WorldWind.configuration.baseUrl + "images/white-dot.png";
                cfg.attributes.imageScale = 0.25;
                cfg.attributes.imageColor = WorldWind.Color.WHITE;
                cfg.attributes.labelAttributes.offset = new WorldWind.Offset(
                    WorldWind.OFFSET_FRACTION, 0.5,
                    WorldWind.OFFSET_FRACTION, 1.5);
            } else if (x.isPolygonType()) { // Configure polygon-based features (countries, in this example).
                cfg.attributes = new WorldWind.ShapeAttributes(null);
                cfg.attributes.drawInterior = false;
                cfg.attributes.outlineWidth = 10;
                cfg.attributes.outlineColor = new WorldWind.Color(
                    0.375 + 0.5 * Math.random(),
                    0.375 + 0.5 * Math.random(),
                    0.375 + 0.5 * Math.random(),
                    1.0);
            } else {
                //TODO other shape types
            }
            
            return cfg;
        };

        new WorldWind.Shapefile(this.url).load(null, shapeFileOutlineLoader, this.layer);

        super.start(f);
    }

    stop(focus) {
        super.stop(focus);
        this.layer.removeAllRenderables();
    }

}

