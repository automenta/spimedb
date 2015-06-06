"use strict";

function newWikiBrowser(options) {
    if (!options)
        options = {};

    var b = newDiv('WikiBrowserContainer');
    var header = newDivClassed('WikiBrowserHeader').appendTo(b);
    var headerTitle = newDivClassed('WikiBrowserTitle').appendTo(header);
    var headerMenu = newDivClassed('WikiBrowserMenu').appendTo(header);


    //var backButton = $('<button disabled><i class="fa fa-arrow-left"></i></button>');

    var searchInput = $('<input placeholder="Search"/>');
    var searchInputIcon = $('<i class="search icon"></i>');
    var searchInputArea = $('<div class="ui icon input"/>').append(searchInput, searchInputIcon);

    searchInput.keyup(function (event) {
        if (event.keyCode == 13)
            searchInputIcon.click();
    });
    searchInputIcon.click(function () {
        b.gotoTag(searchInput.val(), true);
    });
    headerMenu.append(
        /*
        '<button disabled title="Bookmark"><i class="fa fa-star"></i></button>',
        '<button disabled><i class="fa fa-refresh"></i></button>',
        */
        searchInputArea
    );


    var br = newDivClassed('WikiBrowser');

    function loading() {
        br.html('Loading...');
    }

    var currentTag = null; //configuration.wikiStartPage;

    b.wikipage = function (t) {
        if (t.indexOf('/') != -1) {
            t = t.substring(t.lastIndexOf('/') + 1, t.length);
        }
        return t;
    }

    b.menu = headerMenu;

    b.gotoTag = function (t, search) {
        if (!t)
            return;

        loading();


        currentTag = t = b.wikipage(t);

        var url;
        var extractContent;
        //if (configuration.wikiProxy) {
        //    if (search) {
        //        var tt = t.replace(/ /g, '_'); //hack, TODO find a proper way of doing this
        //        url = configuration.wikiProxy + 'en.wikipedia.org/w/index.php?search=' + encodeURIComponent(tt);
        //    }
        //    else
        //        url = configuration.wikiProxy + 'en.wikipedia.org/wiki/' + encodeURIComponent(t);
        //    extractContent = true;
        //}
        //else {
            url = search ?
                '/wikipedia/search/' + encodeURIComponent(t) + '/html' :
                '/wikipedia/page/' + encodeURIComponent(t) + '/html';
            extractContent = false;
        //}


        $.get(url, function (d) {


            //HACK rewrite <script> tags so they dont take effect
            d = d.replace(/<script(.*)<\/script>/g, '');

            br.empty().append(d);
            var metaele = br.find('#_meta');
            var metadata = JSON.parse(metaele.text());
            metaele.remove();

            var url = metadata.url;
            currentTag = url.substring(url.lastIndexOf('/') + 1, url.length);

            if (b.onURL)
                b.onURL('dbpedia.org/resource/' + currentTag);

            var heading = br.find('#firstHeading');
            var pageTitle = heading.text();

//            if (extractContent) {
//                //br.find('head').remove();
//                //br.find('script').remove();
//                //br.find('link').remove();
//                if (search) {
//                    //TODO this is a hack of a way to get the acctual page name which may differ from the search term after a redirect happened
//                    var pp = 'ns-subject page-';
//                    var ip = d.indexOf(pp) + pp.length;
//                    var ip2 = d.indexOf(' ', ip);
//                    currentTag = d.substring(ip, ip2);
//                }
//            }
//            else {
//                //WIKIPAGEREDIRECTOR is provided by the node.js server, so don't look for it when using CORS proxy
//                if (search) {
//
//
//                }
//            }


            function setTagButton(a, target) {
                return a.click(function (e) {
                    b.gotoTag(target);
                    return false;
                });
            }

            function newGotoButton(target) {
                return $('<a href="#"><i class="icon setting"></i></a>').click(function () {
                    if (b.onSelected) {
                        var prototag = {
                            id: 'dbpedia.org/resource/' + target,
                            name: target,
                            extend: []
                        };

                        b.onSelected(prototag);
                    }
                    return false;
                });
            }


            br.find('a').each(function () {
                var t = $(this);
                var h = t.attr('href');

                if (h) {
                    if (h[0] == '#') {
                        //override anchor link because this page already uses an anchor for routing
                        t.attr('href', '#');

                        if ($(h).first()) {
                            var target = h + '';
                            t.click(function () {
                                var te = $(target).first();
                                $('html,body,#View').animate({
                                    scrollTop: (te.offset().top) - ($('#View').offset().top)
                                }, 500);
                            });
                        }
                    }
                    else if (h.indexOf('/wiki') == 0) {
                        if (h.indexOf('/wiki/File:') == -1) {

                            t.attr({
                                'title': null,
                                'href': '#'
                            });

                            var target = h.substring(6);

                            setTagButton(t, target);

                            t.after(newGotoButton(target));
                        }
                    }
                    else {
                        t.attr('target', '_blank');
                    }
                }
            });

            //remove opaque table backgrounds
            var tables = br.find('table');
            tables.css('background', 'transparent');
            tables.css('border', 'none');


            //move page header to top section
            var heading = br.find('#firstHeading');
            heading.detach();
            headerTitle.html(heading);

            heading.html(setTagButton($('<a href="#">' + pageTitle + '</a>'), currentTag));
            heading.append(newGotoButton(currentTag));

        });

    }
    if (options.initialSearch) {
        searchInput.val(options.initialSearch);
        b.gotoTag(options.initialSearch, true);
    }
    else {
        if (currentTag != null)
            b.gotoTag(currentTag);
    }

    b.append(br);

    return b;
}

