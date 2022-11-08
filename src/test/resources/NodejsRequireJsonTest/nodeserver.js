function Server(serverFunc){

	this.serverFunc = serverFunc;

  this.listen = function(port) {
    this.port = port
  }
}

function MyHttp() {
  this.createServer = function(serverFunc){
      var server = new Server(serverFunc);

      this.server = server;

      return server;
  }
}

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

function processor (request, response) {
  if (request.method == 'GET') {
      throw new Error('Request must be POST');
  }
  var redirectUri = request.body.redirect_uri;
  response.redirect(redirectUri);
}

var http = new MyHttp();
http.createServer(processor).listen(8080);

request = new Request();
request.method = "POST"

response = new Response();
request.body.redirect_uri = "www.myurl.com";

http.server.serverFunc(request, response);