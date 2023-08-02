class SpimeDBLayer extends GeoLayer {
    constructor(name, url) {
        super(name, new WorldWind.RenderableLayer(name));
        this.url = url;
        this.socket = undefined;
        this.active = new Map();
    }

    start(f) {
        // Set up the common placemark attributes.
        const placemarkAttributes = new WorldWind.PlacemarkAttributes(null);
        placemarkAttributes.imageScale = 1;
        placemarkAttributes.imageColor = WorldWind.Color.WHITE;
        placemarkAttributes.labelAttributes.offset = new WorldWind.Offset(
            WorldWind.OFFSET_FRACTION, 0.5,
            WorldWind.OFFSET_FRACTION, 1.5);
        placemarkAttributes.imageSource = WorldWind.configuration.baseUrl + "images/white-dot.png";

        const shapeConfigurationCallback = function (geometry, properties) {
            const cfg = {};
            //console.log(geometry, properties);
            if (geometry.isPointType() || geometry.isMultiPointType()) {
                cfg.attributes = new WorldWind.PlacemarkAttributes(placemarkAttributes);

                if (properties && (properties.name || properties.Name || properties.NAME)) {
                    cfg.name = properties.name || properties.Name || properties.NAME;
                }

                //AirNow HACK
                if (properties.SiteName)
                    cfg.name = properties.SiteName + "\n" + properties.PM_AQI_LABEL;

                if (properties && properties.POP_MAX) {
                    const population = properties.POP_MAX;
                    cfg.attributes.imageScale = 0.01 * Math.log(population);
                }

            } else if (geometry.isLineStringType() || geometry.isMultiLineStringType()) {
                cfg.attributes = new WorldWind.ShapeAttributes(null);
                cfg.attributes.drawOutline = true;
                cfg.attributes.outlineColor = new WorldWind.Color(
                    0.1 * cfg.attributes.interiorColor.red,
                    0.3 * cfg.attributes.interiorColor.green,
                    0.7 * cfg.attributes.interiorColor.blue,
                    1.0);
                cfg.attributes.outlineWidth = 2.0;
            } else if (geometry.isPolygonType() || geometry.isMultiPolygonType()) {
                cfg.attributes = new WorldWind.ShapeAttributes(null);

                // Fill the polygon with a random pastel color.
                cfg.attributes.interiorColor = new WorldWind.Color(
                    0.375 + 0.5 * Math.random(),
                    0.375 + 0.5 * Math.random(),
                    0.375 + 0.5 * Math.random(),
                    0.5);
                // Paint the outline in a darker variant of the interior color.
                cfg.attributes.outlineColor = new WorldWind.Color(
                    0.5 * cfg.attributes.interiorColor.red,
                    0.5 * cfg.attributes.interiorColor.green,
                    0.5 * cfg.attributes.interiorColor.blue,
                    1.0);
            }

            return cfg;
        };


        this.reconnect(f);


        super.start(f);
    }

    _update(f) {
        const p = f.view.pos();
        const alt = p.altitude;
        const r = Math.min(0.1, p.altitude/100.0); //TODO fix
        const latMin = p.latitude - r;
        const latMax = p.latitude + r;
        const lonMin = p.longitude - r;
        const lonMax = p.longitude + r;
        //TODO LOD filtering
        const m = "{'_':'earth','@':[" + lonMin + "," + lonMax + "," + latMin + "," + latMax+"]}";
        this.socket.send(m);
    }

    reconnect(f) {
        if (this.socket) {
            this.close();
        }

        this.socket = new WebSocket(this.url);
        this.socket.onopen = () => {
            setTimeout(()=>{
                if (!this.view_change) {
                    this.view_change = f.event.on('view_change', _.throttle(() => {
                        this._update(f);
                    }, 100));
                }

                //this.socket.send("{'_':'index'}");
                this._update(f);
            }, 0);
        };
        // socket.addEventListener('close', closeConnection);
        this.socket.onmessage = (x) => {
            const d = JSON.parse(x.data);
            this.addAll(d);
        };
        this.socket.onclose = (e) => {
            this.close();
        };
        this.socket.onerror = (e) => {
            console.error(e);
        };
    }


    addAll(d) {
        this.active.forEach((v, k) => {
            v.unseen = true;
        });
        _.forEach(d, i => {
            const e = this.active.get(i.I);
            if (e) {
                //exists TODO check for difference?
                e.unseen = false;
            } else {
                //console.log(i);
                this.active.set(i.I, i);
                // const cfg = {};
                // cfg.attributes = new WorldWind.ShapeAttributes(null);
                // cfg.attributes.drawOutline = true;
                // cfg.attributes.outlineColor = new WorldWind.Color(
                //     0.1 * cfg.attributes.interiorColor.red,
                //     0.3 * cfg.attributes.interiorColor.green,
                //     0.7 * cfg.attributes.interiorColor.blue,
                //     1.0);
                // cfg.attributes.outlineWidth = 2.0;

                if (i["@"]) {
                    //point
                    const ii = i["@"];
                    const pos = new WorldWind.Position(ii[2], ii[1], ii[3]);
                    const point = new WorldWind.Placemark(pos, true, null);
                    if (i.N)
                        point.label = i.N;
                    else
                        point.label = i[">"];

                    point.altitudeMode = WorldWind.RELATIVE_TO_GROUND;
                    this.layer.addRenderable(i.renderable = point);
                } else if (i["g-"]) {
                    //path
                    const pathPositions = _.map(i["g-"], p => new WorldWind.Position(p[0], p[1], 100 /* TODO */));

                    // Create the path.
                    var path = new WorldWind.Path(pathPositions, null);
                    path.altitudeMode = WorldWind.RELATIVE_TO_GROUND; // The path's altitude stays relative to the terrain's altitude.
                    path.followTerrain = true;
                    //path.extrude = true; // Make it a curtain.
                    //path.useSurfaceShapeFor2D = true; // Use a surface shape in 2D mode.
                    this.layer.addRenderable(i.renderable = path);
                } else {
                    console.error("unhandled geometry type: ", i);
                }
            }
        });
        this.active.forEach((v, k) => {
            if (v.unseen) {
                this.active.delete(k);
                if (v.renderable) {
                    this.layer.removeRenderable(v.renderable);
                    delete v.renderable;
                }
            }
        });
        console.log(this.active);
    }

    close() {
        this.socket.close();
        delete this.socket;
    }

// changeConnection(event) {
    //     // open the connection if it's closed, or close it if open:
    //     if (socket.readyState === WebSocket.CLOSED) {
    //         openSocket(serverURL);
    //     } else {
    //         socket.close();
    //     }
    // }


    // closeConnection() {
    //     this.socket.close();
    // }


    // function sendMessage() {
    //     //if the socket's open, send a message:
    //     if (socket.readyState === WebSocket.OPEN) {
    //         socket.send(outgoingText.value);
    //     }
    // }

    stop(focus) {
        super.stop(focus);
        this.layer.removeAllRenderables();
    }

}
