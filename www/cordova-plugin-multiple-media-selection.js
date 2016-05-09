var exec = require('cordova/exec');

var MultipleMediaSelection = function(){};

MultipleMediaSelection.prototype.getPictures = function(success, error, options) {
	options = options || {};

	options.minImages = options.minImages || 0;
	options.maxImages = options.maxImages || 5;
	options.mediaType = options.mediaType || 'any';
	options.width = options.width || 0;
	options.height = options.height || 0;
	options.quality = options.quality || 100;

	exec(success, error, 'MultipleMediaSelection', 'getPictures', [options]);
};

window.multipleMediaSelection = new MultipleMediaSelection();
