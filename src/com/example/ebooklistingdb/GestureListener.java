package com.example.ebooklistingdb;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;



public class GestureListener extends
        GestureDetector.SimpleOnGestureListener {

	private View currentView;//currentView must contents the destiny ImageView to allocate the cover.
	private String path;     //epub file to extract and show the cover image.
	
	/**
	 * The construct received 2 parameter, the view to place the coverImage and the path of this one.
	 * */
	public GestureListener(View v, String path){
		currentView = v;
		this.path = path;
	}
	
	@Override
	public boolean onDown(MotionEvent e){		
		return true;
	}
	
    // event when double tap occurs
    @Override
    public boolean onDoubleTap(MotionEvent e) {
    	Log.i("GestureListener.java", "DOUBLECLICK on " + path);
    	
    	//Call task to show the cover photo double-clicled.
        DownloadPicAsyncTask task = new DownloadPicAsyncTask(MainActivity.getAppContext(),currentView);
		task.execute(new String[] { path }); //it needs the epub file path to be showed.

        return true;
    }
}