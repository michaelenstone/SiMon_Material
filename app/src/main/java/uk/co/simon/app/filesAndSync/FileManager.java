package uk.co.simon.app.filesAndSync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import uk.co.simon.app.R;

public class FileManager {

    private static final String applicationStorage = "/Android/data/uk.co.simon.app";
    private static final String pdfs = "/pdfs";
    private static Context mContext;

    public FileManager(Context context) {
        mContext = context;
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    public File importImageFile(File importFile) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        File result = createImageFile();
        try {
            input = new FileInputStream(importFile);
            output = new FileOutputStream(result);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } finally {
            input.close();
            output.close();
        }
        return result;
    }

    public static File getPDFStorageLocation(Context context) {
        File dir = null;
        try {
            dir = new File(Environment.getExternalStorageDirectory() + applicationStorage + pdfs);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            Toast toast = Toast.makeText(context, context.getString(R.string.errCreatingDirectory), Toast.LENGTH_LONG);
            toast.show();
        }
        return dir;
    }

    public Bitmap resizeImage(String selectedImage, int size) throws FileNotFoundException {

        if (selectedImage.equals("Default")) {

            // Decode image size
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(mContext.getResources(), R.drawable.placeholder, options);

            // Find the correct scale value.
            options.inSampleSize = calculateInSampleSize(options, size, size);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeResource(mContext.getResources(), R.drawable.placeholder, options);
        } else {
            Uri imageURI = Uri.parse(selectedImage);
            File imageFile = new File(imageURI.getPath());

            // Decode image size
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            // Find the correct scale value.
            options.inSampleSize = calculateInSampleSize(options, size, size);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    public String getRealPath(Uri uri) {if (Build.VERSION.SDK_INT < 19)
            return getRealPathFromURI_API11to18(uri);
        else
            return getRealPathFromURI_API19(uri);
    }

    @SuppressLint("NewApi")
    public String getRealPathFromURI_API19(Uri uri){
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }


    @SuppressLint("NewApi")
    public String getRealPathFromURI_API11to18(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        String result = null;

        CursorLoader cursorLoader = new CursorLoader(
                mContext,
                contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        if(cursor != null){
            int column_index =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
        }
        return result;
    }
}