function newSubjectDropdown() {

//<select class="ui search dropdown">
//    <option value="">State</option>
//    <option value="AL">Alabama</option>
    var s = $('<select class="ui search dropdown">');
    s.append('<option value="">Subject</option>');
    s.append('<option value="me" selected>Me</option>');
    s.append('<option value="else">Someone else</option>');
    return s;
}


class WikipediaView extends NView {

    constructor(homepage) {
        super('Wikipedia', 'tasks');
        this.homepage = homepage;
    }

    start(v, app, cb) {

        var goWiki = function(page, search) {

            var wiki = newWikiBrowser({ });

            wiki.onSelected = function onSelected(t) {
                newWikiTagger(t).modal("show");
            };

            wiki.onURL = function (u) {
                //uri = u;
            };

            v.html(wiki);

            wiki.gotoTag(page, search);

        }

        goWiki(this.homepage, false);
    }

    stop(v) {

    }
}


function getENWikiURL(t) {
    if (t == null)
        return '#';

    if (t[0] == '_')
        return 'http://en.wiktionary.org/wiki/' + t.substring(1).toLowerCase();
    else
        return 'http://en.wikipedia.org/wiki/' + t;
}



function newWikiTagTagger(tag) {
    var d = newDiv().addClass('well');

    var tagBar = newTagBar(self, d);
    var saveButton = newTagBarSaveButton(self, tag, tagBar, function () {
        //d.dialog('close');
        //d.empty();
    });
    var otherButton = $('<button class="btn btn-default" title="More tags.."><i class="fa fa-search-plus"></i></button>').click(function (e) {

    });


    /*var cancelButton = $('<button title="Cancel" class="cancelButton"><i class="fa fa-times"></i></button>').click(function () {
     //d.dialog('close');
     d.empty();
     });*/

    var userSelect = newAuthorCombo($N.id());
    userSelect.change(function (v) {
        saveButton.subject(v);
    });
    d.append(newEle('p').append(userSelect));

    d.append($('<div class="quicktag" style="float: left"></div>').append(tagBar));
    d.append($('<div class="save" style="float: right"></div>').append(saveButton, '<br/>', otherButton));
    d.append('<div style="clear: both;"/>');
    return d;
}



