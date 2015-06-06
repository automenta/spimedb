function getENWikiURL(t) {
    if (t == null)
        return '#';

    if (t[0] == '_')
        return 'http://en.wiktionary.org/wiki/' + t.substring(1).toLowerCase();
    else
        return 'http://en.wikipedia.org/wiki/' + t;
}


function newWikiBrowser(onTagAdded, options) {
    if (!options)
        options = {};

    var b = newDiv();

    var menu = newDiv();
    menu.addClass('WikiBrowserHeader');


    //var backButton = $('<button disabled><i class="fa fa-arrow-left"></i></button>');    

    var searchInput = $('<input placeholder="Search"/>');
    var searchInputButton = $('<button><i class="fa fa-play"></i></button>');
    searchInput.keyup(function (event) {
        if (event.keyCode == 13)
            searchInputButton.click();
    });
    searchInputButton.click(function () {
        b.gotoTag(searchInput.val(), true);
    });
    menu.append('<button disabled title="Bookmark"><i class="fa fa-star"></i></button>');
    menu.append('<button disabled><i class="fa fa-refresh"></i></button>');
    menu.append(searchInput);
    menu.append(searchInputButton);

    b.append(menu);

    var br = $('<div/>');
    br.addClass('WikiBrowser');


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

    b.menu = menu;

    b.gotoTag = function (t, search) {
        if (!t)
            return;

        loading();

        currentTag = t = b.wikipage(t);

        var url;
        var extractContent;
        if (configuration.wikiProxy) {
            if (search) {
                var tt = t.replace(/ /g, '_'); //hack, TODO find a proper way of doing this
                url = configuration.wikiProxy + 'en.wikipedia.org/w/index.php?search=' + encodeURIComponent(tt);
            }
            else
                url = configuration.wikiProxy + 'en.wikipedia.org/wiki/' + encodeURIComponent(t);
            extractContent = true;
        }
        else {
            url = search ? '/wiki/search/' + encodeURIComponent(t) :
                    '/wiki/' + encodeURIComponent(t) + '/html';
            extractContent = false;
        }


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

            //remove useless content
            _.each(
                    ['#coordinates', '.ambox', '.noprint', '.editlink',
                        '.thumbcaption .magnify', '.mw-editsection', 'siteNotice'],
                    function (x) {
                        br.find(x).remove();
                    }
            );


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
                a.addClass(linkClass);
                a.click(function (e) {
                    if (onTagAdded) {
                        var prototag = {
                            id: 'dbpedia.org/resource/' + target,
                            name: target,
                            extend: []
                        };

                        onTagAdded(prototag, e);
                    }
                    return false;
                });
                return a;
            }

            var linkClass = "link pulse outline-inward";

            function newGotoButton(target) {
                var p = $('<a href="#">▹</a>').addClass(linkClass);
                p.click(function () {
                    b.gotoTag(target);
                    return false;
                });
                return p;
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

