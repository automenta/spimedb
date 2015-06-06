"use strict";


function newPopupObjectEdit(n, p) {
    var popup = newPopup('', p);
    return newObjectEdit(popup, n, true);
}


function newObjectEdit(target, ix, editable) {
    return newObjectEdit2(target, ix, editable);
}

function newObjectEdit2(D, ix, editable) {
    if (typeof ix === 'string')
        ix = $N.object[ix];


    var edit = newDiv().addClass('object_edit_content').attr({
        'contenteditable': true,
        'autofocus': true
    }).appendTo(D);

    edit.popline({position: "relative"});


    var menuWrap = newDiv().addClass('nav object_edit_menu').appendTo(D);

    //TODO editable mode of Metadata line for editing tags directly
    newMetadataLine(ix, false).appendTo(menuWrap);

    //load from object into edit area
    if (ix.value && ix.value.html)
        edit.html( ix.value.html);
    else if (ix.name)
        edit.html( ix.name );

        
    function getTitle() {
        return $(edit[0].firstChild).text().trim();
    }
    
    function getEditedFocus() {
        if (edit && edit.children() && (edit.children().length > 0) ) {
            ix.name = getTitle();;
            if (!ix.value) ix.value = { };
            //TODO avoid repeating name in description by comparing common prefix
            ix.value.html = edit.html();
        }
        
        return ix;
    }
    
    function insertTag(t) {
        if (!getTitle()) edit.append('?<br/>');
        else if (edit.children().length < 2) edit.append('<br/>');
        
        var x = newDiv().attr('contenteditable', 'false').css('display', 'inline').append(newTagButton(t));
        edit.append('&nbsp;', x, '&nbsp;');
        
        /*
         if(document.getSelection){
            var range = document.getSelection().getRangeAt(0);
            var nnode = document.createElement("b");
            range.surroundContents(nnode);
            nnode.innerHTML = "Some bold text";
        }
        */
    }
    

    /*if ((hideWidgets !== true) && (!x.readonly))*/ {


        var addButtons = newEle('span').appendTo(menuWrap);

        var whatButton = $('<button title="What?"><i class="fa fa-plus-square"/></button>').click(function (e) {
            var p;
            var taggerOptions;
            p = newPopup('Select Tags', e);
            taggerOptions = [];

            var tagger = newTagger(taggerOptions, function (t) {
                var y = getEditedFocus();
                for (var i = 0; i < t.length; i++) {
                    var T = $N.getTag(t[i]);
                    if ((T) && (T.reserved)) {
                        notify('Tag "' + T.name + '" can not be added to objects.');
                    } else {
                        //y = objAddTag(y, t[i]);
                    }
                    insertTag(T);
                }
                //update(y);

                if (p && p.dialog)
                    p.dialog('close');
            });


            p.append(tagger);

        });

        var whenButton = $('<button title="When?" id="AddWhenButton" ><i class="fa fa-clock-o"/></button>').click(function () {
            update(objAddValue(getEditedFocus(), 'timepoint', ''));
        });

        var whereButton = $('<button title="Where?"><i class="fa fa-map-marker"/></button>').click(function () {
            update(objAddValue(getEditedFocus(), 'spacepoint', {}));
        });

        var whoButton = $('<button disabled title="Who?" id="AddWhoButton"><i class="fa fa-user"/></button>');

        var drawButton = $('<button title="Draw"><i class="fa fa-pencil"/></button>').click(function () {
            update(objAddValue(getEditedFocus(), 'sketch', ''));
        });

        var webcamButton = $('<button title="Webcam"><i class="fa fa-camera"/></button>').click(function () {
            newWebcamWindow(function (imgURL) {
                update(objAddValue(getEditedFocus(), 'image', imgURL));
            });
        });

        var uploadButton = $('<button title="Add Media (Upload or Link)"><i class="fa fa-file-picture-o"/></button>').click(function () {


            function attachURL(url) {
                if (url.endsWith('.png') || url.endsWith('.jpeg') || url.endsWith('.jpg') || url.endsWith('.svg') || url.endsWith('.gif')) {
                    update(objAddValue(getEditedFocus(), 'image', url));
                }
                else {
                    update(objAddValue(getEditedFocus(), 'url', url));
                }

                later(function () {
                    x.dialog('close');
                });
            }

            var y = newDiv();

            var fuf = $('<form id="FocusUploadForm" action="/upload" method="post" enctype="multipart/form-data">File:</form>').appendTo(y);
            var fileInput = $('<input type="file" name="uploadfile" />').appendTo(fuf);
            fuf.append('<br/>');
            var fileSubmit = $('<input type="submit" value="Upload" />').hide().appendTo(fuf);

            fileInput.change(function () {
                if (fileInput.val().length > 0)
                    fileSubmit.show();
            });

            var stat = $('<div class="FocusUploadProgress"><div class="FocusUploadBar"></div><div class="FocusUploadPercent">0%</div></div><br/><div id="FocusUploadStatus"></div>').appendTo(y).hide();

            y.append('<hr/>');

            var mediaInput = $('<input type="text" placeholder="Image or Video URL"/>').appendTo(y);
            var mediaButton = $('<button>Attach</button>').appendTo(y).click(function () {
                attachURL(mediaInput.val());
            });


            y.append('<hr/>');
            var okButton = $('<button class="btn">Cancel</button>').appendTo(y);


            var x = newPopup('Add Media', {
                modal: true,
                width: '50%'
            });
            x.append(y);

            okButton.click(function () {
                x.dialog('close');
            });

            var bar = $('.FocusUploadBar');
            var percent = $('.FocusUploadPercent');
            var status = $('#FocusUploadStatus');

            $('#FocusUploadForm').ajaxForm({
                beforeSend: function () {
                    status.empty();
                    var percentVal = '0%';
                    bar.width(percentVal);
                    percent.html(percentVal);
                    stat.show();
                },
                uploadProgress: function (event, position, total, percentComplete) {
                    var percentVal = percentComplete + '%';
                    bar.width(percentVal);
                    percent.html(percentVal);
                },
                complete: function (xhr) {
                    var url = xhr.responseText;
                    if ((url) && (url.length > 0)) {
                        status.html($('<a>File uploaded</a>').attr('href', url));
                        var absURL = url.substring(1);
                        attachURL(absURL);
                    }
                }
            });

        });

        addButtons.append(whatButton, whenButton, whereButton, whoButton, drawButton, webcamButton, uploadButton);
        addButtons.find('button').addClass('btn btn-default');




        var scopeSelect = null;
        /*if (!objHasTag(getEditedFocus(), 'User'))*/ {
            scopeSelect = $('<select class="form-control" style="width:auto;float:right"/>').append(
                    //store on server but only for me
                    '<option value="2">Private</option>',
                    //store on server but share with who i follow
                    '<option value="5">Trusted</option>',
                    //store on server for public access (inter-server)
                    '<option value="7">Public</option>',
                    '<option value="7a">Anonymous</option>',
                    '<option value="8">Advertise</option>').
                    val(getEditedFocus().scope);

            /*if (configuration.connection == 'static')
             scopeSelect.attr('disabled', 'disabled');
             else {*/
            scopeSelect.change(function () {
                var e = getEditedFocus();
                e.scope = scopeSelect.val();
                update(e);
            });
            //}
        }

        var saveButton = $('<button class="btn btn-primary" style="float:right"><b>Save</b></button>').click(function () {

            var e = getEditedFocus();

            if (e.scope === '7a') {
                e.scope = 7;
            }
            else {
                e.author = $N.id();
            }

            e.scope = parseInt(e.scope);

            objTouch(e);


            notify({
                title: 'Saved (' + e.id.substring(0, 6) + ')',
                text: JSON.stringify(e, null, 4)
                        //text: '<button disabled>Goto: ' + x.name + '</button>'  //TODO button to view object
            });

            $N.pub(e, function (err) {
                notify({
                    title: 'Unable to save.',
                    text: e.name,
                    type: 'Error'
                });
            }, function () {
                notify({
                    title: 'Saved (' + e.id.substring(0, 6) + ')',
                    text: JSON.stringify(e)
                            //text: '<button disabled>Goto: ' + x.name + '</button>'  //TODO button to view object
                });
            });
            //D.parent().dialog('close');
        });

        var mwb = newDiv().css('float', 'right');
        mwb.append(saveButton);
        if (scopeSelect)
            mwb.append(scopeSelect);
        //if (tagInput)	mwb.append(tagInput);

        menuWrap.append(mwb);
        menuWrap.prependTo(D);
    }


    return D;
}

