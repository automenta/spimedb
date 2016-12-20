"use strict";
function newTagger(options, onFinished, tagRestrictions, maxTags) {
    var selected, inDialog;
    var headerTarget;
    var addImmediately = false;
    var cancelButton = true;
    if (!options) {
        options = {};
    }
    else if (Array.isArray(options)) {
        selected = options;
    }
    else {
        selected = options.selected;
        inDialog = options.inDialog;
        addImmediately = options.addImmediately;
        cancelButton = options.cancelButton;
        headerTarget = options.headerTarget;
    }

    if (!selected)
        selected = [];
    if (!Array.isArray(selected))
        selected = [selected];

    var tags = _.clone(selected) || [];

    var d = newDiv();
    var t = newDiv('TagSelectWidget'); //target for the browser instances
    t.addClass('taggerContent');

    var currentBrowser = null;

    var clearButton = $('<button title="Clear the selected tags">Clear</button>');

    var b = $('<button class="btn-primary"><b>OK</b></button>');
    if (!cancelButton)
        b.hide();

    var tagsCombo = $('<span></span>').attr('id', 'tagsCombo').addClass('well well-sm');
    tagsCombo.update = function () {
        tagsCombo.empty();
        if (tags.length === 0) {
            tagsCombo.hide();
            clearButton.hide();
            b.text('Cancel');
        }
        else {
            tagsCombo.show();
            clearButton.show();
            b.html('<b>Select</b>');

            for (var i = 0; i < tags.length; i++)
                tagsCombo.append(newTagButton(tags[i]));
        }
    };
    tagsCombo.update();

    function onTagAdded(t) {
        if (addImmediately) {
            addImmediately(t);
            return;
        }

        if (maxTags)
            if (tags.length >= maxTags)
                return;

        tags = _.unique([t].concat(tags));

        if (maxTags)
            if (tags.length === maxTags) {
                onFinished(tags);
                return;
            }

        tagsCombo.update();
    }

    function loadBrowser(w) {
        later(function () {
            currentBrowser = w(onTagAdded, selected);
            t.html(currentBrowser);
        });
    }

    var selectBar = $('<div/>').attr('id', 'tagSelectHeader').addClass('taggerHeader');

    var modeFunctions = [];
    function addOption(label, browserFunction) {
        var b = $('<button type="button">' + label + '</button>')
                .appendTo(selectBar)
                .click(function () {
                    loadBrowser(browserFunction);
                });
        modeFunctions.push(browserFunction);
    }
    if (!tagRestrictions) {
        addOption('Index', newTreeBrowser);
        addOption('Wiki', newWikiBrowser);
        addOption('Object', newObjectSelector(null));
        addOption('Who', newObjectSelector($N.class.User));
        addOption('Emotion', newEmotionBrowser);
        addOption('Body', newBodyBrowser);
        addOption('Needs', newNeedsBrowser);
        addOption('&#9733;', newNeedsBrowser); //favorites
    }
    else {
        if (!Array.isArray(tagRestrictions))
            tagRestrictions = [tagRestrictions];

        _.each(tagRestrictions, function (t) {
            if (t === 'Object') {
                addOption('Object', newObjectSelector(null));
            }
            else {
                var T = $N.class[t];
                if (T)
                    addOption(T.name, newObjectSelector(T));
            }

        });
    }

    if (!options.addImmediately) {
        var saveBar = $('<span/>').css('float', 'right');
        tagsCombo.update();
        saveBar.append(tagsCombo);

        clearButton.click(function () {
            if (tags.length)
                if (confirm('Clear selected tags?')) {
                    tags = [];
                    tagsCombo.update();
                }
        });
        saveBar.append(clearButton);

        if (maxTags !== 1) {
            b.click(function () {
                onFinished(tags);
            }).appendTo(saveBar);
        }

        selectBar.append(saveBar);
    }



    if (headerTarget === undefined) {
        later(function () {
            d.parent().before(selectBar);
        });
    }
    else {
        headerTarget.html(selectBar);
    }


    t.attr('style', 'clear: both');
    d.append(t);

    selectBar.find('button').addClass('btn btn-default');

    //default
    if (modeFunctions[0])
        loadBrowser(modeFunctions[0]);

    return d;
}

