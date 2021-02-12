package com.example.firebase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.Modelo.Videos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GalleryFragment : Fragment() {

    private lateinit var db: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var rvVideos: RecyclerView
    private lateinit var adaptadorVideo: AdapterVideo
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_gallery, container, false)
        rvVideos = root.findViewById(R.id.rvVideos2)
        cargarVideo()
        return root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference()
    }

    private fun cargarVideo() {
        firebaseAuth.currentUser?.getUid()?.let {
            db.child("Usuarios").child(it).child("listaVideos")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val listaVideo: java.util.ArrayList<Videos> = java.util.ArrayList<Videos>()
                        listaVideo.clear()
                        for (xVideo in snapshot.children) {
                            var v = xVideo.getValue(Videos::class.java)!!
                            listaVideo.add(v)
                        }
                        adaptadorVideo = AdapterVideo(context!!, listaVideo,firebaseAuth,db,1)
                        rvVideos.adapter = adaptadorVideo
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }


}



