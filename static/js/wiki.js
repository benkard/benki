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
            jQuery('#wiki-page-content').html(data);
            console.log('Page saved.');
        },
        error: function() {
            console.log('Error saving content.');
        }
    });
    $('#wiki-page-content').removeAttr('contenteditable');
};

/*
jQuery(function ($) {
    $('#wiki-page-content').on('blur', function() {
        mulk.savePage();
        document.designMode = 'off';
    });
    $('body').on('click', function() {
        mulk.savePage();
        document.designMode = 'off';
    });
    $('#wiki-page-content').on('dblclick', function() {
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
        var editable = Aloha.editables[0];
        editable.disable();
	$('#wiki-page-content').on('blur', function() {
            mulk.savePage();
            editable.disable();
        });
        $('#wiki-page-content').dblclick(function() {
            editable.enable();
        });
    });
});

