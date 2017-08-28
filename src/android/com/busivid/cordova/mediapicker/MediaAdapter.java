package com.busivid.cordova.mediapicker;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView.RecyclerListener;
import android.widget.RelativeLayout;

import com.busivid.cordova.mediapicker.imageloader.MediaImageLoader;
import com.busivid.cordova.mediapicker.utils.MediaUtils;
import com.busivid.cordova.mediapicker.widget.PickerImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for display media item list.
 */
public class MediaAdapter extends CursorAdapter implements RecyclerListener {
	private int mMediaType;
	private MediaImageLoader mMediaImageLoader;
	private List<MediaItem> mMediaListSelected = new ArrayList<MediaItem>();
	private MediaOptions mMediaOptions;
	private int mItemHeight = 0;
	private int mNumColumns = 0;
	private RelativeLayout.LayoutParams mImageViewLayoutParams;
	private List<PickerImageView> mPickerImageViewSelected = new ArrayList<PickerImageView>();

	public MediaAdapter(Context context, Cursor c, int flags, MediaImageLoader mediaImageLoader, int mediaType,
			MediaOptions mediaOptions) {
		this(context, c, flags, null, mediaImageLoader, mediaType, mediaOptions);
	}

	public MediaAdapter(Context context, Cursor c, int flags, List<MediaItem> mediaListSelected,
			MediaImageLoader mediaImageLoader, int mediaType, MediaOptions mediaOptions) {
		super(context, c, flags);
		if (mediaListSelected != null)
			mMediaListSelected = mediaListSelected;
		mMediaImageLoader = mediaImageLoader;
		mMediaType = mediaType;
		mMediaOptions = mediaOptions;
		mImageViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final ViewHolder holder = (ViewHolder) view.getTag();
		final String path;
		Uri uri;

		if (mMediaType == MediaItem.PHOTO) {
			uri = MediaUtils.getPhotoUri(cursor);
			path = MediaUtils.getRealImagePathFromURI(context.getContentResolver(), uri);

			holder.imageView.label = "";
			holder.thumbnail.setVisibility(View.GONE);
		} else {
			uri = MediaUtils.getVideoUri(cursor);
			path = MediaUtils.getRealVideoPathFromURI(context.getContentResolver(), uri);

			MediaMetadataRetriever mmr = new MediaMetadataRetriever();
			mmr.setDataSource(path);

			float durationMillis = Float.parseFloat(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
			dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

			holder.imageView.label = dateFormat.format(durationMillis).replaceAll("^00:", "");
			holder.thumbnail.setVisibility(View.VISIBLE);
		}

		boolean isSelected = isSelected(uri);
		holder.imageView.setSelected(isSelected);
		if (isSelected) {
			mPickerImageViewSelected.add(holder.imageView);
		}

		uri = Uri.fromFile(new java.io.File(path));
		mMediaImageLoader.displayImage(uri, holder.imageView);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		ViewHolder holder = new ViewHolder();
		View root = View.inflate(mContext, mContext.getResources().getIdentifier("list_item_mediapicker", "layout", mContext.getPackageName()), null);
		holder.imageView = (PickerImageView) root.findViewById(mContext.getResources().getIdentifier("thumbnail", "id", mContext.getPackageName()));
		holder.thumbnail = root.findViewById(mContext.getResources().getIdentifier("overlay", "id", mContext.getPackageName()));

		holder.imageView.setLayoutParams(mImageViewLayoutParams);
		// Check the height matches our calculated column width
		if (holder.imageView.getLayoutParams().height != mItemHeight) {
			holder.imageView.setLayoutParams(mImageViewLayoutParams);
		}
		root.setTag(holder);
		return root;
	}

	private class ViewHolder {
		PickerImageView imageView;
		View thumbnail;
	}

	public boolean hasSelected() {
		return mMediaListSelected.size() > 0;
	}

	/**
	 * Check media uri is selected or not.
	 *
	 * @param uri Uri of media item (photo, video)
	 * @return true if selected, false otherwise.
	 */
	public boolean isSelected(Uri uri) {
		if (uri == null)
			return false;
		for (MediaItem item : mMediaListSelected) {
			if (item.getUriOrigin().equals(uri))
				return true;
		}
		return false;
	}

	/**
	 * Check {@link MediaItem} is selected or not.
	 *
	 * @param item {@link MediaItem} to check.
	 * @return true if selected, false otherwise.
	 */
	public boolean isSelected(MediaItem item) {
		return mMediaListSelected.contains(item);
	}

	/**
	 * Set {@link MediaItem} selected.
	 *
	 * @param item {@link MediaItem} to selected.
	 */
	public void setMediaSelected(MediaItem item) {
		syncMediaSelectedAsOptions();
		if (!mMediaListSelected.contains(item))
			mMediaListSelected.add(item);
	}

	/**
	 * If item selected then change to unselected and unselected to selected.
	 *
	 * @param item Item to update.
	 */
	public void updateMediaSelected(MediaItem item, PickerImageView pickerImageView) {
		if (mMediaListSelected.contains(item)) {
			mMediaListSelected.remove(item);
			pickerImageView.setSelected(false);
			this.mPickerImageViewSelected.remove(pickerImageView);
		} else {
			boolean value = syncMediaSelectedAsOptions();
			if (value) {
				for (PickerImageView picker : this.mPickerImageViewSelected) {
					picker.setSelected(false);
				}
				this.mPickerImageViewSelected.clear();
			}
			mMediaListSelected.add(item);
			pickerImageView.setSelected(true);
			this.mPickerImageViewSelected.add(pickerImageView);
		}
	}

	/**
	 * @return List of {@link MediaItem} selected.
	 */
	public List<MediaItem> getMediaSelectedList() {
		return mMediaListSelected;
	}

	/**
	 * Set list of {@link MediaItem} selected.
	 *
	 * @param list
	 */
	public void setMediaSelectedList(List<MediaItem> list) {
		mMediaListSelected = list;
	}

	/**
	 * Whether clear or not media selected as options.
	 *
	 * @return true if clear, false otherwise.
	 */
	private boolean syncMediaSelectedAsOptions() {
		switch (mMediaType) {
		case MediaItem.PHOTO:
			if (!mMediaOptions.canSelectMultiPhoto()) {
				mMediaListSelected.clear();
				return true;
			}
			break;
		case MediaItem.VIDEO:
			if (!mMediaOptions.canSelectMultiVideo()) {
				mMediaListSelected.clear();
				return true;
			}

			break;
		default:
			break;
		}
		return false;
	}

	/**
	 * {@link MediaItem#VIDEO} or {@link MediaItem#PHOTO}
	 *
	 * @param mediaType
	 */
	public void setMediaType(int mediaType) {
		mMediaType = mediaType;
	}

	// set numcols
	public void setNumColumns(int numColumns) {
		mNumColumns = numColumns;
	}

	public int getNumColumns() {
		return mNumColumns;
	}

	// set photo item height
	public void setItemHeight(int height) {
		if (height == mItemHeight) {
			return;
		}
		mItemHeight = height;
		mImageViewLayoutParams.height = height;
		mImageViewLayoutParams.width = height;
		notifyDataSetChanged();
	}

	@Override
	public void onMovedToScrapHeap(View view) {
		PickerImageView imageView = (PickerImageView) view.findViewById(mContext.getResources().getIdentifier("thumbnail", "id", mContext.getPackageName()));
		mPickerImageViewSelected.remove(imageView);
	}

	public void onDestroyView() {
		mPickerImageViewSelected.clear();
	}
}
