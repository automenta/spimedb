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

        var uiBoundsReactionPeriodMS = 10;

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
        map.setView([35.98909,-84.2566178],13);
        //map.setView([0,0], 7);

        L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);



        /** focus callback, used whenever an object that may be displayed is received asynchronously */
        var focus = function(x) {
            console.log('TODO display', x);
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
