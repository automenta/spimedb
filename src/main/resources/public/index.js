"use strict";


function ready() {

    var VIEW_STARTUP = 'feed';

    var app = new NClient();

    window.app = app; //for console access

    Backbone.Router.extend({

        routes: {
//                "help":                 "help",    // #help
//                "search/:query":        "search",  // #search/kiwis
//                "search/:query/p:page": "search"   // #search/kiwis/p7
        }


//            help: function() {
//
//            },
//
//            search: function(query, page) {
//
//            }

    });
    Backbone.history.start({pushState: true});


    $('#sidebar').append(app.newViewControl());

    /*app.index = new TagIndex(function (i) {

     //called after index has been loaded, but this won't be necessary when events are used

     function newIndex() {
     var t = new TagIndexAccordion(app.index);

     t.newElementHeader = function(tag) {

     //http://codepen.io/thehonestape/pen/yjlGi
     //http://thecodeplayer.com/walkthrough/spicing-up-the-html5-range-slider-input

     var d = newDiv();
     var ii = $('<input class="tagSlider" type = "range" value="0" min="0" max="100" _onchange="rangevalue.value=value"/>');
     ii.change(function(c) {
     app.setFocus(tag, parseInt(ii.val()) * 0.01);
     });
     d.html(ii);
     return d;
     };

     t.addClass('tagIndexAccordion');
     $('#sidebar').append(t);

     }

     newIndex();

     //TODO make this part of TagIndexAccordion when it is refactored as a class
     app.on('index.change', function() {
     $('.' + 'tagIndexAccordion').remove();
     newIndex();
     });

     });
     */

    app.setView(VIEW_STARTUP);

}


class NClient extends EventEmitter {

    constructor() {
        super();

        this.focus = {};
        this.index = {};
        this.views = {

            'map2d': new Map2DView(),
            'map3d': new Map3DView(),
            //'feed': new FeedView(),
            //'graph': new GraphView(),
            //'wikipedia': new WikipediaView('Happiness'),
            'time': new TimeView()

            //    'edit1': new NObjectEditView('New NObject')
            //'space1': new HTMLView('Spaces Test', 'lab', 'space.html')
        };

        /** creates a websocket connection to a path on the server that hosts the currently visible webpage */
        const session = new ReconnectingWebSocket(
            'ws://' + window.location.hostname + ':' + window.location.port + '/session',
            null /* protocols */,
            /*options ||*/ {
                //Options: //https://github.com/joewalnes/reconnecting-websocket/blob/master/reconnecting-websocket.js#L112
                /*
                 reconnectInterval: 1000, // The number of milliseconds to delay before attempting to reconnect.
                 maxReconnectInterval: 30000, // The maximum number of milliseconds to delay a reconnection attempt.
                 reconnectDecay: 1.5, // The rate of increase of the reconnect delay. Allows reconnect attempts to back off when problems persist.
                 timeoutInterval: 2000, // The maximum time in milliseconds to wait for a connection to succeed before closing and retrying.
                 */
            }
        );
        session.onopen = ((e)=>{
            session.send("connecttt");
        });

    }

    data(channel) {

        var d = this.index.tag.node(channel);
        if (d.channel) {
            try {
                //TODO this is a mess:
                return d.channel.subs[channel].data;
            }
            catch (e) {
                console.error('no channel data for channel', channel, e)
                return d;
            }
        }

        return d;
    }


    setView(v, cb) {

        if (this.currentView) {
            this.currentView.stop(this);
            $('#view').remove();
        }

        this.currentView = this.views[v];

        if (this.currentView) {
            var viewTarget = $('<div id="view"></div>');
            $('body').append(viewTarget);

            this.currentView.start(viewTarget, this, cb);
        }

    }

    newViewControl() {
        //http://semantic-ui.com/elements/button.html#colored-group
        /*<div class="large ui buttons">
         <div class="ui button">One</div>
         <div class="ui button">Two</div>
         <div class="ui button">Three</div>
         </div>
         */
        var that = this;

        var d = $('<span/>')
        _.each(that.views, function (v, k) {
            d.append($('<button/>')
            //.append($('<button>' + v.icon + '</button>')
                .append($('<i class="fa fa-' + v.icon + '"></i>'))
                /*.attr('data-content', v.name)
                 .popup()*/
                .click(function () {
                    //setTimeout(function() {
                    that.setView(k);
                    //}, 0);
                }));
        });

        var graphPopup;

        d.append(
            $('<i class="fa fa-adjust"><input type="checkbox"/></i>').click(e => {
                var checked = $(e.target).is(':checked');
                if (checked) {
                    var target = $('<div id="graphpopup"/>').css({
                        position: 'fixed',
                        width: '100%', height: '100%',
                        zIndex: 5000
                    }).appendTo($('#sidebar'));
                    graphPopup = new GraphView();
                    graphPopup.start(target, app);
                } else {
                    if (graphPopup) {
                        graphPopup.stop();
                        $('#graphpopup').remove();
                    }
                }
            })
        );

        return d;

    }

    // /*addTag(tags) {
    //     if (!Array.isArray(tags)) tags = [tags];
    //
    //     for (var i = 0; i < tags.length; i++)
    //         this.index.tag.setNode(tags[i].id, tags[i]);
    //
    //     this.emit('index.change', [tags /* additions */, null /* removals */]);
    // }*/

    spaceOn(bounds, onFocus, onError) {
        //adds a spatial boundary region to the focus
        //console.log('spaceOn', bounds);


        //https://github.com/jDataView/jBinary ?
        const oReq = new XMLHttpRequest();
        oReq.open("GET", bounds.toURL(), true);
        oReq.responseType = "arraybuffer"; //"blob"

        oReq.onload = function (oEvent) {
            const arrayBuffer = oReq.response;
            if (arrayBuffer) {
                const byteArray = new Uint8Array(arrayBuffer);
                onFocus(msgpack.decode(byteArray));
            }
        };
        oReq.send(null);


        /*$.get(bounds.toURL())
         .done(function(s) {
         if (s.length == 0) return;
         try {
         //var p = JSON.parse(s);

         var p = msgpack.decode(s);
         console.log(s.length, p);
         onFocus(p);
         } catch (e) {
         onError(e);
         }
         }).fail(onError);*/
    }
}


// setFocus(tag, amount) {
//     var prevFocus = this.focus[tag] || 0;
//     if (prevFocus === amount) return;
//
//     this.focus[tag] = amount;
//
//     var t = this.index.tag.node(tag);
//
//     var app = this;
//
//     if (prevFocus === 0) {
//         //add focus
//
//         if (t.channel) {
//             //already open??? this shouldnt happen normally
//             console.error('newly focused tag', t, 'already has channel opened');
//         } else if (t.newChannel) {
//             var c = t.newChannel({
//
//                 onOpen: function() {
//
//                 },
//
//                 //TODO onClose ?
//
//                 onChange: function (cc) {
//                     //console.log('change', cc.data);
//                     app.emit( tag + '.change', cc.data);
//                     app.emit('change', this);
//                 }
//             });
//             t.channel = c; //the default, we'll store there
//         }
//
//         console.log('app focus on: ', tag, t, c);
//     }
//     else if (amount == 0) {
//         //remove focus
//         if (t.channel) {
//             if (t.channel.off)
//                 t.channel.off();
//             delete t.channel;
//         }
//         console.log('app focus off: ', tag, t, c);
//         delete this.focus[tag];
//     }
//     else {
//         //change focus
//     }
//
//     console.log('app focus: ', app.focus);
//
//     app.emit('focus'); //focus change
//
// }
