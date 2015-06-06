"use strict";


function newPopupObjectView(obj, popupParam, objectViewParam) {
    var x;
    if (typeof(obj) === 'string')
        x = $N.object[obj];
    else
        x = obj;

    if (!x) {
        notify('Unknown object: ' + obj);
        return;
    }

    if (objectViewParam === undefined) {
        objectViewParam = {
            depthRemaining: 4,
            nameClickable: false,
            showName: false,
            showAuthorIcon: false
        };
    }

    return newPopup(x.name, popupParam).append( 
            newObjectView(x, objectViewParam).css('border', 'none') 
    );
}


function newChannelWidget(objectURI) {
    var channel = objectURI + '_channel';
    var h;

    var chat = newChatWidget(function onSend(x) {        
        //console.log('send: ' , JSON.stringify(x));
        bus.publish(channel, x);
    }, { });
        
    bus.registerHandler(channel, h = function(x) {
        //console.log('receive: ' , JSON.stringify(x));
        chat.receive(x);
    }); 
        
    chat.on("remove", function () {
        chat.send('(exit)');
        bus.unregisterHandler(channel, h);        
    });
    chat.send('(enter)');
    return chat;
}

function newPopupObjectViews(objectIDs, e) {
    if (objectIDs.length === 0)
        return;
    if (objectIDs.length === 1)
        return newPopupObjectView(objectIDs[0], e);

    var objects = objectIDs.map(function(i) {
        if (typeof i === 'string')
            return $N.getObject(i);
        else
            return i;
    });

    var maxDisplayableObjects = 64;

    var e = newDiv();
    var displayedObjects = 0;
    _.each(objects, function(o) {
        if (displayedObjects === maxDisplayableObjects) {
            return;
        }

        if (o) {
            e.append(newObjectView(o, {
                depthRemaining: 0
            }));
            displayedObjects++;
        }

        if (displayedObjects === maxDisplayableObjects) {
            e.prepend('WARNING: Too many objects selected.  Only showing the first ' + maxDisplayableObjects + '.<br/>');
        }
    });
    return newPopup(displayedObjects + ' Object' + ((displayedObjects > 1) ? 's' : ''), true).append(e);

}


function newAvatarImage(s) {
    return newDiv().attr({
        'class': 'AvatarIcon',
        'style': 'background-image:url(' + getAvatarURL(s) + ')',
        'title': (s ? s.name : '')
    });
}




//(function is globalalized for optimization purposes)
function _onTagButtonClicked() {
    var ti = $(this).attr('taguri');
    var t = $N.class[ti] || $N.instance[ti];
    if (t)
        newPopupObjectView(t, true);
    return false;
}


function newTagImage(ti) {
    return newEle('img').attr({
        'src': ti,
        'class': 'TagButtonIcon'
    });
}

function newTagButton(t, onClicked, isButton, dom) {
    var ti = null;
    
    if (t) {
        if (!t.id) {
            var to = $N.class[t];
            if (to)
                t = to;
			else {
				//try if instance
				var tii = $N.instance[t];
				if (tii)
					t = tii;
			}
        }
        if (t.id) {
            ti = getTagIcon(t.id);
        }
    }
    if (!ti)
        ti = getTagIcon(null);

    var b;
    if (isButton) {
        b = newEle('button', true);
    }
    else {
        b = newEle('a', true);
    }

    b.setAttribute('class', 'tagLink pulse outline-inward');
    b.style.backgroundImage = 'url(' + ti + ')';

    if (t && (t.name||t.id)) {
        b.innerHTML = t.name || t.id;
        b.setAttribute('taguri', t.id || (t + ''));
    }
    else if (t) {
        b.innerHTML = t.id || t;
    }

    if (!onClicked)
        onClicked = _onTagButtonClicked;

    b.onclick = onClicked;

    if (dom)
        return b;
    return $(b);
}