//
//function goHome() {
//    resetView();
//
//    uri = '/';
//
//    //$("time.timeago").timeago();
//
//    var log = $('<div></div>').appendTo('#content');
//
//
//    function display(x) {
//        var d = $('<div></div>').addClass('item');
//
//        if (typeof x === "object") {
//            for (var k in x) {
//                if (k === 'id') {
//                    d.append('<h3>' + JSON.stringify(x[k]) + '</h3>');
//                }
//                else {
//                    var e = $('<div/>').addClass('section');
//                    e.append('<h4>' + k + '</h4>');
//                    e.append('<pre>' + JSON.stringify(x[k], null, 4) + '</pre>');
//                    d.append(e);
//                }
//            }
//        }
//        else {
//            d.append('<h3>' + JSON.stringify(x) + '</h3>');
//        }
//
//        //  d.append($('<time>').append($.timeago(new Date())));
//
//        log.prepend(d);
//        return d;
//    }
//
//
//    var publicHandler;
//    bus.registerHandler('say', publicHandler = function (message) {
//        if (!log.is(':visible')) {
//            bus.unregisterHandler('say', publicHandler);
//            return;
//        }
//
//        try {
//            display(JSON.parse(message));
//        }
//        catch (e) {
//            display(message);
//        }
//
//    });
//    later(function () {
//        bus.publish("interest", $N.myself().id);
//    });
//}

//function goTagTable() {
//    resetView();
//
//    var basetag = 'User';
//
//    function update() {
//
//        $.getJSON('object/tag/' + basetag + '/json', function (d) {
//            //$('body').append(JSON.stringify(j));
//
//            $('#content').empty();
//
//            var t = $('<table cellpadding="0" cellspacing="0" border="0" class="display" style="width:100%"></table>');
//            t.appendTo('#content');
//
//
//            var data = [];
//            _.each(d, function (subject) {
//                var name = subject.name;
//                if (subject.out) {
//                    for (var pred in subject.out) {
//                        var objList = subject.out[pred];
//                        for (var i = 0; i < objList.length; i++) {
//                            var obj = objList[i];
//                            data.push([name, pred, obj]);
//                        }
//                    }
//                }
//
//            });
//
//            var table = t.dataTable({
//                'data': data,
//                'columns': [
//                    {'title': '<i class="fa fa-cubes"></i>'/*, class: ''*/},
//                    {'title': '<i class="fa fa-arrows-h"></i>'},
//                    {'title': '<i class="fa fa-cubes"></i>'}
//                    //{ 'title': 'Author' }
//                ],
//                'deferRender': true,
//
//                //http://www.datatables.net/extensions/scroller/examples
//                /*
//                 "scrollY": "200px",
//                 "dom": "frtiS",
//                 'scrollCollapse': true,
//                 */
//                'searching': true,
//                'lengthChange': true,
//                'paging': true,
//                'ordering': true,
//                //'order': [2, 'desc'],
//                '_columnDefs': [
//                    {
//                        'targets': [0],
//                        'visible': true,
//                        'searchable': true
//                    }
//                    /*{
//                     // The `data` parameter refers to the data for the cell (defined by the
//                     // `data` option, which defaults to the column being worked with, in
//                     // this case `data: 0`.
//                     "render": function (data, type, row) {
//                     return '';
//                     },
//                     "targets": 1
//                     },*/
//
//
//                ],
//
//                /*
//                 'createdRow': function(row, data, index ) {
//                 $('td', row).eq(0).html(
//                 newObjectView(data[0], {
//                 scale: 0.5,
//                 depthRemaining: 0,
//                 startMinimized: false,
//                 showAuthorName: false,
//                 transparent: true
//                 })
//                 );
//                 $('td', row).eq(1).html($.timeago(new Date(data[2])));
//                 }
//                 */
//            });
//        });
//    }
//
//    update();
//
//}

