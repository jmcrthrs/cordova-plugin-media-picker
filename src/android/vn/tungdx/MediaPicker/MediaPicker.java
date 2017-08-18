/**
 * A Media Picker Plugin for Cordova/PhoneGap.
 */
package vn.tungdx.mediapicker;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.util.Log;
import android.os.Environment;
import android.os.Build;


import vn.tungdx.mediapicker.activities.MediaPickerActivity;
import com.busivid.one.R;

public class MediaPicker extends CordovaPlugin {
	public static String TAG = "MediaPicker";
	private static final String EXTRA_MEDIA_OPTIONS = "extra_media_options";

	private CallbackContext callbackContext;
	private JSONObject params;

	private int REQUEST_CODE_GET_PICTURES = 1000;

	String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		this.params = args.getJSONObject(0);
		if (action.equals("getPictures")) {
			if (hasPermisssion()) {
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
		String mediaType = this.params.getString("mediaType");
		Boolean includeImages = true;
		Boolean includeVideos = true;

		if (mediaType != null) {
			includeImages = mediaType.equals("image");
			includeVideos = mediaType.equals("video");
		}

		int maxImages = this.params.getInt("maxImages");

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

		MediaOptions options = builder.build();

		Context context = this.cordova.getActivity().getApplicationContext();
		Intent intent = new Intent(context, MediaPickerActivity.class);
		intent.putExtra(EXTRA_MEDIA_OPTIONS, options);
		if (this.cordova != null) {
			this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
		}
	}

	public void onRequestPermissionResult(int requestCode, String[] permissions,
										  int[] grantResults) throws JSONException {
		PluginResult result;
		if (callbackContext != null) {
			for (int r : grantResults) {
				if (r == PackageManager.PERMISSION_DENIED) {
					result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
					callbackContext.sendPluginResult(result);
					return;
				}
			}

			if (requestCode == REQUEST_CODE_GET_PICTURES) {
				getPictures();
			}
		}
	}

	public boolean hasPermisssion() {
		for (String p : permissions) {
			if (!PermissionHelper.hasPermission(this, p)) {
				return false;
			}
		}
		return true;
	}

	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		final CallbackContext callbackContext = this.callbackContext;
		final Context context = this.cordova.getActivity().getApplicationContext();
		final JSONObject params = this.params;
		new Thread(new Runnable() {
			public void run() {
				ArrayList<String> fileNames = new ArrayList<String>();

				switch (resultCode) {
					case 0:
						callbackContext.error("Cancelled");
						break;
					case -1:
						List<MediaItem> mediaSelectedList = MediaPickerActivity
								.getMediaItemSelected(data);

						for (int i = 0; i < mediaSelectedList.size(); i++) {
							File inputFile = new File(mediaSelectedList.get(i).getPathOrigin(context).toString());

							Boolean isTemporaryFile = params.optBoolean("isTemporaryFile", true);

							String ext = inputFile.getAbsolutePath().substring(inputFile.getAbsolutePath().lastIndexOf(".") + 1);
							File outputFile = getWritableFile(ext, isTemporaryFile);

							try {
								copyFile(inputFile, outputFile);
							} catch (IOException exception) {
								callbackContext.error(exception.getMessage());
								return;
							}

							fileNames.add(outputFile.getAbsolutePath());
						}

						JSONArray res = new JSONArray(fileNames);
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

	private File getWritableFile(String ext, Boolean isTemporaryPath) {
		int i = 1;
		File storageDirectory = isTemporaryPath
				? cordova.getActivity().getCacheDir()
				: cordova.getActivity().getApplicationContext().getFilesDir();

		//hack for galaxy camera 2.
		if (Build.MODEL.equals("EK-GC200") && Build.MANUFACTURER.equals("samsung") && new File("/storage/extSdCard/").canRead()) {
			storageDirectory = new File("/storage/extSdCard/.com.buzzcard.brandingtool/");
		}

		// Create the storage directory if it doesn't exist
		storageDirectory.mkdirs();
		String dataPath = storageDirectory.getAbsolutePath();
		File file;
		do {
			file = new File(dataPath + String.format("/capture_%05d." + ext, i));
			i++;
		} while (file.exists());

		return file;
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
}
