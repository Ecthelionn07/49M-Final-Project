package com.boun.atakantwitter2

import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date


class Login : AppCompatActivity() {

    private var myAuth:FirebaseAuth?=null
    private var database=FirebaseDatabase.getInstance("https://atakantwitter-99046-default-rtdb.europe-west1.firebasedatabase.app/")
    private var myRef=database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        myAuth= FirebaseAuth.getInstance()

        ivImagePerson.setOnClickListener(View.OnClickListener {
            checkPermission()
        })

    }

    fun loginToFirebase(email:String, password:String){

        myAuth!!.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this){ task ->

                if(task.isSuccessful){
                    Toast.makeText(applicationContext,"Login successful.", Toast.LENGTH_LONG).show()

                    saveImageToFirebase()
                }
                else{
                    Toast.makeText(applicationContext,"Login failed!", Toast.LENGTH_LONG).show()
                }
            }
    }


    fun saveImageToFirebase(){

        var currentUser = myAuth!!.currentUser
        var email:String=currentUser!!.email.toString()

        val storage=FirebaseStorage.getInstance()
        val storageRef=storage.getReferenceFromUrl("gs://atakantwitter-99046.appspot.com")
        val df=SimpleDateFormat("ddMMyyHHmmss")
        val dataObject=Date()
        val imagePath=splitString(email) + "." + df.format(dataObject)+".jpg"
        val imageRef=storageRef.child("images/" + imagePath)

        ivImagePerson.isDrawingCacheEnabled=true
        ivImagePerson.buildDrawingCache()

        val drawable=ivImagePerson.drawable as BitmapDrawable
        val bitmap=drawable.bitmap
        val bytes=ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100, bytes)

        val data=bytes.toByteArray()
        val uploadTask=imageRef.putBytes(data)
        uploadTask.addOnFailureListener{Toast.makeText(applicationContext,"Image couldn't be uploaded, please try again.", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener{taskSnapshot ->

            var downloadURL = taskSnapshot.storage.downloadUrl.toString()!!

            myRef.child(currentUser.uid).child("email").setValue(currentUser.email)
            myRef.child(currentUser.uid).child("profile_picture").setValue(downloadURL)
            loadTweets()
        }
    }

    fun splitString(email:String):String{
        val split=email.split("@")
        return split[0]
    }

    override fun onStart() {
        super.onStart()
        loadTweets()
    }


    fun loadTweets(){

        var currentUser=myAuth!!.currentUser

        if(currentUser!=null){

            var intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email",currentUser.email)
            intent.putExtra("uid",currentUser.uid)

            startActivity(intent)
        }

    }

    val readImage:Int=777

    fun checkPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){

                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),readImage)
                return
            }
        }

        loadImage()
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {

        when(requestCode){
            readImage->{
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    loadImage()
                }
                else{
                    Toast.makeText(this, "Can't access your images.", Toast.LENGTH_LONG).show()
                }
            }
            else->super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    val pickImageCode=777
    fun loadImage(){

        var intent=Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent,pickImageCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==pickImageCode && data!=null){

            val selectedImage=data.data
            val filePathColumn= arrayOf(MediaStore.Images.Media.DATA)
            val cursor=contentResolver.query(selectedImage!!,filePathColumn,null,null,null)
            cursor!!.moveToFirst()
            val columnIndex=cursor.getColumnIndex(filePathColumn[0])
            val picturePath=cursor.getString(columnIndex)
            cursor.close()

            ivImagePerson.setImageBitmap(BitmapFactory.decodeFile(picturePath))
        }
    }

    fun buLogin(view:View){

        loginToFirebase(etEmail.text.toString(),etPassword.text.toString())


    }
}