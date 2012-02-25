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
