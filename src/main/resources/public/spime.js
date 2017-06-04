"use strict";

var uiBoundsReactionPeriodMS = 25;

var MEMORY_SIZE = 512;
var ACTIVATION_RATE = 0.5;





class MMEE extends LFUGraph {

    constructor() {
        super(MEMORY_SIZE);
    }

}

const ME = new MMEE(); //new Map();

var clusters = {};
var facets = undefined; //HACK
var timeline = undefined;
//    var IF = new RuleReactor({}, true);
//    IF.when = IF.createRule;
//
//    IF.when("show", 1, {n: NObject},
//        (n) => {
//            return n.visible;
//        },
//        (n) => {
//            console.log('show', n.I);
//        }
//    );
//    IF.when("hide", 0, {n: NObject},
//        (n) => {
//            return !n.visible;
//        },
//        (n) => {
//            console.log('hide', n.I);
//
//            if (n.what) {
//                n.what.remove();
//                n.what = null;
//            }
//
//            if (n.where) {
//                n.where.remove();
//                n.where = null;
//            }
//
//            IF.retract(n);
//        }
//    );
//
//    IF.trace(0);
//    IF.run(Infinity, true, function () {
//        console.log(JSON.stringify(p));
//    });


function ADD(y) {
    var id = y.I;
    if (!y || !id)
        throw new Error("missing ID");

    var x = ME.get(id);
    if (x) {
        x.update(y);
        return x;
    } else {

        y = new NObject(y);

        ME.set(id, y);

        return y;
    }
}


function REMOVE(id) {

    const r = ME.get(id);
    if (!r)
        return;

    r.remove();

    ME.delete(id);
}

function CLEAR() {
    ME.forEach((value, key) => {
        REMOVE(key);
    });
    ME.clear();
    clusters = {};
}

//TODO see this active eviction is compatible with LFU
function FORGET(decay, maxItems) {
    /*if (!ME.size() > maxItems) {
     //dont have to sort
     }*/
    const n = ME.size;

    const filteredIterator = ME.values();
    const nn = filteredIterator.next;
    filteredIterator.next = () => {
        const v = nn.call(filteredIterator);

        v.pri *= decay;

        return v;
    };

    const a = Array.from(filteredIterator);


    a.sort((x, y) => {

        if (x === y) return 0;

        const xp = x.pri;
        const yp = y.pri;
        if (xp > yp) return -1;
        else return +1;
    });


    const toRemove = n - maxItems;

    for (var i = 0; i < toRemove; i++) {
        const z = a.pop();
        REMOVE(z.I);
    }
}

const facetButtonBuilder = (v) => {

    const id = v[0]
        .replace(/_/g, ' ')
        .replace(/\-/g, ' ')
    ; //HACK


    return new NIcon(ME.computeIfAbsent(v[0], (v)=>{
        return new NObject({I: id});
    })).scale(v[1]).ele;

};


function loadFacets(result) {
    facets.html('');


    addToGrid(result, facetButtonBuilder, facets);


}


function LOAD(ss, activationRate) {


    //setTimeout(() => {

    const results = ss[0]; //first part: search results
    const facets = ss[1]; //second part: facets



    const yy = _.map(results, x => {
        if (!x.I) return;
        const score = x['*'];
        const y = ADD(x);
        if (y) {
            y.activate(score * ACTIVATION_RATE * activationRate);
        }
        return y;
    });

    FORGET(0.9, MEMORY_SIZE);

    loadFacets(facets);

    return yy;

//            _.each(clusters, (c, k) => {
//
//                if (c.length < 2)
//                    return; //ignore clusters of length < 2
//
//                const start = c[0];
//
//                const d = DIVclass('list-item result');
//                $(start).before(d);
//                c.forEach(cc => {
//                    /* {
//
//                     d = cc;
//                     } else {
//                     children.push(cc);
//                     }*/
//                    cc.detach();
//                    cc.addClass('sub');
//                    if (cc.data('o').I !== k) //the created root entry for this cluster, ignore for now
//                        d.append(cc);
//                });
//
//                //HACK if there was only 1 child, just pop it back to top-level subsuming any parents
//                var dc = d.children();
//                if (dc.length == 1) {
//                    $(dc[0]).removeClass('sub');
//                    d.replaceWith(dc[0]);
//                }
//
//
//            });


    //}, 0);

}

