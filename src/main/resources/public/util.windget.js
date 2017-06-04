function newWindow(content) {
    const w = newFrame();

    // var closeButton = $('<button/>').text('x').addClass('close_button').click(function() {
    //     w.fadeOut(150, function() { $(this).remove(); });
    // });

    /*var fontSlider = NSlider({ }).addClass('font_slider').css({
        width: '1em',
        position: 'absolute',
        left: 0,
        top: 0
    });*/
  
    w.append(content = (content || $('<div/>')), /*fontSlider,*/);

    content.addClass('content');

    return w;
}

function newFrame() {
    //http://interactjs.io/


    var div = $('.windgets');
    if (div.length === 0)
        div = $('<div>').addClass('windgets').prependTo($('body'));

    var content = DIVclass('windget')/*.fadeIn()*/.appendTo(div);

    var dragMoveListener = function (event) {
        var target = event.target,
        // keep the dragged position in the data-x/data-y attributes
            x = (parseFloat(target.getAttribute('data-x')) || 0) + event.dx,
            y = (parseFloat(target.getAttribute('data-y')) || 0) + event.dy;

        // translate the element
        target.style.webkitTransform =
            target.style.transform =
                'translate(' + x + 'px, ' + y + 'px)';

        // update the posiion attributes
        target.setAttribute('data-x', x);
        target.setAttribute('data-y', y);
    }


    interact(content[0])
        .draggable({
            onmove: dragMoveListener
        })
        .resizable({
            edges: {left: true, right: true, bottom: true, top: true}
        })
        .on('resizemove', function (event) {
            var target = event.target,
                x = (parseFloat(target.getAttribute('data-x')) || 0),
                y = (parseFloat(target.getAttribute('data-y')) || 0);

            // update the element's style
            target.style.width = parseInt(event.rect.width);// + 'px';
            target.style.height = parseInt(event.rect.height);// + 'px';

            // translate when resizing from top or left edges
            x += event.deltaRect.left;
            y += event.deltaRect.top;

            target.style.webkitTransform = target.style.transform =
                'translate(' + parseInt(x) + ',' + parseInt(y) + ')';

            target.setAttribute('data-x', x);
            target.setAttribute('data-y', y);
            //target.textContent = event.rect.width + '×' + event.rect.height;
        });

    //content.close = ...

    return content;
}
