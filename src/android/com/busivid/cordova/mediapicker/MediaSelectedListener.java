package com.busivid.cordova.mediapicker;

import java.util.List;

/**
 * Listener for select media item.
 *
 */
public interface MediaSelectedListener {
	public void onHasNoSelected();

	public void onHasSelected(List<MediaItem> mediaSelectedList);
}