//$N.once('bus.start', function () {
//
//    bus.on('say', function (message) {
//
//
//        var c;
//        try {
//            c = JSON.parse(message);
//        }
//        catch (e) {
//            console.error('Unable to parse: ' + message);
//            return;
//        }
//
//        var m = '';
//
//        if (c.id === uri) {
//            if ((c.activity) && (c.activity.in)) {
//                $('#sidebar .activity').remove();
//                var t = $('<div></div>').addClass('activity').appendTo('#sidebar');
//                for (var tag in c.activity.in) {
//                    var a = c.activity.in[tag];
//                    t.append('<h2>' + tag + '</h2>');
//                    t.append('<pre>' + JSON.stringify(a) + '</pre>');
//                }
//            }
//
//            if (c.context) {
//
//                $('#sidebar .related').remove();
//                var t = $('<div></div>').addClass('related').appendTo('#sidebar');
//                t.append('<h2>Related</h2>');
//                for (var k in c.context) {
//                    var count = parseInt(c.context[k]);
//
//                    var label = k;
//                    if (k.indexOf('/') != -1)
//                        label = k.substring(k.lastIndexOf('/') + 1, k.length);
//
//                    var l = $('<a href="#">' + label + '</a>');
//
//                    l.css('font-size', 50.0 * Math.log(1 + count) + '%');
//                    t.append(l, '<br/>');
//                }
//            }
//        }
//
//    });
//
//});

//$N.once('session.start', function () {
//
//
//    $('#GoSelf').click(function () {
//        $N.router.navigate('me', {trigger: true});
//    });
//    $('#GoHome').click(function () {
//        $N.router.navigate('', {trigger: true});
//    });
//    $('#GoWiki').click(function () {
//        $N.router.navigate('wiki/Portal:Current_events', {trigger: true});
//    });
//    $('#GoTagTable').click(function () {
//        $N.router.navigate('tagtable', {trigger: true});
//    });
//
//    if (!$N.myself()) {
//
//        $N.become($N.newUser('Anonymous'));
//        console.log('New ID: ' + $N.myself());
//
//        $N.myself().name = 'Anonymous';
//
//        later(function () {
//            $N.router.navigate('me', {trigger: true});
//        });
//    }
//
//});


//function selfEdit() {
//    resetView();
//
//    var ps = $('#ProfileSelect');
//    ps.show();
//
//    var modal = $('#ProfileSelect .modal-body');
//    if (modal.children().size() === 0) {
//        modal.html(profileWidget());
//    }
//}

//originally from widget.profile.js
function profileWidget() {

    var d = newDiv();


    var nameField = $('<input type="text" placeholder="Name"></input>');
    d.append(nameField).append('<br/>');


    var emailField = $('<input type="text" placeholder="E-Mail (optional)"></input>');
    d.append(emailField).append('<br/>');


    var configuration = { };

    var extraProperties = configuration.newUserProperties;
    if (extraProperties) {
        var extraPropertyInputs = [];
        for (var i = 0; i < extraProperties.length; i++) {
            var e = extraProperties[i];
            var ep = $N.getProperty(e);
            var en = ep ? ep.name : e;
            var ei = $('<input type="text"/>');
            d.append(en, ei, '<br/>');
            extraPropertyInputs.push(ei);
        }
    }


    var geolocate = !($N.myself().geolocation);
    var geo = $N.myself().geolocation;
    var location = ((geo && geo.lat && geo.lon) ? geo : false) || configuration.mapDefaultLocation;
    var locationEnabled = (location !== '');
    var locEnabled = $('<input type="checkbox" checked="true"/>');

    d.append('<br/>').append(locEnabled).append('Location Enabled').append('<br/>');

    var cm = $('<div id="SelfMap"/>').appendTo(d);

    var lmap;
    later(function () {
        lmap = initLocationChooserMap('SelfMap', location, 7, geolocate);
    });

    locEnabled.change(function () {
        locationEnabled = locEnabled.is(':checked');
        if (locationEnabled) {
            cm.show();
        }
        else {
            cm.hide();
        }
    });


    $('#ProfileSave').click(function () {
        var m = $N.myself();
        m.name = nameField.val();
        m.email = emailField.val();

        if (!locationEnabled) {
            m.geolocation = '';
        }
        else {
            if (lmap.location)
                m.geolocation = lmap.location();
            else
                m.geolocation = '';
        }

        objTouch(m);

        $N.pub(m, function (e) {
            notify('Error saving profile: ' + e);
        }, function () {
            notify('Saved profile');
        });

        $N.router.navigate('', {trigger: true});
    });

    nameField.val($N.myself().name);
    emailField.val($N.myself().email);

    return d;
}
