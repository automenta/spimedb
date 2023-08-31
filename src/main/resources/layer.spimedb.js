class SpimeDBLayer extends GeoLayer {
    constructor(name, url) {
        super(name, new WorldWind.RenderableLayer(name));
        this.url = url || ("ws://" + window.location.host);
        this.socket = undefined;
        this.cache = new Map(); //TODO LRU
        this.active = new Map();
    }

    start(f) {
        this.reconnect(f);

        super.start(f);
    }

    pointVisible(latitude, longitude, altitude, f) {
        const p = WorldWind.Vec3.zero();
        f.view.w.globe.computePointFromPosition(latitude, longitude, altitude, p);
        return f.view.w.drawContext.frustumInModelCoordinates.containsPoint(p);
    }

    _update(f) {
        const p = f.view.pos();
        const lat = p.latitude, lon = p.longitude;

        const W = f.view.w.viewport.width;
        const H = f.view.w.viewport.height;

        let latMin, latMax, lonMin, lonMax;
        const points = [];

        this.pickPos(W/2, H/2, points, f); //CENTER

        this.pickPos(0, 0, points, f);
        this.pickPos(W/2, 0, points, f);
        this.pickPos(W, 0, points, f);
        this.pickPos(0, H/2, points, f);
        this.pickPos(0, H, points, f);
        this.pickPos(W/2, H, points, f);
        this.pickPos(W, H/2, points, f);
        this.pickPos(W, H, points, f);

        if (points.length <= 1) {
            //entire hemisphere
             lonMin = lon - 90;
             lonMax = lon + 90;
             latMin = lat - 45;
             latMax = lat + 45;
        } else {
            //extent sector
            lonMin = latMin = Number.POSITIVE_INFINITY;
            lonMax = latMax = Number.NEGATIVE_INFINITY;
            for (const x of points) {
                lonMin = Math.min(lonMin, x.longitude); lonMax = Math.max(lonMax, x.longitude);
                latMin = Math.min(latMin, x.latitude);  latMax = Math.max(latMax, x.latitude);
            }
        }

        // const p = f.view.pos();
        // const lat = p.latitude, lon = p.longitude;
        // const minDX = 0.001;
        // const decay =
        //     //0.5;
        //     0.75;
        //     //0.9;
        // {
        //     let dx = 90;
        //     let latMinNext;
        //     do {
        //         latMinNext = lat - dx;
        //         if (latMinNext < -90) { latMinNext = -90; break; }
        //         if (!this.pointVisible(latMinNext, lon, 0, f)) {
        //             dx *= decay;
        //             if (dx < minDX)
        //                 break;
        //         } else
        //             break;
        //     } while (true);
        //     latMin = latMinNext;
        // }
        // {
        //     let dx = 90;
        //     let latMaxNext;
        //     do {
        //         latMaxNext = lat + dx;
        //         if (latMaxNext < +90) { latMaxNext = +90; break; }
        //         if (!this.pointVisible(latMaxNext, lon, 0, f)) {
        //             dx *= decay;
        //             if (dx < minDX)
        //                 break;
        //         } else
        //             break;
        //     } while (true);
        //     latMax = latMaxNext;
        // }
        // {
        //     let dx = 180;
        //     let lonMinNext;
        //     do {
        //         lonMinNext = lon - dx;
        //         if (!this.pointVisible(lat,  lonMinNext, 0, f)) {
        //             dx *= decay;
        //             if (dx < minDX)
        //                 break;
        //         } else
        //             break;
        //     } while (true);
        //     lonMin = lonMinNext;
        // }
        // {
        //     let dx = 180;
        //     let lonMaxNext;
        //     do {
        //         lonMaxNext = lon + dx;
        //         if (!this.pointVisible(lat, lonMaxNext, 0, f)) {
        //             dx *= decay;
        //             if (dx < minDX)
        //                 break;
        //         } else
        //             break;
        //     } while (true);
        //     lonMax = lonMaxNext;
        // }

        //console.log([latMin, latMax], [lonMin, lonMax]);
        this.getAll(lonMin, lonMax, latMin, latMax, "id");
    }

    pickPos(x, y, points, f) {
        const xy = f.view.w.pickTerrain(new WorldWind.Vec2(x, y));
        if (xy.objects.length > 0) {
            points.push(xy.objects[0].position);
        }
    }

    getAll(lonMin, lonMax, latMin, latMax, output) {
        //TODO LOD filtering
        const m = "{'_':'earth','@':[" + lonMin + "," + lonMax + "," + latMin + "," + latMax + "],output:\"" + output  + "\"}";
        this.socket.send(m);
    }

    reconnect(f) {
        if (this.socket) {
            this.close();
        }

        this.socket = new WebSocket(this.url);
        this.socket.onopen = () => {
            //setTimeout(()=>{
                if (!this.view_change) {
                    this.view_change = f.event.on('view_change', _.debounce(() => {
                        //setTimeout(()=> {
                            this._update(f);
                        //});
                    }, 50));
                }

                this.socket.send("{'_':'tag'}");
                this._update(f);
            //}, 0);
        };
        // socket.addEventListener('close', closeConnection);
        this.socket.onmessage = (x) => {
            const d = JSON.parse(x.data);
            if (d.full)
                this.addAll(d.full, f);
            else if (d.id)
                this.addAllID(d.id, f);
            else if (d.tag)
                this.tag(d.tag, f);
            else
                console.warn('unknown message', d);
        };
        this.socket.onclose = () => {
            this.close();
        };
        this.socket.onerror = (e) => {
            console.error(e);
        };
    }

    tag(tags, f) {
        //console.log(tags);
        const w = $('<div>').css({
            'position': 'relative',
            'max-width': '100%',
            'max-height': '100%',
            'overflow': 'auto',
            'background-color': 'black',
            'color': 'orange'
        });
        // for (const t of tags)
        //     w.append($('<div>').text(JSON.stringify(t)));

        for (const t of tags)
            f.addNode(t);

        //const ww = this.newTableWindow(tags);

        // new DataTable(ww, {
        //     data: data,
        //     paging: false,
        //     columns: [
        //         {
        //             title: 'Enable',
        //             // render: (data, type) =>
        //             //     '<button>' + data + '</button>'
        //         },
        //         { title: 'Tag' },
        //         { title: 'Count' }
        //     ]
        // });

        //winbox("Tags", w.append(ww));
    }

    newTableWindow(tags) {
        const data = _.map(tags, t => {
            const v = {};
            v.enable = true;
            v.label = t.label;
            v.count = t.value;
            return v;
        });

        const ww = $('<table>'); //.css('width', '100%');

        /* https://datatables.net/manual/options */
        var table = new Tabulator(ww[0], {
            width: '100%',
            resizableColumnFit: true,
            layout: "fitColumns",
            data: data,
            columns: [
                {
                    title: "Enable", field: "enable", sorter: "boolean",
                    formatter: 'tickCross', editor: "tickCross"
                },
                { title: "Name", field: "label", sorter: "string" },
                { title: "Count", field: "count", sorter: "number" },
                // {title:"Rating", field:"rating", formatter:"star", hozAlign:"center", width:100},
                // {title:"Favourite Color", field:"col", sorter:"string"},
                // {title:"Date Of Birth", field:"dob", sorter:"date", hozAlign:"center"},
                // {title:"Driver", field:"car", hozAlign:"center", formatter:"tickCross", sorter:"boolean"},
            ],
        });
        table.on("cellEdited", function (cell) {
            //console.log('edited', cell);
            const row = cell._cell.row.data;
            const colField = cell._cell.column.field;

        });
        return ww;
    }

    addAllID(xx, f) {
        this.active.forEach((v, k) => {
            v.visible = false;
        });

        const need = [];

        for (const x of xx) {
            const X = this.cache.get(x);
            if (X) {
                X.visible = true;
                this.active.set(x, X);
            } else {
                need.push(x);
            }
        }
        if (need.length > 0) {
            this.socket.send(
                JSON.stringify({"_":"getAll",id:need}, null)
                //'{"_":"getAll", "id":' + JSON.stringify(need, null) + '}'
            );
        } else {
            this.addAll([], f); //just commit
        }
        //console.log('refresh', needFull.length + '/' + xx.length);
    }

    addAll(d, f) {

        const renderables = [];

        for (const i of d) {
            //INSTANTIATE:

            const tags = i['>'];
            const id = i.I;

            if (tags) {
                if (_.isArray(tags)) {
                    for (const t of tags)
                        f.link(id, t);
                } else {
                    f.link(id, tags);
                }
            }

            // _.forEach(i, (value, key) => {
            //     if (key === 'id' || key === 'name' || key === 'ele' || key.startsWith('addr') || key==='odbl' || key==='layer' || key === 'website' || key.startsWith('source') || key.startsWith('gnis') || key.startsWith('tiger') || key.startsWith('brand:'))
            //         return;
            //
            //     //custom rewrites:
            //     if (key === 'foot') key = 'walkable';
            //
            //     const keyvalue = key + '=' + value;
            //     f.link(i, keyvalue);
            //     f.link(keyvalue, key);
            //     if (value!=='yes' && value!=='no' && !_.isNumber(parseFloat(value)))
            //         f.link(keyvalue, value);
            //     //const xx = f.attn.getNodeAttributes(i);
            //     //xx.instance = x;
            //     //xx.style('display', 'none');
            // });

            //console.log(i);
            // const prev = this.cache.get(i.I);
            // if (prev) {
            //     console.log('rev remvomv');
            //     this.layer.removeRenderable(prev.renderable); //HACK
            // }
            this.cache.set(id, i);
            this.active.set(id, i);
            // const cfg = {};
            // cfg.attributes = new WorldWind.ShapeAttributes(null);
            // cfg.attributes.drawOutline = true;
            // cfg.attributes.outlineColor = new WorldWind.Color(
            //     0.1 * cfg.attributes.interiorColor.red,
            //     0.3 * cfg.attributes.interiorColor.green,
            //     0.7 * cfg.attributes.interiorColor.blue,
            //     1.0);
            // cfg.attributes.outlineWidth = 2.0;

            if (i["g*"]) {
                //POLYGON
                // const pathPositions = _.map(i["g*"], p => new WorldWind.Location(p[0], p[1], 1 /* TODO */));
                // const p = new WorldWind.SurfacePolygon(pathPositions, null);
                const pathPositions = _.map(i["g*"], p => new WorldWind.Position(p[0], p[1], 1 /* TODO */));
                const p = new WorldWind.Polygon(pathPositions, null);
                p.altitudeMode = WorldWind.RELATIVE_TO_GROUND;
                //p.altitudeMode = WorldWind.CLAMP_TO_GROUND;
                //path.attributes.extrude = true;
                p.attributes.drawInterior = true;
                p.attributes.drawOutline = false;
                p.attributes.interiorColor = new WorldWind.Color(1, 0.5, 0, 0.9);
                p.nobject = i; renderables.push(i.renderable = p);
            } else if (i["g-"]) {
                //LINESTRING (path)

                const pathPositions = _.map(i["g-"], p => new WorldWind.Position(p[0], p[1], 1 /* TODO */));
                const p = new WorldWind.Path(pathPositions, null);

                //p.altitudeMode = WorldWind.RELATIVE_TO_GROUND; // The path's altitude stays relative to the terrain's altitude.
                p.altitudeMode = WorldWind.CLAMP_TO_GROUND;

                p.followTerrain = true;
                p.extrude = false; // Make it a curtain.
                p.useSurfaceShapeFor2D = false; // Use a surface shape in 2D mode.

                // const pathPositions = _.map(i["g-"], p => new WorldWind.Location(p[0], p[1], 0 /* TODO */));
                // const p = new WorldWind.SurfacePolyline(pathPositions, null);
                // //path.attributes.extrude = true;
                // p.attributes.drawInterior = false;
                // p.attributes.drawOutline = true;
                p.attributes.outlineWidth = 5;
                p.attributes.outlineColor = new WorldWind.Color(1, 0, 1, 0.75);


                p.nobject = i; renderables.push(i.renderable = p);


            } else if (i["@"]) {
                //point
                const ii = i["@"];
                const pos = new WorldWind.Position(ii[2], ii[1], ii[3]);

                const p = new WorldWind.Placemark(pos, true);
                if (i.N)
                    p.label = i.N;
                // else if (i['>'])
                //     point.label = i[">"];

                p.altitudeMode = WorldWind.RELATIVE_TO_GROUND;


                const placemarkAttributes = new WorldWind.PlacemarkAttributes(null);
                placemarkAttributes.imageScale = 0.5;
                placemarkAttributes.imageColor = new WorldWind.Color(1, 0, 0, 0.5);
                // placemarkAttributes.labelAttributes.offset = new WorldWind.Offset(
                //     WorldWind.OFFSET_FRACTION, 0.5,
                //     WorldWind.OFFSET_FRACTION, 1.5);
                placemarkAttributes.imageSource = WorldWind.configuration.baseUrl + "images/white-dot.png";
                p.attributes = placemarkAttributes;

                p.nobject = i; renderables.push(i.renderable = p);
            } else {
                console.error("unhandled geometry type: ", i);
            }
            i.visible = true;
            this.active.set(id, i);
        }
        this.active.forEach((v,k) => {
            if (v.visible) {
                if (v.hidden) {
                    //reactivate
                    delete v.hidden;
                    renderables.push(v.renderable);
                }
            } else {
                if (this.active.delete(k)) {
                    if (v.renderable) {
                        this.layer.removeRenderable(v.renderable);
                        v.hidden = true;
                    }
                }
            }
        });

        for (const r of renderables) {
            this.layer.addRenderable(r);
        }

        //console.log(this.active.size, 'active', this.cache.size, 'cached');
        f.view.redraw();
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
