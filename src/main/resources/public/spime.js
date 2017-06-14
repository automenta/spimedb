"use strict";

import $ from "jquery";
//import _ from "lodash";

import pouch from "pouchdb";
import pouchUpsert from "pouchdb-upsert";

import interact from "interact.js";


const jQuery = window.jQuery = window.$ = $;


pouch.plugin(pouchUpsert);

var MEMORY_SIZE = 512;
var ACTIVATION_RATE = 0.5;




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

        if (!this.what) {
            //this.what.remove(); //remove existing node
            //this.what = null;


            this.what = ResultNode(x);
            $('#results').append(this.what);

            if (this.where) {
                this.where.remove();
                this.where = null;
            }

            //if (map) {
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
        //}

        /*if (timeline) {
            const bounds = x['@']; if (bounds) {
                const when = bounds[0];
                if (typeof(when)==='number' || typeof(when)==='array')
                    console.log(x, when);
            }
        }*/
    }

}

/** nobject viewer/editor interface model */
class NView {

    constructor(n) {
        this.n = n;
        this.ele = D('box');
        const b = this.ele;

        const content = D();
        content.html(JSON.stringify(n));

        var font = 1.0;

        function updateFont() {
            b.attr('style', 'font-size:' + (parseInt(font * 100.0)) + '%');
        }

        const controls = D('controls').append(
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
        this.ele = D('grid-item-content')
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


    if (x.inh) {
        x.out = x.inh['>'];

        const vin = x.inh['<'];
        if (vin && !(vin.length === 1 && vin[0].length === 0)) { //exclude root tag
            x.in = vin;
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




function e(eleID, cssclass) {
    var x = document.createElement(eleID);
    if (cssclass)
        x.setAttribute('class', cssclass);
    return x;
}

function E(eleID, cssclass) {
    return $(e(eleID, cssclass));
}

function D(cssclass) {
    return E('div', cssclass);
}


function SPANclass(cssclass) {
    const x = E('span');
    if (cssclass)
        x.attr('class', cssclass);
    return x;
}

//faster than $('<div/>');
function DIV(id) {
    var e = newEle('div');
    if (id) e.attr('id', id);
    return e;
}

function SPAN(id) {
    var e = newEle('span');
    if (id) e.attr('id', id);
    return e;
}

function newSpan(id) {
    var e = newEle('span');
    if (id) e.attr('id', id);
    return e;
}

function divCls(c) {
    var d = DIV();
    d.attr('class', c);
    return d;
}

function newEle(e, dom) {
    var d = document.createElement(e);
    if (dom)
        return d;
    return $(d);
}





function jsonUnquote(json) {
    return json.replace(/\"([^(\")"]+)\":/g, "$1:");  //This will remove all the quotes
}

function notify(x) {
    PNotify.desktop.permission();
    if (typeof x === "string")
        x = {text: x};
    else if (!x.text)
        x.text = '';
    if (!x.type)
        x.type = 'info';
    x.animation = 'none';
    x.styling = 'fontawesome';

    new PNotify(x);
    //.container.click(_notifyRemoval);
}


function urlQuery(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split("=");
        if (pair[0] === variable) {
            return pair[1];
        }
    }
    return (false);
}

var ajaxFail = function (v, m) {
    console.error('AJAJ Err:', v, m);
};


function loadCSS(url, med) {
    $(document.head).append(
        $("<link/>")
            .attr({
                rel: "stylesheet",
                type: "text/css",
                href: url,
                media: (med !== undefined) ? med : ""
            })
    );
}

function loadJS(url) {
    $(document.head).append(
        $("<script/>")
            .attr({
                type: "text/javascript",
                src: url
            })
    );
}

const DEFAULT_MAX_LISTENERS = 12;

//TODO use ES6 Map for better performance: http://jsperf.com/map-vs-object-as-hashes/2
class EventEmitter {
    constructor() {
        this._maxListeners = DEFAULT_MAX_LISTENERS
        this._events = {}
    }

    on(type, listener) {

        var that = this;
        if (Array.isArray(type)) {
            _.each(type, function (t) {
                that.on(t, listener);
            });
            return;
        }

        if (typeof listener != "function") {
            throw new TypeError()
        }
        var listeners = this._events[type] || (this._events[type] = [])
        if (listeners.indexOf(listener) != -1) {
            return this
        }
        listeners.push(listener)
        if (listeners.length > this._maxListeners) {
            error(
                "possible memory leak, added %i %s listeners, " +
                "use EventEmitter#setMaxListeners(number) if you " +
                "want to increase the limit (%i now)",
                listeners.length,
                type,
                this._maxListeners
            )
        }
        return this
    }

    once(type, listener) {
        var eventsInstance = this

        function onceCallback() {
            eventsInstance.off(type, onceCallback)
            listener.apply(null, arguments)
        }

        return this.on(type, onceCallback)
    }

    off(type, listener) {

        var that = this;
        if (Array.isArray(type)) {
            _.each(type, function (t) {
                that.off(t, listener);
            });
            return;
        }


        if (typeof listener != "function") {
            throw new TypeError()
        }
        var listeners = this._events[type]
        if (!listeners || !listeners.length) {
            return this
        }
        var indexOfListener = listeners.indexOf(listener)
        if (indexOfListener == -1) {
            return this
        }
        listeners.splice(indexOfListener, 1)
        return this
    }

    emit(type, args) {
        var listeners = this._events[type]
        if (!listeners || !listeners.length) {
            return false
        }
        for (var i = 0; i < listeners.length; i++)
            listeners[i].apply(null, args);
        //listeners.forEach(function(fn) { fn.apply(null, args) })
        return true
    }

    setMaxListeners(newMaxListeners) {
        if (parseInt(newMaxListeners) !== newMaxListeners) {
            throw new TypeError()
        }
        this._maxListeners = newMaxListeners
    }
}

/** https://raw.githubusercontent.com/ianp/es6-lru-cache */
/**
 * A cache that can exhibit both least recently used (LRU) and max time to live (TTL) eviction policies.
 *
 * Internally the cache is backed by a `Map` but also maintains a linked list of entries to support the eviction policies.
 */
class Cache {

    // cache entries are objects with
    //   key - duplicated here to make iterator based methods more efficient
    //   value
    //   prev - a pointer
    //   next - a pointer
    //   expires - time of death in Date.now

    /**
     *
     * @param {number} ttl - the max. time to live, in milliseconds
     * @param {number} max - the max. number of entries in the cache
     * @param {Object|Iterable} data - the data to initialize the cache with
     */
    constructor(ttl, max) {
        this.data = new Map();
        if (max) {
            this.max = max
        }
        if (ttl) {
            this.ttl = ttl
        }
        // this.head = undefined
        // this.tail = undefined
        // if (data) {
        //     if (data[Symbol.iterator]) {
        //         for (let [key, value] in data) {
        //             this.set(key, value)
        //         }
        //     } else {
        //         Object.keys(data).forEach(key => this.set(key, data[key]))
        //     }
        // }
    }

    clear() {
        this.data.clear()
        this.head = undefined
        this.tail = undefined
    }

    delete(key) {
        const curr = this.data.get(key)
        if (this.data.delete(key)) {
            this._remove(curr)
            return true
        }
        return false
    }

    entries() {
        return this._iterator(entry => [entry.key, entry.value])
    }

    evict() {
        let count = 0
        let max = this.max
        let now = this.ttl ? Date.now() : false
        for (let curr = this.head; curr; curr = curr.next) {
            ++count
            if ((max && max < count) || (now && now > curr.expires)) {
                this.data.delete(curr.key)
                this._remove(curr)
            }
        }
        return count
    }

    forEach(callback) {
        const iterator = this._iterator(entry => {
            callback(entry.key, entry.value) // todo: support thisArg parameter
            return true
        })
        while (iterator.next()) { /* no-op */
        }
    }

    get(key) {
        const entry = this.data.get(key);
        if (entry) {
            if (entry.expires && entry.expires < Date.now()) {
                this.delete(key)
            } else {
                return entry.value
            }
        }
        return undefined;
    }

    has(key) {
        const entry = this.data.get(key)
        if (entry) {
            if (entry.expires && entry.expires < Date.now()) {
                this.delete(key)
            } else {
                return true
            }
        }
        return false
    }

    keys() {
        return this._iterator(entry => entry.key)
    }

    set(key, value) {
        let curr = this.data.get(key)
        if (curr) {
            this._remove(curr)
        } else {
            this.data.set(key, curr = {})
        }
        curr.key = key
        curr.value = value
        if (this.ttl) {
            curr.expires = Date.now() + this.ttl
        }
        this._insert(curr)
        this.evict()
        return this
    }

    get size() {
        // run an eviction then we will report the correct size
        return this.evict()
    }

    values() {
        return this._iterator(entry => entry.value)
    }

    [Symbol.iterator]() {
        return this._iterator(entry => [entry.key, entry.value])
    }

    /**
     * @param {Function} accessFn - the function used to convert entries into return values
     * @returns {{next: (function())}}
     * @private
     */
    _iterator(accessFn) {
        const max = this.max
        let now = this.ttl ? Date.now() : false
        let curr = this.head
        let count = 0
        return {
            next: () => {
                while (curr && (count > max || now > curr.expires)) { // eslint-disable-line no-unmodified-loop-condition
                    this.data.delete(curr.key)
                    this._remove(curr)
                    curr = curr.next
                }
                const it = curr
                curr = curr && curr.next
                return it ? accessFn(it) : undefined
            }
        }
    }

    /**
     * Remove entry `curr` from the linked list.
     * @private
     */
    _remove(curr) {
        if (!curr.prev) {
            this.head = curr.next
        } else {
            curr.prev.next = curr.next
        }
        if (!curr.next) {
            this.tail = curr.prev
        } else {
            curr.next.prev = curr.prev
        }
    }

    /**
     * Insert entry `curr` into the head of the linked list.
     * @private
     */
    _insert(curr) {
        if (!this.head) {
            this.head = curr
            this.tail = curr
        } else {
            const node = this.head
            curr.prev = node.prev
            curr.next = node
            if (!node.prev) {
                this.head = curr
            } else {
                node.prev.next = curr
            }
            node.prev = curr
        }
    }
}


// class Tag {
//
//     constructor(id/*, data*//*tagJSON*/) {
//
//
//         this.id = id.I;
//
//         this.meta = id;
//         /*this.meta = data;
//          this.name = data.name;
//          this.inh = data.inh;*/
//
//         //var n = tag.node(i);
//         //if (!n) {
//         //n = newTag(i);
//         //}
//         //
//         ////TODO copy other metadata, use _.extend
//         //n.name = l.name || i;
//         //
//         //if (l.style) {
//         //    n.style = l.style;
//         //}
//         //if (l.styleUrl) {
//         //    n.styleUrl = l.styleUrl;
//         //}
//         //
//         //n.meta = l.meta || { };
//         //
//         //if (n.meta.wmsLayer) {
//         //    //n.features[n.meta.wmsLayer] = newWMSLayer(n.meta.wmsLayer);
//         //}
//         //if (n.meta.tileLayer) {
//         //    //n.features[n.meta.tileLayer] = newTileLayer(n.meta.tileLayer);
//         //}
//         //
//         //
//         //if (l.inh) {
//         //    n.inh = l.inh;
//         //}
//         //
//         //n.update();
//
//     }
//
//     getPanelHTML() {
//         var x = '<div style="width: 100%; height: 100%; color: black; background-color: orange; border: 2px solid black;">';
//         x += '<b>' + this.name + '</b>';
//         x += JSON.stringify(this.inh);
//         x += '</div>'
//         return x;
//     }
//
//     //creates a new channel object to manage
//     newChannel(opts) {
//         var activation;
//         if (this.meta.ws) {
//             var uu = this.meta.ws.split('#');
//             var path = uu[0];
//             var chanID = uu[1];
//
//             if (!opts) opts = { };
//
//
//             var _onOpen = opts.onOpen;
//             opts.onOpen = function() {
//
//                 activation.on(chanID);
//
//                 console.log('Websocket connect: ' + uu);
//
//                 //activation.channel = new SocketChannel(s, { });
//
//                 if (_onOpen) _onOpen(); //chained callback
//             };
//
//             activation = new Websocket(path, opts);
//         }
//         else {
//             activation = { };
//         }
//         return activation;
//     }
// }
//
// class TagIndex {
//
//     constructor(callback) {
//         "use strict";
//
//         this.tag = new graphlib.Graph({multigraph: true});
//
//         /*
//          $.getJSON('/tag/meta', {id: JSON.stringify(layerIDs)})
//          .done(function (r) {
//          updateTags(r);
//          if (callback)  callback();
//          })
//          .fail(ajaxFail);
//          */
//
//     }
//
//
//     activateRoots(levels, MAX_NODES) {
//         var count = 0;
//
//         var roots = [];
//         var nodes = [];
//         var edges = [];
//
//         var nn = this.tag.nodes();
//         for (var i = 0; i < nn.length; i++) {
//             var t = this.tag.node(nn[i]);
//             if (!t) continue;
//
//
//
//             //TODO temporary - should be filtered by server
//             if (!t.id || t.id.indexOf(' ')!=-1) {
//                 console.error('invalid tag', t);
//                 continue;
//             }
//
//             var id = t.id;
//
//
//             var parent = this.tag.predecessors( id );
//             if (parent && parent.length > 0) {
//                 continue;
//             }
//
//             roots.push(t);
//
//             this.graphize(id, levels, nodes, edges);
//
//             if (MAX_NODES && count++ == MAX_NODES) break;
//         }
//
//         this.channel.add(nodes, edges);
//
//         return roots;
//     }
//
//     //nodes and edges are arrays which new elements are stored.
//     // after the root callee returns, they can be added to a spacegraph all at once
//     graphize(t, levels, nodes, edges) {
//
//
//         if (typeof(t) === "string") t = this.tag.node(t);
//
//         if (!t)
//             return;
//
//         //TODO temporary
//         if (!t.id || t.id.indexOf(' ')!=-1) {
//             console.error('invalid tag ID: ' + t.id);
//             return null;
//         }
//
//         var n;
//         if (Math.random() < 0.5) {
//             var n = {
//                 id: t.id,
//                 style: {
//                     shape: 'rectangle',
//                     width: 160,
//                     height: 120
//                 },
//                 widget: {
//                     html: t.getPanelHTML ? t.getPanelHTML() : null,
//                     style: {},
//                     scale: 0.9,
//                     pixelScale: 160.0,
//                     minPixels: 8
//                 }
//             };
//
//         }
//         else {
//             var n = {
//                 id: t.id,
//                 content: t.name,
//                 style: {
//                     shape: 'rectangle',
//                     width: 160,
//                     height: 120,
//                 }
//             };
//
//         }
//
//         nodes.push(n);
//
//
//         if (levels > 0) {
//             var children = this.tag.successors(t.id);
//
//             for (var i = 0; i < children.length; i++) {
//                 var v = this.graphize(children[i], levels - 1, nodes, edges);
//                 if (v) {
//
//                     if (Math.random() < 0.5 && !(n.widget)) {
//                         //create parent child containment; only works for non-widgets if the width/height are not specified
//
//                         delete n.style.width;
//                         delete n.style.height;
//
//                         v.parent = t.id;
//                     }
//                     else {
//
//                         //create edge from this node to child
//                         var edgeID = t.id + '_' + children[i];
//                         var e = {
//                             id: edgeID, source: t.id, target: children[i],
//                             style: {
//                                 'opacity': 0.5,
//                                 //'target-arrow-shape': 'triangle',
//                                 'line-color': 'purple',
//                                 'width': 25
//                             }
//                         };
//                         edges.push(e);
//                     }
//
//                 }
//             }
//
//         }
//
//         return n;
//     }
//
//     updateTag(i) {
//         "use strict";
//         this.tag.setNode(i, new Tag(i));
//     }
//
//
//
// }
//
//
//
// function error(message, args){
//     console.error.apply(console, [message].concat(args))
//     console.trace()
// }
//


//
// class Channel extends EventEmitter {
//
//     //EVENTS
//     //.on("graphChange", function(graph, nodesAdded, edgesAdded, nodesRemoved, edgesRemoved) {
//
//     constructor(initialData) {
//         super();
//
//
//         this.ui = null;
//
//         this.prev = { };
//         this.commit = function() { }; //empty
//
//         //set channel name
//         if (typeof(initialData)==="string")
//             initialData = { id: initialData };
//         this.data = initialData || { };
//         if (!this.data.id) {
//             //assign random uuid
//             this.data.id = uuid();
//         }
//
//         if (!this.data.nodes) this.data.nodes =[];
//         if (!this.data.edges) this.data.edges =[];
//
//         var u = uuid();
//         var uc = 0;
//
//         var ensureID = function(x) {
//             if (!x.id) x.id = u + (uc++);
//         };
//
//         //assign unique uuid to any nodes missing an id
//         _.each(this.data.nodes, ensureID);
//         _.each(this.data.edges, ensureID);
//
//     }
//
//
//     init(ui) {
//         this.ui = ui;
//     }
//
//     id() {
//         return this.data.id;
//     }
//
//     clear() {
//         //TODO
//     }
//
//
//     removeNode(n) {
//         n.data().removed = true;
//
//         var removedAny = false;
//         var id = n.data().id;
//         this.data.nodes = _.filter(this.data.nodes, function(e) {
//             if (e.id === id) {
//                 removedAny = true;
//                 return false;
//             }
//         });
//
//         if (removedAny)
//             this.emit('graphChange', [this, null, null, n, null]);
//
//         return removedAny;
//     }
//
//     //TODO: removeEdge
//
//     //TODO batch version of addNode([n])
//     addNode(n) {
//         this.data.nodes.push(n);
//         this.emit('graphChange', [this, [n], null, null, null]);
//     }
//
//     addEdge(e) {
//         this.data.edges.push(e);
//         this.emit('graphChange', [this, null, [e], null, null]);
//     }
//
//
//     //nodes and edges are arrays
//     add(nodes, edges) {
//         var that = this;
//         _.each(nodes, function(n) { that.data.nodes.push(n); });
//         _.each(edges, function(e) { that.data.edges.push(e); });
//         //nodes.forEach(this.data().nodes.push); //??
//         //edges.forEach(this.data().edges.push);
//         this.emit('graphChange', [this, nodes, edges, null, null]);
//     }
// }
//
// class SocketChannel extends Channel {
//
//     constructor(initialData,connection) {
//
//         super(initialData);
//
//         this.socket = connection;
//
//         var synchPeriodMS = 500;
//
//         this.commit = _.throttle(function() {
//             if (!this.socket || !this.socket.opened) {
//                 return;
//             }
//
//             /** include positions in update only if p is defined and is object */
//             if (this.data.p && typeof(this.data.p)==="object") {
//                 //get positions
//                 var eles = this.ui.elements();
//                 var P = {};
//                 for (var i = 0; i < eles.length; i++) {
//                     var ele = eles[i];
//                     //console.log( ele.id() + ' is ' + ( ele.selected() ? 'selected' : 'not selected' ) );
//                     var p = ele.position();
//                     var x = p.x;
//                     if (!isFinite(x))
//                         continue;
//                     var y = p.y;
//                     P[ele.id()] = [parseInt(x), parseInt(y)];
//                 }
//                 this.data.p = P; //positions; using 1 character because this is updated frequently
//             }
//
//             //https://github.com/Starcounter-Jack/Fast-JSON-Patch
//             var diff = jsonpatch.compare(this.prev, this.data);
//
//             this.prev = _.clone(this.data, true);
//
//             if (diff.length > 0) {
//                 this.socket.send(['p' /*patch*/, this.data.id, diff]);
//             }
//
//         }, synchPeriodMS);
//
//     }
// }
//
//
// /** creates a websocket connection object */
// function Websocket(path, conn) {
//
//     if (!conn) conn = { };
//
//     if (!conn.url)
//         conn.url = 'ws://' + window.location.hostname + ':' + window.location.port + '/' + path;
//
//     //subscriptions: channel id -> channel
//     conn.subs = { };
//
//
//     var ws = conn.socket = new WebSocket(conn.url);
//
//     ws.onopen = function () {
//
//         conn.opened = true;
//
//         //console.log('websocket connected');
//
//         if (conn.onOpen)
//             conn.onOpen(this);
//
//
//     };
//
//     ws.onclose = function () {
//         //already disconnected?
//         if (!this.opt)
//             return;
//
//         conn.opened = false;
//
//         //console.log("Websocket disconnected");
//
//         if (conn.onClose)
//             conn.onClose();
//
//         //attempt reconnect?
//     };
//     ws.onerror = function (e) {
//         console.log("Websocket error", e);
//         if (conn.onError)
//             conn.onError(e);
//     };
//
//     conn.send = function(data) {
//         var jdata = /*jsonUnquote*/( JSON.stringify(data) );
//
//         //console.log('send:', jdata.length, jdata);
//
//         this.socket.send(jdata);
//     };
//
//     conn.handler = {
//         '=': function(d) {
//             var channelData = d[1];
//
//             //console.log('replace', channelData);
//
//             var chanID = channelData.id;
//             var chan = conn.subs[chanID];
//             if (!chan) {
//                 chan = new Channel( channelData, conn );
//                 //if (window.s)
//                 //  window.s.addChannel(chan);
//
//                 if (conn.onChange)
//                     conn.onChange(chan);
//             }
//             else {
//                 chan.data = channelData;
//                 //if (window.s)
//                 //  window.s.updateChannel(chan);
//
//                 if (conn.onChange)
//                     conn.onChange(chan);
//             }
//         },
//         '+': function(d) {
//             var channelID = d[1];
//             var patch = d[2];
//
//
//             //{ id: channelData.id, data:channelData}
//             var c = conn.subs[channelID];
//             if (c) {
//                 //console.log('patch', patch, c, c.data);
//
//                 jsonpatch.apply(c.data, patch);
//
//                 //if (window.s)
//                 // window.s.addChannel(c);
//
//                 if (conn.onChange)
//                     conn.onChange(c);
//             }
//             else {
//                 console.error('error patching', d);
//             }
//         }
//
//
//     };
//
//     ws.onmessage = function (e) {
//         /*e.data.split("\n").forEach(function (l) {
//          output(l, true);
//          });*/
//
//         //try {
//         var d = JSON.parse(e.data);
//
//         if (d[0]) {
//
//             //array, first element = message type
//             var messageHandler = conn.handler[d[0]];
//             if (messageHandler) {
//                 //return conn.apply(messageHandler,d);
//                 return messageHandler(d);
//             }
//         }
//
//         notify('websocket data (unrecognized): ' + JSON.stringify(d));
//         /*}
//          catch (ex) {
//          notify('in: ' + e.data);
//          console.log(ex);
//          }*/
//     };
//
//     conn.on = function(channelID, callback) {
//
//         if (conn.subs[channelID]) {
//             //already subbed
//         }
//         else {
//             conn.subs[channelID] = new Channel(channelID);
//             if (callback)
//                 callback.off = function() { conn.off(channelID); };
//         }
//
//         conn.send(['on', channelID]);
//
//         //TODO save callback in map so when updates arrive it can be called
//
//         return callback;
//     };
//
//     //reload is just sending an 'on' event again
//     conn.reload = function(channelID) {
//         conn.send(['!', channelID]);
//     };
//
//     conn.operation = function(op, channelID) {
//         conn.send([op, channelID]);
//     };
//
//     conn.off = function(channelID) {
//         if (!channelID) {
//             //close everything and stop the wbsocket
//             for (var k in conn.subs) {
//                 conn.off(k);
//             }
//             ws.close();
//         }
//         else {
//
//             if (!conn.subs[channelID]) return;
//
//             delete conn.subs[channelID];
//
//             conn.send(['off', channelID]);
//         }
//
//     };
//
//     return conn;
//
// }


/** https://github.com/kolodny/member-berry
 *
 *     var obj1 = {};
 var obj2 = {};
 expect(membered(obj1, obj2)).toEqual(1);
 expect(membered(obj1, obj2)).toEqual(1, 'ooh, I member!');
 * */
var resultObject = {};

function MEMOIZE(fn) {
    var wrappedPrimitives = {};
    var map = new WeakMap();
    return function () {
        var currentMap = map;
        for (var index = 0; index < arguments.length; index++) {
            var arg = arguments[index];
            if (typeof arg !== 'object') {
                var key = (typeof arg) + arg
                if (!wrappedPrimitives[key]) wrappedPrimitives[key] = {};
                arg = wrappedPrimitives[key];
            }
            var nextMap = currentMap.get(arg);
            if (!nextMap) {
                nextMap = new WeakMap();
                currentMap.set(arg, nextMap);
            }
            currentMap = nextMap;
        }
        if (!currentMap.has(resultObject)) {
            currentMap.set(resultObject, fn.apply(null, arguments));
        }
        return currentMap.get(resultObject);
    }
}

/** https://github.com/kapouer/node-lfu-cache/blob/master/index.js */
class LFU extends Map {

    constructor(cap, halflife) {
        super();
        this.cap = cap;
        this.halflife = halflife || null;
        this.head = this.freq();
        this.lastDecay = Date.now();
    }

    get(key) {
        var el = super.get(key);
        if (!el) return;
        var cur = el.parent;
        var next = cur.next;
        if (!next || next.weight !== cur.weight + 1) {
            next = this.entry(cur.weight + 1, cur, next);
        }
        this.removeFromParent(el.parent, key);
        next.items.add(key);
        el.parent = next;
        var now = Date.now();
        el.atime = now;
        if (this.halflife && now - this.lastDecay >= this.halflife)
            this.decay(now);
        this.atime = now;
        return el.data;
    }

    /** follows java's Map.computeIfAbsent semantics */
    computeIfAbsent(key, builder) {
        var x = this.get(key);
        if (!x) {
            x = builder.apply(key);
            this.set(key, x);
        }
        return x;
    }

    decay(now) {
        // iterate over all entries and move the ones that have
        // this.atime - el.atime > this.halflife
        // to lower freq nodes
        // the idea is that if there is 10 hits / minute, and a minute gap,

        this.lastDecay = now;
        var diff = now - this.halflife;
        //var halflife = this.halflife;
        var weight, cur, prev;
        for (var [key, value] of this) {
            if (diff > value.atime) {
                // decay that one
                // 1) find freq
                cur = value.parent;
                weight = Math.round(cur.weight / 2);
                if (weight === 1) continue;
                prev = cur.prev;
                while (prev && prev.weight > weight) {
                    cur = prev;
                    prev = prev.prev;
                }
                if (!prev || !cur) {
                    throw new Error("Empty before and after halved weight - please report");
                }
                // 2) either prev has the right weight, or we must insert a freq with
                // the right weight
                if (prev.weight < weight) {
                    prev = this.entry(weight, prev, cur);
                }
                this.removeFromParent(value.parent, key);
                value.parent = prev;
                prev.items.add(key);
            }
        }
    }


    set(key, obj) {

        var now = Date.now();

        const existing = this.remove(key, true);
        if (existing===undefined) {
            //ensure room for the new entry
            while (this.size + 1 > this.cap) {
                if (this.halflife && now - this.lastDecay >= this.halflife) {
                    this.decay(now);
                }

                try {
                    this.evict();
                } catch (e) {
                    console.error(e);
                    break;
                }
            }
        }


        var cur = this.head.next;
        if (!cur || cur.weight !== 1) {
            cur = this.entry(1, this.head, cur);
        }
        if (!cur.items.add(key)) {
            console.error('duplicate', key);
        }


        super.set(key, { //TODO store this as a 3 element tuple
            data: obj,
            atime: now,
            parent: cur
        });

        return existing;

    }

    remove(key, reparentOnly=false) {
        var el = super.get(key);
        if (!el)
            return undefined;
        this.removeFromParent(el.parent, key);
        if (!reparentOnly)
            this.delete(key);
        return el.data;
    }


    removeFromParent(parent, key) {
        if (parent.items.delete(key)) {
            if (parent.items.size === 0) {
                parent.prev.next = parent.next;
                if (parent.next) parent.next.prev = parent;
            }
        }
    }

    evict() {
        const least = this.next();
        if (least) {
            const victim = this.remove(least);
            if (victim) {
                this.evicted(least, victim);
            }
        } else {
            throw new Error("Cannot find an element to evict - please report issue");
        }
    }

    next() {
        if (this.head.next) {
            var next = this.head.next; //its either head.next or just head
            while (next.items.size === 0) {
                next = next.next;
            }

            return next.items.keys().next().value;
        }
        return null;
    }

    evicted(key, value) {

    }

    freq() {
        return {
            weight: 0,
            items: new Set()
        }
    }

    item(obj, parent) {
        return {
            obj: obj,
            parent: parent
        };
    }

    entry(weight, prev, next) {
        var node = this.freq();
        node.weight = weight;
        node.prev = prev;
        node.next = next;
        prev.next = node;
        if (next) next.prev = node;
        return node;
    }
}

class Node {
    constructor(id) {
        this.id = id;
        //edge maps: (target, edge_value)
        //this.i = new LFU(8)
        this.i = new LFU(8);
        this.o = new LFU(8); //LFU(8);
    }

}


class LFUGraph extends LFU {

    constructor(maxNodes, halflife) {
        super(maxNodes, halflife);
    }

    node(nid, createIfMissing=true) {
        const x = this.get(nid);
        if (x || !createIfMissing)
            return x;

        const n = new Node(nid);
        this.set(nid, n);
        this.nodeAdded(nid, n);
        return n;
    }

    nodeIfPresent(nodeID) {
        return this.get(nodeID);
    }

    evicted(nid, n) {
        super.evicted(nid, n);

        if (n.o) {
            for (var tgtNode of n.o.keys()) {
                //tgtNode = tgtNode.data;
                //console.log('evict', nid, n, tgtNode);
                tgtNode = this.get(tgtNode);

                const e = tgtNode.i.remove(nid);
                if (e)
                    this.edgeRemoved(n, tgtNode, e);

            }
        }

        if (n.i) {
            for (var srcNode of n.i.keys()) {
                //srcNode = srcNode.data;
                //console.log('evict', nid, n, this.get(srcNode));
                srcNode = this.get(srcNode);

                const e = srcNode.o.remove(nid);
                if (e)
                    this.edgeRemoved(srcNode, n, e);

            }
        }

        this.nodeRemoved(nid, n);

        delete n.o;
        delete n.i;


    }

    nodeAdded(nid, n) {
    }

    nodeRemoved(nid, n) {
    }

    edgeAdded(src, tgt, e) {
    }

    edgeRemoved(src, tgt, e) {
    }

    edge(src, tgt, value) {
        if (src == tgt)
            return null; //no self-loop

        if (value === undefined) {
            value = src.toString() + "_" + tgt.toString();
        } else if (value === null) {

        }

        const T = this.node(tgt, value ? true : false);
        if (!T)
            return null;

        const S = this.node(src, value ? true : false);
        if (!S)
            return null;

        const ST = S.o.get(tgt);
        if (ST) {
            return ST;
        } else if (value && S.o && T.i) {
            value = (typeof value === "function") ? value() : value;
            S.o.set(tgt, value);
            T.i.set(src, value);
            this.edgeAdded(S, T, value);
            return value;
        } else {
            return null;
        }
    }

    edgeIfPresent(src, tgt) {
        return this.edge(src, tgt, null);
    }

    forEachNode(nodeConsumer) {
        for (var [nodeID, node] of this) {
            const n = node.data;
            if (n)
                nodeConsumer(n);
        }
    }

    forEachEdge(edgeConsumer) {
        for (var [nodeID, srcVertex] of this) {
            const vv = srcVertex.data;
            if (vv) {
                for (var [targetID, edge] of vv.o) {
                    edgeConsumer(vv, targetID, edge.data);
                }
            }
        }
    }

    // getNodesAndEdgesArray() {
    //     var a = [];
    //     for (var [vertexID, vertex] of this) {
    //         a.push( vertex.data )
    //     }
    //     return a;
    // }


    /** computes a node-centric Map snapshot of the values */
    treeOut() {
        var x = {};
        this.forEachEdge((src,tgtID,E)=>{
            const vid = src.id;
            const eid = tgtID;
            var ex = x[vid];
            if (!ex)
                x[vid] = ex = {};
            var ee = ex[eid];
            if (!ee)
                ex[eid] = ee = [];
            ee.push(eid);
        });
        return x;
    }

    edgeList() {
        var x = [];
        this.forEachEdge((src,tgtID,E)=>{
            x.push([src.id, tgtID]);
        });
        return x;
    }


}

// (function (exports) {
//
//     exports.LFUGraph = LFUGraph;
//
// }(typeof exports === 'undefined' ? this.share = {} : exports));

class MMEE extends LFUGraph {

    constructor() {
        super(MEMORY_SIZE);

        const db = this.db = new pouch("spime");
        console.log(db);

        db.changes({
            since: 'now',
            live: true,
            include_docs: true
        }).on('change', function (change) {
            // change.id contains the doc id, change.doc contains the doc
            if (change.deleted) {
                // document was deleted
                console.log('delete', change);
            } else {
                // document was added/modified
                console.log('change', change);
            }
        })/*.on('error', function (err) {
            // handle errors
        });*/

    }

    evicted(key, value) {
        super.evicted(key, value);
        this.REMOVE(key);
    }


    ADD(n) {
        var id = n.I;
        if (!id)
            throw new Error("missing ID");

        var y;
        var x = this.remove(id);
        if (x) {
            (y = x).update(n);
        } else {
            y = new NObject(n);
            // if (map && y.where) { //HACK
            //     y.where.addTo(map);
            // }
        }

        this.set(id, y); //update LFU cache by reinserting

        this.db.upsert(id, (d)=> {

            n._id = id;
            n._rev = d._rev; //temporary
            if (_.isEqual(d, n)) {
                return false; //no change
            } else {
                //console.log(JSON.stringify(d), JSON.stringify(n), _.isEqual(d, n));
                delete n._rev; //undo the temporarily added revision for the comparison
                return n;
            }

        })/*.then((d)=>{
            console.log('then', d);
        })*/.catch(function (err) {
            console.error(err)
        });


        return y;
    }


    REMOVE(id) {

        const r = this.get(id);
        if (!r)
            return;

        r.remove();

        //if (map && r.where) {
            //map.remove(r.where);
            //r.where = undefined;
        //}

        this.delete(id);

    }

    CLEAR() {
        this.forEach((value, key) => {
            REMOVE(key);
        });
        this.clear();
        //clusters = {};
    }

//TODO see this active eviction is compatible with LFU
    FORGET(decay, maxItems) {
        /*if (!ME.size() > maxItems) {
         //dont have to sort
         }*/
        const n = this.size;

        const filteredIterator = this.values();
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
            this.REMOVE(z.I);
        }
    }


    LOAD(ss, activationRate) {


        //setTimeout(() => {

        const results = ss[0]; //first part: search results
        const facets = ss[1]; //second part: facets



        const that = this;

        const yy = _.map(results, x => {
            if (!x.I) return;
            const score = x['*'];
            const y = that.ADD(x);
            if (y) {
                y.activate(score * ACTIVATION_RATE * activationRate);
            }
            return y;
        });

        this.FORGET(0.9, MEMORY_SIZE);

        //TODO use abstraction
        if (this.facets) {

            this.facets.html('');

            const that = this;
            var newItems = _.map(facets, (v) => {

                const id = v[0]
                    .replace(/_/g, ' ')
                    .replace(/\-/g, ' ')
                ; //HACK


                const c = new NIcon(that.computeIfAbsent(v[0], (v)=>{
                    return new NObject({I: id});
                })).scale(v[1]).ele;

                return ($(e('div')).attr('class', 'grid-item').append(c))[0];

            });

            var nn = $(newItems);
            this.facets.append(nn);
        }

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

}




function newWindow(content) {
    const w = newFrame();

    // var closeButton = $('<button/>').text('x').addClass('close_button').click(function() {
    //     w.fadeOut(150, function() { $(this).remove(); });
    // });

    /*var fontSlider = NSlider({ }).addClass('font_slider').css({
        width: '1em',
        position: 'absolute',
        left: 0,
        top: 0
    });*/

    w.append(content = (content || $('<div/>')), /*fontSlider,*/);

    content.addClass('content');

    return w;
}

function newFrame() {
    //http://interactjs.io/


    var div = $('.windgets');
    if (div.length === 0)
        div = D('windgets').prependTo($('body'));

    var content = D('windget')/*.fadeIn()*/.appendTo(div);
    var dragMoveListener = event => {
        var target = event.target,
            // keep the dragged position in the data-x/data-y attributes
            x = (parseFloat(target.getAttribute('data-x')) || 0) + event.dx,
            y = (parseFloat(target.getAttribute('data-y')) || 0) + event.dy;

        // translate the element
        target.style.webkitTransform =
            target.style.transform =
                'translate(' + x + 'px, ' + y + 'px)';

        // update the posiion attributes
        target.setAttribute('data-x', x);
        target.setAttribute('data-y', y);
    };



    interact(content[0])
        .draggable({
            onmove: dragMoveListener
        })
        .resizable({
            edges: {left: true, right: true, bottom: true, top: true}
        })
        .on('resizemove', function (event) {
            var target = event.target,
                x = (parseFloat(target.getAttribute('data-x')) || 0),
                y = (parseFloat(target.getAttribute('data-y')) || 0);

            // update the element's style
            target.style.width = parseInt(event.rect.width);// + 'px';
            target.style.height = parseInt(event.rect.height);// + 'px';

            // translate when resizing from top or left edges
            x += event.deltaRect.left;
            y += event.deltaRect.top;

            target.style.webkitTransform = target.style.transform =
                'translate(' + parseInt(x) + ',' + parseInt(y) + ')';

            target.setAttribute('data-x', x);
            target.setAttribute('data-y', y);
            //target.textContent = event.rect.width + '×' + event.rect.height;
        });

    //content.close = ...

    return content;
}



const EXPORT = function () {
    return new MMEE();
};
EXPORT.D = D;
EXPORT.E = E;

export default EXPORT;




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
