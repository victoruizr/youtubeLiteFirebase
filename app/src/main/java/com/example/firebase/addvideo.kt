package com.example.firebase

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.firebase.Modelo.Videos
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_addvideo.*

class addvideo : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var root: View
    private lateinit var actionBar: ActionBar

    private val VIDEO_PICK_GALLERY_CODE = 100

    val REQUEST_VIDEO_CAPTURE = 1

    private val CAMERA_REQUEST_CODE = 102

    private lateinit var cameraPermisions: Array<String>

    private lateinit var progresDialog: ProgressDialog

    private var videoUri: Uri? = null

    private var title: String = ""
    private lateinit var btnUploadVideo:Button
    private lateinit var fabChooseVideo:FloatingActionButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference()
        activity?.actionBar?.title = "Añade video"

        activity?.actionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.actionBar?.setDisplayShowHomeEnabled(true)

        cameraPermisions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        progresDialog = ProgressDialog(context)
        progresDialog.setTitle("Por favor espere")
        progresDialog.setMessage("Subiendo video")
        progresDialog.setCanceledOnTouchOutside(false)


    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        root =inflater.inflate(R.layout.fragment_addvideo, container, false)
        fabChooseVideo = root.findViewById(R.id.fabChooseVideo )
        btnUploadVideo = root.findViewById(R.id.btnUploadVideo)


            btnUploadVideo.setOnClickListener {
                title = titleEt.text.toString().trim()
                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(context, "El titulo es requerido", Toast.LENGTH_SHORT).show()
                } else if (videoUri == null) {
                    Toast.makeText(context, "Seleccione un video", Toast.LENGTH_SHORT).show()
                } else {
                    uploadVideoFirebase()
                }
            }

        fabChooseVideo.setOnClickListener {
            videoPickDialog()
        }

        return root
    }


    private fun uploadVideoFirebase() {
        progresDialog.show()
        val timestamp = "" + System.currentTimeMillis()
        val filePathAndName = "Videos/video_$timestamp"

        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)

        storageReference.putFile(videoUri!!).addOnSuccessListener { taskSnapshot ->
            val uriTask = taskSnapshot.storage.downloadUrl
            while (!uriTask.isSuccessful);
            val downloadUri = uriTask.result
            if (uriTask.isSuccessful) {
                val hashMap = HashMap<String, Any>()
                hashMap["id"] = "$timestamp"
                hashMap["title"] = "$title"
                hashMap["timestamp"] = "$timestamp"
                hashMap["videoUri"] = "$downloadUri"

                guardarVideo(hashMap["id"],hashMap["title"],hashMap["timestamp"],hashMap["videoUri"])

                val dbReference = FirebaseDatabase.getInstance().getReference("Videos")
                dbReference.child(timestamp)
                    .setValue(hashMap)
                    .addOnSuccessListener { taskSnapshot ->
                        progresDialog.dismiss()
                        Toast.makeText(context, "Video subido", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        progresDialog.dismiss()
                        Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }

        }.addOnFailureListener { e ->
            progresDialog.dismiss()
            Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun guardarVideo(hashMap: Any?, any: Any?, any1: Any?, any2: Any?) {
        firebaseAuth.currentUser?.getUid()?.let {
            db.child("Usuarios").child(it).child("listaVideos")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val listaVideo: java.util.ArrayList<Videos> = java.util.ArrayList<Videos>()
                        listaVideo.clear()
                        for (xVideo in snapshot.children) {
                            var v = xVideo.getValue(Videos::class.java)!!
                            listaVideo.add(v)
                        }
                        var vide = Videos(
                            hashMap as String?, any as String?, any1 as String?,
                            any2 as String?
                        )
                        listaVideo.add(vide)
                        db.child("Usuarios").child(it).child("listaVideos").setValue(listaVideo)



                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun setVideoToVideoView() {
        val mediaController = MediaController(context)
        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)

        videoView.setVideoURI(videoUri)
        videoView.requestFocus()
        videoView.setOnPreparedListener {
            videoView.pause()
        }


    }

    private fun videoPickDialog() {
        val options = arrayOf("Camera", "Gallery")

        val builder = context?.let { AlertDialog.Builder(it) }
        builder?.setTitle("Selecciona un video de")
            ?.setItems(options) { dialogInterface, i ->
                if (i == 0) {
                    if (!checkCameraPermissions()) {
                        requestCameraPermissions()
                    } else {
                        videoPickCamera()
                    }
                } else {
                    videoPickGallery()
                }
            }
            ?.show()
    }

    private fun requestCameraPermissions() {
        activity?.let {
            ActivityCompat.requestPermissions(
                it,
                cameraPermisions,
                CAMERA_REQUEST_CODE
            )
        }
    }

    private fun checkCameraPermissions(): Boolean {
        val result1 = context?.let {
            ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.CAMERA
            )
        } == PackageManager.PERMISSION_GRANTED

        val result2 = context?.let {
            ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } == PackageManager.PERMISSION_GRANTED

        return result1 && result2

    }

    private fun videoPickGallery() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(
            Intent.createChooser(intent, "Escoge un video"),
            VIDEO_PICK_GALLERY_CODE
        )
    }

    private fun videoPickCamera() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(requireContext().packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            CAMERA_REQUEST_CODE ->
                if (grantResults.size > 0) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted) {
                        videoPickCamera()
                    } else {
                        Toast.makeText(context, "Permisos denegados", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            videoUri = data?.data!!
            setVideoToVideoView()
        } else if (requestCode == VIDEO_PICK_GALLERY_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            videoUri = data?.data!!
            setVideoToVideoView()
        } else {
            Toast.makeText(context, "Cancelado", Toast.LENGTH_SHORT).show()

        }
    }
}