function newReplyWidget(onReply, onCancel) {
    var w = newDiv();
    w.addClass('ReplyWidget');

    var ta = $('<textarea/>');
    w.append(ta);

    var bw = $('<div style="text-align: left"></div>');
    w.append(bw);

    var c = $('<button>Cancel</button>');
    c.click(function() {
        var ok;
        if (ta.val().length > 0) {
            ok = confirm('Cancel this reply?');
        } else {
            ok = true;
        }

        if (ok)
            onCancel();
    });
    bw.append(c);

    var b = $('<button>Reply</button>');
    b.click(function() {
        if (ta.val().length > 0) {
            onReply(ta.val());
        }
    });
    bw.append(b);

    return w;
}


function getTagStrengthClass(s) {
    if (s === 0.0)
        return 'tag0';
    else if (s <= 0.25)
        return 'tag25';
    else if (s <= 0.50)
        return 'tag50';
    else if (s <= 0.75)
        return 'tag75';
    else
        return 'tag100';
}

function applyTagStrengthClass(e, s) {
    e.addClass(getTagStrengthClass(s));
}





function newReplyPopup(x, onReplied) {
    x = $N.instance[x];

    var pp = newPopup('Reply to: ' + x.name);

    function closeReplyDialog() {
        pp.dialog('close');
    }

    pp.append(newReplyWidget(
        //on reply
        function(text) {

            closeReplyDialog();

            var rr = {
                name: text,
                id: uuid(),
                value: [],
                author: $N.id(),
                replyTo: [x.id],
                createdAt: Date.now()
            };

            $N.pub(rr, function(err) {
                notify({
                    title: 'Error replying (' + x.id.substring(0, 6) + ')',
                    text: err,
                    type: 'Error'
                });
            }, function() {
                //$N.notice(rr);
                notify({
                    title: 'Replied (' + x.id.substring(0, 6) + ')'
                });
            });

            if (onReplied)
                onReplied(rr);

        },
        //on cancel
        function() {
            closeReplyDialog();
        }
    ));

}

function newSimilaritySummary(x) {
    var s = {};
    var count = 0;
    for (var i = 0; i < x.value.length; i++) {
        var v = x.value[i];
        if (v.id === 'similarTo') {
            s[v.value] = v.strength || 1.0;
            count++;
        }
    }
    if (count === 0)
        return newDiv();


    function newSimilarityList(X) {
        var d = newEle('ul');
        _.each(X.value, function(v) {
            if (v.id == 'similarTo') {
                var stf = v.strength || 1.0;
                var st = parseFloat(stf * 100.0).toFixed(1);
                var o = $N.getObject(v.value);
                var name = o ? o.name : '?';
                var li = $('<li></li>').appendTo(d);
                var lia = $('<a>' + _s(name, 32, true) /*+ ' (' + st + '%) */ + '</a>').appendTo(li);
                li.append('&nbsp;');
                lia.click(function(e) {
                    newPopupObjectView(v.value, e);
                });
                lia.css('opacity', 0.5 + (0.5 * stf));
            }
        });
        return d;
    }

    function newSimilarityAreaMap(s) {
        var d = newDiv().css('clear', 'both');

        var width = 100;
        var height = 100;

        var treemap = d3.layout.treemap()
                .size([width, height])
                //.sticky(true)
                .value(function(d) {
                    return d.size;
                });

        var div = d3.selectAll(d.toArray())
                .style('position', 'relative')
                .style('width', (width) + '%')
                .style('height', '10em')
                .style('left', 0 + 'px')
                .style('top', 0 + 'px');

        var data = {
            name: '',
            children: [
            ]
        };
        _.each(s, function(v, k) {
            var o = $N.getObject(k);
            if (o)
                data.children.push({
                    id: o.id,
                    name: o.name,
                    size: v
                });
        });

        var color = d3.scale.category20c();

        var node = div.datum(data).selectAll('.node')
                .data(treemap.nodes)
                .enter().append('div')
                .attr('class', 'node')
                .style('position', 'absolute')
                .style('border', '1px solid gray')
                .style('overflow', 'hidden')
                .style('cursor', 'crosshair')
                .style('text-align', 'center')
                .call(position)
                .on('click', function(d) {
                    newPopupObjectView(d.id);
                })
                .style('background', function(d) {
                    return color(d.name);
                })
                .text(function(d) {
                    return d.children ? null : d.name;
                });

        d3.selectAll('input').on('change', function change() {
            var value = this.value === 'count' ? function() {
                return 1;
            } : function(d) {
                return d.size;
            };

            node
                    .data(treemap.value(value).nodes).call(position);
            /*		  	.transition()
             .duration(1500)
             .call(position);*/
        });

        function position() {
            this.style('left', function(d) {
                return (d.x) + '%';
            })
                    .style('top', function(d) {
                        return (d.y) + '%';
                    })
                    .style('width', function(d) {
                        return d.dx + '%';
                    })
                    .style('height', function(d) {
                        return d.dy + '%';
                    });
        }
        return d;
    }


    var g = newDiv().addClass('SimilaritySection');

    var e = newDiv().css('float', 'left');
    var eb = $('<button title="Similarity"></button>').appendTo(e);
    eb.append(newTagButton('Similar').find('img'));


    g.append(e);

    var h = newSimilarityList(x);

    g.append(h);


    var areaMap = null;
    eb.click(function() {
        if ((!areaMap) || (!areaMap.is(':visible'))) {
            if (!areaMap) {
                areaMap = newSimilarityAreaMap(s);
                h.append(areaMap);
            }
            areaMap.show();
        } else {
            areaMap.hide();
        }
        reflowView();
        return false;
    });

    return g;
}


