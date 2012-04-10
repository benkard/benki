jQuery(function($) {
    var notificationsp = false;

    if (WebSocket) {
        var websocket_base = $('head').attr('data-websocket-base');
        var open_socket = function() {
            var socket = new WebSocket(websocket_base + '/lafargue/events');
            socket.onmessage = function(event) {
                var message = JSON.parse(event.data);
                $('.lafargue-list').prepend(message.html);
                if (window.webkitNotifications && notificationsp) {
                    var notification = window.webkitNotifications.createNotification(null, '', 'Lafargue', 'New message by ' + message.first_name);
                    notification.show();
                }
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

    var updateNotificationStatus = function() {
        var status = $('#notification-status');
        if (notificationsp) {
            status.removeClass('bad');
            status.addClass('good');
            status.html('Notifications are ON.');
        } else {
            status.addClass('bad');
            status.removeClass('good');
            status.html('Notifications are OFF.');
        }
    };

    var toggleNotifications = function() {
        if (!notificationsp) {
            if (!window.webkitNotifications.checkPermission() !== 0) {
                window.webkitNotifications.requestPermission(function() {
                    updateNotificationStatus();
                });
            }
            notificationsp = true;
        } else {
            notificationsp = false;
            updateNotificationStatus();
        }
    };

    if (window.webkitNotifications) {
        var notify_button = $('.notifications').append('<a href="#" class="notify"><div id="notification-status" class="notification bad">Notifications are OFF.</div></a>');
        notify_button.click(toggleNotifications);
        if (window.webkitNotifications.checkPermission() === 0) {
            notificationsp = true;
        }
        updateNotificationStatus();
    };
});
