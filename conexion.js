var mysql      = require('mysql')
var connection = mysql.createConnection({
  host     : 'localhost:3306',
  user     : 'alejandro',
  password : '123456'
});
 
connection.connect(function(err) {
  if (err) {
    console.error('error connecting: ' + err.stack);
    return;
  }
 
  console.log('connected as id ' + connection.threadId);
});

