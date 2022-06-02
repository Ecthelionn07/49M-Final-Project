package com.boun.atakantwitter2

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets_ticket.view.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var database= FirebaseDatabase.getInstance("https://atakantwitter-99046-default-rtdb.europe-west1.firebasedatabase.app/")
    private var myRef=database.reference

    var listTweets=ArrayList<Ticket>()
    var adapter:MyTweetAdapter?=null

    var myEmail:String?=null
    var myUID:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var b:Bundle= intent.extras!!
        myEmail=b.getString("email")
        myUID=b.getString("uid")

        listTweets.add(Ticket("0","MIS 49M: Mobile Application Development","url","add"))
        //listTweets.add(Ticket("0","him","url","irfan"))
        //listTweets.add(Ticket("0","him","url","atakan"))
        //listTweets.add(Ticket("0","him","url","atakan"))

        adapter=MyTweetAdapter(this,listTweets)
        lvTweets.adapter=adapter

        LoadPost()
    }

    inner class  MyTweetAdapter: BaseAdapter {
        var listTweetsAdapter=ArrayList<Ticket>()
        var context: Context?=null

        constructor(context:Context, listTweetsAdapter:ArrayList<Ticket>):super(){
            this.listTweetsAdapter=listTweetsAdapter
            this.context=context
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {

            var myTweet=listTweetsAdapter[p0]

            if(myTweet.tweetPersonUID.equals("add")){
                var myView=layoutInflater.inflate(R.layout.add_ticket,null)

                myView.iv_attach.setOnClickListener(View.OnClickListener {

                    loadImage()
                })

                myView.iv_post.setOnClickListener(View.OnClickListener {

                    myRef.child("posts").push().setValue(
                        PostInfo(myUID!!,
                        myView.etPost.text.toString(), downloadURL!!))

                    myView.etPost.setText("")
                })
                return myView
            }

            else if(myTweet.tweetPersonUID.equals("loading")){
                var myView=layoutInflater.inflate(R.layout.loading_ticket,null)
                return myView
            }


            else{
                var myView=layoutInflater.inflate(R.layout.tweets_ticket,null)
                myView.txt_tweet.text = myTweet.tweetText

                Picasso.with(context).load(myTweet.tweetImageURL).into(myView.tweet_picture)


                myRef.child("Users").child(myTweet.tweetPersonUID!!)
                    .addValueEventListener(object :ValueEventListener{

                        override fun onDataChange(dataSnapshot: DataSnapshot) {

                            try {

                                var td= dataSnapshot!!.value as HashMap<String,Any>

                                for(key in td.keys){

                                    var userInfo= td[key] as String

                                    if(key.equals("ProfileImage")){
                                        Picasso.with(context).load(userInfo).into(myView.picture_path)
                                    }

                                    else{
                                        myView.txtUserName.text = userInfo
                                    }
                                }

                            }catch (ex:Exception){}


                        }

                        override fun onCancelled(p0: DatabaseError) {

                        }
                    })

                return myView
            }
        }


        override fun getItem(p0: Int): Any {
            return listTweetsAdapter[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {
            return listTweetsAdapter.size
        }

    }

    val pickImageCode=777
    fun loadImage(){

        var intent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
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

            uploadImage(BitmapFactory.decodeFile(picturePath))
        }
    }

    var downloadURL:String?=null

    fun uploadImage(bitmap:Bitmap){

        listTweets.add(0,Ticket("0","him","url","loading"))
        adapter!!.notifyDataSetChanged()

        val storage= FirebaseStorage.getInstance()
        val storageRef=storage.getReferenceFromUrl("gs://atakantwitter-99046.appspot.com")
        val df= SimpleDateFormat("ddMMyyHHmmss")
        val dataObject= Date()
        val imagePath=splitString(myEmail!!) + "." + df.format(dataObject)+".jpg"
        val imageRef=storageRef.child("postedImages/" + imagePath)

        val bytes= ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100, bytes)

        val data=bytes.toByteArray()
        val uploadTask=imageRef.putBytes(data)
        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext,"Image couldn't be uploaded, please try again.", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener{taskSnapshot ->

            downloadURL = taskSnapshot.storage.downloadUrl.toString()!!
            listTweets.removeAt(0)
            adapter!!.notifyDataSetChanged()

        }
    }

    fun splitString(email:String):String{
        val split=email.split("@")
        return split[0]
    }

    fun LoadPost(){

        myRef.child("posts")
            .addValueEventListener(object :ValueEventListener{

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    try {

                        listTweets.clear()
                        listTweets.add(Ticket("0","MIS 49M: Mobile Application Development","url","add"))
                        listTweets.add(Ticket("0","MIS 49M: Mobile Application Development","url","ads"))
                        var td= dataSnapshot!!.value as HashMap<String,Any>

                        for(key in td.keys){

                            var post= td[key] as HashMap<String,Any>

                            listTweets.add(Ticket(key,

                                post["text"] as String,
                                post["postImage"] as String
                                ,post["userUID"] as String))

                        }


                        adapter!!.notifyDataSetChanged()
                    }catch (ex:Exception){}


                }

                override fun onCancelled(p0: DatabaseError) {

                }
            })
    }




}