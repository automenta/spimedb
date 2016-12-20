var channelsOpen = {};

function newChannelPopup(channel) {

//    if (channelsOpen[channel]) {
//        var w = channelsOpen[channel];
//        w.dialog('close');
//        delete channelsOpen[channel];
//        return;
//    }

    var o = $N.class[channel];
    if (!o)
        return;

    var s = newPopupObjectView(o, {title: channel, width: '50%'}, {
        showMetadataLine: false,
        showName: false,
        showActionPopupButton: false,
        showSelectionCheck: false,
        transparent: true
    });
    /*if (configuration.webrtc) {
     s.append(newWebRTCRoster());
     }*/

    
    channelsOpen[channel] = s;
    
    

    s.bind('dialogclose', function () {
        delete channelsOpen[channel];
    });

    return s;
}

function newChatWidget(onSend, options) {
    var c = newDiv();

    options = options || {
        localEcho: false
    };

    var history = [];
    var log = newDiv().addClass('ChatLog').appendTo(c);

    var input = newDiv().addClass('ChatInput').appendTo(c);

    c.send = function(m) {
        var mm = {a: $N.id(), m: m};
        onSend(mm);
        if (options.localEcho)
            c.receive(mm); //local echo    
    };
    
    var textInput = $('<input type="text"/>').appendTo(input);
    textInput.keydown(function (e) {
        if (e.keyCode === 13) {
            c.send( $(this).val() );
            $(this).val('');
        }
    });

    function chatlineclick() {
        var line = $(this).parent();
        var n = new $N.nobject();
        n.setName(line.find('span').html());

        newPopupObjectEdit(n);
    }

    var scrollbottom = function () {
        log.scrollTop(log.prop('scrollHeight'));
    };

    function newChatLine(l) {
        var d = newDiv();

        if (l.a) {
            var A = $N.instance[l.a];
            if (A) {
                d.append(newEle('a').html(newAvatarImage(A)).click(chatlineclick));
            }
            else {
                d.append(newEle('a').html(l.a + ': '));
            }
        }

        d.append(newEle('span').html(l.m));

        //TODO scroll to bottom

        return d;
    }

    function updateLog() {
        log.empty();
        for (var i = 0; i < history.length; i++) {
            var h = history[i];
            log.append(newChatLine(h));
        }
        scrollbottom();
    }
    updateLog();

    function appendLog(m) {
        history.push(m);
        log.append(newChatLine(m));
        scrollbottom();
    }

    c.receive = function (m) {
        appendLog(m);

        if (m.a !== $N.id()) {
            var aname = $N.label(m.a);
            notify({title: aname, text: m.m});
        }
    };
    c.disable = function () {
        textInput.val('Disconnected');
        textInput.attr('disabled', 'disabled');
    };

    return c;
}

