jQuery(function($) {
    if (WebSocket) {
        var socket = new WebSocket('ws://localhost:3001/lafargue/events');
        socket.onmessage = function(event) {
            $('.lafargue-list').prepend(event.data);
        };
    }
});