//static global functions for optimization
function _refreshActionContext() {
    refreshActionContext();
    return true;
}
function _objectViewEdit() {
    var xid = $(this).parent().attr('xid') || $(this).parent().parent().attr('xid') || $(this).parent().parent().parent().attr('xid');
    var x = $N.object[xid];
    var windowParent = $(this).parent().parent().parent().parent();
    if (windowParent.hasClass('ui-dialog-content')) {
        windowParent.dialog('close');
    }
    newPopupObjectEdit(x, true);
    return false;
}

function _objectViewContext() {
    var that = $(this);
    var xid = $(this).parent().parent().parent().attr('xid');
    var x = $N.object[xid];

    //click the popup menu button again to disappear an existing menu
    if (that.parent().find('.ActionMenu').length > 0)
        return closeMenu();

    var popupmenu = that.popupmenu = newContextMenu([x], true, closeMenu).addClass('ActionMenuPopup');

    function closeMenu() {
        that.parent().find('.ActionMenu').remove();
        return false;
    }


    var closeButton = $('<button>Close</button>')
            .click(closeMenu)
            .appendTo(that.popupmenu);

    $(this).after(that.popupmenu);
    return false;
}

function _addObjectViewPopupMenu(authored, target) {
    if (authored) {
        newEle('button').text('+').attr({
            title: 'Edit',
            class: 'btn btn-default ObjectViewPopupButton'
        }).click(_objectViewEdit).appendTo(target);
    }

    newEle('button').html('&gt;').attr({
        title: 'Actions...',
        class: 'btn btn-default ObjectViewPopupButton'
    }).appendTo(target).click(_objectViewContext);
}


var subjectTag = {
    'Like': { objSuffix: '_Likes', objTag: 'Value', objProperty: 'value', objName: 'Likes' },
    'Dislike': { objSuffix: '_Dislikes', objTag: 'Not', objProperty: 'not', objName: 'Dislikes'},
    'Trust': { objSuffix: '_Trusts', objTag: 'Trust', objProperty: 'trust', objName: 'Trusts' }
};

function _newSubjectTagButtonClick() {
    var data = $(this).data();

    var x = $N.instance[$(this).parent().data('id')];
    if (!x) return;
    

    var defaultLikesID = $N.id() + data.objSuffix;
    var defaultLikes = $N.instance[defaultLikesID];

    defaultLikes = new $N.nobject(defaultLikesID, data.objName + ': ' + objName(x), data.objTag);
    defaultLikes.author = $N.id();
    defaultLikes.value.g = [ [ $N.id(), data.objTag, x.id ] ];

    $N.pub(defaultLikes, null, function(x) {
		notify({
			title: data.objTag,
			text: 'Saved'
		});
	});

    return false;
}

