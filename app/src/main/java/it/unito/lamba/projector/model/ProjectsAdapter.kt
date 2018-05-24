package it.unito.lamba.projector.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import android.view.View
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.robertlevonyan.views.chip.Chip
import it.unito.lamba.projector.Database
import it.unito.lamba.projector.data.Project
import it.unito.lamba.projector.R
import kotlinx.android.synthetic.main.project_card.view.*
import org.greenrobot.eventbus.EventBus
import it.unito.lamba.projector.FragmentInteractionEvent
import com.google.firebase.database.DatabaseError
import it.lamba.utilslibrary.Utils.createChipBitmap
import it.lamba.utilslibrary.inflate
import it.unito.lamba.projector.FragmentInteractionEvent.*


class ProjectsAdapter(options: FirebaseRecyclerOptions<Project>, val context: Context): FirebaseRecyclerAdapter<Project, ProjectVH>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ProjectVH(parent.inflate(R.layout.project_card).apply {
        open_button.setOnClickListener {
            EventBus.getDefault().post(FragmentInteractionEvent(OpenProject.apply { data =  it.tag as String}))
        }
    })

    override fun onBindViewHolder(holder: ProjectVH, position: Int, model: Project){
        holder.bind(model.apply {
            id = snapshots.getSnapshot(position).key
        }, context)
    }

    override fun onDataChanged() {
    }

    override fun onError(e: DatabaseError) {
        Log.e("ProjectsAdapter", e.message)
    }



}

class ProjectVH(private val v: View): RecyclerView.ViewHolder(v) {

    private val TAG = "ProjectVH"

    fun bind(project: Project, context: Context) {
        if(project.owners == null) return
        for(userId in project.owners!!.keys){
            Database.getUserByUid(userId, {
                val chip = Chip(context)
                chip.setOnClickListener { _ ->
                    EventBus.getDefault().post(FragmentInteractionEvent(OpenProfile.apply { data =  it!!.uid}))
                }
                chip.chipText = it!!.getDisplayName()
                if(it.imgUri != null) {
                    Database.getFile(it.imgUri!!, {
                        chip.isHasIcon = true
                        chip.setChipIcon(createChipBitmap(it, context))
                        v.flowLayout.addView(chip)
                    })
                } else {
                    v.flowLayout.addView(chip)
                }
            }, {
                Log.e(TAG, it.message)
            })
        }
        v.card_project_title.text = project.title
        v.card_project_description.text = project.description
        v.open_button.tag = project.id
    }
}