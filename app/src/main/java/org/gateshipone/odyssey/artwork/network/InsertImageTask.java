/*
 * Copyright (C) 2019 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.artwork.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.gateshipone.odyssey.artwork.storage.ArtworkDatabaseManager;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;

import java.io.ByteArrayOutputStream;

public class InsertImageTask extends AsyncTask<ImageResponse, Object, ArtworkRequestModel> {

    public interface ImageSavedCallback {
        void onImageSaved(ArtworkRequestModel artworkRequestModel, Context context);
    }

    /**
     * Maximmum size for either x or y of an image
     */
    private static final int MAXIMUM_IMAGE_RESOLUTION = 500;

    /**
     * Compression level if images are rescaled
     */
    private static final int IMAGE_COMPRESSION_SETTING = 80;

    /**
     * Maximum size of an image blob to insert in SQLite database. (1MB)
     */
    private static final int MAXIMUM_IMAGE_SIZE = 1024 * 1024;

    @SuppressLint("StaticFieldLeak")
    private final Context mApplicationContext;

    private final ImageSavedCallback mImageSavedCallback;

    public InsertImageTask(final Context applicationContext, final ImageSavedCallback imageSavedCallback) {
        mApplicationContext = applicationContext;
        mImageSavedCallback = imageSavedCallback;
    }

    @Override
    protected ArtworkRequestModel doInBackground(ImageResponse... params) {
        ImageResponse response = params[0];

        if (response.image == null) {
            insertImage(response.model, null);
            return response.model;
        }

        // Rescale them if to big
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
        if ((options.outHeight > MAXIMUM_IMAGE_RESOLUTION || options.outWidth > MAXIMUM_IMAGE_RESOLUTION)) {
            // Calculate minimal scaling factor
            float factor = Math.min((float) MAXIMUM_IMAGE_RESOLUTION / (float) options.outHeight, (float) MAXIMUM_IMAGE_RESOLUTION / (float) options.outWidth);
            options.inJustDecodeBounds = false;
            Bitmap bm = BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            Bitmap.createScaledBitmap(bm, (int) (options.outWidth * factor), (int) (options.outHeight * factor), true)
                    .compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SETTING, byteStream);

            if (byteStream.size() <= MAXIMUM_IMAGE_SIZE) {
                insertImage(response.model, byteStream.toByteArray());
            }
        } else {
            if (response.image.length <= MAXIMUM_IMAGE_SIZE) {
                insertImage(response.model, response.image);
            }
        }

        return response.model;
    }

    @Override
    protected void onPostExecute(ArtworkRequestModel artworkRequestModel) {
        mImageSavedCallback.onImageSaved(artworkRequestModel, mApplicationContext);
    }

    private void insertImage(final ArtworkRequestModel model, final byte[] image) {
        final ArtworkDatabaseManager artworkDatabase = ArtworkDatabaseManager.getInstance(mApplicationContext);

        switch (model.getType()) {
            case ALBUM:
                artworkDatabase.insertAlbumImage(mApplicationContext, (AlbumModel) model.getGenericModel(), image);
                break;
            case ARTIST:
                artworkDatabase.insertArtistImage(mApplicationContext, (ArtistModel) model.getGenericModel(), image);
                break;
        }
    }
}
