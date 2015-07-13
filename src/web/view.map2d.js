"use strict";

/** fixed point conversion of a floating point number, encoded in base64 */
function b36float(x, decimals) {
    //(parseInt(x.toPrecision(decimals)) * (Math.pow(10,decimals))).toString(36)
    parseInt( (x*Math.pow(10,decimals)).toFixed(decimals) ).toString(36)
}
function circleBoundsCompact(lat, lon, radMeters, decimals) {

    return {
        "y":  b36float(lat,decimals),
        "x":  b36float(lon,decimals),
        "r":  b36float(radMeters,decimals),
        "p": "e" //earth
    }
}
function circleBounds(lat, lon, radMeters, decimals) {

    return {
        "y":  lat.toFixed(decimals),
        "x":  lon.toFixed(decimals),
        "r":  radMeters.toFixed(decimals),
        "p": "e", //earth
        toURL: function() {
            return "/space?R=c&x=" + // "Region" = "circular"
                this.x + "&y=" + this.y +
                "&r=" + this.r; //"radius"
        }
    }
}



class Map2DView extends NView {

    constructor() {
        super("Map (2D)", "road");
    }

    start(v, app, cb) {

        var uiBoundsReactionPeriodMS = 100;

        var testIcon = L.icon({
            iconUrl: 'icon/unknown.png',
            iconSize: [32, 32],
            iconAnchor: [16, 16]
        });

        var map = this.map = L.map(v[0], {
            // This map option disables world wrapping. by default, it is false.
            continuousWorld: true,
            worldCopyJump: true
        });
        //map.setView([51.505, -0.09], 13);
        map.setView([-35.98909,-54.2566178],9);
        //map.setView([0,0], 7);

        L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);


        var geojsonMarkerOptions = {
            radius: 8,
            fillColor: "#ff7800",
            color: "#000",
            weight: 1,
            opacity: 1,
            fillOpacity: 0.8
        };


        /*
         //not used yet:
         var geojsonoption = {
         onEachFeature: function onEachFeature(feature, layer) {
         // does this feature have a property named popupContent?
         if (feature.properties && feature.properties.popupContent) {
         layer.bindPopup(feature.properties.popupContent);
         }
         },
         pointToLayer: function (feature, latlng) {
         return L.circleMarker(latlng, geojsonMarkerOptions);
         }
         };
         */

        var features = new Map();

        function addFeatures(ff) {
            var i;
            for (i = 0; i < ff.length; i++) {

                var f = ff[i];
                var id = f.I;

                if (!f.S)
                    return;

                if (features.get(id))
                    return;

                //console.log('showing: ' , f.N, f.I, f.S);

                var m = L.circleMarker( [ f.S[0], f.S[1] ],
                    geojsonMarkerOptions);
                m.addTo(map);

                features.set(id, m);

            }

            console.log(features.keys(), " features displayed");

            //f.type = "Feature";
            //f.geometry = f.geom;
            //
            ///*
            // if (f.styleUrl) {
            // if (!styles[f.styleUrl]) {
            // console.log(f.id + ' missing style: ' + f.styleUrl);
            //
            // //TODO batch these
            // //loadStyles([f.styleUrl]);
            // }
            // }
            // */
            //
            //
            //if (f.geom.type === "point")
            //    f.geometry.type = "Point";
            //if (f.geom.type === "linestring")
            //    f.geometry.type = "LineString";
            //if (f.geom.type === "polygon")
            //    f.geometry.type = "Polygon";
            //
            //
            //if (!f.style)
            //    f.style = { };
            //
            //if (!f.properties) {
            //    f.properties = { };
            //
            //    if (f.name) {
            //        f.properties.name = f.name;
            //    }
            //    if (f.description) {
            //        f.properties.description = f.description;
            //    }
            //}
            //
            //if (f.name && f.description)
            //    f.popupTemplate = "<strong>{name}</strong><br/>{description}";
            //else if (f.name)
            //    f.popupTemplate = "<strong>{name}</strong>";
            //else if (f.description)
            //    f.popupTemplate = "{description}";
            //
            ////https://github.com/albburtsev/Leaflet.geojsonCSS
            ////L.geoJson.css(json).addTo(map);
            //
            //var layer = L.geoJson.css(f);
            //layer.setOpacity = function(o) {
            //    for (var l in this._layers) {
            //        var ll = this._layers[l];
            //        if (ll.setOpacity)
            //            ll.setOpacity(o);
            //    }
            //};
            //
            //layer.data = f;
            //features[id] = layer;
            //
            //layer.addTo(map);
        }

        function removeFeature(i) {

            //features[i].removeFrom(map);
        }

