package it.unito.lamba.projector.model

import android.content.Context
import android.provider.ContactsContract
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import it.lamba.utilslibrary.inflate
import it.unito.lamba.projector.Database
import it.unito.lamba.projector.R
import it.unito.lamba.projector.data.User
import kotlinx.android.synthetic.main.user_profile_card.view.*
import org.greenrobot.eventbus.EventBus
import it.unito.lamba.projector.FragmentInteractionEvent
import it.unito.lamba.projector.FragmentInteractionEvent.*

class UsersAdapter(options: FirebaseRecyclerOptions<User>, val context: Context): FirebaseRecyclerAdapter<User, UsersAdapter.UserVH>(options) {

    private val TAG = "UsersAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserVH(parent.inflate(R.layout.user_profile_card).apply {
        root_viewgroup.setOnClickListener{
            EventBus.getDefault().post(FragmentInteractionEvent(OpenProfile.apply { data = root_viewgroup.tag as String }))
        }
        user_link_to_project.setOnClickListener {
            EventBus.getDefault().post(FragmentInteractionEvent(OpenProject.apply { data = user_link_to_project.tag as String }))
        }
    })

    override fun onBindViewHolder(holder: UserVH, position: Int, model: User) = holder.bind(model.apply {
        uid = snapshots.getSnapshot(position).key
    }, context)


    class UserVH(val v: View): RecyclerView.ViewHolder(v){

        private val TAG = "UserVH"

        fun bind(user: User, context: Context){
            v.root_viewgroup.tag = user.uid
            Database.loadUserProfilePictureToImageView(user, context, v.user_card_image, {}, {
                Log.e(TAG, it.message)
            })
            v.user_card_display_name.text = user.getDisplayName()
            v.user_card_email.text = user.email
            v.user_card_badge.text = user.badgeNumber
            if(user.projectId != null){
                Database.getProject(user.projectId!!, {
                    v.user_project_title.text = it.title
                    v.user_link_to_project.tag = it.id
                    v.textView13.visibility = VISIBLE
                    v.user_project_title.visibility = VISIBLE
                    v.user_link_to_project.visibility = VISIBLE
                })
            } else {
                v.user_link_to_project.tag = null
                v.textView13.visibility = GONE
                v.user_project_title.visibility = GONE
                v.user_link_to_project.visibility = GONE
            }
        }
    }
}