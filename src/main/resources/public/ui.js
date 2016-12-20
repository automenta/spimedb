//DEPRECATED
"do not use"

/** this is the system of menus and views in which Spacegraph(s) can be viewed and controlled */
/*function Spacegrapher(e) {
 
 e.find('#AddMenu li a').click(function() {
 var t = $(this).text();
 notify(t);
 });
 
 $('#nodefocus').hide();
 }*/

function UI(e, s) {
    var u = {
    };
    
    $('#ChannelMenu').append($('<a href="#">F</a>').click(function() {
        s.setLayout({
           name: 'cose' 
        });
    }));
    $('#ChannelMenu').append($('<a href="#">G</a>').click(function() {
        s.setLayout({
           name: 'grid' 
        });
    }));
    $('#ChannelMenu').append($('<a href="#">C</a>').click(function() {
        s.setLayout({
           name: 'concentric' 
        });
    }));
    $('#ChannelMenu').append($('<a href="#">T</a>').click(function() {
        s.setLayout({
           name: 'breadthfirst' 
        });
    }));
    $('#ChannelMenu').append($('<a href="#">R</a>').click(function() {
        s.setLayout({
           name: 'random' 
        });
    }));

    var newContentTypes = ['text', 'data', 'map', 'timeline', 'sketch', 'www'];

    u.addChannel = function (space, c) {
        var cclass = c.id() + '_menu';
        e.find('.' + cclass).remove();



        var button = $('<a href="#">' + c.id() + '</a>');
        var dropdown = $('<ul id="drop_' + c.id() + '" class="dropdown"></ul>');


        for (var i = 0; i < newContentTypes.length; i++) {
            var t = newContentTypes[i];

            var l = $('<li></li>').append(a);
            var a, v;
            dropdown.append(l);

            if (t === 'www') {
                v = $('<input style="margin: 4px; width:85%" type="text" placeholder="http://"></input>');
                a = $('<button style="float: right">www</button>').data('type', t);

                l.append(v, a);
            }
            else {
                a = $('<a href="#">' + t + '</a>').data('type', t).appendTo(l);
            }

            a.click(function () {
                

                var type = $(this).data('type');
                
                space.newNode(c, type, null, v.val());

            });
        }

/*
        e.find('#ChannelMenu').append(
                $('<li class="has-dropdown not-click"></li>').
                append(button, dropdown).addClass(cclass)
                ).foundation();
        */


        /*
         <a href="#" class="button" data-dropdown="drop">Link Dropdown &raquo;</a>
         <ul id="drop" class="[tiny small medium large content]f-dropdown" data-dropdown-content>
         <li><a href="#">This is a link</a></li>
         <li><a href="#">This is another</a></li>
         <li><a href="#">Yet another</a></li>
         </ul> */


    };


    u.removeChannel = function (c) {

    };

    return u;
}
