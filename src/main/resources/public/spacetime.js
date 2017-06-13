import Backbone from "backbone-lodash";
import L from "leaflet";

var uiBoundsReactionPeriodMS = 25;

function MAP(me, target) {

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
                const sep = '/';
                $.getJSON(  '/earth/lonlat/rect/' +
                    this.x1.toPrecision(precision) + sep +
                    this.x2.toPrecision(precision) + sep +
                    this.y1.toPrecision(precision) + sep +
                    this.y2.toPrecision(precision) +
                    '/json'
                    ,
                    (x) => me.LOAD(x, 0.5));
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


import('./spime.js').then(S => {


    const ME = S.default();
    ME.facets = $('#overfacets'); //HACK

    import('./lib/edsc-timeline.min.js').then(Timeline => {
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

    });

    MAP(ME, 'map');

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

    new Router();
    Backbone.history.start();
});