function ALL(query, withResult) {
    $.get('/all', {q: query}, withResult);
}

function FACETS(query, withResult) {
    $.get('/facet', query, withResult);
}

function QueryPrompt(withSuggestions, withResults) {


    const queryText = $('<input type="text"/>');
    const onQueryTextChanged = _.throttle(() => {

        const qText = queryText.val();
        if (qText.length > 0) {
            //$('#query_status').html('Suggesting: ' + qText);

            $.get('/suggest', {q: qText}, withSuggestions);
        } else {
            withSuggestions('[]' /* HACK */);
        }

    }, 100, true, true);

    queryText.submit = function () {
        ALL(queryText.val(), withResults);
    };

    queryText.on('input', onQueryTextChanged);

    queryText.on('keypress', (e) => {
        if (e.keyCode === 13)
            queryText.submit();
    });

    return queryText;
}

//const mapClustering = new L.MarkerClusterGroup().addTo(map);

var geojsonMarkerOptions = {
    radius: 8,
    fillColor: "#ff7800",
    color: "#000",
    weight: 1,
    opacity: 1,
    fillOpacity: 0.8
};

function clickHandler(e) {
    var obj = e.target.options.data;
    if (obj.what) {
        obj.what[0].scrollIntoView();
    }

    /*var x = JSON.stringify(obj, null, 4);

     var w = newWindow($('<pre>').text(x));
     $.getJSON('/obj/' + obj.I, function(c) {
     var desc = c['^']['_'];
     if (desc)
     w.html(desc);
     else
     w.html(JSON.stringify(c, null, 4));
     } );*/
}

function overHandler(e) {
    var o = e.target.options;


    /*if (o.ttRemove) {
     clearTimeout(o.ttRemove);
     o.tt.fadeIn();
     }
     else */
    {
        if (o.tt)
            return; //already shown


        $('.map2d_status').remove();

        //setTimeout(function () {
        var tt = $('<div>').addClass('map2d_status');

        tt.html($('<a>').text(o.title).click(function () {
        }));

        tt.css('left', e.containerPoint.x);
        tt.css('top', e.containerPoint.y);

        tt.appendTo($('#map'));

        o.tt = tt;
        //}, 0);
    }
}

function outHandler(e) {

    var o = e.target.options;
    if (o.tt) {
        o.tt.remove();
        delete o.tt;
        /*
         var delay = 1500; //ms
         var fadeTime = 500; //ms
         o.ttRemove = setTimeout(function() {
         o.tt.fadeOut(fadeTime);
         delete o.tt;
         delete o.ttRemove;
         }, delay);
         */
    }
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
            const bounds = x['@']; if (bounds) {

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

        if (timeline) {
            const bounds = x['@']; if (bounds) {
                const when = bounds[0];
                if (typeof(when)==='number' || typeof(when)==='array')
                    console.log(x, when);
            }
        }
    }

}

/** nobject viewer/editor interface model */
class NView {

    constructor(n) {
        this.n = n;
        this.ele = DIVclass('box');
        const b = this.ele;

        const content = D();
        content.html(JSON.stringify(n));

        var font = 1.0;

        function updateFont() {
            b.attr('style', 'font-size:' + (parseInt(font * 100.0)) + '%');
        }

        const controls = DIVclass('controls').append(
            SPANclass('label').append(n.N || n.I),

            SPANclass('button').text('v').click(()=>{
                font*= 0.75; updateFont(); //font shrink
            }),

            SPANclass('button').text('^').click(()=>{
                font*= 1.333; updateFont(); //font grow
            }),

            // SPANclass('button').text('~').click(()=>{
            //     newWindow(b);
            // }),

            SPANclass('button').text('x').click(()=>b.hide())
        );

        b.append( controls );

        if (content)
            b.append( content );

    }

    showPopup() {
        return newWindow(this.ele);
    }
}

