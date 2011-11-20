/*
jQuery(function ($) {
    $('#wiki-page-content').on('blur', function() {
        // FIXME: Save.
        document.designMode = 'off';
    });
    $('#wiki-page-content').on('focus', function() {
        document.designMode = 'on';
    });
});
*/


jQuery(function ($) {
    if (!window.Aloha) {
	window.Aloha = {};		
    }
    window.Aloha.settings = {
	logLevels: {'error': true, 'warn': true, 'info': true, 'debug': false},
	errorhandling : false,
	ribbon: true,

	"i18n": {
	    "current": "de"
	},
	"repositories": {
	},
	"plugins": {
	    "format": {
		config : [ 'b', 'i','sub','sup'],
		editables : {
		    '#title'    : [ ],
		    'div'       : [ 'b', 'i', 'del', 'sub', 'sup'  ],
		    '.article'  : [ 'b', 'i', 'p', 'title', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'pre', 'removeFormat']
		}
	    },
	    "list": {
	    },
	    "link": {
		config : [ 'a' ],
	    },
	    "table": {
		config : [ ],
	    },
	    "image": {
	    }
	}
    };

    Aloha.ready(function() {
	var $$ = Aloha.jQuery;
	$$('#wiki-page-content').aloha();
        var save = function() {
            console.log('Saving changes.');
            $.ajax({
                url: "?save",
                data: { content: $('#wiki-page-content').html() },
                type: "POST",
                success: function() {
                    console.log('Success.');
                },
                error: function() {
                    console.log('Error.');
                }
            });
        };
	$('#wiki-page-content').on('blur', save);
	//$('#wiki-page-content').on('datachanged', save);
    });
});
