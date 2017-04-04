"use strict";

var MEMORY_SIZE = 512;
var ACTIVATION_RATE = 0.5;

var app = {};


const ME = new Map();

$(() => {

    $('#query').w2toolbar({
        name: 'myToolbar',
        items: [
            {
                type: 'html',
                id: 'queryEdit',
                html: '<input id="query_text" type="text" placeholder="Search"/><button id="query_update">&gt;</button>'
            },
            {type: 'check', id: 'item1', caption: 'Check', img: 'icon-add', checked: true},
            {type: 'break'},
            {
                type: 'menu', id: 'item2', caption: 'Drop Down', img: 'icon-folder',
                items: [
                    {text: 'Item 1', img: 'icon-page'},
                    {text: 'Item 2', img: 'icon-page'},
                    {text: 'Item 3', img: 'icon-page'}
                ]
            },
            {type: 'break'},
            {type: 'radio', id: 'item3', group: '1', caption: 'Radio 1', img: 'icon-page'},
            {type: 'radio', id: 'item4', group: '1', caption: 'Radio 2', img: 'icon-page'},
            {type: 'spacer'},
            {type: 'button', id: 'item5', caption: 'Item 5', img: 'icon-save'}
        ]
    });

    var pstyle = 'background-color: rgba(127,127,127,0.5); border: 1px solid #dfdfdf; padding: 5px;';
    $('#layout').w2layout({
        name: 'dock',
        panels: [
            {type: 'top', size: 50, resizable: false, style: pstyle, content: ''},
            {type: 'left', size: 200, resizable: true, style: pstyle, content: ''},
            {type: 'main'},
//            { type: 'preview', size: '50%', resizable: true, style: pstyle, content: 'preview' },
            {type: 'right', size: 200, resizable: true, style: pstyle, content: ''},
            {type: 'bottom', size: 150, resizable: true, style: pstyle, content: ''}
        ]
    });
    const dock = w2ui.dock;
    $('#menu').detach().appendTo(dock.el('left'));
    $('#query').detach().appendTo(dock.el('top'));
    $('#resultsPane').detach().appendTo(dock.el('right'));
    $('#timeline').detach().appendTo(dock.el('bottom'));


    var clusters = {};

    const map = MAP('map',
        (x) => {
            LOAD(x, 0.5);
        }
    );


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

    const facets = newGrid($('#facets'));


    const qs = $('#query_suggestions');

    const queryText = new QueryPrompt(
        function (suggestions) {
            if (suggestions.length === 0) {
                qs.html('');
            } else {
                setTimeout(() =>
                        qs.html(_.map(JSON.parse(suggestions), (x) =>
                            DIVclass('grid-item').append(
                                DIVclass('grid-item-content').text(x).click((e) => {
                                    queryText.val(x);
                                    update(x);
                                })
                            )
                        )),
                    0);
            }
        },
        function (result) {

        }
    );

    queryText.submit = () => {
        update(queryText.val()); //intercept this
    };


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
                    Backbone.history.navigate("all/" + id);

                    //querySubmit();

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


    //PACKERY.js
    //http://codepen.io/desandro/pen/vKjAPE/
    //http://packery.metafizzy.co/extras.html#animating-item-size

    function updateFacet(dimension, label) {

        const klass = label;

        //            $('#facets.' + klass).remove();
        //
        //            const f = $('<svg width="250" height="250">').attr('class', klass);//.html(label + '...');
        //            $('#facets').append($('<div>').append(f));

        FACETS({q: dimension}, function (result) {

            if (!result || result.length === 0)
                return;

            try {
                result = JSON.parse(result);
            } catch (e) {
                console.error(e, result);
                return;
            }

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


