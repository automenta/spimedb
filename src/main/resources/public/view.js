"use strict";

/** netention view, abstract interface */
class NView {

    constructor(name, icon) {
        this.name = name;
        this.icon = icon;
    }

    /** start the view in the target container element */
    start(v, cb) {

    }

    /** called before the container is destroyed */
    stop(v) {

    }

}

/* loads a static HTML file via AJAX URL to the view */
class HTMLView extends NView {

    constructor(name, icon, url) {
        super(name, icon);
        this.url = url;
    }

    start(v, cb) {

        //v.append('<iframe src="" style="width:100%;height100%">');

        $.get(this.url)
            .done(function (h) {
                v.html(h);
            })
            .fail(function (err) {
                v.html(err);
            });

    }

    stop(v) {

    }
}





class FeedView extends NView {

    constructor() {
        super("Feeds", "list layout");
    }

    start(v, app, cb) {

        app.on(['focus','change'], this.listener = function(c) {

            //var vv = $('<div class="ui items" style="background-color: white"></div>');

            v.html('');
            v.addClass('ui items maxFullHeight');

            /*
             <div class="ui items">
                 <div class="item">
                     <div class="ui tiny image">
                         <img src="/images/wireframe/image.png">
                     </div>
                     <div class="middle aligned content">
                         <a class="header">12 Years a Slave</a>
                     </div>
                 </div>
             */
            var count = 0;
            for (var c in app.focus) {

                var ii = $('<div class="ui segment inverted" style="background-color: white"></div>').appendTo(v);

                //var jj = $('<div class="middle aligned content"></div>');



                    var chan, meta;
                    try {
                        meta = app.index.tag.node(c);
                        chan = app.data(c);
                    }
                    catch(e) {
                        meta = null;
                        chan = 'empty';
                    }

                    new ChannelSummaryWidget(c, meta, chan, ii);



                count++;
            }

            if (!count)
                v.append('Focus empty');


        });

        this.listener(); //first update
    }

    stop(app) {
        app.off(['focus','change'], this.listener);
    }
}

/** spacegraph (via cytoscape.js) */
class GraphView extends NView {

    constructor() {
        super("Graph", "cubes");
    }

    start(v, app, cb) {

      var degreeScale = function ( node ) { // returns numeric value for each node, placing higher nodes in levels towards the centre
          return 10 + 10 * node.degree();
      };

      var cy = this.s = cytoscape({
          container: v,
          style: [ // the stylesheet for the graph
            {
              selector: 'node',
              style: {
                'color': 'white',
                'shape': 'hexagon',
                'background-color': '#666',
                'label': 'data(N)',
                'width': degreeScale,
                'height': degreeScale,
              }
            },

            {
              selector: 'edge',
              style: {
                'width': 3,
                'line-color': '#ccc',
                'target-arrow-color': '#ccc',
                'target-arrow-shape': 'triangle'
              }
            }
          ],

          layout: {
            name: 'grid'
          }
      });


      var that = this;
      $.getJSON('/tag')
          .done(function(tagMap) {


              cy.batch(()=>{

                _.each(tagMap, function(i) {

                  if (!i)
                    return;

                  i.id = i.I; //HACK


                  cy.add({
                      group: "nodes",
                      data: i
                  });
                  if (i['>']) {
                    _.each(i['>'], function(superTag) {
                      var src = i.id;
                      var tgt = superTag;
                      cy.add({
                          group: 'edges',
                          data: {
                            id: src+"_"+tgt, source: src, target: tgt
                          }
                      });
                    });
                  }



                    //if (i)
                      ///that.updateTag(i);
                });

              });

//              var options = {
//                name: 'cose',
//                idealEdgeLength: 100,
//                nodeOverlap: 20
//              };
//              cy.layout( options );

              var options = {
                name: 'breadthfirst',

                fit: true, // whether to fit the viewport to the graph
                directed: true, // whether the tree is directed downwards (or edges can point in any direction if false)
                padding: 5, // padding on fit
                circle: true, // put depths in concentric circles if true, put depths top down if false
                spacingFactor: 1.25, // positive spacing factor, larger => more space between nodes (N.B. n/a if causes overlap)
                boundingBox: undefined, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
                avoidOverlap: true, // prevents node overlap, may overflow boundingBox if not enough space
                roots: undefined, // the roots of the trees
                maximalAdjustments: 5, // how many times to try to position the nodes in a maximal way (i.e. no backtracking)
                animate: false, // whether to transition the node positions
                animationDuration: 500, // duration of animation in ms if enabled
                animationEasing: undefined, // easing of animation if enabled
                ready: undefined, // callback on layoutready
                stop: undefined // callback on layoutstop
              };
              cy.layout( options );

              //add edges
              /*var nn = that.tag.nodes();
              for (var i = 0; i < nn.length; i++) {
                  var t = that.tag.node(nn[i]);
                  if (!t.inh) continue;

                  for (var j in t.inh) {
                      //var strength = t.inh[j];
                      //that.tag.setEdge(j, t.id);
                  }
              }*/

              /*if (callback)
                  callback(that);*/
          })
          .fail(ajaxFail);



        /*this.s = spacegraph(v, {
            start: function () {

                newSpacePopupMenu(this);

                //s.nodeProcessor.push(new ListToText());
                //s.nodeProcessor.push(new UrlToIFrame());

                var m = newSpacegraphDemoMenu(this);
                m.css('position', 'absolute');
                m.css('right', '0');
                m.css('bottom', '0');
                m.css('z-index', '10000');
                m.css('opacity', '0.8');

                v.append(m);

                if (cb) cb();
            }
        });*/
    }

