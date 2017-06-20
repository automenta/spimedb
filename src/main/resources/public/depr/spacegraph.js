"use strict";

function spacegraph(targetWrapper, opt) {

    targetWrapper.addClass("spacegraph");

    //<div id="overlay"></div>
    var overlaylayer = $('<div class="overlay"/>').prependTo(targetWrapper);

    //<div id="graph"><!-- cytoscape render here --></div>
    var target = $('<div class="graph"/>').appendTo(targetWrapper);

    target.attr('oncontextmenu', "return false;");

    var commitPeriodMS = 300;
    var widgetUpdatePeriodMS = 10;
    var suppressCommit = false;
    var zoomDuration = 64; //ms


    var ready = function() {

        var that = this;

        opt.start.apply(this);

        //http://js.cytoscape.org/#events/collection-events


        this.on('data position select unselect add remove grab drag style', function (e) {

            if (suppressCommit)
                return;

            /*console.log( evt.data.foo ); // 'bar'

             var node = evt.cyTarget;
             console.log( 'tapped ' + node.id() );*/

            var target = e.cyTarget;
            if (target) {
                refresh(target);
                //console.log(this, that, target);
                //that.commit();
            }

        });


        //overlay framenode --------------        
        var frame = NodeFrame(this);


        /*
         var baseRedraw = this._private.renderer.redraw;

         this._private.renderer.redraw = function(options) {
         baseRedraw.apply(this, arguments);

         //frame.hoverUpdate();

         };
         */


        /*var baseDrawNode = this._private.renderer.drawNode;
         this._private.renderer.drawNode = function (context, node, drawOverlayInstead) {
         baseDrawNode.apply(this, arguments);
         };*/




        var that = this;

        var updateAllWidgets = this.updateAllWidgets = _.throttle(function() {

            if (suppressCommit) return;

            that.nodes().each(refresh);

        }, widgetUpdatePeriodMS);

        this.on('layoutready layoutstop pan zoom', updateAllWidgets);

//            function scaleAndTranslate( _element , _x , _y, wx, wy )  {
//                
//                var mat = _element.style.transform.baseVal.getItem(0).matrix;
//                // [1 0 0 1 tx ty], 
//                mat.a = wx; mat.b = 0; mat.c = 0; mat.d = wy; mat.e = _x; mat.f = _y;
//                
//            }        
    };

    function widget(node) {
        if (node.data)
            return node.data().widget;
        return undefined;
    }

    function refresh(target, target2) {
        if (target2) target = target2; //extra parameter to match the callee's list

        if (widget(target)) {
            if (!target.data().updating) {
                target.data().updating = setTimeout(s.updateNodeWidget, widgetUpdatePeriodMS, target); //that.updateNodeWidget(target);
            }
        }
    }

    opt = _.defaults(opt, {
        layout: {
            name: 'cose',
            padding: 5
        },
        ready: ready,
        // initial viewport state:
        zoom: 1,
        pan: {x: 0, y: 0},
        // interaction options:
        minZoom: 1e-50,
        maxZoom: 1e50,
        zoomingEnabled: true,
        userZoomingEnabled: true,
        panningEnabled: true,
        userPanningEnabled: true,
        selectionType: 'single',
        boxSelectionEnabled: false,
        autolock: false,
        autoungrabify: false,
        autounselectify: false,
        // rendering options:
        headless: false,
        styleEnabled: true,
        hideEdgesOnViewport: false,
        hideLabelsOnViewport: false,
        textureOnViewport: false, //true = higher performance, lower quality
        motionBlur: false,
        wheelSensitivity: 1,
        //pixelRatio: 0.25, //downsample pixels
        //pixelRatio: 0.5,
        pixelRatio: 1,

        initrender: function (evt) { /* ... */ },


        renderer: { /* ... */
            name: 'canvas',
            showFps: false
        },
        container: target[0]
    });


    var s = cytoscape(opt);
    s.overlay = overlaylayer;


    // EdgeHandler: the default values of each option are outlined below:
    var ehDefaults = {
        preview: true, // whether to show added edges preview before releasing selection
        handleSize: 3, // the size of the edge handle put on nodes
        handleColor: "rgba(255, 0, 0, 0.5)", // the colour of the handle and the line drawn from it
        handleLineType: 'ghost', // can be 'ghost' for real edge, 'straight' for a straight line, or 'draw' for a draw-as-you-go line
        handleLineWidth: 1, // width of handle line in pixels
        handleNodes: 'node', // selector/filter function for whether edges can be made from a given node
        hoverDelay: 150, // time spend over a target node before it is considered a target selection
        cxt: true, // whether cxt events trigger edgehandles (useful on touch)
        enabled: true, // whether to start the plugin in the enabled state
        toggleOffOnLeave: true, // whether an edge is cancelled by leaving a node (true), or whether you need to go over again to cancel (false; allows multiple edges in one pass)
        edgeType: function (sourceNode, targetNode) {
            // can return 'flat' for flat edges between nodes or 'node' for intermediate node between them
            // returning null/undefined means an edge can't be added between the two nodes
            return 'flat';
        },
        loopAllowed: function (node) {
            // for the specified node, return whether edges from itself to itself are allowed
            return false;
        },
        nodeLoopOffset: -50, // offset for edgeType: 'node' loops
        nodeParams: function (sourceNode, targetNode) {
            // for edges between the specified source and target
            // return element object to be passed to cy.add() for intermediary node
            return {};
        },
        edgeParams: function (sourceNode, targetNode, i) {
            // for edges between the specified source and target
            // return element object to be passed to cy.add() for edge
            // NB: i indicates edge index in case of edgeType: 'node'
            return {};
        },
        /*
         start: function (sourceNode) {
         // fired when edgehandles interaction starts (drag on handle)
         },
         complete: function (sourceNode, targetNodes, addedEntities) {
         // fired when edgehandles is done and entities are added
         },
         stop: function (sourceNode) {
         // fired when edgehandles interaction is stopped (either complete with added edges or incomplete)
         }
         */
    };

    s.edgehandles( ehDefaults );


    s.channels = { };
    s.listeners = { };
    s.widgets = new Map(); //node -> widget DOM element
    s.currentLayout = {
        name: 'cosefast'
        //name: 'grid' //cose implementation is slow as fuck becaause it has all these log debug statements and string generation that serves no purpose but at least its pretty
        //name: 'arbor'

        //name: 'breadthfirst',
        //circle:true,
        //directed:true
    };

    function wrapInData(d) {
        var w = { data: d };
        if (d.style)
            w.css = d.style;
        else {
            //style defaults?
        }

        return w;
    }

    s.removeNodeWidget = function(node) {
        var nodeID = node.id();
        s.widgets.delete(nodeID);
        $('#widget_' + nodeID).remove();
    };

    s.updateNodeWidget = function(node, nodeOverride) {

        if (nodeOverride) node = nodeOverride;

        var data = node.data();

        //if (!widget) return;

        data.updating = null;

        var wEle = s.widgets.get(node.id());
        if (!wEle) return;

        var widget = data.widget; //html string

        s.positionNodeHTML(node, $(wEle), widget.pixelScale, widget.scale, widget.minPixels);

    };


    /** html=html dom element */
    s.positionNodeHTML = function(node, html, pixelScale, scale, minPixels) {

        pixelScale = parseFloat(pixelScale) || 128.0; //# pixels wide

        var pw = parseFloat(node.renderedWidth());
        var ph = parseFloat(node.renderedHeight());

        scale = parseFloat(scale) || 1.0;

        var cw, ch;
        if (pw < ph) {
            cw = parseInt(pixelScale);
            ch = parseInt(pixelScale*(ph/pw));
        }
        else {
            ch = parseInt(pixelScale);
            cw = parseInt(pixelScale*(pw/ph));
        }

        var h = html[0];


        //get the effective clientwidth/height if it has been resized
        if (( (cw+'px') !== h.style.width ) || ((ch + 'px') !== h.style.height)) {
            var hcw = h.clientWidth;
            var hch = h.clientHeight;

            h.style.width = cw;
            h.style.height = ch;

            cw = hcw;
            ch = hch;
        }

        //console.log(html[0].clientWidth, cw, html[0].clientHeight, ch);

        var pos = node.renderedPosition();

        var globalToLocalW = pw / cw;
        var globalToLocalH = ph / ch;

        var wx = scale * globalToLocalW;
        var wy = scale * globalToLocalH;


        //TODO check extents to determine node visibility for hiding off-screen HTML
        //for possible improved performance

        if (minPixels) {
            var hidden = ('none' === h.style.display);

            if ( Math.min(wy,wx) < minPixels/pixelScale ) {
                if (!hidden) {
                    h.style.display = 'none';
                    return;
                }
            }
            else {
                if (hidden) {
                    h.style.display = 'block';
                }
            }
        }

        //console.log(html, pos.x, pos.y, minPixels, pixelScale);

        var transformPrecision = 4;

        var matb, matc, px, py;
        wx = wx.toPrecision(transformPrecision);
        matb = 0;
        matc = 0;
        wy = wy.toPrecision(transformPrecision);

        //parseInt here to reduce precision of numbers for constructing the matrix string
        //TODO replace this with direct matrix object construction involving no string ops        

        px = (pos.x - (scale*pw) / 2.0).toPrecision(transformPrecision);
        py = (pos.y - (scale*ph) / 2.0).toPrecision(transformPrecision);

        //px = parseInt(pos.x - pw / 2.0 + pw * paddingScale / 2.0); //'e' matrix element
        //py = parseInt(pos.y - ph / 2.0 + ph * paddingScale / 2.0); //'f' matrix element
        //px = pos.x;
        //py = pos.y;
        var tt = 'matrix(' + wx+ ',' + matb + ',' + matc + ',' + wy + ',' + px + ',' + py + ')';
        //nextCSS['transform'] = tt;
        //html.css(nextCSS);        
        h.style.transform = tt;
    };

    s.nodeProcessor = [];

    s.updateNode = function(n) {
        for (var i = 0; i < this.nodeProcessor.length; i++)
            s.nodeProcessor[i].apply(n);
    };

    s.updateChannel = function(c) {


        //TODO assign channel reference to each edge as done above with nodes

        var e = {
            nodes: c.data.nodes ? c.data.nodes.map(wrapInData) : [], // c.data.nodes,
            edges: c.data.edges ? c.data.edges.map(wrapInData) : [] //c.data.edges
        };

        var that = this;
        this.batch(function() {

            suppressCommit = true;

            if (c.data.style) {
                var s = [       ];
                for (var sel in c.data.style) {
                    s.push({
                        selector: sel,
                        css: c.data.style[sel]
                    });
                }

                if (s.length > 0) {
                    //TODO merge style; this will replace it with c's        

                    that.style().clear();

                    that.style().fromJson(s);
                    that.style().update();
                }
            }

            /*
             for (var i = 0; i < e.nodes.length; i++) {
             var n = e.nodes[i];
             if (n.data && n.data.id)
             that.remove('#' + n.data.id);
             }
             */

            that.add( e );

            var channelID = c.id();

            //add position if none exist
            for (var i = 0; i < e.nodes.length; i++) {
                var n = e.nodes[i];
                if (n.data && n.data.id) {
                    var nn = that.nodes('#' + n.data.id);

                    nn.addClass(channelID);

                    that.updateNode(nn);

                    var ep = nn.position();
                    if (!ep || !ep.x) {
                        var ex = that.extent();
                        var cx = 0.5 * (ex.x1 + ex.x2);
                        var cy = 0.5 * (ex.y1 + ex.y2);

                        try {
                            nn.position({x: cx, y: cy});
                        }
                        catch (e) { }
                    }
                }
            }

            that.resize();

            suppressCommit = false;

        });

    };

    /** set and force layout update */
    s.setLayout = function(l){

        if (this.currentLayout)
            if (this.currentLayout.stop)
                this.currentLayout.stop();

        var layout;
        if (l.name) {
            layout = this.makeLayout(l);
        }
        else {
            layout = l;
        }

        this.currentLayout = layout;

        if (layout)
            layout.run();


        //this.layout(this.currentLayout);

        /*if (this.currentLayout.eles)
            delete this.currentLayout.eles;*/

        //http://js.cytoscape.org/#layouts


    };

    s.addChannel = function(c) {

        //var nodesBefore = this.nodes().size();

        this.channels[c.id()] = c;


        this.updateChannel(c);

        var that = this;
        var l;
        c.on('graphChange', l = function(graph, nodesAdded, edgesAdded, nodesRemoved, edgesRemoved) {
            "use strict";

            that.updateChannel(c);
        });
        this.listeners[c.id()] = l;

        if (s.currentLayout)
            this.setLayout(s.currentLayout);

    };

    s.clear = function() {
        for (var c in this.channels) {
            var chan = s.channels[c];
            s.removeChannel(chan);
        }
    };

    s.removeChannel = function(c) {

        c.off("graphChange", this.listeners[c.id()]);

        s.nodes('.' + c.id()).remove();

        //TODO remove style

        delete s.channels[c.id()];
        delete s.listeners[c.id()];
        //c.destroy();

        s.layout();

    };

    s.commit = _.throttle(function() {
        for (i in this.channels) {
            var c = this.channels[i];
            c.commit();
        }
    }, commitPeriodMS);

    // ----------------------

    s.on('cxttapstart', function(e) {
        var target = e.cyTarget;

        if (!target) {
            this.zoomTo();
        }
        else {
            this.zoomTo(target);
        }
    });

    s.on('add', function(e) {

        var node = e.cyTarget;

        var data = node.data();
        var widget = data.widget; //html string
        if (!widget) return;

        var wEle = s.widgets.get(node.id());
        if (wEle) return; //widget already exists?

        /*if (w) {
         var whenLastModified = w.data('when');
         if (whenLastModified < Date.now()) {
         }
         }*/

        var style = widget.style || {};
        style.position = 'fixed';
        style.transformOrigin = '0 0';

        var wid = 'widget_' + node.id();
        var w = $('<div></div>').
            attr('id', wid).
            addClass('widget').
            css(style).
            appendTo(overlaylayer);

        w.html(widget.html).data('when', Date.now());

//            var commitWidgetChange = function (e) {
//                "use strict";
//
//                var oh = w[0].innerHTML;
//
//                //this is probably less efficient than going from DOM to JSON directly
//                var html = html2json(oh);
//
//                if (html !== widget.html) {
//                    widget.html = html;
//
//                    //TODO only commit the channel this node belongs to
//                    that.commit();
//                }
//            };
//
//            //TODO use MutationObservers
//            if (widget.live)
//                w.bind("DOMSubtreeModified DOMAttrModified", commitWidgetChange); // Listen DOM changes

        s.widgets.set(node.id(), w[0]);
    });

    s.on('remove', function(e) {
        var node = e.cyTarget;

        //attempt to remove an associated widget (html div in overlay)
        s.removeNodeWidget(node);
    });

    //DEPRECATED, move to a channel
    s.newNode = function(c, type, pos, param) {

        notify('Adding: ' + type);

        var n = null;
        if (type === 'text') {
            n = {
                id: 'txt' + parseInt(Math.random() * 1000),
                style: {
                    width: 64,
                    height: 32
                },
                widget: {
                    html: "<div contenteditable='true' class='editable' style='width: 100%; height: 100%; overflow:auto'></div>",
                    scale: 0.85,
                    style: {}
                }
            };
        }
        else if (type === 'www') {
            var uurrll = param;
            if (!uurrll.indexOf('http://') === 0)
                uurrll = 'http://' + uurrll;

            n = {
                id: 'www' + parseInt(Math.random() * 1000),
                style: {
                    width: 64,
                    height: 64
                },
                widget: {
                    html: '<iframe width="100%" height="100%" src="' + uurrll + '"></iframe>',
                    scale: 0.85,
                    pixelScale: 600,
                    style: {},
                }
            };
        }

        //            

        if (n) {

            c.addNode(n);

            this.updateChannel(c);

            /*
             if (!pos) {
             var ex = this.extent();
             var cx = 0.5 * (ex.x1 + ex.x2);
             var cy = 0.5 * (ex.y1 + ex.y2);
             pos = {x: cx, y: cy};
             }
             */
            if (pos)
                this.getElementById(n.id).position(pos);

            c.commit();

        }

    };

    //DEPRECATED, move to a channel
    s.removeNode = function(n) {
        for (var ch in this.channels) {
            var c = this.channels[ch];

            if (c.removeNode(n)) {
                c.commit();
            }
        }

        n.remove();
    };



    s.zoomTo = function(ele) {
        var pos;
        if (!ele || !ele.position)
            pos = { x: 0, y: 0 };
        else
            pos = ele.position();


        s.animate({
            fit: {
                eles: ele,
                padding: 120
            }
        }, {
            duration: zoomDuration
            /*step: function() {

             }*/
        });
    };

    return s;
};