function newNullBrowser() {
    var e = newDiv();
    e.append('Not available yet');
    return e;
}
function newEmotionBrowser() {
    var e = newDiv();
    var i = $('<img src="/icon/plutchik_emotion_wheel.svg"/>');
    i.click(function () {
        alert('Not available yet.');
    });
    e.append(i);
    return e;
}
function newBodyBrowser() {
    var e = newDiv();
    var i = $('<img src="/icon/human_body_chart.jpg"/>');
    i.click(function () {
        alert('Not available yet.');
    });
    e.append(i);
    return e;
}
function newNeedsBrowser() {
    var e = newDiv();
    var i = $('<img src="/icon/resiliencemaps.org.simple_critical_infrastructure.png"/>');
    i.click(function () {
        alert('Not available yet.');
    });
    e.append(i);
    return e;
}


function newTreeBrowser(onTagAdded, selected) {
    var e = newDiv().addClass('SelfTimeTagTree');

    $('.TagChoice').remove();

    var prefix = 'S_';

    var _onTagAddedFunc = function () {
        onTagAdded($(this).attr('id').substring(prefix.length));
    };

    later(function () {
        newTagTree({
            target: e,
            newTagDiv: function (id, content) {
                var ti = getTagIcon(id) || defaultIcons.unknown;

                return {
                    label: '<button id="' + prefix + id + '" class="TagChoice" style="background-image: url(' + ti + ')">' +
                            content + '</button>'
                };
            },
            onCreated: function () {
                e.find('.TagChoice').each(function (x) {
                    $(this).click(_onTagAddedFunc);
                });
            }
        });

    });

    return e;
}

function newTagTree(param) {
    var a = param.target;
    var onSelectionChange = param.onSelectionChange;
    var addToTree = param.addtoTree;
    var newTagLayerDiv = param.newTagDiv;

    a.empty();


    var tree = newDiv();

    var isGeographic = $('#GeographicToggle').is(':checked');

    var stc;
    if (isGeographic) {
        stc = $N.getTagCount(false, objGeographic);
    } else {
        stc = $N.getTagCount();
    }

    var T = [
        /*        {
         label: 'node1',
         children: [
         { label: '<button>child1</button>' },
         { label: 'child2' }
         ]
         },
         {
         label: 'node2',
         children: [
         { label: 'child3' }
         ]
         } */
    ];


    function subtree(root, i) {
        var name, xi;

        if (i.name) {
            name = i.name;
            xi = i.id;
        } else
            name = xi = i;

        var children = i.subclass || [];

        var label = name;
        if (stc[xi]) {
            if (stc[xi] > 0)
                label += ' (' + _n(stc[xi]) + ')';
        } else {
            /*if (children.length==0)
             return;*/
        }

        var b = newTagLayerDiv(xi, label);

        if (!_.isEmpty(children)) {
            b.children = [];
            _.each(children, function (c, cid) {
                subtree(b.children, c);
            });
        }
        b.id = xi;

        root.push(b);
    }

    function othersubtree(root) {
        var otherFolder = {
            label: 'Other',
            children: []
        };

        var others = [];
        for (var c in stc) {
            if ((!$N.class[c]) && (!$N.property[c]))
                others.push(c);
        }

        if (others.length === 0)
            return;

        others.forEach(function (c) {
            subtree(otherFolder.children, c);
        });
        root.push(otherFolder);
    }

    _.each($N.classRoot, function (c, cid) {
        subtree(T, c);
    });

    othersubtree(T);

    if (addToTree)
        addToTree(T);

    tree.appendTo(a);

    later(function () {
        //a.hide();
        a.tree({
            data: T,
            useContextMenu: false,
            autoEscape: false,
            selectable: false,
            slide: false,
            autoOpen: false
        });

        //autoOpen seems broken in jqtree, so manually open the first level:
        a.find('.jqtree-toggler').click().click();

        //all should be closed now.  now open the first row:
        a.children('ul').children('li').children('div').children('.jqtree-toggler').click();

        if (param.onCreated)
            param.onCreated(a);


    });


    return tree;

}




