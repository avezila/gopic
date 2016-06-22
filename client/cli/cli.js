var prompt = require('prompt');
var grpc = require('grpc');
var fs = require('fs');

var repo = process.argv[2] || "localhost:5353";

if (!repo.match(/\:\d+/))
  repo = repo+":5353";

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
var rpcs = {};
prompt.start();
var inited = false;
function showServices(){
  console.log("\n");
  for (var pack in rpc){
    for (var service in rpc[pack]){
      var s = rpc[pack][service].service;
      if (!s) continue;
      if (!inited){
        client[service] = new rpc[pack][service](repo,
            grpc.credentials.createInsecure());
        for (var name in client[service].__proto__){
          rpcs[service.toLowerCase()+"."+name.toLowerCase()] = [client[service],client[service][name]];//.bind(client[service]);
          rpcs[name.toLowerCase()] = [client[service],client[service][name]];//.bind(client[service]);
        };
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
	//var [service,func] = res.call.split(".");
  //console.log(service,func)
  //var obj = client[service];
  //console.log(Object.keys(obj.__proto__))
  var foo = rpcs[res.call.toLowerCase()][1]; // obj[func.toLowerCase()];
  var obj = rpcs[res.call.toLowerCase()][0]; // obj[func.toLowerCase()];
  if (!foo){
    console.error("unknown rpc");
    return getServ();
  }
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
  var t = new Date().getTime();
  foo.call(obj,res,function(err,res){
    if (err){
      console.error("err:",err);
    }else {
      console.log("response:", res);
    }
    console.log(`time: ${new Date().getTime()-t}ms`);
    return getServ(); 
  });
  //console.log(obj,foo,err,res);
}

function onErr(err) {
	//console.log(err);
  return 1;
}

getServ();
