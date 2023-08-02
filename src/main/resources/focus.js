"use strict";
class Focus {

    constructor() {
        this.layers = [];
        /* DEPRECATED */
        this.view = null;

        this.tree = new RBush();

        this.event = new window.mitt();

        const attn = new graphology.Graph({multi: true, allowSelfLoops: false, type: 'directed'});
        attn.attnUpdated = ()=>{};

        // attn.attnUpdated = _.throttle(() => {
        //
        //     const a = attn;//.$()
        //         // attn.$().filter(e => {
        //         //     return !e.isNode() || !e.data('instance');
        //         // }).kruskal()
        //     ;
        //     {
        //         // const tgt = $('#interests');
        //         // tgt.html('');
        //         //
        //
        //         //
        //         // const rank = a //attn.$()
        //         //     //pageRank().rank;
        //         //     .degreeCentralityNormalized().degree;
        //         // //closenessCentralityNormalized().closeness;
        //         //
        //         // a/*attn*/.nodes().roots().sort((a,b)=>rank(b)-rank(a)).forEach(x=>{
        //         //     //if (x.data('instance')) return;
        //         //     const icon = this.interestIcon(x, rank, attn);
        //         //     tgt.append(icon);
        //         //
        //         //     //console.log(x.outgoers());
        //         //     x.outgoers().nodes().forEach(xe => {
        //         //         icon.append(this.interestIcon(xe, rank, attn));
        //         //     });
        //         //
        //         // });
        //
        //     }
        //
        //
        //     a/*attn*/.nodes().forEach(x => {
        //         //console.log(x, x.outdegree());
        //         const d = x.outdegree();
        //         //if (d===0)
        //         if (d <= 1) {
        //             x.style('display', 'none');
        //             $(x.data('dom')).remove();
        //         }
        //     });
        //
        //
        //     //console.log(attn);
        //     //TODO stop any previous layout?
        //
        //     // try {
        //     //     attn.layout({
        //     //         //name: 'grid'
        //     //         //name: 'cose'//, numIter: 50,  coolingFactor: 0.999, animate: false
        //     //         name: 'breadthfirst', circle: true, nodeDimensionsIncludeLabels: true
        //     //     }).run();
        //     // } catch (e) {
        //     // }
        //
        //     // attn.layout({
        //     //     //name: 'grid'
        //     //     name: 'cose', numIter: 50,  coolingFactor: 0.999, animate: false, randomize:false
        //     //     //name: 'breadthfirst', circle: true/*, nodeDimensionsIncludeLabels: true*/
        //     // }).run();
        //
        // }, 100);
        this.attn = attn;

        this.GOAL_EPSILON =
            //0.01;
            0.05;

        this.spread = _.throttle(() => {
            this._update();
        }, 100);

        // const thatSpread = this.spread;
        // this.spread = () => {
        //     console.log('trying spread');
        //     thatSpread();
        // };

        //const updatePeriodMS = 200;
        //this.running = setInterval(this.run, updatePeriodMS);

        //ontology (defaults)
        this.link('highway', 'way');
        this.link('cycleway', 'way');
        this.link('sidewalk', 'way');
        this.link('sidewalk:left', 'sidewalk');
        this.link('sidewalk:rigtt', 'sidewalk');
        this.link('steps', 'way');

        this.link('oneway', 'way');
        this.link('maxspeed', 'way');
        this.link('direction', 'way');
        this.link('lanes', 'way');
        this.link('footway', 'way');
        this.link('drive_through', 'way');
        this.link('bicycle', 'way');
        this.link('bus', 'way');
        this.link('bridge', 'way');
        this.link('railway', 'way');
        this.link('crossing', 'way');
        this.link('public_transport', 'way');
        this.link('lane_markings', 'way');
        this.link('passenger_lines', 'way');
        this.link('tracktype', 'way');
        this.link('turn', 'way');

        this.link('parking', 'way');
        this.link('park_ride', 'way');

        this.link('denomination', 'religion');
        // this.link('religion', 'social');

        this.link('landuse', 'land');
        this.link('surface', 'land');
        this.link('boundary', 'land');

        this.link('fee', 'shop');
        this.link('atm', 'shop');
        this.link('brand', 'shop');
        this.link('advertising', 'shop');

        this.link('happy_hours', 'eat');
        this.link('outdoor_seating', 'eat');

    }

