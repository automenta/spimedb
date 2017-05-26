"use strict";


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


    facets = newGrid($('#facets'));

    const map = MAP('map');



    $.get('/logo.html', (x) => {
        setTimeout(() => $('#logo').html(x), 0);
    });





    const qs = $('#query_suggestions');

    const queryText = new QueryPrompt(
        function (suggestions) {
            if (suggestions.length === 0) {
                qs.html('');
            } else {
                setTimeout(() =>
                        qs.html(_.map(JSON.parse(suggestions), (x) =>
                            D('grid-item').append(
                                D('grid-item-content').text(x).click((e) => {
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





//                //http://draggabilly.desandro.com/
//                $('#resultsDragger').draggabilly({
//                    axis: 'x'
//                    
//                }).on( 'dragMove', function( event, pointer, moveVector ) {
//                                        
//                });





    //START ----------------->

    const Router = Backbone.Router.extend({

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



            //$('#query_status').html('').append($('<p>').text('Query: ' + qText));
            //$('#results').html('Searching...');

            ALL(qText, (d) => {
                LOAD(d, 1.0);
            });

        },

        start: function () {


            // setTimeout(() => {
            //
            //     facets.html('');
            //
            //     FACETS({q: '>'}, loadFacets);
            //
            // }, 0);

        }

    });

    new Router();
    Backbone.history.start();


}, false);