function newSubjectTagButton(buttonTitle, icon, params) {
    return newEle('a').attr('title', buttonTitle)
			.html('<i class="fa ' + icon + '"></i>')
            .data(params)
            .click(_newSubjectTagButtonClick);
}


function addNewObjectDetails(x, d, excludeTags) {
    if (x.value) {
        for (var i = 0; i < x.value.length; i++) {
            var t = x.value[i];

            if (excludeTags)
                if (excludeTags.indexOf(t.id) !== -1)
                    continue;

            if (x._class) {
                t = $N.object[t];
            }
            else {
                if ($N.class[t.id])
                    continue;   //classes are already shown in metadata line
                if (!$N.property[t.id] && !isPrimitive(t.id))
                    continue;   //non-property tags are like classes, shown in metadata line
            }

            d.append(newTagValueWidget(x, i, t, false));
        }
    }
}


function newSpaceLink(spacepoint) {
    var lat = _n(spacepoint.lat);
    var lon = _n(spacepoint.lon);
    var mll = objSpacePointLatLng($N.myself());

    var spacelink = newEle('a', true);

    var text = '';
    if (mll) {
        var dist = '?';
        //TODO check planet
        var sx = [spacepoint.lat, spacepoint.lon];
        if (mll)
            dist = geoDist(sx, mll);

        if (dist === 0)
            text = ('[ Here ]');
        else
            text = ('[' + _n(lat, 2) + ',' + _n(lon, 2) + ':  ' + _n(dist) + ' km away]');
    } else {
        text = ('[' + _n(lat, 2) + ',' + _n(lon, 2) + ']');
    }

    spacelink.innerHTML = text;

    return spacelink;
}


/*
function ISODateString(d) {
    function pad(n) {
        return n < 10 ? '0' + n : n;
    }
    return d.getUTCFullYear() + '-' + pad(d.getUTCMonth() + 1) + '-' + pad(d.getUTCDate()) + 'T' + pad(d.getUTCHours()) + ':' + pad(d.getUTCMinutes()) + ':' + pad(d.getUTCSeconds()) + 'Z';
}
*/

function newMetadataLine(x, showTime) {
    var mdline = newDiv().addClass('MetadataLine');

    var e = [];

    var ots = objTagStrength(x, false);
    for (var t in ots) {
        var s = ots[t];
        if ($N.property[t])
            continue;

        if (t === x.author)
            continue;
        
        
        var tt = $N.class[t];

        var taglink = newTagButton(tt || t, false, undefined, true);
        taglink.classList.add(getTagStrengthClass(s));

        e.push(taglink);
        e.push(' ');
    }

    var spacepoint = objSpacePoint(x);
    if (spacepoint) {
        e.push(newSpaceLink(spacepoint));
        e.push(' ');
    }

    if (showTime !== false) {
        var ww = objWhen(x);
        if (ww) {
            //e.push(newEle('a').append($.timeago(new Date(ww))));
            e.push($.timeago(new Date(ww)));
        }
    }

    if ($N.dgraph.hasNode(x.id)) { 
        var numIn = $N.dgraph.inEdges(x.id);
        var numOut = $N.dgraph.outEdges(x.id);
        if ((numIn.length > 0) || (numOut.length > 0)) {
            e.push(' ');
        }

        if (numIn.length > 0) {
            e.push(newA('&Larr;' + numIn.length, 'In links'));
        }

        if (numOut.length > 0) {
            if (numIn.length > 0)
                e.push('|');
            e.push(newA(numOut.length + '&Rarr;', 'Out links'));
        }
    }

    mdline.append(e);
    return mdline;
}

function newA(html, title, func) {
    var n = newEle('a', true);
    if (html)
    n.innerHTML = html;
    if (title)
        n.setAttribute('title', title);
    if (func)
        n.onclick = func;
    return n;
}
