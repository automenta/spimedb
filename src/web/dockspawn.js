var socket;

function websocket(s) {
    var socket;
    
    $(document).ready(function() {
        if (!window.WebSocket) {
            window.WebSocket = window.MozWebSocket;
        }
        if (window.WebSocket) {

            socket = new WebSocket('ws://' + window.location.hostname + ':' + window.location.port + '/websocket');
            //socket = new WebSocket("ws://localhost:8080/websocket");    
            socket.onmessage = s.onMessage;
            socket.onopen = s.onConnect;
            socket.onclose = s.onDisconnect;
        } else {
            alert("Your browser does not support Web Socket.");
        }

    });
    
    this.send = function(message) {
        if (!window.WebSocket) {
            return;
        }
        if ((socket) && (socket.readyState == WebSocket.OPEN)) {
            socket.send(JSON.stringify(message));            
        } else {
            alert("Websocket is not connected.");
        }        
    };
    return this;
}

