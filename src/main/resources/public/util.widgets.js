"use strict";

//TODO make a class
function TagIndexAccordion(tagIndex) {

    var roots = tagIndex.activateRoots(1);

    var addChild = function (parent, tag) {
        var id = tag.id;
        var label = tag.name || tag.content || id;



        var y = $('<a class="ui item title" style="width: 100%"/>').text(label);

        if (tag.icon) {
            y.prepend($("<img style='display: inline'/>").attr('src', tag.icon));
        }

        var z = $('<div class="ui segment inverted content"/>').attr('tag', id);
        parent.append(y, z);
    }

    var x;

    //http://semantic-ui.com/modules/accordion.html#/settings

    x = $('<div class="ui inverted accordion" style="max-height: 100%; overflow: scroll" />').accordion({

        exclusive: false,

        duration: 150,

        onOpen: function (e, z) {

            //console.log('open',this, e,z);

            var opened = $(this);

            var t = opened.attr('tag');
            var h;


            if (x.newElementHeader)
                h = x.newElementHeader(t);
            else
                h = '';

            opened.html(h); //TODO maybe not remove if not changed; will also remove subtrees and this could be expensive


            if (t) {
                var nodes = [], edges = [];

                tagIndex.graphize(t, 1, nodes, edges);

                //opened.append(JSON.stringify(nodes));

                _.each(nodes, function (c) {
                    if (c.id !== t)
                        addChild(opened, c);
                });
            }
        }
    });


    var update = function (d) {
        d.accordion('refresh');
    }


    _.each(roots, function (r) {
        addChild(x, r);
    });

    /*
     <div class="ui styled accordion">
     <div class="active title">
     <i class="dropdown icon"></i>
     Level 1
     </div>
     <div class="active content">
     Welcome to level 1
     <div class="accordion">
     <div class="active title">
     <i class="dropdown icon"></i>
     Level 1A
     </div>  */
    return x;

}

function newJSONTable(x, withoutKeys, withoutTypes) {

    //http://semantic-ui.com/collections/table.html

    var n = [];
    for (var k in x) {
        var v = x[k];

        if (!v) continue; //ignore falsy

        if (withoutKeys)
            if (_.indexOf(withoutKeys,k)!==-1) continue; //channel will be displayed beneath

        if (withoutTypes)
            if (_.indexOf(withoutTypes, typeof v)!==-1) continue; //ignore functions

        n.push(
            $('<tr>')
                .append('<td>' + k + '</td>')
                .append('<td>' + JSON.stringify(v) + '</td>') );
    }

    if (n.length == 0) return newDiv(); //empty

    return $('<table class="ui celled striped table"/>').append(n);
}

class ChannelSummaryWidget {

    /** meta = channel metadata, data = channel data */
    constructor(id, meta, data, target) {

        target.append('<h1 class="ui header">' + id + '</h1>');

        if (meta) {

            newJSONTable(meta, ['id', 'channel', 'meta'], ['function']).appendTo(target);


        }

        var content = newDivClassed('ui segment inverted').appendTo(target);

        for (var x in data) {
            var d = data[x];

            if (typeof d !== "object") continue; //ignore string entries

            console.log(d);

            var e = newDivClassed(/*'three wide column'*/).appendTo(content);
            //e.append('<h3 class="header">' + x + '</h3>');
            e.append(// class="ui maxHalfHeight"/>',
                //+ JSON.stringify(d, null, 4) + '</pre>');
                newDivClassed("ui segment inverted").append(
                    newJSONTable(d, [], ['function'])
                )
            );
        }
    }
}