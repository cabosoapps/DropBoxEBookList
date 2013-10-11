package com.example.ebooklistingdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.TokenPair;

public class MainActivity extends FragmentActivity implements
		ActionBar.OnNavigationListener {

	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	private ListView listview;

	final static private String APP_KEY = "pb1ydx24ng860k8";
	final static private String APP_SECRET = "2j85g6czwpb50vm";
	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	// In the class declaration section:
	public static DropboxAPI<AndroidAuthSession> mDBApi;

	private boolean mLoggedIn;

	private static Context context;

	public static RowArrayAdapter myadapter;
	// public static ArrayList<EBook> arrayListEBookList;

	private static ImageView coverImage; // To place the cover photo of the
											// ebook selected.
	
	// Connection detector
	private ConnectionDetector cd;	
	
	//Shared list to read/write EPUB 
	private BufferEPUBList mBuffer;
	//Handler to communicate data changes (new ebooks) to UI thread.
	private Handler handler;
	
	public static ProgressDialog dialog;

	
	//****************************FUNCTIONS****************************
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		context = this.getApplicationContext();
		coverImage = (ImageView) findViewById(R.id.imCoverEBook);
		listview = (ListView) findViewById(R.id.listView1);
		cd = new ConnectionDetector(context);
		handler = new Handler();
		mBuffer = new BufferEPUBList();
		
		
		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								getString(R.string.title_section1),
								getString(R.string.title_section2), }), this);

			
		
		AndroidAuthSession session = buildSession();
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		// MyActivity below should be your activity class name
		mDBApi.getSession().startAuthentication(MainActivity.this);

	}

	
	protected void onResume() {
		super.onResume();

		//Refresh list 
		mBuffer.clearBuffer();
		
		// Check if Internet present
		if (!cd.isConnectingToInternet()) {
			TextView tvLoading = (TextView) findViewById(R.id.tvLoading);
			tvLoading.setText(getString(R.string.no_internet));		          
			return;
		}
		
		if (mDBApi.getSession().authenticationSuccessful()) {
			try {
				Log.i("DbAuthLog", "Succesfull authenticating");
				// Required to complete auth, sets the access token on the
				// session
				mDBApi.getSession().finishAuthentication();

				// Store it locally  for later use
				TokenPair tokens = mDBApi.getSession().getAccessTokenPair();
				storeKeys(tokens.key, tokens.secret);
				mLoggedIn = true;
				
				//Thread to read ebook from DropBox
				Thread readEBooksTask = new Thread(new readEBooksTask());
				readEBooksTask.setName("Productor");
				
				//Thread to write ebooks in the listview while readEBookTask is getting new ones.
				Thread writeEBooksTask = new Thread(new writeEBooksTask());
				writeEBooksTask.setName("Consumidor");
				
				
				TextView tvLoading = (TextView) findViewById(R.id.tvLoading);				
				tvLoading.setText(getString(R.string.loading_eBooks));
				
				//Launch threads. ReadEBooksTask gets ebooks from DropBox and put its on a buffer , when new data is present  
				//writeEBooksTask is able to upload the listview using a UI handler.
				readEBooksTask.start();
				writeEBooksTask.start();
				

			} catch (IllegalStateException e) {
				Log.i("DbAuthLog", "Error authenticating", e);
			}

		}
	}
	
	
	/**
	 * FUNCTION: Context getAppContext OBJECTIVE: Get the context of the
	 * MainActivity from another point RETURN: The context of the MainActivity
	 **/
	public static Context getAppContext() {
		return MainActivity.context;
	}

	/**
	 * FUNCTION: ImageView getCoverImage OBJECTIVE: Get the ImageView where the
	 * cover ebook will be place. RETURN: ImageView
	 **/
	public static ImageView getCoverImage() {
		return MainActivity.coverImage;
	}

	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session;

		String[] stored = getKeys();
		if (stored != null) {
			AccessTokenPair accessToken = new AccessTokenPair(stored[0],
					stored[1]);
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE,
					accessToken);
		} else {
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
		}

		return session;
	}
	
	
	/**
	 * Task readEBooksTask , get all ebooks of DropBox from the root directory "/" (include folder and subfolders)
	 *  and write them on a buffer.
	 * **/
	class readEBooksTask implements Runnable {
		
		
		/**
		 * search all ebooks of DropBox from the root directory "/" (include folder and subfolders)
		 * **/
		public void seachEBooks(String path) throws InterruptedException {
			try {
				//Get entry from dropbox
				Entry booksEntry = MainActivity.mDBApi.metadata(path, 100, null,
						true, null);

				for (Entry ent : booksEntry.contents) {
					Log.i("seaching EBooks in :", ent.path);
					if (ent.isDir) { // Recursive call is the entry is a directory
						seachEBooks(ent.path);
						Log.i(Thread.currentThread().getName(), "Sleep 100"); 
						Thread.sleep(100);//Allow other threads get the cpu .
					} else { //Save the ebook in the buffet.
						Log.i(Thread.currentThread().getName(), ent.fileName());
						mBuffer.addnewEBook(new EBook(ent.clientMtime, ent
								.fileName(), ent.path));
					}
				}
				
			} catch (DropboxException e) {
				mBuffer.setcompleted(true);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
		
		
		public void stop(){
			Thread.currentThread().interrupt();			
		}
		
		@Override		
		public void run() {		
	
			while (!mBuffer.iscomplete()){
					try {
						seachEBooks("/");
						mBuffer.setcompleted(true); //At this point tree folder has been read. So unlook the condition to stop reading.
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}									
			}
		}
	}
	
	
	/**
	 * Task writeEBooksTask , is reading ebook from a buffer and update the listview using a UI handler. 
	 * **/
	class writeEBooksTask implements Runnable {
		TextView ebookNumber = (TextView) findViewById(R.id.tvEBooksNumber);		
		public void stop(){
			Thread.currentThread().interrupt();			
		}
				
		@Override		
		public void run() {			
			while (!mBuffer.iscomplete()){
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(mBuffer.newdataPresent()){//upload lisview only when new data present.
						handler.post(new Runnable() {
							
							@Override
							public void run() {								
									Log.i(Thread.currentThread().getName(), "Handler, uploading listview");	
									List<EBook> listEbook = (List<EBook>) mBuffer.getEBookList().clone();
									
									
									//This is to avoid any automatic movement of the scroll when new data is uploaded. (it is possible scrolling 
									//using fingers).
									if (listview.getAdapter() == null) {
										TextView tvLoading = (TextView) findViewById(R.id.tvLoading);
										tvLoading.setVisibility(TextView.GONE);
										
										myadapter = new RowArrayAdapter(context, listEbook);
										listview.setAdapter(myadapter);
										
									} else {
										//upload listview and ebooks number.
										ebookNumber.setText(mBuffer.getEBooksNumber() + " EBooks");
									    ((RowArrayAdapter)listview.getAdapter()).refill(listEbook);
									}
		
								}
							});
						}
					
			}
			if(mBuffer.getEBooksNumber() == 0){//Empty list
				handler.post(new Runnable() {					
					@Override
					public void run() {								
								Log.i(Thread.currentThread().getName(), "Handler, no ebook found");	
								TextView tvLoading = (TextView) findViewById(R.id.tvLoading);
								tvLoading.setText(getString(R.string.empty_list));
		

						}
					});
				
				
				
				
			}
		}
				
	}
	
	
	private String[] getKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key != null && secret != null) {
			String[] ret = new String[2];
			ret[0] = key;
			ret[1] = secret;
			return ret;
		} else {
			return null;
		}
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a
	 * local store, rather than storing user name & password, and
	 * re-authenticating each time (which is not to be done, ever).
	 */
	private void storeKeys(String key, String secret) {
		// Save the access key for later
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.putString(ACCESS_KEY_NAME, key);
		edit.putString(ACCESS_SECRET_NAME, secret);
		edit.commit();
	}

	private void clearKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}

	public String getFileFromPath(String path) {
		File f = new File(path);
		return f.getName();
	}

	public Comparator<EBook> sortTitleAsc() {
		return new Comparator<EBook>() {
			public int compare(EBook object1, EBook object2) {
				return object1.title.compareToIgnoreCase(object2.title);

			}
		};
	}
		
	public Comparator<EBook> sortDateAsc() {
		return new Comparator<EBook>() {
			public int compare(EBook object1, EBook object2) {
				return object1.time.compareToIgnoreCase(object2.time);

			}
		};
	}


	/**
	 * Backward-compatible version of {@link ActionBar#getThemedContext()} that
	 * simply returns the {@link android.app.Activity} if
	 * <code>getThemedContext</code> is unavailable.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		if(myadapter != null){
			if (id == 0){
				myadapter.sort(sortTitleAsc());
				myadapter.notifyDataSetChanged();			
			}
			else{
				myadapter.sort(sortDateAsc());
				myadapter.notifyDataSetChanged();
				
			}
		}
		
		return true;
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

	}



}