/**
 *  focus - a function that returns the current focus
 *  commitFocus - a function that takes as parameter the next focus to save
 *  TODO - use a parameter object like newObjectView
 */
function newObjectEdit0(ix, editable, hideWidgets, onTagRemove, whenSliderChange, excludeTags, onNameEdit) {
    if (typeof ix === 'string')
        ix = $N.instance[ix];

    var D = newDiv().addClass('ObjectEditDiv');
    var headerTagButtons = ix.tagSuggestions || [];
    var ontocache = {};

    D.id = ix.id;

    function update(x) {
        var widgetsToAdd = [];

        var whenSaved = [];
        var nameInput = null, tagInput = null;

        function getEditedFocus() {
            if (!editable)
                return x;

            var na = nameInput ? nameInput.val() : '';

            var n = objNew(x.id, na);
            n.createdAt = x.createdAt;
            n.author = x.author;

            //copy all metadata

            if (x.subject)
                n.subject = x.subject;
            if (x.when)
                n.when = x.when;
            if (x.duration)
                n.duration = x.duration;
            if (x.expiresAt)
                n.expiresAt = x.expiresAt;
            if (x.replyTo)
                n.replyTo = x.replyTo;
            if (x.in)
                n.in = x.in;
            if (x.out)
                n.out = x.out;
            if (x.with)
                n.with = x.with;
            if (x.inout)
                n.inout = x.inout;

            n.scope = x.scope || configuration.defaultScope;

            //TODO copy any other metadata

            for (var i = 0; i < whenSaved.length; i++) {
                var w = whenSaved[i];
                w(n);
            }
            return n;
        }

        var onAdd = function (tag, value) {
            update(objAddValue(getEditedFocus(), tag, value));
        };
        var onRemove = function (i) {
            var rr = objRemoveValue(getEditedFocus(), i);
            if (onTagRemove)
                onTagRemove(rr);
            update(rr);
        };
        var onChange = function (i, newValue) {
            var f = getEditedFocus();
            f.value[i] = newValue;
            update(f);
        };
        var onStrengthChange = function (i, newStrength) {
            if (x.readonly)
                return;
            var y = getEditedFocus();
            if (!y.value)
                y.value = [];

            if (!y.value[i])
                y.value[i] = {};
            y.value[i].strength = newStrength;
            if (whenSliderChange)
                whenSliderChange(y);
            update(y);
        };
        var onOrderChange = function (fromIndex, toIndex) {
            if (x.readonly)
                return;
            //http://stackoverflow.com/questions/5306680/move-an-array-element-from-one-array-position-to-another
            var y = getEditedFocus();
            y.value.splice(toIndex, 0, y.value.splice(fromIndex, 1)[0]);
            update(y);
        };


        widgetsToAdd = [];

        if (editable) {

            if ((x.name) || (hideWidgets !== true)) {
                nameInput = $('<input type="text"/>').attr('placeholder', 'Title').attr('x-webkit-speech', 'x-webkit-speech').addClass('form-control input-lg nameInput nameInputWide');
                nameInput.val(x.name === true ? '' : x.name);
                widgetsToAdd.push(nameInput);
                if (onNameEdit) {
                    //nameInput.keyup(onNameEdit);
                    nameInput.change(onNameEdit);
                    nameInput.keyup(function (e) {
                        if (e.keyCode == 13)
                            onNameEdit.apply($(this));
                    });
                }
            }

            if (hideWidgets !== true) {
                tagInput = $('<input name="tags" class="tags"/>');
                //$('#tags').importTags('foo,bar,baz');
                //$('#tags').addTag('foo');

                var tagSearchCache = {};
                var addedTags = {};

                later(function () {
                    tagInput.tagsInput({
                        // https://github.com/xoxco/jQuery-Tags-Input#options
                        defaultText: 'Tag..',
                        minChars: 2,
                        width: 'auto',
                        height: 'auto',
                        onAddTag: function (t) {
                            //addedTags[t] = true;
                            update(objAddTag(getEditedFocus(), t));
                        },
                        onRemoveTag: function (t) {
                            delete addedTags[t];
                        },
                        //autocomplete_url: 'http://missingajax',
                        autocomplete: {
                            selectFirst: true,
                            width: '100px',
                            autoFill: true,
                            //source: ['this','real','tags'],
                            source: function (request, response) {
                                var term = request.term;
                                var results = $N.searchOntology(term, tagSearchCache);
                                results = results.map(function (r) {
                                    var x = {};
                                    x.value = r[0];
                                    var rclass = $N.class[r[0]];
                                    if (rclass)
                                        x.label = rclass.name;
                                    else
                                        x.label = r[0];
                                    return x;
                                });
                                response(results);
                            },
                            create: function () {
                                tagInput.next().find('input').addClass('form-control');
                                tagInput.next().css('border', '0');
                                tagInput.next().find().css('border', '0');
                            }

                        }
                        /*onChange: function(elem, elem_tags) {
                         var languages = ['php','ruby','javascript'];
                         $(tagInput, elem_tags).each(function() {
                         if($(this).text().search(new RegExp('\\b(' + languages.join('|') + ')\\b')) >= 0)
                         $(this).css('background-color', 'yellow');
                         });
                         }*/
                        //autocomplete: { }
                        //autocomplete_url:'test/fake_json_endpoint.html' // jquery ui autocomplete requires a json endpoint
                    });

                });


                whenSaved.push(function (y) {
                    objName(y, nameInput.val());

                    for (var i in addedTags) {
                        objAddTag(y, i);
                    }
                    addedTags = {};
                });
            }
        } else {
            widgetsToAdd.push('<h1>' + objName(x) + '</h1>');
        }
        //widgetsToAdd.push($('<span>' + x.id + '</span>').addClass('idLabel'));

        var header = newDiv();
        widgetsToAdd.push(header);

        _.each(headerTagButtons, function (T) {
            if (T == '\n') {
                header.append('<br/>');
            } else {
                newTagButton(T, function () {
                    var y = D.getEditedFocus();
                    objAddTag(y, T);
                    update(y);
                }, true).appendTo(header);
            }
        });

        var tsw = $('<div class="tagSuggestionsWrap mainTagSuggestions"></div>');
        widgetsToAdd.push(tsw);

        var ts = $('<div/>').addClass('tagSuggestions').appendTo(tsw);

        //widgetsToAdd.push(newEle('div').append('&nbsp;').attr('style','height:1em;clear:both'));


        var ontoSearcher;

        var lastNameValue = null;

        function search() {
            if (!tsw.is(':visible')) {
                //clearInterval(ontoSearcher);
                return;
            }
            if (!D.is(':visible')) {
                clearInterval(ontoSearcher);
                return;
            }

            var v = nameInput.val();

            if (v.length === 0)
                return;

            if (lastNameValue !== v) {
                updateTagSuggestions(v, ts, onAdd, getEditedFocus, ontocache);
                lastNameValue = v;
            }
        }

        if (nameInput)
            updateTagSuggestions(nameInput.val(), ts, onAdd, getEditedFocus, ontocache);

        if (objHasTag(getEditedFocus(), 'Tag')) {
            //skip suggestions when editing a Tag
            ts.empty();
        } else {
            if (!x.readonly) {
                if (hideWidgets !== true) {
                    if (editable)
                        ontoSearcher = setInterval(search, configuration.ontologySearchIntervalMS);
                    search();
                }
            }
        }

        D.empty();
        D.getEditedFocus = getEditedFocus;




        if (x.value) {
            var tags = []; //tags & properties, actually

            var tt = null;
            for (var i = 0; i < x.value.length; i++) {
                var t = x.value[i];

                if (excludeTags)
                    if (_.contains(excludeTags, t.id))
                        continue;

                tags.push(t.id);
                tt = newTagValueWidget(x, i, t, editable, whenSaved, onAdd, onRemove, onChange, onStrengthChange, onOrderChange, whenSliderChange);
                widgetsToAdd.push(tt);
            }
            if (tt !== null) {
                //hide the last tag section's down button
                tt.find('.moveDown').hide();
            }

            var missingProp = [];
            //Add missing required properties, min: >=1 (with their default values) of known objects:

            if (!x.readonly) {
                for (var i = 0; i < tags.length; i++) {
                    var t = $N.class[tags[i]];
                    if (!t)
                        continue;

                    var prop = t.property;
                    if (!prop)
                        continue;

                    _.each(prop, function (P, pid) {
                        if (P.min)
                            if (P.min > 0)
                                if (!_.contains(tags, pid))
                                    missingProp.push(pid);
                    });

                }
            }

            missingProp.forEach(function (p) {
                widgetsToAdd.push(newTagValueWidget(x, i + x.value.length, {
                    id: p
                }, editable, whenSaved, onAdd, onRemove, onChange, onStrengthChange, onOrderChange, whenSliderChange));
            });
        }


        D.append(widgetsToAdd);
    }

    update(ix);


    return D;
}
