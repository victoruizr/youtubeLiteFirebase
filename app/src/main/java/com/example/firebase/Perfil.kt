package com.example.firebase

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.fragment_perfil.*

class Perfil : Fragment() {

    private lateinit var root: View

    private val GALLERY_INTENT = 1
    private var referencia: StorageReference? = null
    private var database: DatabaseReference? = null
    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        referencia = FirebaseStorage.getInstance().reference
    }

    fun cargarDatosUsuario(root: View, textView2: TextView, imageView3: ImageView) {
        database!!.child("Usuarios").child(firebaseAuth!!.currentUser!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val email = snapshot.child("email").value.toString()
                        textView2.setText(email)
                        val photoURl = snapshot.child("imagen").value.toString()
                        imageView3.setImageURI(Uri.parse(photoURl))
                        Glide.with(root.context)
                            .load(photoURl)
                            .into(imageView3)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun cambiarImagen() {
        val cameraIntent = Intent(Intent.ACTION_PICK)
        cameraIntent.type = "image/*"
        startActivityForResult(
            cameraIntent,
            GALLERY_INTENT
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_perfil, container, false)


        val imageView3: ImageView = root.findViewById(R.id.imageView3)
        val textView2: TextView = root.findViewById(R.id.textView2)

        cargarDatosUsuario(root, textView2, imageView3)

        imageView3.setOnClickListener {
            cambiarImagen()
            cargarDatosUsuario(root, textView2, imageView3)
        }

        return root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_INTENT && resultCode == Activity.RESULT_OK) {
            val uri = data!!.data
            var ruta = referencia?.child("avatares")?.child(uri?.lastPathSegment!!)

            ruta!!.putFile(uri!!).continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                ruta!!.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val nuevaFoto = task.result.toString()
                    database!!.child("Usuarios").child(firebaseAuth!!.currentUser!!.uid)
                        .child("imagen").setValue(nuevaFoto)
                }
            }
        }
    }
}



