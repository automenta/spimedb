"use strict";

var uiBoundsReactionPeriodMS = 75;


function SEARCHtext(query, withResult) {
    $.get('/search', {q: query}, withResult);
}

function FACETS(query, withResult) {
    $.get('/facet', query, withResult);
}

function QueryPrompt(withSuggestions, withResults) {

    const queryText = $('<input type="text"/>');
    const onQueryTextChanged = _.throttle(() => {

        const qText = queryText.val();
        //$('#query_status').html('Suggesting: ' + qText);

        $.get('/suggest', {q: qText}, withSuggestions);

    }, 100, true, true);

    queryText.submit = function () {
        SEARCHtext(queryText.val(), withResults);
    };

    queryText.on('input', onQueryTextChanged);

    queryText.on('keypress', (e) => {
        if (e.keyCode === 13)
            queryText.submit();
    });

    return queryText;
}

class NObject {

    constructor(x) {

        this.pri = 0.0;

        this.visible = true;
        this.where = false;
        this.when = false;
        this.what = false;

        this.update(x);
    }

    activate(p) {
        this.pri = Math.min(1, Math.max(0, this.pri + p));
    }

    remove() {
        if (this.what) {
            this.what.remove();
            this.what = null;
        }
        if (this.where) {
            this.where.remove();
            this.where = null;
        }
    }

    update(x) {

        const id = x.I;

        const that = this;
        _.each(x, (v, k) => {
            that[k] = v;
        });

        if (this.what) {
            this.what.remove(); //remove existing node
            //this.what = null;
        }

        this.what = ResultNode(x);
        $('#results').append(this.what);

        if (this.where) {
            this.where.remove();
            this.where = null;
        }

        if (map) {
            const bounds = x['@'];
            if (bounds) {

                //Leaflet uses (lat,lon) ordering but SpimeDB uses (lon,lat) ordering

                //when = bounds[0]
                var lon = bounds[1];
                var lat = bounds[2];
                //alt = bounds[3]

                var label = x.N || id || "?";

                var m;

                var linePath, polygon;
                if (linePath = x['g-']) {
                    //TODO f.lineWidth

                    m = L.polyline(linePath, {color: x.color || 'gray', data: x, title: label});
                    //.addTo(map);

                } else if (polygon = x['g*']) {

                    m = L.polygon(polygon, {color: x.polyColor || x.color || 'gray', data: x, title: label});
                    //.addTo(map);

                } else {
                    //default point or bounding rect marker:

                    var mm = {
                        data: x,
                        title: label,
                        stroke: false,
                        fillColor: "#0078ff",
                        fillOpacity: 0.5,
                        weight: 1
                    };

                    if (!(Array.isArray(lat) || Array.isArray(lon))) {
                        mm.zIndexOffset = 100;
                        //f.iconUrl
                        m = L.circleMarker([lat, lon], mm);
                        //.addTo(map);
                    } else {
                        var latMin = lat[0], latMax = lat[1];
                        var lonMin = lon[0], lonMax = lon[1];


                        mm.fillOpacity = 0.3; //TODO decrease this by the bounds area

                        m = L.rectangle([[latMin, lonMin], [latMax, lonMax]], mm);
                        //.addTo(map);
                    }


                }

                if (m) {
                    //m.on('click', clickHandler);
                    //m.on('mouseover', overHandler);
                    //m.on('mouseout', outHandler);

                    this.where = m;
                }
            }
        }
    }

}

function ResultNode(x) {
    const y = DIVclass('list-item result');
    y.data('o', x);

    var tgt = x.I;
    if (x.inh) {
        x.out = x.inh['>'];

        const vin = x.inh['<'];
        if (vin && !(vin.length === 1 && vin[0].length === 0)) { //exclude root tag
            x.in = vin;
            tgt = vin;
        }
    }


    // if (clusters[tgt] === undefined) {
    //     clusters[tgt] = [y];
    // } else {
    //     clusters[tgt].push(y);
    // }


    const header = DIVclass('header');
    if (x.data) {
        header.append(
            //E('a').attr('href', x.data).attr('target', '_').append(
            E('h2').text(x.N)
            //)
        );
    } else {
        header.append(
            E('h2').text(x.N)
        );

    }

    const meta = DIVclass('meta');


    y.append(
        header,
        meta
    );

    if (x.thumbnail) {
        const tt =
                //E('a').attr('class', 'fancybox').attr('rel', 'group').append(
                E('img').attr('src', "/thumbnail?I=" + x.thumbnail)
            //)
        ;
        y.append(
            tt
        );

        //http://fancyapps.com/fancybox/#examples
        //tt.fancybox();
    }


    if (x['_']) {
        y.append(E('p').attr('class', 'textpreview').html(x['_'].replace('\n', '<br/>')));
    }


    if (x.data) {
        y.click(() => {
            focus(x.data);
        });
    }


    return y;

}


