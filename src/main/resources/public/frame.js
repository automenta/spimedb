"use strict";

function NodeFrame(spacegraph) {

    var f = {
        hoverUpdate: function () { /* implemented below */
        },
        hide: function () { /* see below */
        },
        hovered: null
    };

    var frameVisible = false;
    var frameTimeToFade = 2000; //ms
    var frameHiding = -1;
    var frameNodePixelScale = 300;
    var frameNodeScale = 1.25;
    var frameEleNode = null;
    var frameEleResizing = false;


    $.get('frame.html', {"_": $.now() /* overrides cache */}, function (x) {

        spacegraph.overlay.append(x);

        var frameEle = $('#nodeframe');
        f.hovered = null;


        initFrameDrag(f);


        //http://threedubmedia.com/code/event/drag/demo/resize2

        spacegraph.on('zoom', function (e) {
            setTimeout(f.hoverUpdate, 0);
        });

        spacegraph.on('mouseover mouseout mousemove', function (e) {

            var target = e.cyTarget;
            var over = (e.type !== "mouseout");

            if (frameEleResizing) {
                target = frameEleNode;
                frameEle.show();
                over = true;
            }

            if (target && target.isNode && target.isNode()) {
                if ((over || (f.hovered !== target)) && (frameHiding !== -1)) {
                    //cancel any hide fade
                    clearTimeout(frameHiding);
                    frameHiding = -1;
                }

                if (f.hovered !== target) {
                    frameEle.hide();
                    frameVisible = true;
                    frameEle[0].style.width = undefined; //reset width
                    frameEle.fadeIn();
                    f.hovered = target;
                }
            } else {
                frameVisible = false;
            }


            setTimeout(f.hoverUpdate, 0);

        });

        f.hide = function () {
            frameVisible = false;
            this.hovered = null;
            setTimeout(this.hoverUpdate, 0);


            //TODO fadeOut almost works, but not completely. so hide() for now
            frameEle.hide();
        };

        f.hoverUpdate = function () {

            if (frameVisible) {
                if (!this.currentlyVisible) {
                    this.currentlyVisible = true;
                    frameEle[0].style.width = undefined; //reset width
                    frameEle.fadeIn();
                }
            }
            else {
                if ((frameHiding === -1) && (this.currentlyVisible)) {
                    frameHiding = setTimeout(function () {
                        //if still set for hiding, actually hide it
                        if (!frameVisible) {
                            frameEle.fadeOut(function () {
                                f.hovered = null;
                                frameEleNode = null;
                                frameHiding = -1;
                            });
                            this.currentlyVisible = false;
                        }
                    }, frameTimeToFade);
                }
            }

            if (this.currentlyVisible && f.hovered) {
                spacegraph.positionNodeHTML(f.hovered, frameEle, frameNodePixelScale, frameNodeScale);
                frameEleNode = f.hovered;
            }

        };


    });

    function initFrameDrag(nodeFrame) {
        var frameEle = $('#nodeframe');


        var close = $('#nodeframe #close');
        close.click(function () {
            var node = frameEleNode;
            if (node) {
                var space = node.cy();

                space.removeNode(node);

                frameEleNode = null;
                nodeFrame.hide();
            }
        });

        var se = $('#nodeframe #resizeSE');
        $(function () {
            //$( "#draggable" ).draggable({ revert: true });
            se.draggable({
                revert: true,
                helper: "clone",
                appendTo: '#nodeframe #resizeSE',
                //appendTo: "body",

                //http://api.jqueryui.com/draggable/#event-drag
                start: function (event, ui) {
                    var node = frameEleNode;

                    if (!node)
                        return;

                    var pos = node.position();
                    if (!pos) {
                        console.error('node ', node, 'has no position');
                        return;
                    }

                    frameEleResizing = true;

                    this.originalNode = node;
                    this.originalPos = [parseFloat(pos.x), parseFloat(pos.y)];
                    this.originalSize = [node.width(), node.height(), node.renderedWidth(), node.renderedHeight()];
                    this.originalOffset = [ui.offset.left, ui.offset.top];
                },
                drag: function (event, ui) {

                    var node = frameEleNode;
                    if (node !== this.originalNode)
                        return;

                    var dx = parseFloat(ui.offset.left - this.originalOffset[0]);
                    var dy = parseFloat(ui.offset.top - this.originalOffset[1]);

                    var p = this.originalPos;
                    var os = this.originalSize;

                    var dw = dx * (os[0] / os[2]);
                    var dh = dy * (os[1] / os[3]);
                    var w = os[0] + dw;
                    var h = os[1] + dh;
                    var x = p[0] + dw / 2.0;
                    var y = p[1] + dh / 2.0;


                    node.cy().startBatch();
                    node.position({x: x, y: y});
                    node.css({
                        'width': w,
                        'height': h
                    });
                    node.cy().endBatch();
                },
                stop: function (event, ui) {
                    frameEle[0].style.width = undefined; //reset width

                    frameEleResizing = false;
                }

            });
        });
    }

    return f;
}


