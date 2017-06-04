"use strict";

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
        var el = super.get(key);
        if (el) {
            el.data = obj;
            return;
        }

        var now = Date.now();

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


    }

    remove(key) {
        var el = super.get(key);
        if (!el)
            return;
        this.removeFromParent(el.parent, key);
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

        for (var tgtNode of n.o.keys()) {
            //tgtNode = tgtNode.data;
            //console.log('evict', nid, n, tgtNode);
            tgtNode = this.get(tgtNode);

                const e = tgtNode.i.remove(nid);
                if (e)
                    this.edgeRemoved(n, tgtNode, e);

        }

        for (var srcNode of n.i.keys()) {
            //srcNode = srcNode.data;
            //console.log('evict', nid, n, this.get(srcNode));
            srcNode = this.get(srcNode);

                const e = srcNode.o.remove(nid);
                if (e)
                    this.edgeRemoved(srcNode, n, e);

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

(function (exports) {

    exports.LFUGraph = LFUGraph;

}(typeof exports === 'undefined' ? this.share = {} : exports));


