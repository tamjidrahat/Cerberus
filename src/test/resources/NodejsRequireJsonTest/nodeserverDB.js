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

function DB() {
  this.read = function(key) {
    return true;
  };
  this.write = function(key, value) {
    return true;
  };
  this.delete = function(key) {
    return true;
  };
}

function Verrifier() {
  this.isNull = function(value) {
      if (value) {
        return false;
      }
      return true;
  };
}
function RegExp(pattern) {
    this.test = function(val) {
        return true;
    };
}
function processor (request, response) {

//  if (new Verrifier().isNull(request.method) == true) {
//      throw new Error('Request method cant be null');
//  }
  if (new RegExp('foo*').test(request.method)) {
      throw new Error('Request method cant be null');
  }

  if (request.method == 'GET') {
      throw new Error('Request must be POST');
  }

  var redirectUri = request.body.redirect_uri;
  var client_id = request.body.client_id;

  new DB().read(client_id);

  response.redirect(redirectUri);
}


var http = new MyHttp();
http.createServer(processor).listen(8080);

request = new Request();
request.method = "POST";
request.body.redirect_uri = "www.myurl.com";
request.body.client_id = "12345";

response = new Response();

http.server.serverFunc(request, response);