    // graphView(elementID) {
    //     //TODO
    //     const v = cytoscape({
    //         //headless:true
    //         container: document.getElementById(elementID),
    //         style: [ // the stylesheet for the graph
    //             {
    //                 selector: 'node',
    //                 style: {
    //                     //'label': 'data(id)',
    //                     //'background-color': '#666',
    //                     'background-opacity': 0,
    //                     'width': x => {
    //                         return 10 * (1 + Math.log(1 + x.outdegree() / 1));
    //                     },
    //                     'height': x => {
    //                         return 10 * (1 + Math.log(1 + x.outdegree() / 1));
    //                     }
    //                 }
    //             }
    //
    //             // {
    //             //     selector: 'edge',
    //             //     style: {
    //             //         'width': 3,
    //             //         'line-color': '#ccc',
    //             //         'target-arrow-color': '#ccc',
    //             //         'target-arrow-shape': 'triangle',
    //             //         'curve-style': 'bezier'
    //             //     }
    //             // }
    //         ],
    //     });
    //     v.domNode();
    //     return v;
    // }

    /** spreading activation iteration */
    _update() {

        const inRate = 0, outRate = 0.75, selfRate = 0, iters = 2;

        for (let iter = 0; iter < iters; iter++) {
            this.attn.forEachNode((_n, n) => {
                if (n.specified) return; //dont modify, set by user

                let v = this.goal(n) * selfRate;
                let sum = selfRate;

                //TODO double-buffer
                if (inRate!==0) {
                    this.attn.forEachInNeighbor(_n, (_x, x) => {
                        const gy = this.goal(x);
                        v += gy * inRate;
                        sum += inRate;
                    });
                }
                if (outRate!==0) {
                    this.attn.forEachOutNeighbor(_n, (_x, x) => {
                        const gy = this.goal(x);
                        v += gy * outRate;
                        sum += outRate;
                    });
                }

                const vv = sum === 0 ? 0 : v / sum;
                this.goalSet(n, vv);
            });
        }


        //accumualate value flows
        const values = new Map();
        this.attn.forEachNode((_n, x) => {
            //let icon = x.icon;
            //if (!icon) return;
            // icon = $(icon);
            // const iconColorIntensity = 0.5;
            // const green = Math.round(_green * 256 * iconColorIntensity);
            // const red = Math.round(_red * 256 * iconColorIntensity);
            // const blue = 0;
            // icon.css('background-color', 'rgba(' + red + ',' + green + ',' + blue + ', 1)');

            const gx = this.goal(x);
            // if (Math.abs(gx) < this.GOAL_EPSILON)
            //     return;

            graphologyLibrary.traversal.dfsFromNode(this.attn, _n, (_v, v) => {
                //console.log(_n, x, _v, v);
                const xi = v.instance;
                if (xi && xi.renderables) {
                    let d = values.get(_v) || 0;
                    d += gx;
                    values.set(_v, d);
                }
            });
        });

        //apply values to renderable
        this.attn.forEachNode((_n, x) => {
            const xi = x.instance;
            if (!xi || !xi.renderables)
                return;

            const d = values.get(_n) || 0;

            const da = Math.abs(d);
            const _red = Math.max(-d, 0);
            const _green = Math.max(+d, 0);

            _.forEach(xi.renderables, r => {
                if (da < this.GOAL_EPSILON)
                    r.enabled = false;
                else {
                    r.enabled = true;
                    const a = r.attributes;
                    if (a && a.interiorColor) {
                        a.interiorColor.red = _red;
                        a.interiorColor.green = _green;
                        a.interiorColor.blue = 0;
                        a.interiorColor.alpha = da;
                    }
                }
            });
        });

        this.view.redraw();
    }

    goal(x) {
        if (typeof(x) === 'string')
            x = this.attn.getNodeAttributes(x);
        if (x === undefined) return 0;

        const y = x.goal;
        return y === undefined ? 0 : y;
    }

    goalSet(x, value) {
        x.goal = value;
    }

    goalAdd(x, dg, update = false) {
        if (Math.abs(dg) < this.GOAL_EPSILON)
            return;

        let g = x.goal;
        if (g === undefined) g = 0;
        g += dg;
        if (g > +1) g = +1;
        else if (g < -1) g = -1;
        x.goal = g;
        if (update)
            this.spread();
    }

