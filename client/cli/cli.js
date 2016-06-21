var prompt = require('prompt');
var grpc = require('grpc');
var fs = require('fs');

/*
var auth = grpc.load('./grpc/auth.proto').auth;
var c = new auth.Auth("localhost:5353",grpc.credentials.createInsecure());
console.log(auth)
console.log(auth.Auth.prototype);
console.log(c.register);
return;
*/

dir = fs.readdirSync("grpc");
var rpc = {};
dir.forEach(function(file){
  f = grpc.load("grpc/"+file);
  rpc = Object.assign(rpc,f);
});

var client = {};
prompt.start();
var inited = false;
function showServices(){
  console.log("\n");
  for (var pack in rpc){
    for (var service in rpc[pack]){
      var s = rpc[pack][service].service;
      if (!s) continue;
      if (!inited){
        client[service] = new rpc[pack][service]("localhost:5353",
            grpc.credentials.createInsecure());
        //console.log(client[service].__proto__)
      }
      for (var i in s.children){
        console.log(`${s.name}.${s.children[i].name}()`);
      }
    }
  }
  inited = true;
  console.log();
}

function getServ (){
  showServices();
	prompt.get(['call'], getParams);
}

function getParams (err,res){
	if (err)return getServ();
	var [service,func] = res.call.split(".");
  //console.log(service,func)
  var obj = client[service];
  var foo = obj[func.toLowerCase()];
  var types = foo.requestType._fieldsByName;
  console.log(foo.requestType.name+":")
  for (var key in types){
    var t = types[key];
    var s = key+" ";
    if (t.repeated)
      s+="[]";
    s+= t.type.name;
    console.log(s);
  }
  prompt.get(Object.keys(types), sendRequest.bind(this,obj,foo));
}

function sendRequest(obj,foo,err,res){
  if (err) return getServ();
  foo.call(obj,res,function(err,res){
    if (err){
      console.error("err:",err);
    }else {
      console.log("response:", res.message);
    }
    return getServ(); 
  });
  //console.log(obj,foo,err,res);
}

function onErr(err) {
	//console.log(err);
  return 1;
}

getServ();