        function newTag(layerID) {
            var t = {
                id: layerID,
                name: undefined,
                features: { },
                inh: { },
                opacity: 0
            };

            tag.setNode(layerID, t);

            t.setOpacity = function(o) {

                var prevOpacity = t.opacity || 0;

                this.opacity = o;

                var add=false, remove=false;

                if /*((prevOpacity === 0) && */(this.opacity > 0.01) {
                    //add to map
                    add = true;
                }
                if /*((prevOpacity > 0) && */(this.opacity < 0.01) {
                    //remove from map
                    remove = true;
                }

                for (var f in this.features) {
                    var x = features[f];
                    if (!x) {
                        console.error('missing feature: ', f);
                        continue;
                    }

                    if (x.setOpacity) {
                        x.setOpacity(o);
                    }

                    if (add) {
                        if (x.addTo)
                            x.addTo(map);
                    }
                    else if (remove) {
                        if (x.remove)
                            x.remove();
                    }

                }

            };

            t.update = function() {
                if (this.inh) {
                    //TODO remove existing edges

                    _.each(this.inh, function(strength, superTag) {
                        var n = tag.node(superTag);
                        if (!n) {
                            newTag(superTag);
                        }

                        tag.setEdge(superTag, layerID);
                    });
                }
            };


            return t;
        }

        function updateGeoJSONFeatures(r) {

            /*
             for (var i in features) {
             if (!r[i]) {
             removeFeature(i);
             }
             }*/

            var unknownLayers = {};
            var updatedLayers =  { };

            for (var id in r) {
                var f = r[id];

                addFeatures(id, f);

                var p = f.path || [];

                //TODO cache known paths

                var parent = null;
                var layerNode = null;

                var maxOpacity = 0.0;

                for (var i = 0; i < p.length; i++) {
                    var layerID = p[i];
                    layerNode = tag.node(layerID);


                    if (layerNode) {
                    }
                    else {
                        layerNode = newTag(layerID);
                        layerNode.inh[parent] = 1.0;
                        layerNode.update();

                        unknownLayers[layerID] = true;

                    }
                    updatedLayers[layerID] = true;

                    if (layerNode.opacity > maxOpacity)
                        maxOpacity = layerNode.opacity;


                    parent = layerID;

                    layerNode.features[id] = f;
                }

                features[id].setOpacity(maxOpacity);

            }


            function updateChangedLayers() {
                renderLayerView(_.keys(updatedLayers), false);
            }

            if (unknownLayers.length > 0)
                loadTags(unknownLayers, updateChangedLayers);
            else
                updateChangedLayers();
        }



        /** focus callback, used whenever an object that may be displayed is received asynchronously */
        var focus = function(x) {
            if (x) {
                setTimeout(function () {
                    addFeatures(x)
                }, 0);
            }
        };

        var agentIcons = { };



        var updateBounds = _.throttle(function (e) {
            var b = map.getBounds();

            var radiusMeters = b.getSouthEast().distanceTo(b.getNorthWest()) / 2.0;
            var lon = b.getCenter().lng;
            var lat = b.getCenter().lat;



            app.spaceOn(circleBounds/*Compact*/(lat, lon, radiusMeters, 4),

                focus, function(errV, errM) {
                    console.error('err', errV, errM);
                }

            );

                /*.done(focus) //function (r) {
                    //console.log(r);

                    //updateGeoJSONFeatures(r);
                //})
                .fail(function (v, m) {
                    console.log('err', v, m);
                });*/

        }, uiBoundsReactionPeriodMS );



        map.on('viewreset', nextUpdateBounds);
        map.on('moveend', nextUpdateBounds);
        map.on('resize', nextUpdateBounds);

        function nextUpdateBounds() {
            setTimeout(updateBounds, 0);
        }

        updateBounds();

        if (cb) cb();

        app.on(['focus','change'], this.listener = function(c) {

            //TODO remove removed icons

            for (var c in app.focus) {
                var d = app.data(c);
                for (var a in d) {
                    //console.log(d[a]);

                    if (d[a].where) {

                        if (!agentIcons[a]) {
                            var marker = L.marker([0, 0], {
                                icon: testIcon
                            }).addTo(map);
                            agentIcons[a] = marker;
                        }


                        //console.log(a, d[a].coordinates[0][0 /* first poly corner */]);
                        agentIcons[a].setLatLng({
                            lat: d[a].where[0],
                            lng: d[a].where[1]
                        });
                    }
                }
            }
        });
    }

    stop() {

        app.off(['focus','change'], this.listener);

        this.map.remove();
        this.map = null;

    }

}
