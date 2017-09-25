var MediaPicker = function() {
	
};
	
MediaPicker.prototype.getPictures = function(success, fail, options) {
	options = options || {};

	options.height = options.height || 0;
	options.isTemporaryFile = typeof options.isTemporaryFile === 'undefined' || options.isTemporaryFile == true;
	options.maxImages = options.maxImages || 20;
	options.mediaType = options.mediaType || 'any';
	options.minImages = options.minImages || 0;
	options.progress = options.progress || function(result) {};
	options.quality = options.quality || 100;
	options.width = options.width || 0;

	successCallback = function(result) {
		if (typeof result !== 'undefined' && typeof result.type !== 'undefined' && typeof options.progress === 'function') {
			options.progress(result);
			return;
		}

		success(result);
	}

	return cordova.exec(successCallback, fail, 'MediaPicker', 'getPictures', [options]);
};

window.mediaPicker = new MediaPicker();


/*var exec = require('cordova/exec');

exports.ERROR_CANCELLED = 'CANCELLED';
exports.PROGRESS_MEDIA_IMPORTED = 'MEDIA_IMPORTED';
exports.PROGRESS_MEDIA_IMPORTING = 'MEDIA_IMPORTING';

exports.cleanUp = function (success, error) {
	options = {};

	exec(success, error, 'MediaPicker', 'cleanUp', [options]);
}

exports.getPictures = function (success, error, options) {
	options = options || {};

	options.height = options.height || 0;
	options.isTemporaryFile = typeof options.isTemporaryFile === 'undefined' || options.isTemporaryFile == true;
	options.maxImages = options.maxImages || 5;
	options.mediaType = options.mediaType || 'any';
	options.minImages = options.minImages || 0;
	options.progress = options.progress || function(result) {};
	options.quality = options.quality || 100;
	options.width = options.width || 0;

	successCallback = function(result) {
		if (typeof result !== 'undefined' && typeof result.type !== 'undefined' && typeof options.progress === 'function') {
			options.progress(result);
			return;
		}

		success(result);
	}

	exec(successCallback, error, 'MediaPicker', 'getPictures', [options]);
};

exports.requestPermission = function(success, error) {
	options = {};

	exec(success, error, 'MediaPicker', 'requestPermission', [options]);
};

*/
