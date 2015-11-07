// BASE SETUP
// =============================================================================

// call the packages we need
var express = require('express');
var bodyParser = require('body-parser');
var app = express();
var morgan = require('morgan');
var autoIncrement = require('mongoose-auto-increment');

// configure app
app.use(morgan('dev')); // log requests to the console

function defaultContentTypeMiddleware(req, res, next) {
    req.headers['content-type'] = req.headers['content-type'] || 'application/xml';
    next();
}
app.use(defaultContentTypeMiddleware);
// configure body parser
app.use(bodyParser.json()); // for parsing application/json
app.use(bodyParser.urlencoded({extended: true})); // for parsing application/x-www-form-urlencoded


var port = process.env.PORT || 8080; // set our port

var mongoose = require('mongoose');
var vcap_services = JSON.parse(process.env.VCAP_SERVICES);
var mongocon = vcap_services.mongodb[0].credentials.uri;

mongoose.connect(mongocon); // connect to our database

mongoose.connection.on('connected', function () {
    console.log("mongo connected");
});
mongoose.connection.on('error', function () {
    console.log("mongo error");
});
mongoose.connection.on('disconnected', function () {
    console.log("mongo disconnected");
});

var Umbrella = require('./app/models/umbrella');

var parseString = require('xml2js').parseString;

// ROUTES FOR OUR API
// =============================================================================

// create our router
var router = express.Router();

// middleware to use for all requests
router.use(function (req, res, next) {
    // do logging
    console.log('Something is happening.');
    next();
});

// test route to make sure everything is working (accessed at GET http://localhost:8080/api)
router.get('/', function (req, res) {
    res.json({message: 'hooray! welcome to our api!'});
});

router.route('/umbrella')
    .post(function (req, res) {
        var body = "";
        req.on('data', function (chunk) {
            body += chunk;
        });
        req.on('end', function () {
            //console.log(req.method + ' ' + req.url);
            //console.log(req.headers);
            //console.dir(body);

            parseString(body, function (err, result) {
                //var js = JSON.parse(result);
                if(!err){
                    console.log("json obj: ---------------------");
                    if(result.DevEUI_uplink){
                        console.log(result.DevEUI_uplink.payload_hex);

                        var umb = new Umbrella();
                        umb.id = result.DevEUI_uplink.DevEUI[0];
                        umb.longitude = result.DevEUI_uplink.LrrLON[0];
                        umb.latitude = result.DevEUI_uplink.LrrLAT[0];

                        umb.save(function (err) {
                            if (err)
                                res.send(err);

                            res.json({message: 'umbrella created!'});
                        });

                    }else{
                        console.log("nope");
                    }
                    console.log("json obj: ---------------------");
                }
            });
            res.json({message: 'sth went wromg perhaps'});
        })
    })
    .get(function (req, res) {
        Umbrella.find(function (err, umbrellas) {
            if (err) {res.send(err)}
            res.json(umbrellas);
        });
    });

router.route('/umbrella/dummyData')
    .post(function (req, res) {
        var umb = new Umbrella();
        var blubb = JSON.stringify(req.body);
        console.dir(req.body);
        console.log(req.params);
        umb.id = req.body.id;
        umb.latitude = req.body.latitude;
        umb.longitude = req.body.longitude;
        umb.volume = req.body.volume;
        umb.heat = req.body.heat;
        umb.id = 1;

        umb.save(function (err) {
            if (err)
                res.send(err);

            res.json({message: req.body});
        });
    });




// REGISTER OUR ROUTES -------------------------------
app.use('/api', router);

// START THE SERVER
// =============================================================================
app.listen(port);
console.log('Magic happens on port ' + port);
