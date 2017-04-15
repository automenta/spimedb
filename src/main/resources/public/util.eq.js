"use strict";

/** TODO attention metadata for each tag with callback stream */
class Tag {

}

function EqualizeDiv(id, content) {
    const div = $(document.createElement('div')).attr('id', id);

    var capacity = 1.0;

    /** neutral/unknown/maybe = 0, +1 = enabled, -1 = hidden */
    var attn = 0.0;

    var hovering = false;
    var deferredUntilUnhover = undefined;
    div.hover(()=>{
        //on in
        hovering = true;
    }, ()=>{
        //on out
        hovering = false;

        if (deferredUntilUnhover) {
            deferredUntilUnhover();
            deferredUntilUnhover = undefined;
        }
    });

    function update() {
        //deferredUntilUnhover = ()=> {
            div.attr('style',
                'font-size:' + (65.0 + 30 * (attn + (Math.log(1 + capacity))) ) + '%'
            );
        //};

        content.attr('style',
            'color: rgb(' + parseInt(192.0 * -Math.min(attn, 0.0)) + ','
            + parseInt(192.0 * Math.max(attn, 0.0)) + ',0);'
            + ((attn < 0) ? 'text-decoration:line-through' : '')
        );
    }

    var increment = 0.33;

    const eqCut = $(document.createElement('button')).text('-').click(()=>{
        attn = Math.max(-1, attn - increment);
        update();
    });
    const eqBoost = $(document.createElement('button')).text('+').click(()=>{
        attn = Math.min(+1, attn + increment);
        update();
    });

    div.append(
        content,
        eqCut,
        eqBoost
    );


    div.capacity = function(newCapacity) {
        if (capacity!==newCapacity) {
            capacity = newCapacity;
            update();
        }
        return this;
    };
    div.attn = function(newAttn) {
        if (attn!==newAttn) {
            attn = newAttn;
            update();
        }
        return this;
    };
    return div;
}

