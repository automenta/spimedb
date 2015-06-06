"use strict";

//nobject edit
class NObjectEdit {

    /** target div, id, obj */
    constructor(target, obj) {

        //if (obj && typeof(obj) === "string")
        //    obj = new NObject(id, obj);

        var tag = {id: 'NoTag', name: 'Not Tagged' }; //TEMPORARY

        this.obj = obj;
        this.target = target;

        var d = target;
        d.addClass("nobjedit");
        d.addClass("ui");
        d.addClass("modal");
        d.addClass("inlineModal");
        d.append('<i class="close icon"></i>');


        var subj = newSubjectDropdown();
        subj.css('padding', '0');

        var header = $('<div class="header"/>').appendTo(d);


        var titleEdit = $('<input class="name" type="text" placeholder="?"/>"').appendTo(header)
        if (obj.name) titleEdit.val(obj.name);



        var c = $('<div class="content"/>').appendTo(d);
        c.append('<div class="ui image"><i class="icon tags"></i></div>');

        var cc = $('<div class="description"/>').appendTo(c);
        //cc.append('<div class="ui header"></div>');

        var content = newDiv().attr('contenteditable', 'true').appendTo(cc);

        //.append('Tag ', subj,  ' with: ', $('<i>' + tag.name + '</em>')));

        var insertWidget = function(x) {
            if (typeof(x) === "string") x = $(x);

            x.attr('contenteditable', 'false');
            x.addClass('ui label')
            content.append(x);
        }

        var insertCheckbox = function(id, label) {
            insertWidget(newSpan().append(
                $('<input class="ui button" type="checkbox" name="' + id + '"/>'),
                label + '?'
            ));
            //insertWidget('<span class="ui checkbox"><input type="checkbox" name="' + id + '"><label>' + label + '</label></span>?');

            //insertWidget($('<div class="ui toggle button">' + label + '</div>').button());
        }
        var insertSpectrumSlider = function(left, right) {
            insertWidget(newSpan().append(
                left.name,
                '<input type="range" min="0" max="100"/>',
                right.name
            ));
        }
        var insertCombo = function(options) {
            insertWidget(newSpan().append(
                $('<input class="ui button" type="checkbox" name="' + id + '"/>'),
                label + '?'
            ));
            //insertWidget('<span class="ui checkbox"><input type="checkbox" name="' + id + '"><label>' + label + '</label></span>?');

            //insertWidget($('<div class="ui toggle button">' + label + '</div>').button());
        }


        content.append('Accessibility: ');
        insertCheckbox('can', 'Can');
        insertCheckbox('need', 'Need');
        insertCheckbox('not', 'Not');
        content.append('<br/>');


        content.append('Knowledge: ');
        insertSpectrumSlider({id: "learn", name: "Learn"}, {id: "teach", name: "Teach"});

        content.append('<br/>');
        content.append('More details?');

        /*

         <div class="description">
         <div class="ui header">We've auto-chosen a profile image for you.</div>
         <p>We've grabbed the following image from the <a href="https://www.gravatar.com" target="_blank">gravatar</a> image associated with your registered e-mail address.</p>
         <p>Is it okay to use this photo?</p>
         </div>
         */
        var tagid = tag.id;

        var a = $('<div class="actions"/>').appendTo(d);;
        a.append('<div class="ui black button">Cancel</div>');
        a.append('<div class="ui positive right labeled icon button">Save<i class="checkmark icon"></i></div>');


        if (target!==d)
            target.html(d);


    }



}

function newWikiTagger(tag) {
    //http://semantic-ui.com/modules/modal.html#/usage

    var d = newDiv();
    new NObjectEdit(d, uuid());
    d.removeClass('inlineModal');
    d.hide();
    return d;

}
