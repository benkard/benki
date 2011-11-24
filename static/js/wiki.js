var mulk;

if (!mulk) {
    mulk = {};
}

mulk.savePage = function() {
    jQuery.ajax({
        url: "?save",
        data: { content: $('#wiki-page-content').html() },
        type: "POST",
        dataType: "text",
        success: function(data) {
            mulk.savePage();
            console.log(data);
            jQuery('#wiki-page-content').html(data);
            console.log('Success.');
        },
        error: function() {
            console.log('Error.');
        }
    });
};

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
	ribbon: false,

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
            mulk.savePage();
        };
	$('#wiki-page-content').on('blur', save);
	//$('#wiki-page-content').on('datachanged', save);
    });
});

