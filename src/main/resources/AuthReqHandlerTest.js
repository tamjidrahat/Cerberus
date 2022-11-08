
function Request() {
  this.body = {}
  this.method = null;
}

function Response() {
  this.redirect = function(url) {
    this.Location =  url;
    this.status = 302;
  };
}

processor.prototype.isValid= function(redirect_uri) {
  var index = redirect_uri.indexOf('#');
  if ( index != -1) {
    return redirect_uri;
  }
  return redirect_uri;
};

function processor (request, response) {

  var redirectUri = request.body.redirect_uri;
  var valid = this.isValid(redirectUri);
  if (!valid) {
      throw new Error('Redirect URI cannot contain any fragment');
  }

  //response.redirect(redirectUri);
}





request = new Request();
request.method = "POST";
request.body.redirect_uri = "www.myurl.com";
request.body.client_id = "12345";

response = new Response();
new processor(request, response);
