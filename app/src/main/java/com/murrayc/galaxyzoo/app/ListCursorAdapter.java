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
import android.support.v4.util.LruCache;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;

/**
 * Created by murrayc on 5/16/14.
 */
class ListCursorAdapter extends RecyclerView.Adapter<ListCursorAdapter.ViewHolder> {

    final LruCache<String, Bitmap> mCache = new LruCache<>(20);

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

    private static class ImageLoadedCallback implements Callback {
        final WeakReference<Context> contextReference;
        final WeakReference<ViewHolder> viewHolderReference;
        final int position;
        final String itemId;


        public  ImageLoadedCallback(final Context context, final ViewHolder viewHolder, int position, final String itemId) {
            this.contextReference = new WeakReference<>(context);
            this.viewHolderReference = new WeakReference<>(viewHolder);
            this.position = position;
            this.itemId = itemId;

        }

        private ViewHolder getValidViewHolder() {
            if (viewHolderReference == null) {
                return null;
            }

            final ViewHolder viewHolder = viewHolderReference.get();
            if (viewHolder == null) {
                return null;
            }

            //Check that we are still dealing with the same position,
            //because the ImageView might be recycled for use with a different position.
            if (viewHolder.getPosition() != position) {
                return null;
            }

            return viewHolder;
        }

        @Override
        public void onSuccess() {
            final ViewHolder viewHolder = getValidViewHolder();
            if (viewHolder == null) {
                return;
            }

            //Hide the progress indicator now that we are showing the image.
            viewHolder.progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onError() {
            final ViewHolder viewHolder = getValidViewHolder();
            if (viewHolder == null) {
                return;
            }

            //Show the progress indicator because we have no image:
            viewHolder.progressBar.setVisibility(View.VISIBLE);


            if (contextReference == null) {
                return;
            }

            final Context context = contextReference.get();
            if (context == null) {
                return;
            }

            //Something was wrong with the (cached) image,
            //so just abandon this whole item.
            //That seems safer and simpler than trying to recover just one of the 3 images.
            Utils.abandonItem(context, itemId);

            Log.error("ListCursorAdaptor.onBindViewHolder.onError().");
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

                //Cancel any previous requests for this ImageView.
                //TODO: Is this really necessary? - Doesn't Picasso do this automatically?
                Picasso.with(mContext).cancelRequest(viewHolder.imageView);

                // TODO: We could use this, but how would we be able to pass a position to check,
                // so we can call viewHolder.progressBar.setVisibility(View.GONE) after its loaded?
                Picasso.with(mContext).load(imageUriStr).into(viewHolder.imageView,
                        new ImageLoadedCallback(mContext, viewHolder, viewHolder.getPosition(), itemId));
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

    public void onViewRecycled(final ViewHolder viewHolder) {
        //Picasso's into() documentation tells us to use cancelRequest() to avoid a leak,
        //though it doesn't suggest where/when to call it:
        //http://square.github.io/picasso/javadoc/com/squareup/picasso/RequestCreator.html#into-android.widget.ImageView-com.squareup.picasso.Callback-
        Picasso.with(mContext).cancelRequest(viewHolder.imageView);
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