function newObjectSelector(T) {
    var maxDisplayedObjects = 64;
    var updatePeriodMS = 300;

    return function (onTagAdded, selected) {
        var e = newDiv();
        var keywordFilter = null;

        var keywordInput = $('<input type="text" placeholder="Keywords"/>');
        keywordInput.bind('propertychange keyup input paste', function () {
            keywordFilter = keywordInput.val().toLowerCase();
            if (keywordFilter.length === 0)
                keywordFilter = null;
            update();
        });
        e.append(keywordInput);

        e.append('<hr/>');

        var d = newDiv().appendTo(e);

        function _update() {
            d.empty();

            var tl = T ? $N.objectsWithTag(T, false) : _.keys($N.objects());
            if (keywordFilter) {
                tl = _.filter(tl, function (x) {
                    var O = $N.object[x];
                    if (O.name)
                        return (O.name.toLowerCase().indexOf(keywordFilter) !== -1);
                    return false;
                });
            }

            var ol = tl.slice(0, maxDisplayedObjects);
            if (ol.length < tl.length) {
                d.append('Only displaying the first ' + ol.length + ' objects.  Please narrow the query.<br/><br/>');
            }

            _.each(ol, function (o) {
                var O = $N.getObject(o);
                var s = newObjectView(O, {
                    showActionPopupButton: false,
                    titleClickMode: function (x) {
                        onTagAdded(x);
                    }
                });
                d.append(s);
            });
        }

        var update = _.throttle(_update, updatePeriodMS);
        update();

        return e;
    };
}



function newTagBarSaveButton(s, prototag, tagBar, onSave) {
    var currentTag = prototag.id;
    var currentTagName = prototag.name;

    var saveButton = $('<button class="btn btn-success" title="Save"><i class="fa fa-floppy-o"></i></button>').click(function () {
        if (currentTag === null) {
            alert('Choose a wikitag.');
            return;
        }

        var selTags = _.keys(tagBar.selected);

        if (selTags.length > 0) {
            var id = uuid();
            var o = objNew(id, currentTag);

            o.author = $N.id();
            
            var subject = saveButton.subj || o.author;
            var object =  currentTag;

            var predicates = [];
            for (var i = 0; i < selTags.length; i++) {
                var predicate = selTags[i];
                predicates.push(predicate);
            }
            
            o.value = { "g": [[subject, predicates, object ]] };
            
            o.name = predicates + ': ' + currentTagName;            

            $N.pub(o, function (err) {
                notify({
                    title: 'Error saving:',
                    text: err,
                    type: 'error'
                });
            }, function () {
                $N.notice(o);
                notify({
                    title: 'Saved',
                    text: currentTag
                });
            });

            if (onSave)
                onSave();
            
        } else {
            alert('Choose 1 or more tags to combine with the wikitag.');
        }
    });

    saveButton.subject = function (s) {
        saveButton.subj = s;
    };

    return saveButton;
}

function newTagToggleButton(t, selection) {
    var l = $(newEle('li'));

    var b = newTagButton(t, function () {
        b.checked = !b.checked;

        if (b.checked) {
            selection[t] = true;
            l.addClass('active');
        }
        else {
            delete selection[t];
            l.removeClass('active');
        }

    }).addClass('TagToggleButton btn-s').appendTo(l);

    b.checked = false;

    return l;
}

/** speed-dial selector for predicates of a relation to an object */
function newTagBar(s, object) {
    var tagBar = newDiv();
    var s = {};
    tagBar.selected = s;

    return tagBar.append(
            newEle('p').append($('<ul class="nav nav-pills"/>').append(
            newTagToggleButton('Learn', s),
            newTagToggleButton('Do', s),
            newTagToggleButton('Teach', s))),
            newEle('p').append($('<ul class="nav nav-pills"/>').append(
            newTagToggleButton('Can', s),
            newTagToggleButton('Need', s),
            newTagToggleButton('Not', s))));
}

