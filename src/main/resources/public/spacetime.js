"use strict";

import Backbone from "backbone-lodash";
import L from "leaflet";
import Spime from "./spime.js";
import Timeline from "./lib/edsc-timeline.min.js";
import Packery from "packery";

const ME = Spime();
const facetsDiv = ME.facets = $('#overfacets'); //HACK


//var timelineRows = new
var $timelines = $('.timeline');
var data1 = {
    start: Date.UTC(2015, 1) / 1000,
    end: Date.UTC() / 1000,
    resolution: 'day',
    intervals: [
        [Date.UTC(2015, 4) / 1000, Date.UTC(2015, 5) / 1000],
        [Date.UTC(2015, 6) / 1000, Date.UTC(2015, 9) / 1000]
    ]
};
var data2 = {
    start: Date.UTC(2015, 1) / 1000,
    end: Date.UTC() / 1000,
    resolution: 'day',
    color: '#ff0000',
    intervals: [
        [Date.UTC(2015, 4) / 1000, Date.UTC(2015, 5) / 1000],
        [Date.UTC(2015, 6) / 1000, Date.UTC(2015, 9) / 1000]
    ]
};
var row1 = {
    id: "examplerow1",
    title: "Example row 1",
    min: Date.UTC(0) / 1000,
    max: Date.UTC() / 1000
};
var row2 = {
    id: "examplerow2",
    title: "Example row 2",
    min: Date.UTC(0) / 1000,
    max: Date.UTC() / 1000,
};
$timelines.on('rangechange.timeline', function (e, start, end) {
    console.log('range change', start, end);
});
$timelines.on('temporalset.timeline', function (e, start, end) {
    console.log('temporal set', start, end);
});
$timelines.on('temporalremove.timeline', function (e) {
    console.log('temporal cleared');
});
$timelines.on('focusset.timeline', function (e, start, end, resolution) {
    console.log('focus set', start, end, resolution);
});
$timelines.on('focusremove.timeline', function (e) {
    console.log('focus cleared');
});
$timelines
    .timeline()
    .timeline('show');
$('#timeline-multiple')
    .timeline('rows', [row1, row2])
    .timeline('data', row1.id, data1)
    .timeline('data', row2.id, data2)
    //.timeline('setTemporal', [[Date.UTC(2015, 1), Date.UTC(2015, 2)]])
    .timeline('setRowTemporal', 'examplerow2', [[Date.UTC(2015, 5), Date.UTC(2015, 6)]])


var mapUpdatePeriodMS = 30;
var mapBoundsPeriodMS = 60;

const map = L.map('map', {
    continuousWorld: true,
    worldCopyJump: true,
    preferCanvas: true,
    renderer: L.canvas()
}).setView([51.505, -0.09], 5);

/** batch map changes */
map.toAdd = new Set();
map.toRemove = new Set();
const mapChange = _.debounce(() => {

    const r = map.toRemove;
    r.forEach(x => x.remove());
    r.clear();

    const a = map.toAdd;
    a.forEach(x => x.addTo(map));
    a.clear();


}, mapUpdatePeriodMS, {
    'leading': true,
    'trailing': false
});
mapChange();

L.tileLayer(
    'http://{s}.tile.osm.org/{z}/{x}/{y}.png'
    //'http://{s}.tile.opentopomap.org/{z}/{x}/{y}.png'
    , {
        //attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);


$('#settings').click(() => {
    $('body').prepend('<iframe id="settings" src="/shell.html" width="100%" height="90%"></iframe>')
});

const Router = Backbone.Router.extend({

    routes: {
        "": "start",
    },

    start: function () {
    }
});


ME.nodeAdded = (nid, n) => {
    const w = n.where();
    if (w) {
        //console.log('add', w);
        map.toAdd.add(w);
        mapChange();
    }
};

ME.nodeRemoved = (nid, n) => {
    const w = n.where();
    if (w) {
        //console.log('remove', w);
        map.toRemove.add(w);
        mapChange();
    }
};


function rectBounds(b, precision = 7) {
    return {
        "x1": b.getWest(),
        "x2": b.getEast(),
        "y1": b.getSouth(),
        "y2": b.getNorth(),
        update: function (each) {
            const sep = '/';
            $.getJSON('/earth/lonlat/rect/' +
                this.x1.toPrecision(precision) + sep +
                this.x2.toPrecision(precision) + sep +
                this.y1.toPrecision(precision) + sep +
                this.y2.toPrecision(precision) +
                '/json',
                each);
        }
    };
}

function LOAD(ss) {


    //setTimeout(() => {

    const results = ss[0]; //first part: search results
    const facets = ss[1]; //second part: facets


    const yy = _.map(results, x => {
        if (!x.I) return;
        const score = x['*'];
        const y = ME.ADD(x);
        if (y) {
            y.activate(score);
        }
        return y;
    });


    //TODO use abstraction
    if (facets) {

        facetsDiv.html('');

        const that = this;
        var newItems = _.map(facets, (v) => {

            const id = v[0]
                .replace(/_/g, ' ')
                .replace(/\-/g, ' ')
            ; //HACK


            const c = new Spime.NIcon(ME.computeIfAbsent(v[0], (v) => {
                return new Spime.NObject({I: id});
            })).scale(v[1]).ele;

            return (Spime.D('grid-item').append(c))[0];

        });

        var nn = $(newItems);
        facetsDiv.append(nn);
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


const updateBounds = _.debounce(() => {

    rectBounds(map.getBounds()).update(LOAD);

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
}, mapBoundsPeriodMS, {
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


new Router();
Backbone.history.start();


//http://leaflet-extras.github.io/leaflet-providers/preview/


//setTimeout(() =>


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

