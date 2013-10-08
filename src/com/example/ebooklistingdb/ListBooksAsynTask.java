package com.example.ebooklistingdb;

import java.util.ArrayList;
import java.util.Comparator;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;

/**
 * Class ListBooksAsynTask, this class it is encharge to connect to DROPBOX,
 * search all EPUB files (subfolders included) and show it in a listView.
 * 
 * **/

class ListBooksAsynTask extends AsyncTask<String, Void, String> {

	/* Variables needed from outside */
	private Context context;
	private ListView listview;
	private RowArrayAdapter myadapter;

	private ProgressDialog dialog;
	private EBook eBookList[];
	private ArrayList<EBook> arrayListEBookList;

	public ListBooksAsynTask(Context context, ListView listview,
			RowArrayAdapter adapter) {
		this.context = context;
		this.listview = listview;
		this.myadapter = adapter;
	}

	@Override
	protected void onPreExecute() {
		// dialog = ProgressDialog.show(MainActivity.this,
		// null,MainActivity.this.getString(R.string.loading_eBooks), true);
	}

	@Override
	protected String doInBackground(String... urls) {
		arrayListEBookList = new ArrayList<EBook>();
		String response = "";

		for (String url : urls) {
			seachEBooks("/", 0);
		}
		return response;
	}

	@Override
	protected void onPostExecute(String result) {

		int size = arrayListEBookList.size();
		eBookList = new EBook[size];
		for (int i = 0; i < size; i++) {
			eBookList[i] = new EBook(arrayListEBookList.get(i).time,
					arrayListEBookList.get(i).title,
					arrayListEBookList.get(i).path);
		}

		myadapter = new RowArrayAdapter(context, eBookList);
		myadapter.sort(sortTitleDes());
		listview.setAdapter(myadapter);
		// dialog.dismiss();

	}

	public void seachEBooks(String path, int depth) {

		try {
			Entry booksEntry = MainActivity.mDBApi.metadata(path, 100, null,
					true, null);

			for (Entry ent : booksEntry.contents) {

				if (ent.isDir) { // Busqueda dentro del subdirectorio
					//seachEBooks(ent.path,depth);

				} else { // guardamos el libro y su path
					Log.i("LIBROS", ent.fileName());
					arrayListEBookList.add(new EBook(ent.clientMtime, ent
							.fileName(), ent.path));
				}

			}

		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Comparator<EBook> sortTitleAsc() {
		return new Comparator<EBook>() {
			public int compare(EBook object1, EBook object2) {
				return object1.title.compareToIgnoreCase(object2.title);

			}
		};
	}

	public Comparator<EBook> sortTitleDes() {
		return new Comparator<EBook>() {
			public int compare(EBook object1, EBook object2) {
				return object2.title.compareToIgnoreCase(object1.title);

			}
		};
	}

}