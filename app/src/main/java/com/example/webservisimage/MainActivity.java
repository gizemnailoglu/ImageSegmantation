package com.example.webservisimage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    private FirebaseFunctions mFunctions;;
    private StorageReference mStorageRef;
    private Uri selectedImage;
    ImageView img;
    ImageView resultImg;
    EditText editTextRate;
    List<Bitmap> myBitmaps;
    List<String> myNames;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

    }

    private void init() {
        img = findViewById(R.id.imageView2);
        resultImg=findViewById(R.id.imageView1);
        mFunctions = FirebaseFunctions.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        editTextRate=findViewById(R.id.edittext_rate);
    }

    public void Compress(View view){
        if (selectedImage!=null){
            if (!TextUtils.isEmpty(editTextRate.getText())){
                Toast.makeText(this, "Resim Sunucuda Compress edilicek", Toast.LENGTH_LONG).show();
                final int x=100-((Integer.parseInt(editTextRate.getText().toString()))%100);
                addFirebaseFile(selectedImage,1,x);
            }else Toast.makeText(this, "Sıkıştırma oranı gir", Toast.LENGTH_SHORT).show();
        }else Toast.makeText(this, "Bir resim seçilmedi", Toast.LENGTH_SHORT).show();

    }

    public void Segmantation(View view){
        if (selectedImage!=null){
          //  segma();
             addFirebaseFile(selectedImage,2,0);
        }else Toast.makeText(this, "Bir resim seçilmedi", Toast.LENGTH_SHORT).show();
    }


    // güncellenecek
    private void segma() {
          try {
            final Bitmap bitmap;
             if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),selectedImage);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImage);
            }
            FirebaseVisionImage image =  FirebaseVisionImage.fromBitmap(bitmap);
              FirebaseVisionObjectDetectorOptions options =
                      new FirebaseVisionObjectDetectorOptions.Builder()
                              .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                              //.enableMultipleObjects()
                              .enableClassification()  // Optional
                              .build();

            FirebaseVisionObjectDetector objectDetector =
                    FirebaseVision.getInstance().getOnDeviceObjectDetector(options);



            objectDetector.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionObject>>() {
                @Override
                public void onSuccess(List<FirebaseVisionObject> firebaseVisionObjects) {
                    resultImg.setBackgroundColor(Color.rgb(255,255,255));
                    for (FirebaseVisionObject obj : firebaseVisionObjects) {
                        Integer id = obj.getTrackingId();
                        Rect bounds = obj.getBoundingBox();
                        // If classification was enabled:

                       int category = obj.getClassificationCategory();
                        Float confidence = obj.getClassificationConfidence();
                        System.out.println("beyter "+id+" "+bounds.left +" "+bounds.right+" "+bounds.bottom+" " +bounds.top);

                        Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                        Canvas canvas = new Canvas(newBitmap);
                       // resultImg.draw(canvas);
                        Paint p = new Paint();

                        p.setColor(Color.rgb(0,255,0));
                        p.setStyle(Paint.Style.STROKE);
                    /*    Rect bound2 = new Rect() ;
                        bound2.top=bounds.top+1;
                        bound2.bottom=bounds.bottom+1;
                        bound2.left=bounds.left+1;
                        bound2.right=bounds.right+1;
                        Rect bound3 = new Rect() ;
                        bound3.top=bounds.top+2;
                        bound3.bottom=bounds.bottom+2;
                        bound3.left=bounds.left+2;
                        bound3.right=bounds.right+2;

                        Rect bound4 = new Rect() ;
                        bound4.top=bounds.top+3;
                        bound4.bottom=bounds.bottom+3;
                        bound4.left=bounds.left+3;
                        bound4.right=bounds.right+3;


                        Rect bound5 = new Rect() ;
                        bound5.top=bounds.top+4;
                        bound5.bottom=bounds.bottom+4;
                        bound5.left=bounds.left+4;
                        bound5.right=bounds.right+4;


                        Rect bound6 = new Rect();
                        bound6.top=bounds.top+5;
                        bound6.bottom=bounds.bottom+5;
                        bound6.left=bounds.left+5;
                        bound6.right=bounds.right+5;
*/


                        canvas.drawRect(bounds,p);
                     /*   canvas.drawRect(bound2,p);
                        canvas.drawRect(bound3,p);
                        canvas.drawRect(bound4,p);
                        canvas.drawRect(bound5,p);
                        canvas.drawRect(bound6,p);*/
                        resultImg.setImageBitmap(newBitmap);


                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    System.out.println("beyter "+e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void ImageClick(View view){
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]
                    {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        } else ShowDialog();
    }

    private void ShowDialog() {
        AlertDialog.Builder myDialog = new AlertDialog.Builder(MainActivity.this);
        myDialog.setTitle("Resim Yükle");
        myDialog.setMessage("Nasıl yükleme yapmak istersiniz");
        myDialog.setPositiveButton("GALERİ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,2);
            }
        });
        myDialog.setNegativeButton("KAMERA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE,"Yeni Foto");
                values.put(MediaStore.Images.Media.DESCRIPTION,"Kamera");
                selectedImage=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,selectedImage);
                startActivityForResult(cameraIntent, 3);
            }
        });
        myDialog.show();
    }

    private void addFirebaseFile(Uri selectedImage, final int operation ,final int compRate)  {
        UUID uuıd = UUID.randomUUID();
        final String imageName = "images/"+uuıd+".jpg";
        StorageReference storageReference = mStorageRef.child(imageName);
        storageReference. putFile(selectedImage)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            if (operation==1){
                                ImageCompress(imageName,compRate).addOnCompleteListener(new OnCompleteListener<String>() {
                                    @Override
                                    public void onComplete(@NonNull Task<String> task) {
                                        if (task.isSuccessful()){
                                            Picasso.get().load(task.getResult()).into(resultImg);
                                            System.out.println("beyter 5 "+task.getResult());
                                        }
                                    }
                                });
                            }
                            else if (operation==2){
                                ImageSegmantation(imageName,String.valueOf(imageName)).addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Map<String, Object>> task) {
                                        if (task.isSuccessful()){
                                            List<Map<String,Object>> myObjects= (List<Map<String, Object>>) task.getResult().get("result");
                                            myObject(myObjects);
                                        }else{
                                            Toast.makeText(MainActivity.this, "Upsss....", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("beyter "+e.getMessage());
            }
        });
    }

    private void myObject(List<Map<String, Object>> myObjects) {
         Bitmap bitmap = null;

         try {
             if (Build.VERSION.SDK_INT >= 28) {
                 ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),selectedImage);
                 bitmap = ImageDecoder.decodeBitmap(source);
             } else {
                 bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImage);
             }

         } catch (IOException e) {
             e.printStackTrace();
         }

        for (Map<String,Object> object : myObjects){
            Map<String, Object> myBoundingPoly = (Map<String, Object>) object.get("boundingPoly");
            List<Map<String,Object>> myVertices = (List<Map<String, Object>>) myBoundingPoly.get("normalizedVertices");
            Rect bound = new Rect();
            for (Map<String,Object> vertices: myVertices){
                int x = (int)((double)vertices.get("x")*bitmap.getWidth());
                int y = (int)((double)vertices.get("y")*bitmap.getHeight());
                if(x>bound.right)bound.right=x;//450
                else if(x<bound.left || bound.left==0) bound.left=x;//160
                if(y>bound.top) bound.top=y;//600
                else if(y<bound.bottom || bound.bottom==0) bound.bottom=y;
             }
            myNames.add((String) object.get("name"));
            if(bitmap!=null){
                Bitmap newBitmap = Bitmap.createBitmap(bitmap,bound.left,bound.bottom,bound.right-bound.left,(bound.top-bound.bottom));
                myBitmaps.add(newBitmap);
            }
        }

    }


    private Task<String> ImageCompress(String pathName, int compRate) {
        Map<String, Object> data = new HashMap<>();
        data.put("compRate",compRate);
        data.put("path_name",pathName);
        return mFunctions.getHttpsCallable("ImageCompress").call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        return String.valueOf(result.get("result")) ;
                    }
                });
    }

    private Task<Map<String, Object>> ImageSegmantation(String pathName,String uri){
        Map<String, Object> data = new HashMap<>();
        data.put("dowloandUrl", uri);
        data.put("path_name",pathName);
        return mFunctions .getHttpsCallable("ImageSegmantation").call(data)
                .continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
            @Override
            public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                return result ;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==1 && grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
            ShowDialog();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK ){
            if (requestCode == 2 && data != null ) {
                selectedImage = data.getData();
                img.setBackgroundColor(Color.rgb(255,255,255));
                img.setImageURI(selectedImage);
            }else if(requestCode==3){
                img.setImageURI(selectedImage);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