function WebSocket() {
    const path = "admin";
    const defaultHostname = window.location.hostname || 'localhost';
    const defaultWSPort = window.location.port || 8080;
    const options = undefined;

    /** creates a websocket connection to a path on the server that hosts the currently visible webpage */
    const ws = new ReconnectingWebSocket(
        'ws://' + window.location.hostname + ':' + window.location.port + '/' + path,
        null /* protocols */,
        options || {
            //Options: //https://github.com/joewalnes/reconnecting-websocket/blob/master/reconnecting-websocket.js#L112
            /*
             // The number of milliseconds to delay before attempting to reconnect.
             reconnectInterval: 1000,
             // The maximum number of milliseconds to delay a reconnection attempt.
             maxReconnectInterval: 30000,
             // The rate of increase of the reconnect delay. Allows reconnect attempts to back off when problems persist.
             reconnectDecay: 1.5,

             // The maximum time in milliseconds to wait for a connection to succeed before closing and retrying.
             timeoutInterval: 2000,
             */
        });

    ws.binaryType = 'arraybuffer';

    ws.onopen = function () {

        add('websocket connect');

    };

    ws.onclose = function () {
        //already disconnected?
        if (!this.opt)
            return;

        add("Websocket disconnect");

        //attempt reconnect?
    };

    ws.onmessage = function (m) {
        recv(msgpack.decode(new Uint8Array(m.data)));
    };

    /*ws.onmessage = function(e) {
     try {
     var c = e.data;
     var d = JSON.parse(c);
     showCode(d);
     } catch (e) {
     showCode(c);
     }
     };*/

    ws.onclose = function (e) {
        add(['Websocket disconnected', e]);
    };

    ws.onerror = function (e) {
        add(["Websocket error", e]);
    };

    return ws;
}


function MAP(target, withResults) {

    var map = L.map(target, {
        continuousWorld: true,
        worldCopyJump: true
    }).setView([51.505, -0.09], 5);

    //http://leaflet-extras.github.io/leaflet-providers/preview/
    setTimeout(() =>
            L.tileLayer(
                'http://{s}.tile.osm.org/{z}/{x}/{y}.png'
                //'http://{s}.tile.opentopomap.org/{z}/{x}/{y}.png'
                , {
                    //attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                }).addTo(map),
        0);


    //                map.on('click', function(e) {
    //
    //                    const center = e.latlng;
    //                    //var myRenderer = L.svg({ padding: 0.5 }); //TODO use hexagon polygon renderer
    //
    //
    //                    var m = L.circle( center, {
    //                        radius: 1000 //meters
    //                        //renderer: myRenderer
    //                    } );
    //
    //                    m.addTo(map);
    //                } );

    var seeing = undefined;

    const errFunc = function (errV, errM) {
        console.error('err', errV, errM);
    };

    function diff(curBounds, prevBounds) {
        if (curBounds.intersects(prevBounds)) {
            //console.log('diff', curBounds, prevBounds);
            //TODO http://stackoverflow.com/questions/25068538/intersection-and-difference-of-two-rectangles/25068722#25068722
            //return L.bounds([[p1y,p1x],[p2y,p2x]]);
            return curBounds;
        } else {
            return curBounds; //no commonality to subtract
        }
    }

    function rectBounds(b) {
        return {
            "x1": b.getWest(),
            "x2": b.getEast(),
            "y1": b.getSouth(),
            "y2": b.getNorth(),
            update: function () {
                $.get('/earth', {r:
                    this.x1 + '_' +
                    this.y1 + '_' +
                    this.x2 + '_' +
                    this.y2
                }, withResults);
            }
        };
    }

    var updateBounds = _.debounce(function (e) {

        var curBounds = map.getBounds();

        var b = seeing ? /*difference*/diff(curBounds, seeing) : curBounds;

        seeing = curBounds;

        var r = rectBounds(b);
        r.update();

        /*var radiusMeters =
         Math.max(b.getEast()-b.getWest(), b.getNorth()-b.getSouth()) / 2.0;*/




        //var center = b.getCenter();
        //var lon = center.lng;
        //var lat = center.lat;
        //app.spaceOn(circleBounds/*Compact*/(lon, lat, radiusMeters, 4),

        //me.spaceOn(rectBounds(b), focus, errFunc);

        /*.done(focus) //function (r) {
         //console.log(r);

         //updateGeoJSONFeatures(r);
         //})
         .fail(function (v, m) {
         console.log('err', v, m);
         });*/

        //}, uiBoundsReactionPeriodMS );
    }, uiBoundsReactionPeriodMS, {
        'leading': true,
        'trailing': false
    });



    map.on('viewreset', nextUpdateBounds);
    map.on('moveend', nextUpdateBounds);
    map.on('resize', nextUpdateBounds);


    function nextUpdateBounds() {
        setTimeout(updateBounds, 0);
    }

    updateBounds();

    return map;
}
