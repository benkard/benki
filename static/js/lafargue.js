jQuery(function($) {
    if (WebSocket) {
        var websocket_base = $('head').attr('data-websocket-base');
        var open_socket = function() {
            var socket = new WebSocket(websocket_base + '/lafargue/events');
            socket.onmessage = function(event) {
                $('.lafargue-list').prepend(event.data);
            };
            var reconnect = function() {
                window.setTimeout(function() {
                    open_socket();
                    socket.close();
                }, 2000);
            };
            socket.onclose = reconnect;
        };
        open_socket();
    }
});
