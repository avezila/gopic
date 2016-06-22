db.user.ensureIndex({id:1},{unique:true});
db.user.ensureIndex({email:1},{unique:true});
db.user.ensureIndex({login:1},{unique:true});
db.user.ensureIndex({session:1});
