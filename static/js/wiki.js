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
};

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
            linklist: "/3rdparty/alohaeditor/aloha/plugins/common/link/extra"
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

        $('html').click(function() {
            mulk.savePage();
            $$('#wiki-page-content').mahalo();
        });
        $('#wiki-page-content').click(function(event) {
            event.stopPropagation();
        });
        $('.aloha-floatingmenu').click(function(event) {
            event.stopPropagation();
        });
        $('.aloha-sidebar-bar').click(function(event) {
            event.stopPropagation();
        });

        $('#wiki-page-content').click(function() {
            $$('#wiki-page-content').aloha();
        });
    });
});
