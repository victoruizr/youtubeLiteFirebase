package com.example.firebase

import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.Modelo.Videos
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import kotlin.collections.ArrayList

class AdapterVideo(
    private var context: Context,
    private var videoArrayList: ArrayList<Videos>?,
    private var firebaseAuth: FirebaseAuth,
    var db: DatabaseReference,
    var id: Int

) :RecyclerView.Adapter<AdapterVideo.HolderVideo>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderVideo {
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_item_video,parent,false)
        return HolderVideo(view)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onBindViewHolder(holder: HolderVideo, position: Int) {
        val modelVideo = videoArrayList!![position]

        val id: String? = modelVideo.id
        val title: String? = modelVideo.title
        val timestamp: String? = modelVideo.timestamp
        val videoUri: String? = modelVideo.videoUri

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp!!.toLong()
        val formattedDateTime = DateFormat.format("dd/MM/yyy hh:mm:ss", calendar).toString()

        holder.titleTv.text = title
        holder.time.text=formattedDateTime

        setVideoUrl(modelVideo,holder)

        holder.borrar.setOnClickListener {
            borrarVideo(modelVideo)

        }

        holder.descargar.setOnClickListener {
            descargarVideo(modelVideo)
        }

        if (id?.equals(1)!!){
            holder.borrar.visibility=View.GONE
        }

    }

    private fun descargarVideo(modelVideo: Videos) {
        val videoUrl=modelVideo.videoUri!!

        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl)
        storageReference.metadata
            .addOnSuccessListener {storageMetadata->
                val fileName = storageMetadata.name
                val fileType = storageMetadata.contentType
                val fileDirectory = Environment.DIRECTORY_DOWNLOADS

                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                val uri = Uri.parse(videoUrl)

                val request = DownloadManager.Request(uri)

                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                request.setDestinationInExternalPublicDir("$fileDirectory","$fileName.mp4")

                downloadManager.enqueue(request)

            }
            .addOnFailureListener{e->
                Toast.makeText(context,e.message,Toast.LENGTH_SHORT).show()
            }

    }

    private fun borrarVideo(modelVideo: Videos) {
        val progressDialog:ProgressDialog = ProgressDialog(context)
        progressDialog.setTitle("Espere porfavor")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        val videoId = modelVideo.id!!
        val videoUrl = modelVideo.videoUri!!

        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl!!)
        storageReference.delete()
            .addOnSuccessListener {
                val databaseReference = FirebaseDatabase.getInstance().getReference("Videos")
                databaseReference.child(videoId)
                    .removeValue()
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(context,"Eliminado con exito",Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener{e->
                        progressDialog.dismiss()
                        Toast.makeText(context,e.message,Toast.LENGTH_SHORT).show()

                    }
            }
            .addOnFailureListener{ e->
                progressDialog.dismiss()
                Toast.makeText(context,e.message,Toast.LENGTH_SHORT).show()
            }

        eliminarVideoUsuario(modelVideo)

    }

    private fun eliminarVideoUsuario(modelVideo: Videos) {
        firebaseAuth.currentUser?.getUid()?.let {
            db.child("Usuarios").child(it).child("listaVideos")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (xVideo in snapshot.children) {
                            var v = xVideo.getValue(Videos::class.java)!!
                            if(v.id?.equals(modelVideo.id)!!){
                                db.child("Usuarios").child(it).child("listaVideos")
                                    .child(xVideo.key.toString()).removeValue()
                            }
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun setVideoUrl(modelVideo: Videos, holder: HolderVideo) {
        holder.progressBar.visibility=View.VISIBLE

        val videoUrl: String? = modelVideo.videoUri

        val mediaController = MediaController(context)
        mediaController.setAnchorView(holder.videoView)
        val videoUri = Uri.parse(videoUrl)

        holder.videoView.setMediaController(mediaController)
        holder.videoView.setVideoURI(videoUri)
        holder.videoView.requestFocus()
        holder.videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.start()
        }
        holder.videoView.setOnInfoListener(MediaPlayer.OnInfoListener{mp,what, extra->

            when(what){
                MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START ->{
                    holder.progressBar.visibility = View.VISIBLE
                    return@OnInfoListener true
                }
                MediaPlayer.MEDIA_INFO_BUFFERING_START->{
                    holder.progressBar.visibility = View.VISIBLE
                    return@OnInfoListener true
                }
                MediaPlayer.MEDIA_INFO_BUFFERING_END->{
                    holder.progressBar.visibility = View.GONE
                    return@OnInfoListener true
                }
            }

            false
        })

        holder.videoView.setOnCompletionListener {mediaPlayer ->
            holder.progressBar.visibility = View.GONE
            mediaPlayer.start()
        }


    }

    override fun getItemCount(): Int {
        return videoArrayList!!.size
    }

    class HolderVideo(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var videoView: VideoView = itemView.findViewById(R.id.videoView)
        var titleTv: TextView = itemView.findViewById(R.id.tituloVideo)
        var time: TextView = itemView.findViewById(R.id.timeVideo)
        val borrar:FloatingActionButton = itemView.findViewById(R.id.borrarFab)
        val descargar:FloatingActionButton = itemView.findViewById(R.id.descargarFab)
        var progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }

}