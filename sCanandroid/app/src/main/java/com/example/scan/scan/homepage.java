package com.example.scan.scan;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.util.SparseArray;
import android.widget.ImageView;
import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.Frame.Builder;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class homepage extends AppCompatActivity {
    Button btnscan;
    Button btnPicLib;
    Button btnDocList;
    public String textFileName = "";
    private static final int
        REQUEST_IMAGE_CAPTURE = 1, PICK_IMAGE = 100;

    private static final String LOG_TAG = "testOCR.java";
    private static final String TAG = "testOCR.java";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        Intent intent = getIntent();
        String s = intent.getStringExtra("username");
        s = s.trim();

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

        btnDocList = (Button) findViewById(R.id.doclibrarybutton);
        btnDocList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startListView = new Intent(homepage.this, ListFilesActivity.class);
                startActivity(startListView);
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
        if (requestCode == PICK_IMAGE){
            Uri imageUri = data.getData();

            try {
                imageBitmap = decodeUri(imageUri);
            } catch (FileNotFoundException e) { // this should never happen
                e.printStackTrace();
                return;
            }
        }

        assert imageBitmap != null; // this should never happen


        // PREPROCESSING (has been removed temporarily due to dysfunctionality.
        //imageBitmap = PreProcessing.doStuff(imageBitmap);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Type text file name:");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

// Set up the buttons
        final Bitmap finalImageBitmap = imageBitmap;
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                textFileName = input.getText().toString();
                String answer = "";
                Context context = getApplicationContext();
                TextRecognizer ocrFrame = new TextRecognizer.Builder(context).build();
                Frame frame = new Frame.Builder().setBitmap(finalImageBitmap).build();
                SparseArray<TextBlock> textBlocks = ocrFrame.detect(frame);
                for (int i = 0; i < textBlocks.size(); i++) {
                    answer += textBlocks.get(textBlocks.keyAt(i)).getValue();
                }


                try {

                    OutputStreamWriter out= new OutputStreamWriter(openFileOutput(textFileName + ".txt", 0));

                    out.write(answer);

                    out.close();

                    Toast.makeText(getApplicationContext(), "The contents are saved in the file "+ textFileName + ".txt.", Toast.LENGTH_LONG).show();

                }

                catch (Throwable t) {

                    Toast.makeText(getApplicationContext(), "Exception: "+t.toString(), Toast.LENGTH_LONG).show();

                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

        // OCR


        //passing the detected text to DisplayText.class
        /*Intent intent = new Intent(this, DisplayText.class);
        intent.putExtra("text", answer);
        startActivity(intent);*/


        // THIS IS USED FOR DEBUGGING BITMAPS. TRIAL PURPOSE ONLY
        /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        Intent intent = new Intent(this, DisplayImage.class);
        intent.putExtra("picture", byteArray);
        startActivity(intent);*/

    }

    //Method to Decode URI to Bitmap
    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 400;

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