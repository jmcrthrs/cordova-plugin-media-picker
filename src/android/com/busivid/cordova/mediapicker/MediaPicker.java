package com.busivid.cordova.mediapicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.busivid.cordova.mediapicker.activities.MediaPickerActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MediaPicker extends CordovaPlugin {
	private static final String ERROR_CANCELLED = "CANCELLED";
	private static final String EXTRA_MEDIA_OPTIONS = "extra_media_options";
	private static final int REQUEST_CODE_GET_PICTURES = 1000;
	private static final String PROGRESS_MEDIA_IMPORTED = "MEDIA_IMPORTED";
	private static final String PROGRESS_MEDIA_IMPORTING = "MEDIA_IMPORTING";
	public static String TAG = "MediaPicker";

	private JSONObject _args;
	private CallbackContext _callbackContext;

	private final String[] permissions = { Manifest.permission.WRITE_EXTERNAL_STORAGE };

	private void cleanUp() {
		String tmpPath = getStoragePath(false);

		File tmpDir = new File(tmpPath);
		for (File file : tmpDir.listFiles())
			if (!file.isDirectory())
				if (!file.delete())
					LOG.d(TAG, "unable to delete: " + file.getAbsolutePath());
	}

	private void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);
		try {
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.flush();
		} finally {
			in.close();
			out.close();
		}
	}

	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		_args = args.getJSONObject(0);
		_callbackContext = callbackContext;

		if (action.equals("cleanUp")) {
			cleanUp();
			callbackContext.success();
			return true;
		}

		if (action.equals("getPictures")) {
			if (hasPermission()) {
				getPictures();
			} else {
				PermissionHelper.requestPermissions(this, REQUEST_CODE_GET_PICTURES, permissions);
			}
			return true;
		}

		return false;
	}

	// Name getPictures to match cordova-plugin-camera.
	private void getPictures() throws JSONException {
		final String mediaType = _args.getString("mediaType");

		Boolean includeImages = true;
		Boolean includeVideos = true;
		if (mediaType != null) {
			includeImages = mediaType.equals("image");
			includeVideos = mediaType.equals("video");
		}

		int maxImages = _args.getInt("maxImages");

		MediaOptions.Builder builder = new MediaOptions.Builder();
		builder = builder.canSelectMultiPhoto(true).canSelectMultiVideo(true);

		if (includeImages && includeVideos) {
			builder = builder.canSelectBothPhotoVideo();
		} else if (includeImages) {
			builder = builder.selectPhoto();
		} else if (includeVideos) {
			builder = builder.selectVideo();
		}

		builder.setMaxImages(maxImages);

		final MediaOptions options = builder.build();

		final Context context = this.cordova.getActivity().getApplicationContext();
		final Intent intent = new Intent(context, MediaPickerActivity.class);
		intent.putExtra(EXTRA_MEDIA_OPTIONS, options);
		if (this.cordova != null) {
			this.cordova.startActivityForResult(this, intent, 0);
		}
	}

	private String getStoragePath(Boolean isTemporaryPath) {
		File storageDirectory = isTemporaryPath
			? cordova.getActivity().getCacheDir()
			: cordova.getActivity().getApplicationContext().getFilesDir();

		// Hack for Samsung Galaxy Camera 2
		if (Build.MANUFACTURER.equals("samsung") && Build.MODEL.equals("EK-GC200") && new File("/storage/extSdCard/").canRead())
			storageDirectory = new File("/storage/extSdCard/." + cordova.getActivity().getApplicationContext().getPackageName() + "/");

		storageDirectory = new File(storageDirectory.getAbsolutePath() + "/mediapicker/");

		// Create the storage directory if it doesn't exist
		//noinspection ResultOfMethodCallIgnored
		storageDirectory.mkdirs();

		return storageDirectory.getAbsolutePath();
	}

	private File getWritableFile(String ext, Boolean isTemporaryPath) throws Exception {
		String dataPath = getStoragePath(isTemporaryPath);

		File file;
		for (int i = 0; i <= 99999; i++) {
			file = new File(dataPath + String.format("/capture_%05d." + ext, i));
			if (!file.exists())
				return file;
		}

		throw new Exception("Unable to getWritableFile");
	}

	/**
	 * Note: There is an unrelated incorrectly named function hasPermisssion in CordovaPlugin.
	 */
	private boolean hasPermission() {
		for (String p : permissions)
			if (!PermissionHelper.hasPermission(this, p))
				return false;

		return true;
	}

	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		final CallbackContext callbackContext = _callbackContext;
		final Context context = this.cordova.getActivity().getApplicationContext();
		final JSONObject params = _args;

		new Thread(new Runnable() {
			public void run() {
				final ArrayList<String> fileNames = new ArrayList<String>();
				final Boolean isTemporaryFile = params.optBoolean("isTemporaryFile", true);

				switch (resultCode) {
					case 0:
						callbackContext.error(ERROR_CANCELLED);
						break;

					case -1:
						final List<MediaItem> mediaSelectedList = MediaPickerActivity.getMediaItemSelected(data);

						onMediaImporting(mediaSelectedList.size());

						for (int i = 0; i < mediaSelectedList.size(); i++) {
							File inputFile = new File(mediaSelectedList.get(i).getPathOrigin(context));
							String ext = inputFile.getAbsolutePath().substring(inputFile.getAbsolutePath().lastIndexOf(".") + 1);

							try {
								File outputFile = getWritableFile(ext, isTemporaryFile);
								copyFile(inputFile, outputFile);
								fileNames.add(outputFile.getAbsolutePath());
								onMediaImported(outputFile.getAbsolutePath());
							} catch (Exception exception) {
								callbackContext.error(exception.getMessage());
								return;
							}
						}

						final JSONArray res = new JSONArray(fileNames);
						callbackContext.success(res);
						break;

					default:
						callbackContext.error(resultCode);
						break;
				}
			}
		}).start();

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void onMediaImported(String path) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("data", path);
			jsonObj.put("type", PROGRESS_MEDIA_IMPORTED);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		PluginResult progressResult = new PluginResult(PluginResult.Status.OK, jsonObj);
		progressResult.setKeepCallback(true);

		_callbackContext.sendPluginResult(progressResult);
	}

	private void onMediaImporting(int count) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("data", count);
			jsonObj.put("type", PROGRESS_MEDIA_IMPORTING);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		PluginResult progressResult = new PluginResult(PluginResult.Status.OK, jsonObj);
		progressResult.setKeepCallback(true);

		_callbackContext.sendPluginResult(progressResult);
	}

	public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
		PluginResult result;
		if (_callbackContext != null) {
			for (int r : grantResults) {
				if (r == PackageManager.PERMISSION_DENIED) {
					result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
					_callbackContext.sendPluginResult(result);
					return;
				}
			}

			if (requestCode == REQUEST_CODE_GET_PICTURES) {
				getPictures();
			}
		}
	}
}
