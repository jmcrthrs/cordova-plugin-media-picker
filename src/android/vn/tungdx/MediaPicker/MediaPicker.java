/**
 * A Media Picker Plugin for Cordova/PhoneGap.
 */
package vn.tungdx.mediapicker;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
				fileNames.add(mediaSelectedList.get(i).getPathOrigin(context).toString());
			}

			JSONArray res = new JSONArray(fileNames);
			this.callbackContext.success(res);
			return;
		}

		JSONArray res = new JSONArray(fileNames);
		this.callbackContext.success(res);
	}
}