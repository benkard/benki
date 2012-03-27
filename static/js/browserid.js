// -*- js-indent-level: 2 -*-

jQuery(function($) {  
  var loggedIn = function(res) {
    console.log(res);
    if (res.returnURI) {
      window.location.assign(res.returnURI);
    } else {
      window.location.reload(true);
    }
  };
  var loggedOut = function(res) {
  };

  var gotAssertion = function(assertion) {
    // got an assertion, now send it up to the server for verification
    if (assertion) {
      $.ajax({
        type: 'POST',
        url: '/login/browserid/verify',
        data: { assertion: assertion },
        success: function(res, status, xhr) {
          if (res === null) {
            loggedOut();
          }
          else {
            loggedIn(res);
          }
        },
        error: function(res, status, xhr) {
          //console.log(res);
          //console.log(status);
          alert("Whoops, I failed to authenticate you! " + res.responseText);
        }
      });
    } else {
      loggedOut();
    }
  }

  $('#browserid').click(function() {
    navigator.id.get(gotAssertion, {allowPersistent: true});
    return false;
  });

  // Query persistent login.
  var login = $('head').attr('data-logged-in');
  if (login === "false") {
    navigator.id.get(gotAssertion, {silent: true});
  }
});
