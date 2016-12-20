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
        this.s = spacegraph(v, {
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
        });
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