/** label-sized icon which can become an NView */
class NIcon {
    constructor(n) {
        this.n = n;
        var d = this.ele = D('grid-item-content')
            .text(n.I).click(() => {

                //queryText.val(/* dimension + ':' + */ id);
                //Backbone.history.navigate("all/" + id);

                //querySubmit();

                new NView(n).showPopup();

                return false;
            });


        // d.append(E('button').text(n.N).click(()=>{
        //     //popup
        //     console.log(n, 'clicked');
        // }));
    }

    scale(s) {
        this.ele.attr('style',
            'font-size:' + (75.0 + 20 * (Math.log(1 + s))) + '%');
        return this;
    }
}

function ResultNode(x) {
    const y = D('list-item result');
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


    const header = D('header');
    // if (x.data) {
    //     header.append(
    //         //E('a').attr('href', x.data).attr('target', '_').append(
    //         E('h2').text(x.N)
    //         //)
    //     );
    // } else {

    const label = E('h2').text(x.N || x.I);
    header.append(
        x.url ? newEle('a').attr('href', x.url).append(label) : label
    );


    //}

    const meta = D('meta');


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
        var t = (x['_']);
        if (typeof t === "object")
            t = newEle('pre').append(JSON.stringify(t, null, 2));
        else //if (typeof t === "string")
            t = E('p').attr('class', 'textpreview').html((t+'').replace('\n', '<br/>'));

        y.append(t);
    }


    if (x.data) {
        y.click(() => {
            focus(x.data);
        });
    }


    return y;

}


function SpimeSocket(path, add) {

    const defaultHostname = window.location.hostname || 'localhost';
    const defaultWSPort = window.location.port || 8080;
    const options = undefined;

    /** creates a websocket connection to a path on the server that hosts the currently visible webpage */
    const ws = new ReconnectingWebSocket(
        'ws://' + defaultHostname + ':' + defaultWSPort + '/' + path,
        null /* protocols */,
        options); //{
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
        //});

    ws.binaryType = 'arraybuffer';

    ws.onopen = function () {

        add('websocket connect');

    };

    ws.onmessage = m => add(msgpack.decode(new Uint8Array(m.data)));

    // ws.onmessage = function (e) {
    //     try {
    //         var c = e.data;
    //         var d = JSON.parse(c);
    //         add(d);
    //     } catch (e) {
    //         add(c);
    //     }
    // };

    ws.onclose = e => add(['Websocket disconnected', e]);

    ws.onerror = e => add(["Websocket error", e]);

    return ws;
}


function MAP(target) {

    var map = L.map(target, {
        continuousWorld: true,
        worldCopyJump: true
    }).setView([51.505, -0.09], 5);

    //http://leaflet-extras.github.io/leaflet-providers/preview/
    //setTimeout(() =>
    L.tileLayer(
        'http://{s}.tile.osm.org/{z}/{x}/{y}.png'
        //'http://{s}.tile.opentopomap.org/{z}/{x}/{y}.png'
        , {
            //attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
    //    0);


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

    var curBounds = undefined;


    // function diff(curBounds, prevBounds) {
    //     if (curBounds.intersects(prevBounds)) {
    //         //console.log('diff', curBounds, prevBounds);
    //         //TODO http://stackoverflow.com/questions/25068538/intersection-and-difference-of-two-rectangles/25068722#25068722
    //         //return L.bounds([[p1y,p1x],[p2y,p2x]]);
    //         return curBounds;
    //     } else {
    //         return curBounds; //no commonality to subtract
    //     }
    // }

    function rectBounds(b, precision=7) {
        return {
            "x1": b.getWest(),
            "x2": b.getEast(),
            "y1": b.getSouth(),
            "y2": b.getNorth(),
            update: function () {
                let sep = '/';
                $.getJSON(  '/earth/lonlat/rect/' +
                        this.x1.toPrecision(precision) + sep +
                        this.y1.toPrecision(precision) + sep +
                        this.x2.toPrecision(precision) + sep +
                        this.y2.toPrecision(precision) +
                        '/json'
                ,
                (x) => {
                    const yy = LOAD(x, 0.5);
                    _.forEach(yy, y => {
                        if (y && y.where) {
                            y.where.addTo(map);
                        }
                    });
                });
            }
        };
    }

    const updateBounds = _.debounce(() =>{

        rectBounds( curBounds = map.getBounds() ).update();

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
