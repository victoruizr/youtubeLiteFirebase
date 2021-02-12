package com.example.firebase

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.Modelo.Videos
import com.example.firebase.Modelo.Usuarios
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.lang.NullPointerException

class HomeFragment : Fragment() {

    private lateinit var videArrayList: ArrayList<Videos>
    private lateinit var adaptadorVideo: AdapterVideo
    private lateinit var db: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    var activityX: MainActivity? = null
    private lateinit var imagen: String
    private lateinit var photoURl: String
    private lateinit var selectVideo: FloatingActionButton
    private lateinit var rvVideos: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,

        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        usuario();
        cargarVideos()
        rvVideos = root.findViewById(R.id.rvVideos)
        selectVideo = root.findViewById(R.id.selectVideo)
        selectVideo.setOnClickListener {
            this.findNavController().navigate(R.id.action_nav_home_to_addvideo)
        }




        return root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference()


    }

    private fun cargarVideos() {

            videArrayList = ArrayList()

            val ref = FirebaseDatabase.getInstance().getReference("Videos")

            ref.addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        videArrayList.clear()
                        for (videos in snapshot.children) {
                            val modeloVideos = videos.getValue(Videos::class.java)
                            videArrayList.add(modeloVideos!!)
                        }
                        adaptadorVideo = AdapterVideo(context!!, videArrayList, firebaseAuth, db, 0)
                        rvVideos.adapter = adaptadorVideo
                    } catch (e: Exception) {
                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })



    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityX = context as MainActivity
    }


    private fun usuario() {
        db.child("Usuarios").child(firebaseAuth.currentUser!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val email = snapshot.child("email").value.toString()
                        photoURl = snapshot.child("imagen").value.toString()
                        activityX?.changeNavHeaderData(email, photoURl)
                    } else {
                        crearUsuario()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun crearUsuario() {
        if (firebaseAuth.currentUser!!.photoUrl == null) {
            imagen =
                "https://firebasestorage.googleapis.com/v0/b/videos-fe898.appspot.com/o/886399_user_512x512.png?alt=media&token=d2f71094-cd0b-4aae-ba1c-70371ccbf59e"
        } else {
            imagen = firebaseAuth.currentUser!!.photoUrl.toString()
        }


        val video = Videos()
        val listaVideos: ArrayList<Videos> = ArrayList<Videos>()
        listaVideos.add(video)
        val usu = firebaseAuth.currentUser!!.email?.let {
            Usuarios(
                it,
                imagen,
                listaVideos
            )
        }

        db.child("Usuarios").child(firebaseAuth.currentUser!!.uid).setValue(usu)
    }
}