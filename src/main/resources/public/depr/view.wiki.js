addView({
    id: 'wiki',
    name: 'Wiki',
    icon: 'icon/view.wiki.svg',
    start: function (v, param) {
        var onWikiTagAdded = function (prototag, e) {

            var tagURI = prototag.id;
            if (!$N.object[tagURI]) {
                //create proto-tag                
                $N.add(prototag);
            }
            var v = newPopupObjectView(tagURI, e, {
                tabs: [
                    {label:'Tag', view: {
                            id: 'wikitag',
                            start: function() {
                                return newWikiTagTagger(prototag);
                            },
                            stop: function() { }
                    }}
                ]
                    
            });

            var l = $('<button class="btn btn-primary" style="float: right" href="#">Read more..</a>');
            l.click(function () {
                v.remove();
                wiki._gotoTag(tagURI);
            });
            v.append(l);
        };

        var wiki = newWikiBrowser(onWikiTagAdded);

        wiki._gotoTag = wiki.gotoTag; //HACK

        if (!param) {
            wiki.gotoTag(configuration.wikiStartPage, false);
        }
        else {
            var search = param.search;
            var target = param.target;
            wiki._gotoTag(target, search);
        }


        var frame = newDiv().attr('class', 'SelfView');
        frame.append(wiki);

        v.append(frame);

        wiki.onURL = function (u) {

            /*var r;
             if (search) {
             r = 'wiki/search/' + encodeURIComponent(page);
             }
             else {
             
             }*/
            u = wiki.wikipage(u);
            var r = 'wiki/' + encodeURIComponent(u);
            $N.router.navigate(r, {trigger: false});

        };

        wiki.gotoTag = function (page, search) {
            wiki._gotoTag(page, search);
        };

        var wikimenu = wiki.menu;
        wikimenu.detach();
        $('#ViewMenu').append(wikimenu);

        frame.onChange = function () {
            //update user summary?
        };
        return frame;

    },
    stop: function () {

    }
});
