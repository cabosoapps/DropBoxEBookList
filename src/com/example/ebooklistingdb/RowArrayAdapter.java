package com.example.ebooklistingdb;

import android.content.Context;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RowArrayAdapter extends ArrayAdapter<EBook> {
	private final Context context;
	private final EBook values[];

	public RowArrayAdapter(Context context, EBook[] values) {
		super(context, R.layout.list_row, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = inflater.inflate(R.layout.list_row, parent, false);

		// Set EBook file name
		TextView textView = (TextView) rowView.findViewById(R.id.bookTitle);
		textView.setText(values[position].title);

		// Set Icon to EBook
		ImageView iconImage = (ImageView) rowView
				.findViewById(R.id.lvIconEBook);

		ImageView coverImage = MainActivity.getCoverImage();

		// Each icon needs to pass to the construct gestureDetector listener the
		// ImageView to be showed and the cover image path.
		final GestureDetector gestureDetector = new GestureDetector(context,
				new GestureListener(coverImage, values[position].path));
		iconImage.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				return gestureDetector.onTouchEvent(arg1);
			}

		});

		return rowView;
	}

}