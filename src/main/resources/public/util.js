"use strict";

function e(eleID) {
    return document.createElement(eleID);
}
function E(eleID) {
    return $(e(eleID));
}

function DIVclass(cssclass) {
    const x = $(e('div'));
    if (cssclass) {
        x.attr('class', cssclass);
    }
    return x;
}


//faster than $('<div/>');
function newDiv(id) {
    var e = newEle('div');
    if (id) e.attr('id', id);
    return e;
}
function newSpan(id) {
    var e = newEle('span');
    if (id) e.attr('id', id);
    return e;
}

function divCls(c) {
    var d = newDiv();
    d.attr('class', c);
    return d;
}

function newEle(e, dom) {
    var d = document.createElement(e);
    if (dom)
        return d;
    return $(d);
}



function newGrid(selector) {
    return selector.packery({});
}

function addToGrid(result, builder, grid) {

    var newItems = _.map(result, (v) => {

        const c = builder(v);

        return ($(e('div')).attr('class', 'grid-item').append(c))[0];

    });

    var nn = $(newItems);

    grid.append(nn).packery('appended', nn);

}


function jsonUnquote(json) {
    return json.replace(/\"([^(\")"]+)\":/g, "$1:");  //This will remove all the quotes
}

function notify(x) {
    PNotify.desktop.permission();
    if (typeof x === "string")
        x = { text: x };
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
    return(false);
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
    constructor(){
        this._maxListeners = DEFAULT_MAX_LISTENERS
        this._events = {}
    }
    on(type, listener) {

        var that = this;
        if (Array.isArray(type)) {
            _.each(type, function(t) {
                that.on(t, listener);
            });
            return;
        }

        if(typeof listener != "function") {
            throw new TypeError()
        }
        var listeners = this._events[type] ||(this._events[type] = [])
        if(listeners.indexOf(listener) != -1) {
            return this
        }
        listeners.push(listener)
        if(listeners.length > this._maxListeners) {
            error(
                "possible memory leak, added %i %s listeners, "+
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
        function onceCallback(){
            eventsInstance.off(type, onceCallback)
            listener.apply(null, arguments)
        }
        return this.on(type, onceCallback)
    }
    off(type, listener) {

        var that = this;
        if (Array.isArray(type)) {
            _.each(type, function(t) {
                that.off(t, listener);
            });
            return;
        }


        if(typeof listener != "function") {
            throw new TypeError()
        }
        var listeners = this._events[type]
        if(!listeners || !listeners.length) {
            return this
        }
        var indexOfListener = listeners.indexOf(listener)
        if(indexOfListener == -1) {
            return this
        }
        listeners.splice(indexOfListener, 1)
        return this
    }
    emit(type, args){
        var listeners = this._events[type]
        if(!listeners || !listeners.length) {
            return false
        }
        for (var i = 0; i < listeners.length; i++)
            listeners[i].apply(null, args);
        //listeners.forEach(function(fn) { fn.apply(null, args) })
        return true
    }
    setMaxListeners(newMaxListeners){
        if(parseInt(newMaxListeners) !== newMaxListeners) {
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
    constructor (ttl, max) {
        this.data = new Map();
        if (max) { this.max = max }
        if (ttl) { this.ttl = ttl }
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

    clear () {
        this.data.clear()
        this.head = undefined
        this.tail = undefined
    }

    delete (key) {
        const curr = this.data.get(key)
        if (this.data.delete(key)) {
            this._remove(curr)
            return true
        }
        return false
    }

    entries () {
        return this._iterator(entry => [entry.key, entry.value])
    }

    evict () {
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

    forEach (callback) {
        const iterator = this._iterator(entry => {
            callback(entry.key, entry.value) // todo: support thisArg parameter
            return true
        })
        while (iterator.next()) { /* no-op */ }
    }

    get (key) {
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

    has (key) {
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

    keys () {
        return this._iterator(entry => entry.key)
    }

    set (key, value) {
        let curr = this.data.get(key)
        if (curr) {
            this._remove(curr)
        } else {
            this.data.set(key, curr = {})
        }
        curr.key = key
        curr.value = value
        if (this.ttl) { curr.expires = Date.now() + this.ttl }
        this._insert(curr)
        this.evict()
        return this
    }

    get size () {
        // run an eviction then we will report the correct size
        return this.evict()
    }

    values () {
        return this._iterator(entry => entry.value)
    }

    [Symbol.iterator] () {
        return this._iterator(entry => [entry.key, entry.value])
    }

    /**
     * @param {Function} accessFn - the function used to convert entries into return values
     * @returns {{next: (function())}}
     * @private
     */
    _iterator (accessFn) {
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
    _remove (curr) {
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
    _insert (curr) {
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