    stop() {
        if (this.s) {
            this.s.destroy();
            this.s = null;
        }
    }

}



class NObjectEditView extends NView {

    constructor(name) {
        super("Edit: " + name, "edit");
        this.name = name;

    }

    start(v, app, cb) {
        /** see edit.js */
        new NObjectEdit(v, uuid(), this.name);
    }

    stop(v) {
        //ask for save?
    }

}

class TimeView extends NView {

    constructor() {
        super("Time", "calendar");
    }

    start(v, app, cb) {

        $LAB.script('lib/moment/moment.js').wait(function() {
            $LAB
                .script('lib/fullcalendar/fullcalendar.min.js')
                .wait(function () {
                    loadCSS('lib/fullcalendar/fullcalendar.min.css');

                    v.fullCalendar({
                        header: {
                            left: 'prev,next today',
                            center: 'title',
                            right: 'month,basicWeek,basicDay'
                        },
                        defaultDate: '2014-09-12',
                        editable: true,
                        eventLimit: true, // allow "more" link when too many events
                        events: [
                            {
                                title: 'All Day Event',
                                start: '2014-09-01'
                            },
                            {
                                title: 'Long Event',
                                start: '2014-09-07',
                                end: '2014-09-10'
                            },
                            {
                                id: 999,
                                title: 'Repeating Event',
                                start: '2014-09-09T16:00:00'
                            },
                            {
                                id: 999,
                                title: 'Repeating Event',
                                start: '2014-09-16T16:00:00'
                            },
                            {
                                title: 'Conference',
                                start: '2014-09-11',
                                end: '2014-09-13'
                            },
                            {
                                title: 'Meeting',
                                start: '2014-09-12T10:30:00',
                                end: '2014-09-12T12:30:00'
                            },
                            {
                                title: 'Lunch',
                                start: '2014-09-12T12:00:00'
                            },
                            {
                                title: 'Meeting',
                                start: '2014-09-12T14:30:00'
                            },
                            {
                                title: 'Happy Hour',
                                start: '2014-09-12T17:30:00'
                            },
                            {
                                title: 'Dinner',
                                start: '2014-09-12T20:00:00'
                            },
                            {
                                title: 'Birthday Party',
                                start: '2014-09-13T07:00:00'
                            },
                            {
                                title: 'Click for something',
                                url: 'http://.com/',
                                start: '2014-09-28'
                            }
                        ]
                    });
                });
        });
    }

    stop(app) {

    }

}
