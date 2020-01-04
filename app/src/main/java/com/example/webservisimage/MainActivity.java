package com.example.webservisimage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.app.ProgressDialog;
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
import android.widget.TextView;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    private FirebaseFunctions mFunctions;;
    private StorageReference mStorageRef;
    private Uri selectedImage;
    ImageView img,resultImg;
    EditText editTextRate;
    TextView textObjectName;
    List<Bitmap> myBitmaps = new ArrayList<>();
    List<String> myNames= new ArrayList<>();;
    int idBitmap=0;
    ProgressDialog progressDialog ;
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
        textObjectName=findViewById(R.id.txt_obje_name);
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Lütfen bekleyiniz..");
    }

    public void Compress(View view){
        if (selectedImage!=null){
            if (!TextUtils.isEmpty(editTextRate.getText())){
                Toast.makeText(this, "Resim Sunucuda Compress edilicek", Toast.LENGTH_LONG).show();
                int x=100-((Integer.parseInt(editTextRate.getText().toString()))%100);
                if (x>75)x=75;// sıkıştırma doğru çalışması için
                progressDialog.show();
                addFirebaseFile(selectedImage,1,x);
            }else Toast.makeText(this, "Sıkıştırma oranı gir", Toast.LENGTH_SHORT).show();
        }else Toast.makeText(this, "Bir resim seçilmedi", Toast.LENGTH_SHORT).show();

    }

    public void Segmantation(View view){
        if (selectedImage!=null){
             myBitmaps.clear();
             myNames.clear();
             idBitmap=0;
             progressDialog.show();
             addFirebaseFile(selectedImage,2,0);
        }else Toast.makeText(this, "Bir resim seçilmedi", Toast.LENGTH_SHORT).show();
    }

    public  void  CropBitmaps(View view){
        if(!myBitmaps.isEmpty()){
            idBitmap=(idBitmap+1)%myBitmaps.size();
            resultImg.setImageBitmap(myBitmaps.get(idBitmap));
            textObjectName.setText(myNames.get(idBitmap));
        }else{
            Toast.makeText(this, "Kırpılmış bir resim yok", Toast.LENGTH_SHORT).show();
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
                                            progressClosed();
                                        }else{
                                            progressClosed();
                                            Toast.makeText(MainActivity.this, "Upsss....", Toast.LENGTH_SHORT).show();
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
                                            progressClosed();
                                        }else{
                                            progressClosed();
                                            Toast.makeText(MainActivity.this, "Upsss....", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressClosed();
                Toast.makeText(MainActivity.this, "Upssss.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public  void progressClosed(){
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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
              int x=0,y=0;
              try{
                    if (vertices.get("x") != null) x = (int)((double)vertices.get("x")*bitmap.getWidth());
                    if (vertices.get("y") != null) y = (int)((double)vertices.get("y")*bitmap.getHeight());
                }catch (Exception e){
                    if (x==0) x=bitmap.getWidth();
                    if (y==0) y=bitmap.getHeight();
                    System.out.println(e.getMessage());
                }
                if(x>bound.right) bound.right=x;
                else if(x<bound.left || bound.left==0) bound.left=x;
                if(y>bound.top) bound.top=y;
                else if(y<bound.bottom || bound.bottom==0) bound.bottom=y;
             }
             myNames.add((String)object.get("name"));
            if(bitmap!=null){
                Bitmap newBitmap = Bitmap.createBitmap(bitmap,bound.left,bound.bottom,bound.right-bound.left,(bound.top-bound.bottom));
                myBitmaps.add(newBitmap);
            }
        }
        resultImg.setBackgroundColor(Color.rgb(255,255,255));
        resultImg.setImageBitmap(myBitmaps.get(idBitmap));
        textObjectName.setText(myNames.get(idBitmap));
    }


    private Task<String> ImageCompress(String pathName, int compRate) {
        Map<String, Object> data = new HashMap<>();
        data.put("compRate",compRate);
        data.put("path_name",pathName);
        return mFunctions.getHttpsCallable("ImageCompress").call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        if (task.isSuccessful()){
                            Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                            textObjectName.setText("new=> "+result.get("fileSize") +" KB \n"+"old=> "+result.get("orjinalFileSize")+" KB");
                            return String.valueOf(result.get("result")) ;
                        }else{
                            return null;
                        }

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
        Bitmap bitmap = null;


        if (resultCode == RESULT_OK ){
            if (requestCode == 2 && data != null ) {
                selectedImage = data.getData();
                try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),selectedImage);
                        bitmap = ImageDecoder.decodeBitmap(source);
                        img.setBackgroundColor(Color.rgb(255,255,255));
                        img.setImageBitmap(bitmap);
                    }
                    else {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImage);
                        img.setImageBitmap(bitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(requestCode==3){

                try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),selectedImage);
                        bitmap = ImageDecoder.decodeBitmap(source);
                        img.setBackgroundColor(Color.rgb(255,255,255));
                        img.setImageBitmap(bitmap);

                    }
                    else {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImage);
                        img.setImageBitmap(bitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }





/*
        if (resultCode == RESULT_OK ){
            if (requestCode == 2 && data != null ) {
                selectedImage = data.getData();
                img.setBackgroundColor(Color.rgb(255,255,255));
                img.setImageURI(selectedImage);
            }else if(requestCode==3){
                img.setImageURI(selectedImage);
            }
        }*/
        super.onActivityResult(requestCode, resultCode, data);
    }


}
