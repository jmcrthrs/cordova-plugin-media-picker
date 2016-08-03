/**
 * A Media Picker Plugin for Cordova/PhoneGap.
 */
package vn.tungdx.mediapicker;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

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
import android.content.Intent;
import android.util.Log;

import vn.tungdx.mediapicker.activities.MediaPickerActivity;
import com.buzzcard.brandingtool.R;
import android.content.Context;

public class MediaPicker extends CordovaPlugin {
	public static String TAG = "MediaPicker";
	private static final String EXTRA_MEDIA_OPTIONS = "extra_media_options";

	private CallbackContext callbackContext;
	private JSONObject params;

	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		this.params = args.getJSONObject(0);
		if (action.equals("getPictures")) {
			MediaOptions.Builder builder = new MediaOptions.Builder();
			MediaOptions options = builder.canSelectBothPhotoVideo().canSelectMultiVideo(true).canSelectMultiPhoto(true).build();

			Context context=this.cordova.getActivity().getApplicationContext();
			Intent intent = new Intent(context, MediaPickerActivity.class);
			intent.putExtra(EXTRA_MEDIA_OPTIONS, options);

			if (this.cordova != null) {
				this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
			}
		}

		return true;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Context context=this.cordova.getActivity().getApplicationContext();

		ArrayList<String> fileNames = new ArrayList<String>();
		if (resultCode == -1) {
			List<MediaItem> mediaSelectedList = MediaPickerActivity
					.getMediaItemSelected(data);

			for (int i = 0; i < mediaSelectedList.size(); i++) {
				File inputFile = new File(mediaSelectedList.get(i).getPathOrigin(context).toString());
				String ext = inputFile.getAbsolutePath().substring(inputFile.getAbsolutePath().lastIndexOf("."));
				File outputFile = getWritableFile(ext);

				try {
					copyFile(inputFile, outputFile);
				} catch (IOException exception) {
					this.callbackContext.error(exception.getMessage());
					return;
				}

				fileNames.add(outputFile.getAbsolutePath());
			}

			JSONArray res = new JSONArray(fileNames);
			this.callbackContext.success(res);
			return;
		}

		JSONArray res = new JSONArray(fileNames);
		this.callbackContext.success(res);
	}

	private File getWritableFile(String ext) {
		int i = 1;
		File dataDirectory = cordova.getActivity().getApplicationContext().getFilesDir();

		// Create the data directory if it doesn't exist
		dataDirectory.mkdirs();
		String dataPath = dataDirectory.getAbsolutePath();
		File file;
		do {
			file = new File(dataPath + String.format("/capture_%05d." + ext, i));
			i++;
		} while (file.exists());

		return file;
	}

	public void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
}