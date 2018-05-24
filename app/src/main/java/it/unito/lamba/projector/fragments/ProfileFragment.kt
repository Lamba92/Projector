package it.unito.lamba.projector.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import it.lamba.utilslibrary.addOnScrollChangedListener
import it.lamba.utilslibrary.inflate
import it.lamba.utilslibrary.putString_
import it.unito.lamba.projector.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import org.greenrobot.eventbus.EventBus
import it.unito.lamba.projector.data.User
import it.unito.lamba.projector.FragmentInteractionEvent.EditProfile
import it.unito.lamba.projector.FragmentInteractionEvent.OpenProject

class ProfileFragment: ProjectorFragment() {


    companion object {
        const val TAG = "ProfileFragment"
        fun createBundle(userId: String): Bundle{
            return Bundle().putString_(TAG, userId)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container!!.inflate(R.layout.fragment_profile).apply {
            alpha_loading.visibility = View.VISIBLE
            progress_bar.visibility = View.VISIBLE
            val displayedUserId = arguments!!.getString(TAG)
            Database.getUserByUid(displayedUserId, {
                loadUI(it!!, this)
            }, { Log.e(TAG, it.message)})

            val currentAuthUserId = Database.getCurrentAuthUserID()
            if(currentAuthUserId == displayedUserId){
                edit_profile_button.setOnClickListener {
                    EventBus.getDefault().post(FragmentInteractionEvent(EditProfile))
                }
                edit_profile_button.visibility = VISIBLE
            } else {
                edit_profile_button.visibility = GONE
            }

            scroll_view.addOnScrollChangedListener { _, Y ->
                if (Y == 0 ) EventBus.getDefault().post(ChangeToolbarElevationEvent(0f))
                else EventBus.getDefault().post(ChangeToolbarElevationEvent(8f))
            }
            EventBus.getDefault().post(ChangeToolbarElevationEvent(0f))
        }
    }

    private fun loadUI(it: User, v: View) {
        v.display_name.text = it.getDisplayName()
        v.email_text_view.text = it.email
        v.badge_tv.text = it.badgeNumber
        if (it.projectId != null) {
            Database.getProject(it.projectId!!, {
                v.project_title_tv.visibility = VISIBLE
                v.project_tv.visibility = VISIBLE
                v.go_to_project_button.visibility = VISIBLE
                v.project_tv.text = it.title
                v.go_to_project_button.setOnClickListener {_ ->
                    EventBus.getDefault().post(FragmentInteractionEvent(OpenProject.apply { data = it.id!! }))
                }
            })
        } else {
            v.project_title_tv.visibility = GONE
            v.project_tv.visibility = GONE
            v.go_to_project_button.visibility = GONE
        }
        if(it.imgUri != null){
            Database.loadUserProfilePictureToImageView(it, this.context!!, v.profile_image, {
                v.profile_image.alpha = 1f
                v.profile_image_icon.visibility = GONE
            },{
                it.printStackTrace()
            })
        }
        v.progress_bar.visibility = View.GONE
        v.alpha_loading.visibility = View.GONE
    }
}