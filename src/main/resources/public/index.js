"use strict";

var uiBoundsReactionPeriodMS = 75;
var MEMORY_SIZE = 64;
var ACTIVATION_RATE = 0.5;

var app = {};



const ME = new Map();





$(() => {

    $('#query').w2toolbar({
        name : 'myToolbar',
        items: [
            { type: 'html',  id: 'queryEdit', html: 
                    '<input id="query_text" type="text" placeholder="Search"/><button id="query_update">&gt;</button>'},
            { type: 'check',  id: 'item1', caption: 'Check', img: 'icon-add', checked: true },
            { type: 'break' },
            { type: 'menu',   id: 'item2', caption: 'Drop Down', img: 'icon-folder', 
                items: [
                    { text: 'Item 1', img: 'icon-page' }, 
                    { text: 'Item 2', img: 'icon-page' }, 
                    { text: 'Item 3', img: 'icon-page' }
                ]
            },
            { type: 'break' },
            { type: 'radio',  id: 'item3',  group: '1', caption: 'Radio 1', img: 'icon-page' },
            { type: 'radio',  id: 'item4',  group: '1', caption: 'Radio 2', img: 'icon-page' },
            { type: 'spacer' },
            { type: 'button',  id: 'item5',  caption: 'Item 5', img: 'icon-save' }
        ]
    });
    
    var pstyle = 'background-color: rgba(255,255,255,0.8); border: 1px solid #dfdfdf; padding: 5px;';
    $('#layout').w2layout({
        name: 'dock',
        panels: [
            { type: 'top',  size: 50, resizable: false, style: pstyle, content: '' },
            { type: 'left', size: 200, resizable: true, style: pstyle, content: '' },
            { type: 'main' },
//            { type: 'preview', size: '50%', resizable: true, style: pstyle, content: 'preview' },
            { type: 'right', size: 200, resizable: true, style: pstyle, content: '' },
            { type: 'bottom', size: 150, resizable: true, style: pstyle, content: '' }
        ]
    });
    const dock = w2ui.dock;   
    $('#menu').detach().appendTo(dock.el('left'));
    $('#query').detach().appendTo(dock.el('top'));
    $('#resultsPane').detach().appendTo(dock.el('right'));
    $('#timeline').detach().appendTo(dock.el('bottom'));
    
    

    var clusters = {};

    const map = MAP();


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
                    m = L.polyline(linePath, {color: x.color || 'gray', data: x, title: label}).addTo(map);

                } else if (polygon = x['g*']) {

                    m = L.polygon(polygon, {color: x.polyColor || x.color || 'gray', data: x, title: label}).addTo(map);

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
                        m = L.circleMarker([lat, lon], mm).addTo(map);
                    } else {
                        var latMin = lat[0], latMax = lat[1];
                        var lonMin = lon[0], lonMax = lon[1];


                        mm.fillOpacity = 0.3; //TODO decrease this by the bounds area

                        m = L.rectangle([[latMin, lonMin], [latMax, lonMax]], mm).addTo(map);
                    }



                }

                if (m) {

                    m.on('click', clickHandler);
                    m.on('mouseover', overHandler);
                    m.on('mouseout', outHandler);


                    this.where = m;

                }
            }
        }

    }

    function Where(c) {
        this.component = c;
    }

    function When() { }


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


        if (clusters[tgt] === undefined) {
            clusters[tgt] = [y];
        } else {
            clusters[tgt].push(y);
        }


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

    function ADD(y) {
        var id = y.I;
        if (!y || !id)
            return null;

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
        
        
        a.sort((x,y)=>{
           
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

    $.get('/logo.html', (x) => {
        setTimeout(() => $('#logo').html(x), 0);
    });






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
         else */{
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

    const facets = newGrid($('#facets'));

    const queryText = $('#query_text');

    const qs = $('#query_suggestions');

    const onQueryTextChanged = _.throttle(() => {
        const qText = queryText.val();
        //$('#query_status').html('Suggesting: ' + qText);

        $.get('/suggest', {q: qText}, function (result) {


            if (result.length === 0) {
                qs.html('');
            } else {
                setTimeout(() =>
                    qs.html(_.map(JSON.parse(result), (x) =>
                        DIVclass('grid-item').append(
                            DIVclass('grid-item-content').text(x).click((e) => {
                            queryText.val(x);
                            update(x);
                        })
                            )
                    )),
                    0);
            }
        });
    }, 100, true, true);

    const querySubmit = () => {
        update(queryText.val());
    };

    queryText.on('input', onQueryTextChanged);

    queryText.on('keypress', (e) => {
        if (e.keyCode === 13)
            querySubmit();
    });

    function scrollTop() {
        $("body").scrollTop(0);
    }

    function expand() {
        $('#menu').removeClass('sidebar');
        $('#menu').addClass('expand');
        $('#resultsPane').hide();

        $('#facets .list-item').removeClass('list-item').addClass('grid-item');

    }

    function contract() {
        unfocus();
        $('#resultsPane').addClass('main').show();
        $('#menu').removeClass('expand');
        $('#menu').addClass('sidebar');

        $('#facets .grid-item').removeClass('grid-item').attr('style', '').addClass('list-item');
    }

    function focus(url) {
        Backbone.history.navigate("the/" + encodeURIComponent(url), {trigger: true});
    }

    function unfocus() {
        $('#focus').hide();
        $('#focus').html('');
        $('#menu').removeClass('hide');
        $('#resultsPane').removeClass('sidebar').removeClass('shiftdown').show();
    }

    function suggestionsClear() {
        qs.html('');
    }

    function update(qText) {

        Backbone.history.navigate("all/" + encodeURIComponent(qText), {trigger: true});

    }


    function loadFacets(result) {
        facets.html('');

        var facetButtonBuilder = (v) => {

            const id = v[0]
                .replace(/_/g, ' ')
                .replace(/\-/g, ' ')
                ; //HACK
            const score = v[1];


            const c = $(e('div'))
                .attr('class', 'grid-item-content')
                .text(id).click(() => {
                queryText.val(/* dimension + ':' + */ id);
                querySubmit();
                return false;
            })
                .attr('style',
                    'font-size:' + (75.0 + 20 * (Math.log(1 + score))) + '%');

            return c;
        };

        addToGrid(result, facetButtonBuilder, facets);

        //setTimeout(()=>{

        setTimeout(() => {
            facets.packery('layout');

            setTimeout(() => {
                facets.packery('layout');
            }, 300);

        }, 300);
        //}, 0);
    }


    function SEARCHtext(query, withResult) {
        $.get('/search', {q: query}, withResult);
    }


    //PACKERY.js
    //http://codepen.io/desandro/pen/vKjAPE/
    //http://packery.metafizzy.co/extras.html#animating-item-size

    function updateFacet(dimension, label) {

        const klass = label;

        //            $('#facets.' + klass).remove();
        //
        //            const f = $('<svg width="250" height="250">').attr('class', klass);//.html(label + '...');
        //            $('#facets').append($('<div>').append(f));

        $.get('/facet', {q: dimension}, function (result) {
            setTimeout(() => {
                result = JSON.parse(result);

                /*
                 f.html($('<div>').append(
                 $(d('h3')).text(label),
                 $(d('ul')).append(
                 _.map(result, (v) => {
                 return $(d('li')).append(d('a')).click(()=>{
                 queryText.val(dimension + ':' + id);
                 querySubmit();
                 }).text(id);
                 }))
                 ));
                 */

                //BubblesFacetSVG(f, result);


                loadFacets(result);
            }, 0);

        });

    }

//                //http://draggabilly.desandro.com/
//                $('#resultsDragger').draggabilly({
//                    axis: 'x'
//                    
//                }).on( 'dragMove', function( event, pointer, moveVector ) {
//                                        
//                });


    function LOAD(result, activationRate) {


        setTimeout(() => {
            var ss, rr, ff;
            try {
                ss = JSON.parse(result);
                rr = ss[0]; //first part: search results
                ff = ss[1]; //second part: facets
            } catch (e) {
                //usually just empty search result
                //$('#results').html('No matches for: "' + qText + '"');
                return;
            }

            contract();

            loadFacets(ff);
                       
            _.forEach(rr, x => {
                const score = x.score;
                const y = ADD(x);
                if (y) {
                    y.activate(score * ACTIVATION_RATE * activationRate);                
                }
            });
            
            FORGET(0.9, MEMORY_SIZE);


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


        }, 0);

    }

    function MAP() {

        var map = L.map('map', {
            continuousWorld: true,
            worldCopyJump: true
        }).setView([51.505, -0.09], 5);

        //http://leaflet-extras.github.io/leaflet-providers/preview/
        setTimeout(() =>
            L.tileLayer(
                //'http://{s}.tile.osm.org/{z}/{x}/{y}.png'
                'http://{s}.tile.opentopomap.org/{z}/{x}/{y}.png'
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
                    }, (x)=>{
                        LOAD(x, 0.5);
                    });
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




    //START ----------------->

    app.Router = Backbone.Router.extend({

        routes: {
            "": "start",
            "all/:query": "all",
            "the/:query": "the"
        },

        the: function (url) {

            suggestionsClear();

            url = "/data?I=" + url;

            $('#resultsPane').attr('class', 'sidebar shiftdown');
            $('#menu').attr('class', 'hide');
            $('#focus').attr('class', 'main').html(
                E('iframe').attr('src', url).attr('width', '100%').attr('height', '100%')
                ).show();


        },

        all: function (qText) {

            suggestionsClear();

            contract();

            scrollTop();

            //$('#query_status').html('').append($('<p>').text('Query: ' + qText));
            //$('#results').html('Searching...');

            SEARCHtext(qText, (d) => {
                LOAD(d, 1.0);
            });

        },

        start: function () {


            setTimeout(() => {

                facets.html('');

                expand();

                //updateFacet('I', 'Category');
                updateFacet('>', 'Tag');

            }, 0);

        }

    });

    app.router = new app.Router();

    Backbone.history.start();




}, false);


