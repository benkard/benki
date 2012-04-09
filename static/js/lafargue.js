jQuery(function($) {
    if (WebSocket) {
        var websocket_base = $('head').attr('data-websocket-base');
        var socket = new WebSocket(websocket_base + '/lafargue/events');
        socket.onmessage = function(event) {
            $('.lafargue-list').prepend(event.data);
        };
    }
});
