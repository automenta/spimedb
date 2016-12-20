"use strict";

function newSpacegraphDemoMenu(s) {
    var menu = $('<div/>');

    menu.append($('<button>Tag Graph</button>').click(function () {
        s.clear();

        var index = new TagIndex(function (i) {
            i.activateRoots(1, 4);
            s.addChannel(i.channel);
        });
        s.layout({name: 'random'});
    }));

    menu.append($('<button>Tag Accordion</button>').click(function () {

        //http://semantic-ui.com/modules/accordion.html#/examples
        var index = new TagIndex(function (i) {

            s.addChannel(new Channel({
                nodes: [{
                    width: 64,
                    height: 64,
                    widget: {
                        html: new TagIndexAccordion(i),
                        scale: 0.8,
                        minPixels: 8,
                        pixelScale: 300
                    }
                }]
            }));
        });
    }));

    menu.append($('<button>Text</button>').click(function () {
        s.addChannel(new Channel({
            nodes: [{
                width: 64,
                height: 64,
                widget: {
                    html: '<div style="width: 100%; height: 100%;"><div contenteditable="true" class="editable">...</div><br/></div>',
                    style: {},
                    scale: 0.9,
                    minPixels: 8,
                    pixelScale: 200
                }
            }]
        }));
    }));

//        menu.append($('<button>RichText</button>').click(function () {
//
//            //http://alex-d.github.io/Trumbowyg/documentation.html
//            var d = $('<div style="width:600px;height:100%"></div>');
//            setTimeout(function() {
//                d.trumbowyg({
//                    resetCss: true,
//                    fullscreenable: false,
//                    closable: false
//                    //btns: ['bold', 'italic', '|', 'insertImage']
//                });
//            }, 0);
//
//            s.addChannel(new Channel({
//                nodes: [{
//                    width: 600,
//                    height: 400,
//                    widget: {
//                        html: d,
//                        style: {},
//                        scale: 0.9,
//                        minPixels: 8,
//                        pixelScale: 400
//                    }
//                }]
//            }));
//        }));



    menu.append($('<button>Map 2D</button>').click(function () {

        var mapWrap = $('<div style="width: 400px; height: 400px;"></div>');

        setTimeout(function() {
            var map = L.map(mapWrap[0]);
            map.setView([51.505, -0.09], 13);

            L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
        }, 0);

        s.addChannel(new Channel({
            nodes: [{
                width: 400, //matches above dimensions
                height: 400,
                widget: {
                    html: mapWrap,
                    style: {},
                    scale: 0.9,
                    minPixels: 8,
                    pixelScale: 400
                }
            }]
        }));
    }));

    menu.append($('<button>Video</button>').click(function () {

        s.addChannel(new Channel({
            nodes: [{
                width: 64,
                height: 64,
                widget: {
                    html: '<iframe width="560" height="315" src="https://www.youtube.com/embed/vDpGq4QWyUs" frameborder="0" allowfullscreen></iframe>',
                    style: {},
                    scale: 0.8,
                    minPixels: 8,
                    pixelScale: 300
                }
            }]
        }));

    }));

    menu.append($('<button>Wikipedia</button>').click(function () {

        var h = $('<div style="width:100%">');
        var tb = $('<input type="text" placeholder="topic"/>').appendTo(h);
        var bb = $('<button>Wikipedia</button>').appendTo(h);
        bb.click(function () {
            var url = 'http://en.m.wikipedia.org/wiki/' + tb.val();
            h.html('<iframe style="width: 100%; height: 100%" src="' + url + '"></iframe>');
        });
        //'<div style="width: 100%; height: 100%; background-color: orange; border: 2px solid black;"><div contenteditable="true" class="editable">WTF</div><br/><button>OK</button></div>'

        s.addChannel(new Channel({
            nodes: [{
                width: 64,
                height: 64,
                widget: {
                    html: h,
                    style: {},
                    scale: 0.8,
                    minPixels: 8,
                    pixelScale: 300
                }
            }]
        }));
    }));
    menu.append($('<button>Widget Test</button>').click(function () {
        s.clear();
        s.addChannel(new Channel({
            id: 'widgetTest',
            nodes: [{
                width: 64,
                height: 64,
                widget: {
                    html: '<div style="width: 100%; height: 100%; background-color: orange; border: 2px solid black;"><div contenteditable="true" class="editable">WTF</div><br/><button>OK</button></div>',
                    style: {},
                    scale: 0.9,
                    minPixels: 8
                }
            }]
        }));
        s.layout({name: 'random'});
    }));
    menu.append($('<button>Types Demo</button>').click(function () {
        s.clear();
        s.addChannel(new Channel(newExampleChannel1()));
    }));

    menu.append('<hr/>');

    menu.append($('<button><i class="recycle icon"></i></button>').click(function () {
        s.clear();
    }));

    menu.append($('<button>F</button>').click(function () {
        s.setLayout({
            name: 'cose',

            refresh: 4, //4,
            iterations: 8,
            randomize: true
        });
    }));
    menu.append($('<button>G</button>').click(function () {
        s.setLayout({
            name: 'grid'
        });
    }));
    menu.append($('<button>C</button>').click(function () {
        s.setLayout({
            name: 'concentric'
        });
    }));
    menu.append($('<button>T</button>').click(function () {
        s.setLayout({
            name: 'breadthfirst'
        });
    }));
    menu.append($('<button>t</button>').click(function () {
        s.setLayout({
            name: 'breadthfirst',
            directed: true,
            boundingBox: {x1: 0, y1: 0, x2: 100, y2: 800} //vertical
        });
    }));
    menu.append($('<button>S</button>').click(function () {
        s.setLayout({
            name: 'breadthfirst',
            circle: true
        });
    }));
    menu.append($('<button>R</button>').click(function () {
        s.setLayout({
            name: 'random'
        });
    }));
    return menu;

}

