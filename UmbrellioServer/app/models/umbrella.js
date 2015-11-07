var mongoose     = require('mongoose');
var Schema       = mongoose.Schema;


var UmbrellaSchema   = new Schema({
	id : String,
	heat: Number,
	volume: Number,
	timestamp : Number,
	latitude: Number,
	longitude: Number
});

module.exports = mongoose.model('Umbrella', UmbrellaSchema);