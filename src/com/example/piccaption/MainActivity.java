package com.example.piccaption;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	
	private static final int CAMERA_INTENT_REQUEST = 100;
	private static final int GALLERY_REQUEST_CODE = 101;
	
	private Button mButton1;
	private ImageView mImageView;
	private EditText mEditText;
	private String TAG = "PicCaption";
	private Uri imageUri;
	private Button mButton2;
	private Bitmap mutable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mButton1 = (Button)findViewById(R.id.button1);
		mButton2 = (Button)findViewById(R.id.button2);
		mImageView = (ImageView)findViewById(R.id.imageView1);
		mEditText = (EditText)findViewById(R.id.editText1);
		mButton1.setOnClickListener(this);
		mButton2.setOnClickListener(this);
		mImageView.setOnClickListener(this);
		registerForContextMenu(mImageView);
	}
	// when tap on image view of compose Issue Screen
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		MenuInflater m = getMenuInflater();
		m.inflate(R.menu.main, menu);
	}
	// After tapping image frame choose image capture category
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.gallery:
			openGalleryIntent();
			return true;
		case R.id.capture:
			openCameraIntent();
			return true;
		}
		return super.onContextItemSelected(item);
	}
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.imageView1)
			openContextMenu(v);
		else if(v.getId() == R.id.button1)
		{
			mButton1.post(new Runnable() {
				@Override
				public void run() {
					Bitmap bitmap = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();
					addCaption(bitmap, mEditText.getText().toString());
				}
			});
			
		}
		else if(v.getId() == R.id.button2)
		{
			mButton2.post(new Runnable() {
				@Override
				public void run() {
					saveImage();
				}
			});
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CAMERA_INTENT_REQUEST && resultCode == Activity.RESULT_OK) {
			mImageView.setImageBitmap(decodeSampledImage(imageUri.getPath(), 240, 240));
		}
		else if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			// Get the URI of browse photo
			Uri contentUri = data.getData();
			String[] proj = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
			if (cursor != null) {
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				imageUri = Uri.parse(cursor.getString(column_index)); // Photo  URI
																					
			} else 
				imageUri = Uri.parse(contentUri.getPath()); // Photo URI
							
			// set the bitmap to photo imageview
			mImageView.setImageBitmap(decodeSampledImage(imageUri.getPath(), 240, 240));
		}
	}
	
	private Bitmap decodeSampledImage(String selectedImagePath, int reqWidth, int reqHeight)
	{
		try {
			// Decode the image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(selectedImagePath), null, o);
			
			int scale = 1, width_tmp = o.outWidth, height_tmp = o.outHeight;
			while (true) {
				if (width_tmp / 2 < reqWidth || height_tmp / 2 < reqHeight)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}
			
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			o.inJustDecodeBounds = false;
			Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(selectedImagePath), null, o2);
			return bitmap;
		} catch (Exception e) {
			Log.e(TAG, "Error in bitmap Sampling "+e.toString());
			return null;
		}
	}
	
	private void openGalleryIntent() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
		startActivityForResult(intent, GALLERY_REQUEST_CODE);
	}
	/**
	 * Camera Intent
	 */
	private void openCameraIntent() {
		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		try {
			File photo = createPhotoFile();
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
			imageUri = Uri.fromFile(photo);
			startActivityForResult(cameraIntent, CAMERA_INTENT_REQUEST);
		} catch(Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}
	
	private void addCaption(Bitmap bitmap,String text)
	{
		mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		Canvas mCanvas = new Canvas(mutable);
		Paint mPaint = new Paint();
		mPaint.setColor(0xffff00ff);
		mPaint.setStrokeWidth(8);
		mPaint.setTextSize(22);
		mPaint.setTextAlign(Align.LEFT);
		mCanvas.drawText(text, 0, mutable.getHeight()-10, mPaint);
		mImageView.setImageBitmap(mutable);
	}
	
	private File createPhotoFile()
	{
		String capturedPhotoName = System.currentTimeMillis() + ".png";
		File myDirectory = new File(Environment.getExternalStorageDirectory()+ "/PicCaption/");
		if(!myDirectory.exists()) {
			myDirectory.mkdirs();
		}
		File photo = new File(myDirectory, capturedPhotoName);
		return photo;
	}
	
	private void saveImage() {
		try {
			File file = createPhotoFile();
			FileOutputStream fos = new FileOutputStream(file);
			mutable.compress(CompressFormat.PNG, 100, fos);
			fos.close();
			Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		    Uri contentUri = Uri.fromFile(file);
		    mediaScanIntent.setData(contentUri);
		    this.sendBroadcast(mediaScanIntent);
		    Toast.makeText(this, "Image Saved!", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e(TAG, "Error while creating file at: "+e.toString());
			Toast.makeText(this, "Error while saving image!", Toast.LENGTH_LONG).show();
		}
	}
}
