package com.example.ebooklistingdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.exception.DropboxException;

// AsyncTask download the cover pic of an EPUB and show it in an imageView
class DownloadPicAsyncTask extends AsyncTask<String, Void, String> {
	FileOutputStream outputStreamBookFile;
	DropboxFileInfo info;
	Bitmap coverImage;
	ProgressDialog dialog;
	private Context context;
	View view;

	// Connection detector
	ConnectionDetector cd;
	// Alert dialog manager
	AlertDialogManager alert = new AlertDialogManager();
	
	
	public DownloadPicAsyncTask(Context context, View view) {
		this.context = context;
		this.view = view;
		this.cd = new ConnectionDetector(context);
	}

	@Override
	protected void onPreExecute() {
		// dialog = ProgressDialog.show(context, null,
		// context.getString(R.string.loading_image), true);
	}

	@Override
	protected String doInBackground(String... urls) {

		String eBookUrl = "";
		for (String url : urls) {
			eBookUrl = context.getCacheDir() + File.separator
					+ getFileFromPath(url); // path + file
											// (/data/data/com.example.ebooklistingdb/cache/myBook.epub)

			File currentBookFile = new File(eBookUrl);
			if (currentBookFile.exists()) {
				return eBookUrl; // If file exist in cache do nothing. Just return the file path to be showed.
			}

			try {
				outputStreamBookFile = new FileOutputStream(currentBookFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			try {

				info = MainActivity.mDBApi.getFile(url, null,
						outputStreamBookFile, null);
			} catch (DropboxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i("DbExampleLog", "The file's rev is: "
					+ info.getMetadata().rev);
		}
		return eBookUrl;
	}

	@Override
	protected void onPostExecute(String eBookUrl) {
		// Load the cover pic on an imageView

		try {
			// Check if cover imagen exists
			File coverFile = new File(eBookUrl + ".png");
			if (coverFile.exists()) {
				// Open input stream to the cache file
				FileInputStream fis = new FileInputStream(eBookUrl + ".png");
				coverImage = BitmapFactory.decodeStream(fis);

			} else {
				// read epub file
				EpubReader epubReader = new EpubReader();
				Book book = epubReader.readEpub(new FileInputStream(eBookUrl));

				// Log the book's coverimage property
				coverImage = BitmapFactory.decodeStream(book.getCoverImage()
						.getInputStream());

				// save the image on cache disk.
				putBitmapInDiskCache(eBookUrl, coverImage);

			}

			ImageView mImage = (ImageView) view.findViewById(R.id.imCoverEBook);
			mImage.setImageBitmap(coverImage);

			// dialog.dismiss();

		} catch (IOException e) {
			Log.e("epublib", e.getMessage());
		}

	}

	private void putBitmapInDiskCache(String url, Bitmap avatar) {
		File cacheFile = new File(url + ".png");
		try {
			// Create a file at the file path, and open it for writing obtaining
			// the output stream
			cacheFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(cacheFile);
			// Write the bitmap to the output stream (and thus the file) in PNG
			avatar.compress(CompressFormat.PNG, 100, fos);
			// Flush and close the output stream
			fos.flush();
			fos.close();
		} catch (Exception e) {
			// Log anything that might go wrong with IO to file
			Log.e("LOG_TAG", "Error when saving image to cache. ", e);
		}
	}

	public String getFileFromPath(String path) {
		File f = new File(path);
		return f.getName();
	}

}
