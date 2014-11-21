/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-glom.
 *
 * android-glom is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-glom is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with android-glom.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

/**
 * Created by murrayc on 5/16/14.
 */
class ListCursorAdapter extends RecyclerView.Adapter<ListCursorAdapter.ViewHolder> {

    private class CursorObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            //TODO: Be more specific?
            notifyDataSetChanged();
        }
    }

    private final CursorObserver mDataSetObserver = new CursorObserver();

    public Cursor getItem(int position) {
        //TODO Clone or copy it somehow?
        //What does CursorAdapter (for ListView and GridView) do?
        mCursor.moveToPosition(position);
        return mCursor;
    }

    public static interface OnItemClickedListener {
        public void onItemClicked(int position, final View sharedElementView);
    }

    private final Context mContext;
    private Cursor mCursor = null;
    private final OnItemClickedListener mListener;

    ListCursorAdapter(final Context context, final Cursor cursor, final OnItemClickedListener listener) {
        mContext = context;
        mListener = listener;
        changeCursor(cursor);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View v = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.gridview_cell_fragment_list, null);
        return new ViewHolder(v, this);
    }

    class ShowViewHolderImageFromContentProviderTask extends UiUtils.ShowImageFromContentProviderTask {
        final WeakReference<ViewHolder> viewHolderReference;
        final int position;
        final String itemId;

        public ShowViewHolderImageFromContentProviderTask(final Context fragment, final ViewHolder viewHolder, int position, final String itemId) {
            super(viewHolder.imageView, fragment);

            this.viewHolderReference = new WeakReference<>(viewHolder);
            this.position = position;
            this.itemId = itemId;
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {

            if (viewHolderReference == null) {
                return;
            }

            final ViewHolder viewHolder = viewHolderReference.get();
            if (viewHolder == null) {
                return;
            }

            //Check that we are still dealing with the same position,
            //because the ImageView might be recycled for use with a different position.
            if (viewHolder.getPosition() != position) {
                return;
            }

            super.onPostExecute(bitmap);

            if (bitmap != null) {
                //Hide the progress indicator now that we are showing the image.
                viewHolder.progressBar.setVisibility(View.GONE);
            } else {
                //Show the progress indicator because we have no image:
                viewHolder.progressBar.setVisibility(View.VISIBLE);

                //Something was wrong with the (cached) image,
                //so just abandon this whole item.
                //That seems safer and simpler than trying to recover just one of the 3 images.
                Utils.abandonItem(mContext, itemId);
            }
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        mCursor.moveToPosition(i);

        final String itemId = mCursor.getString(ListFragment.COLUMN_INDEX_ID);
        final String imageUriStr = mCursor.getString(ListFragment.COLUMN_INDEX_LOCATION_THUMBNAIL_URI);
        final boolean thumbnailDownloaded = (mCursor.getInt(ListFragment.COLUMN_INDEX_LOCATION_THUMBNAIL_DOWNLOADED) == 1);
        final boolean done = (mCursor.getInt(ListFragment.COLUMN_INDEX_DONE) == 1);
        final boolean uploaded = (mCursor.getInt(ListFragment.COLUMN_INDEX_UPLOADED) == 1);
        final boolean favorite = (mCursor.getInt(ListFragment.COLUMN_INDEX_FAVOURITE) == 1);

        /*
        final TextView textView = (TextView) view.findViewById(R.id.item_text);
        if (textView != null) {
            textView.setText(subjectId);
        }
        */

        if (!TextUtils.isEmpty(imageUriStr)) {
            if (thumbnailDownloaded) {
                //viewHolder.imageView.setImageDrawable(null);
                final ShowViewHolderImageFromContentProviderTask task = new ShowViewHolderImageFromContentProviderTask(mContext, viewHolder, viewHolder.getPosition(), itemId);
                task.execute(imageUriStr);
            } else {
                //We are still waiting for it to download:
                viewHolder.progressBar.setVisibility(View.VISIBLE);
            }

            if (!favorite && !done && !uploaded) {
                viewHolder.iconsPanel.setVisibility(View.GONE);
            } else {
                viewHolder.iconsPanel.setVisibility(View.VISIBLE);
                viewHolder.checkboxFavorite.setVisibility(favorite ? View.VISIBLE : View.GONE);
                viewHolder.checkboxClassified.setVisibility(done ? View.VISIBLE : View.GONE);
                viewHolder.checkboxUploaded.setVisibility(uploaded ? View.VISIBLE : View.GONE);
            }

            //Don't allow the item to be selected (for viewing or classifying)
            //if it is not done yet, so the user cannot skip ahead to classify
            //only interesting images:
            viewHolder.imageView.setEnabled(done);
        }

        //holder.itemView.setTag(item);
    }

    @Override
    public int getItemCount() {
        if(mCursor == null) {
            return 0;
        }

        return mCursor.getCount();
    }

    public void changeCursor(final Cursor cursor) {
        final boolean changed = (mCursor != cursor);

        //TODO: our CursorObserver.onChanged() method never seems to be called
        //but RecyclerView seems to call our getCount() every now and then anyway.
        if (mCursor != null ) {
            mCursor.unregisterDataSetObserver(mDataSetObserver);
        }

        mCursor = cursor;

        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
        }

        //TODO: Can we use the more specific methods.
        //See https://developer.android.com/reference/android/support/v7/widget/RecyclerView.Adapter.html#notifyDataSetChanged%28%29
        if (changed) {
            notifyDataSetChanged();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final WeakReference<ListCursorAdapter> refParent;
        public final ImageView imageView;
        final LinearLayout iconsPanel;
        final ImageView checkboxFavorite;
        final ImageView checkboxClassified;
        final ImageView checkboxUploaded;
        final ProgressBar progressBar;

        public ViewHolder(final View v, final ListCursorAdapter parent) {
            super(v);

            refParent = new WeakReference<>(parent);

            imageView = (ImageView) v.findViewById(R.id.item_image);
            iconsPanel = (LinearLayout) v.findViewById(R.id.itemIconsPanel);
            checkboxFavorite = (ImageView) v.findViewById(R.id.item_checkboxFavorite);
            checkboxClassified = (ImageView) v.findViewById(R.id.item_checkboxClassified);
            checkboxUploaded = (ImageView) v.findViewById(R.id.item_checkboxUploaded);
            progressBar = (ProgressBar) v.findViewById(R.id.imageProgressBar);

            imageView.setOnClickListener(this);
        }

        @Override
        public void onClick(final View v) {
            if (refParent == null) {
                return;
            }

            final ListCursorAdapter parent = refParent.get();
            if (parent == null) {
                return;
            }

            if (parent.mListener == null) {
                return;
            }

            parent.mListener.onItemClicked(getPosition(), imageView);
        }
    }

}
