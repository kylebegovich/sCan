package com.example.scan.scan;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
public class HomePage extends Activity {
    private static final int
            PICK_IMAGE = 420, CAPTURE_IMAGE = 666;

    private static final int
            SAMPLE_HEIGHT = 1920, SAMPLE_WIDTH = (SAMPLE_HEIGHT >> 2) * 3; // * 3/4

    public static final String MIME_TYPE_CONTACT = "vnd.android.cursor.item/vnd.example.contact";

    /** If you put in excessively long names the UI on the home screen will flow oddly */
    private static final int MAX_NAME_CHARS = 11;
    private final String TAG = "emilio";        // Don't hate. Appreciate.


    File photoFile;
    String mCurrentPhotoPath;
    Button btnScan;
    Button btnPicLib;
    Button btnDocList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Intent intent = getIntent();

        // let's greet people nicely
        String s = intent.getStringExtra("username");
        if (s.contains(" "))
            s = s.substring(0, s.indexOf(" ")); // trim to first name
        s = s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
        if (s.length() > MAX_NAME_CHARS)
            s = s.substring(0, MAX_NAME_CHARS);

        TextView user = (TextView) findViewById(R.id.showuser);
        user.setText("Hello, " + s + "!");

        // Scan button invokes Camera
        btnScan = (Button) findViewById(R.id.scanbutton);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                try {
                    photoFile = createImageFile();
                } catch (IOException e){}

                if (photoFile != null){
                    Uri contentUri = FileProvider.getUriForFile(context,
                            "com.example.scan.scan.fileprovider", photoFile);
                    context.grantUriPermission("com.example.scan.scan",
                            contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.grantUriPermission("com.example.scan.scan",
                            contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    setResult(CAPTURE_IMAGE, intent);
                    startActivityForResult(intent, CAPTURE_IMAGE);

                }
                Log.e(TAG, "workin so far");
        }



        });

        // document list button shows everything
        btnDocList = (Button) findViewById(R.id.doclibrarybutton);
        btnDocList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startListView = new Intent(HomePage.this, ListFilesActivity.class);
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

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        if (resultCode != RESULT_OK) {
            Toast.makeText(getApplicationContext(), R.string.cancel, Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap imageBitmap = null;

        // IF PHOTO IS TAKEN USING CAMERA
        if (requestCode == CAPTURE_IMAGE) {
            imageBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        }

        // IF PICTURE IS TAKEN FROM PHOTO LIBRARY
        if (requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();

            // primary method
            try {
                imageBitmap = decodeUri(imageUri, SAMPLE_HEIGHT, SAMPLE_WIDTH);
            } catch (Exception e) { // this should never happen
                Toast.makeText(getApplicationContext(), R.string.except_other, Toast.LENGTH_LONG).show();
                e.printStackTrace();
                return;
            }
        }

        if (imageBitmap == null) {
            switch (requestCode) {
                case PICK_IMAGE:
                    Toast.makeText(getApplicationContext(), "imageBitmap is null.\nA photo was selected.", Toast.LENGTH_LONG).show();
                    break;
                case CAPTURE_IMAGE:
                    Toast.makeText(getApplicationContext(), "imageBitmap is null.\nCheck the camera capture method.", Toast.LENGTH_LONG).show();
                    break;
            }
            (new Exception()).printStackTrace();
            return;
        }

        // PREPROCESSING (has been removed temporarily due to dysfunctionality.
        // imageBitmap = PreProcessing.doStuff(imageBitmap);

        final Bitmap finalImageBitmap = imageBitmap;

        // OCR stuff
        Context context = getApplicationContext();
        TextRecognizer ocrFrame = new TextRecognizer.Builder(context).build();
        Frame frame = new Frame.Builder().setBitmap(finalImageBitmap).build();
        SparseArray<TextBlock> textBlocks = ocrFrame.detect(frame);

        // pipe out text
        StringBuilder answer = new StringBuilder();

        if (textBlocks.size() <= 0) {
            Toast.makeText(getApplicationContext(), R.string.no_text, Toast.LENGTH_LONG).show();
            return;
        }

        for (int i = 0; i < textBlocks.size(); i++)
            answer.append(textBlocks.get(textBlocks.keyAt(i)).getValue());

        System.out.println(answer);
        final String result = answer.toString();

        // alert setup
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Type file name:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // save plz
                String textFileName = input.getText().toString();
                if (textFileName.equals("")) {
                    // random values
                    textFileName = "sCan-" + (int) (Math.random() * 0x66600000);
                }

                if (!textFileName.endsWith(".txt"))
                    textFileName = textFileName + ".txt";
                try {
                    OutputStreamWriter out = new OutputStreamWriter(openFileOutput(textFileName, 0));
                    out.write(result);
                    out.close();
                    Toast.makeText(getApplicationContext(), "Saved text to " + textFileName, Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Toast.makeText(getApplicationContext(), R.string.except_other, Toast.LENGTH_LONG).show();
                    t.printStackTrace();
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

        // uncomment this code to show the text without saving it
        /*
        Intent intent = new Intent(this, DisplayText.class);
        intent.putExtra("text", result);
        startActivity(intent);
        */

        // uncomment this code to show what was captured
        /*
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        finalImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        Intent intent = new Intent(this, DisplayImage.class);
        intent.putExtra("picture", byteArray);
        startActivity(intent);
        */

    }

    private static void setToDecodeFromBitmapOptions(BitmapFactory.Options o, int minHeight, int minWidth) {
        final int height = o.outHeight;
        final int width = o.outWidth;

        // height and width
        float sampleHeight = (float) height / (float) minHeight;
        float sampleWidth = (float) width / (float) minWidth;
        o.inSampleSize = (int) Math.max(1, Math.min(sampleHeight, sampleWidth));

        // Decode bitmap with inSampleSize set
        o.inJustDecodeBounds = false;
    }

    //Method to Decode URI to Bitmap
    private Bitmap decodeUri(Uri selectedImage, int minHeight, int minWidth) throws FileNotFoundException {
        final BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);
        setToDecodeFromBitmapOptions(o, minHeight, minWidth);
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);
    }

    // Helper Function
    private static Bitmap decodeSampledBitmapFromFile(String path, int minHeight, int minWidth) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        setToDecodeFromBitmapOptions(options, minHeight, minWidth);
        return BitmapFactory.decodeFile(path, options);
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.e(TAG, "External");
        Log.e(TAG, storageDir.getAbsolutePath());
        if(!storageDir.exists()) storageDir.mkdir();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.e(TAG, mCurrentPhotoPath);
        return image;
    }
}
