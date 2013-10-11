package com.example.ebooklistingdb;
import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;



public class GestureListener extends
        GestureDetector.SimpleOnGestureListener {

	private View currentView;//currentView must contents the destiny ImageView to allocate the cover.
	private String pathwithFile;     //epub file to extract and show the cover image. (/folder1/folder2/myebook.epub)
	private ConnectionDetector cd;
	Context context;
	/**
	 * The construct received 2 parameter, the view to place the coverImage and the path of this one.
	 * */
	public GestureListener(View v, String pathwithFile, Context context){
		this.currentView = v;
		this.pathwithFile = pathwithFile;
		this.context = context;
		this.cd = new ConnectionDetector(context);
	}
	
	@Override
	public boolean onDown(MotionEvent e){		
		return true;
	}
	
    // event when double tap occurs
    @Override
    public boolean onDoubleTap(MotionEvent e) {
    	Log.i("GestureListener.java", "DOUBLECLICK on " + pathwithFile);
    	    		
    	File currentBookFile = new File( context.getCacheDir() + File.separator + (new File(pathwithFile)).getName());

    	//Try to show cover pic only when there is Internet or  already in cache.    	
    	if(currentBookFile.exists() || cd.isConnectingToInternet()){
	    	//Call task to show the cover photo.
	        DownloadPicAsyncTask task = new DownloadPicAsyncTask(MainActivity.getAppContext(),currentView);
			task.execute(new String[] { pathwithFile }); //it needs the epub file path to be showed.    		
    	}
    	else{
    		Toast.makeText( context, context.getString(R.string.no_internet_alertDialog), Toast.LENGTH_LONG).show();
			return false;
		}



        return true;
    }
    
    
    
}