var person = {
    firstname: 'First',
    surname: 'Last',
    age: 0,

    save: function (e) {
        document.getElementById('metawidget1').save();
        notify('Saved: ' + JSON.stringify(person));
    }
};

function newExampleChannel1() {


    return {
        id: 'untitled_' + parseInt(Math.random() * 100),
        style: {
            'node': {
                'content': 'data(content)',
                'text-valign': 'center',
                'text-halign': 'center',
                'shape': 'rectangle'
            },
            '$node > node': {
                'padding-top': '2px',
                'padding-left': '2px',
                'padding-bottom': '2px',
                'padding-right': '2px',
                'text-valign': 'top',
                'text-halign': 'center'
            },
            'edge': {
                'target-arrow-shape': 'triangle',
                //'line-style': 'dashed',
                'line-width': '16'
            },
            ':selected': {
                //'background-color': 'black',
                'line-color': 'black',
                'target-arrow-color': 'black',
                'source-arrow-color': 'black'
            }
        },
        nodes: [
            {id: 'b',
                style: {
                    shape: 'triangle',
                    height: 24,
                    width: 16
                }
            },

            {id: 'p',
                style: {
                    shape: 'rectangle',
                    height: 60,
                    width: 30
                }
            },

            /*{id: 'b1', parent: 'p',
             style: { _content: 'x', shape: 'triangle', height: 4, width: 4 }
             },*/
            /*{id: 'b2', parent: 'p',
             style: { _content: 'y', shape: 'triangle', width: 8, height: 8 }
             },
             {id: 'b3', parent: 'p',
             style: { _content: 'z', shape: 'triangle', width: 8, height: 8 }
             },
             */
            /*{
             id: 'serial' + parseInt(Math.random()*100),
             width: 16,
             height: 16,

             widget: {
             html: "<div contenteditable='true' class='editable' style='overflow: auto; resizable: both'></div>",
             scale: 0.8,
             style: {width: '300px', height: '300px'},
             },
             },*/
            {id: 'd',
                form: {
                    value: {
                        firstname: 'First',
                        surname: 'Last',
                        age: 0
                    },
                    style: {width: '300px', height: '260px'},
                    scale: 1
                },
                widget: {
                    html: '<x-metawidget id="metawidget1" path="person"></x-metawidget>',
                    style: {},
                    //html: '<iframe width="600px" height="600px" src="http://enenews.com"></iframe><br/><button>x</button>',
                    scale: 1,
                    pixelScale: 300.0,
                    minPixels: 2,
                },
                style: {
                    height: "24",
                    width: "24",
                    opacity: 0.75
                }
            },

            {id: 'u',
                width: 48, height: 32, url: 'http://wikipedia.org' },

            {id: 'e',
                width: 64,
                height: 64,
                widget: {
                    html: '<div style="width: 100%; height: 100%; background-color: orange; border: 2px solid black;"><div contenteditable="true" class="editable">WTF</div><br/><button>OK</button></div>',
                    style: {  },
                    scale: 0.9,
                    minPixels: 8,
                }
            },

            {id: 'e1',
                width: 64,
                height: 64,
                widget: {
                    html: '<div style="background-color: green; border: 2px solid black;"><div contenteditable="true" class="editable">OR(AND(F,B),Z) => X</div><br/><button>OK</button></div>',
                    style: {},
                    scale: 0.9,
                    minPixels: 16,
                }
            }


            //{id: 'f', parent: 'e'}
        ],
        edges: [
            {id: 'eb', source: 'e', target: 'b',
                style: {
                    'line-color': 'blue',
                    'line-width': '44'
                }
            },
            {id: 'db', source: 'd', target: 'b'},
            //{id: 'b1b2', source: 'b1', target: 'b2'},
            //{id: 'b1b3', source: 'b1', target: 'b3'}
            //{id: 'eb', source: 'e', target: 'b'}
        ]
    };
}
