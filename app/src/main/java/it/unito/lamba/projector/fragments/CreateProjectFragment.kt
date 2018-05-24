package it.unito.lamba.projector.fragments

import android.graphics.Rect
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import com.robertlevonyan.views.chip.Chip
import it.lamba.imagesadapter.ImagesAdapter
import it.lamba.utilslibrary.Utils.DpToPx
import it.lamba.utilslibrary.Utils.isEmailValid
import it.lamba.utilslibrary.Utils.isNameValid
import it.lamba.utilslibrary.inflate
import it.unito.lamba.projector.*
import kotlinx.android.synthetic.main.fragment_create_project.view.*
import org.greenrobot.eventbus.EventBus
import kotlinx.android.synthetic.main.fragment_create_project.*
import org.greenrobot.eventbus.Subscribe


class CreateProjectFragment: ProjectorFragment() {

    private val emailsList = HashMap<String, String>()
    private lateinit var imagesAdapter: ImagesAdapter
    private val TAG = "CreateProjectFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container!!.inflate(R.layout.fragment_create_project).apply {
            progress_bar.visibility = GONE
            imagesAdapter = ImagesAdapter(context!!)
            images_rv.adapter = imagesAdapter
            images_rv.setSlideOnFling(true)
            images_rv.addItemDecoration(object : RecyclerView.ItemDecoration(){
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                            state: RecyclerView.State) {
                    outRect.right = DpToPx(resources, 8f).toInt()
                    outRect.left = DpToPx(resources, 8f).toInt()
                }
            })

            next_button.setOnClickListener({
                validateProject()
            })
            add_images_button.setOnClickListener({
                EventBus.getDefault().post(SelectImageEvent(true, this@CreateProjectFragment.javaClass))
            })
            add_friend_button.setOnClickListener({
                addChip()
            })
        }
    }

    private fun addChip() {
        val mail = project_emails_textview.text.toString()
        add_friend_button.error = null
        if(isEmailValid(mail)){
            Database.getUserByEmail(mail, {
                if(it == null || it.uid == Database.getCurrentAuthUserID()) project_emails_textview.error = getString(R.string.error_invalid_email)
                else {
                    val uid = it.uid
                    val displayName = it.getDisplayName()
                    project_emails_textview.text = SpannableStringBuilder("")
                    if (emailsList[uid].isNullOrEmpty()) {
                        emailsList[uid] = displayName
                        val chip = Chip(context)
                        chip.chipText = displayName
                        chip.isClosable = true
                        flowLayout.addView(chip)
                        chip.setOnCloseClickListener {
                            emailsList.remove(uid)
                            flowLayout.removeView(chip)
                        }
                    }
                }
            }, {
                Log.e(TAG, it.toString())
                project_emails_textview.error = getString(R.string.error_invalid_email)
            })
        } else {
            project_emails_textview.error = getString(R.string.error_invalid_email)
        }
    }

    private fun validateProject() {
        next_button.isClickable = false
        progress_bar.visibility = VISIBLE
        // Reset errors.
        title.error = null
        project_description_textview.error = null

        // Store values at the time of the login attempt.
        val titleStr = project_title_textview.text.toString()
        val descriptionStr = project_description_textview.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid email address.
        if (TextUtils.isEmpty(titleStr)) {
            project_title_textview.error = getString(R.string.error_field_required)
            focusView = title
            cancel = true
        } else if (!isNameValid(titleStr)) {
            project_title_textview.error = getString(R.string.at_least_two_chars)
            focusView = project_title_textview
            cancel = true
        }

        if (TextUtils.isEmpty(descriptionStr)) {
            project_description_textview.error = getString(R.string.error_field_required)
            focusView = project_description_textview
            cancel = true
        } else if (!isNameValid(descriptionStr)) {
            project_description_textview.error = getString(R.string.at_least_two_chars)
            focusView = project_description_textview
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
            progress_bar.visibility = View.GONE
        } else {
            Database.addProject(titleStr, descriptionStr, emailsList.keys.toList(), imagesAdapter.getImages(), {
                EventBus.getDefault().post(FragmentInteractionEvent(FragmentInteractionEvent.AllProjects))
            }, {
                Toast.makeText(context, "Qualcosa è andato storto...", Toast.LENGTH_SHORT).show()
                next_button.isClickable = true
                progress_bar.visibility = GONE
            })
        }
    }

    @Subscribe(sticky = true)
    fun storeImagesUris(event: SetProjectImagesEvent){
        EventBus.getDefault().removeStickyEvent(SetProjectImagesEvent::class.java)
        this.imagesAdapter.addAll(event.list)
    }
}