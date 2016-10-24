package com.example.scan.scan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.content.Context;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.Frame.Builder;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;






public class homepage extends AppCompatActivity {
    Button btnscan;
    Button btnPicLib;
    ImageView pictureOcrView;

    private static final String LOG_TAG = "testOCR.java";

    private static final String TAG = "testOCR.java";

    private static final int
        REQUEST_IMAGE_CAPTURE = 1, PICK_IMAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_homepage);
        Intent intent = getIntent();
        String s = intent.getStringExtra("username");
        s = s.trim();

        /* this shouldn't be needed now
        if (s.contains("\n"))
            s = s.substring(0, s.indexOf("\n");
         */
        if (s.contains(" "))
            s = s.substring(0,s.indexOf(" ")); // trim to first name


        s = s.substring(0,1).toUpperCase() + s.substring(1,s.length());
        TextView user = (TextView) findViewById(R.id.showuser);
        user.setText("Hello " + s + "!");

        // Scan button invokes Camera
        btnscan = (Button) findViewById(R.id.scanbutton);
        btnscan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                }
            }
        });

        // Photo Library button helps select a photo from Picture Library and use it
        btnPicLib = (Button) findViewById(R.id.photolibrarybutton);
        btnPicLib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
    }

    private void openGallery(){
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    // returns the image clicked by the camera to displayimage.class activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return; // this should never happen

        Bitmap imageBitmap = null;

        // IF PHOTO IS TAKEN USING CAMERA
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
        }

        // IF PICTURE IS TAKEN FROM PHOTO LIBRARY
        if (requestCode == PICK_IMAGE)
        {
            Uri imageUri = data.getData();
            try {
                imageBitmap = decodeUri(imageUri);
                }
            catch (FileNotFoundException e)
            { // this should never happen
                e.printStackTrace();
                return;
            }
        }
        pictureOcrView=(ImageView) findViewById(R.id.imageView2);
        pictureOcrView.setImageBitmap(imageBitmap);

        assert imageBitmap != null; // this should never happen

        // PREPROCESSING STUFF GOES HERE
        // for encapsulation
//        imageBitmap = PreProcessing.doStuff(imageBitmap);

        // OCR STUFF GOES HERE
        /* OCR TEAM
            return the bitmap to the OCR team for further processing into string.
            I have created another activity to display your string. We will link the string you return
            to that activity on Saturday. Below is the code to display the processed image on a new activity
            I have created another activity to display your string. Below is the code to display the processed image on a new activity
            just for testing if pre processing is working. Will remove it on Saturday.
            If pre processing methods are not working then remove them.
        */

        Context context = getApplicationContext();
        TextRecognizer ocrFrame = new TextRecognizer.Builder(context).build();

        Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
        if (ocrFrame.isOperational()){
            Log.e(TAG, "Textrecognizer is operational");
        }

        Log.e(TAG, "nothing works i hate everything");

        SparseArray<TextBlock> textBlocks = ocrFrame.detect(frame);

        for (int i = 0; i < textBlocks.size(); i++) {
            Log.e(TAG, "this oiasdjasdpiasjdlasjdlkasjd happening?");
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
            Log.i(LOG_TAG, textBlock.getValue());
        }

        Log.e(TAG, "Is this happening?");

        // just getting a pre-processed image onto the screen
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//        byte[] byteArray = stream.toByteArray();
//        Intent intent = new Intent(this, displayimage.class);
//        intent.putExtra("picture", byteArray);
//        startActivity(intent);

        // text view stuff
    }

    //Method to Decode URI to Bitmap
    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 140;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }
}