class PopupMenu {

    //var prevMenu = null;

    //hit background
    //this.newNode(s.defaultChannel, 'text', e.cyPosition);
    //var handler = {
    //
    //    addText: function(position) {
    //        s.newNode(s.defaultChannel, 'text', position);
    //    },
    //
    //    addURL: function(position) {
    //
    //    }
    //
    //};

    constructor(id) {
        "use strict";
        this.id = id;
    }

    hide() {
        $('#' + this.id).remove();
        if (this.onHidden) this.onHidden();
    }

    /** position = { left: .. , top: .. }  */
    show(menu, position, target) {
        "use strict";


        var that = this;

        var handler = {

        };

        //remove existing
        //$('#' + this.id).each(function(x) { $(x).fadeOut(function() { $(this).remove(); }); });
        this.hide();

        //add
        target = target || $('body');
        menu.attr('id', this.id).css('position', 'fixed').appendTo(target);

        //http://codepen.io/MarcMalignan/full/xlAgJ/

        var radius = this.radius || 70.0;

        var cx = position.left - radius / 2.0;
        var cy = position.top - radius / 2.0;

        var items = menu.find('li');

        var closebutton = menu.find('.closebutton');
        closebutton.unbind();
        items.unbind();

        items.css({left: 0, top: 0});
        menu.hide().css({
            left: cx,
            top: cy,
            opacity: 0.0,
            display: 'block'
        }).animate({
            opacity: 1.0
        }, {
            duration: 400,
            step: function (now, fx) {
                for (var i = 0; i < items.length; i++) {
                    var a = (i / (items.length)) * Math.PI * 2.0;
                    a += now;
                    var x = Math.cos(a) * now * radius;
                    var y = Math.sin(a) * now * radius;
                    $(items[i]).css({
                        left: x, top: y
                    });
                }
            }
        });
        items.click(function (e) {

            var a = $(e.target).attr('action');
            if (a && handler[a])
                handler[a](position);

            that.hide();
        });
        closebutton.click(function () {
            that.hide();
        });

        console.log('popup menu visible');
    }


}

function newSpacePopupMenu(s) {

    var prevMenu = null;

    //hit background
    //this.newNode(s.defaultChannel, 'text', e.cyPosition);
    var handler = {

        addText: function (position) {
            s.newNode(s.defaultChannel, 'text', position);
        },

        addURL: function (position) {

        }

    };

    s.on('tap', function (e) {
        var target = e.cyTarget;
        var position = e.cyPosition;

        if (target.isNode) {
            //hit an element (because its isNode function is defined)
            return;
        }

        if (prevMenu) {
            //click to remove existing one, then click again to popup again
            prevMenu.destroy();
            prevMenu = null;
            return;
        }

        var menu = prevMenu = $('#ContextPopup').clone();
        menu.appendTo(s.overlay);
        menu.destroy = function () {
            if (prevMenu === this)
                prevMenu = null;
            menu.fadeOut(function () {
                menu.remove();
            });
        };


        var that = this;

        //http://codepen.io/MarcMalignan/full/xlAgJ/

        var radius = 70.0;

        var cx = e.originalEvent.clientX - radius / 2.0;
        var cy = e.originalEvent.clientY - radius / 2.0;

        var items = menu.find('li');

        var closebutton = menu.find('.closebutton');
        closebutton.unbind();
        items.unbind();

        items.css({left: 0, top: 0});
        menu.hide().css({
            left: cx,
            top: cy,
            opacity: 0.0,
            display: 'block'
        }).animate({
            opacity: 1.0
        }, {
            duration: 400,
            step: function (now, fx) {
                for (var i = 0; i < items.length; i++) {
                    var a = (i / (items.length)) * Math.PI * 2.0;
                    a += now;
                    var x = Math.cos(a) * now * radius;
                    var y = Math.sin(a) * now * radius;
                    $(items[i]).css({
                        left: x, top: y
                    });
                }
            }
        });
        items.click(function (e) {

            var a = $(e.target).attr('action');
            if (a && handler[a])
                handler[a](position);

            menu.destroy();

            return true;
        });
        closebutton.click(function () {
            menu.destroy();
        });

    });

}