    interestIcon(x) {
        //const rx = rank(x);
        const icon = $('<div>').text(x)
            .addClass('interestIcon').addClass('buttonlike')
        //.css('font-size', `${Math.min(150, 100 * Math.log(1 + rx * 1E3))}%`);

        //x.data('icon', icon);

        const clearButton = $('<button>').text('x');
        clearButton.click(() => {
            const X = this.attn.getNodeAttributes(x);
            if (X.specified) {
                X.specified = false; //TODO removeData
                //TODO disable 'x'
                this.spread();
                clearButton.hide();
            }
        });
        clearButton.hide();

        // const slider = $('<input type="range" min="-5" max="5" value="0"/>');
        // slider.css('width', '4em');
        // slider.change(e=>{
        //
        // });

        //const slider = $('<div>'); //TODO 5 buttons

        const upDownButtons = $(document.createElement('div')).append(
            $(document.createElement('button')).text('+').click(() => this.add(x, +0.25, clearButton)),
            $(document.createElement('button')).text('-').click(() => this.add(x, -0.25, clearButton)),
            clearButton
        );
        icon.prepend($(document.createElement('div')).append(
            //slider
            upDownButtons
        ));

        return icon;

        /*
        let goalSelect = $('<select>').append(
            $('<option>').text('++'),
            $('<option>').text('+'),
            $('<option selected>').text(' '),
            $('<option>').text('-'),
            $('<option>').text('--')
        );
        goalSelect.change(e => {
            let goal;
            switch (e.target.value) {
                case '--':
                    goal = -2;
                    break;
                case '-':
                    goal = -1;
                    break;
                default:
                    goal = 0;
                    break;
                case '+':
                    goal = +1;
                    break;
                case '++':
                    goal = +2;
                    break;
            }
            x.data('goal', goal);
            switch(goal) {
                case -2: icon.css('background-color', '#922'); break;
                case -1: icon.css('background-color', '#600'); break;
                case  0: icon.css('background-color', 'transparent'); break;
                case +1: icon.css('background-color', '#060'); break;
                case +2: icon.css('background-color', '#292'); break;
            }

            attn.elements().dfs({
                roots: attn.getElementById(xid),
                visit: (v, e, u, i, depth) => {
                    //console.log(xid, ' -> ' + v);
                    const V = v.data('instance');
                    if (V && V.renderables) {
                        _.forEach(V.renderables, r => {
                            if (goal === 0)
                                r.enabled = false;
                            else
                                r.enabled = true;
                        });
                        this.view.redraw();
                    }
                },
                directed: true
            });
        });
        icon.prepend(goalSelect);
        */


        //return $('<div>').addClass('interestGroup').append(icon);
    }

    add(x, d, clearButton) {
        const X = this.attn.getNodeAttributes(x);
        X.specified = true;
        this.goalAdd(X, d, true);
        clearButton.show();
    }

    run() {
        _.forEach(this.layers, x => {
            x.update(this);
        });
    }

    position(lat, lon, alt) {
        const pos = this.view.pos();
        if (lat && lon && alt) {
            // if (pos.latitude!==lat || pos.longitude!==lon || pos.altitude!==alt) {
            //cam.tilt = 45;
            pos.latitude = lat;
            pos.longitude = lon;
            pos.altitude = alt;
            this.layers.forEach(l => l.position(pos));
            // }
        }
        return pos;
    }

    // cam() {
    //     return this.view.w.camera; //HACK
    // }

    link(x, y, cfg) {
        // if (this.interests.get(i))
        //     return; //ignore duplicate

        let change = this.addNode(y);
        change |= this.addNode(x);

        //TODO mergeEdge
        this.attn.addEdge(y, x, { });

        //TODO only if graph actually changed
        this.event.emit('graph_change', { /* TODO */ } );
    }

    addNode(x) {
        //TODO use mergeNode?
        if (this.attn.hasNode(x))
            return false; //already added


        this.attn.addNode(x, { });
        return true;
    }

    nodeDiv(x) {
        return $(document.createElement("div")).append(x, this.interestIcon(x, () => 1));
        //$(div).append($('<button>').text('+'), $('<button>').text('-'));

        //$(div).append($('<a>').text(id));
        //$(div).click(()=>{console.log(this);});

        //div.classList = ['my-cy-node'];
        //div.style.width = `${Math.floor(Math.random() * 40) + 60}px`;
        //div.style.height = `${Math.floor(Math.random() * 30) + 50}px`;
        //div.style.opacity = 0.5;
        //div.style.margin = '10px';
    }

     addLayer(layer) {
        this.layers.push(layer);

        if (layer.enabled === undefined)
            layer.enable(); //default state

        layer.position(this.position());

        layer.start(this);
    }

    removeLayer(layer) {
        layer.element.remove();
        delete layer.element;

        this.layers = _.filter(this.layers, x => x !== layer);
        layer.stop(this);
    }

    updateMenu(m) {
        this.attn.forEachNode((id, attributes) => {
            if (this.attn.inDegree(id) <= 0) {

                // console.log(id);

                m.addMenu(id, () => {
                    const d = $('<div>');

                    d.append(this.interestIcon(id));

                    // //TODO hierarchical, not flattened:
                    graphologyLibrary.traversal.dfsFromNode(this.attn, id, (s, attr, depth) => {
                        if (id!==s && this.attn.outDegree(s) > 1) {
                            d.append(this.interestIcon(s));
                        }
                    });

                    return d;
                });
            }
        });
    }
}
