package it.unito.lamba.projector.fragments

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Toast
import it.lamba.utilslibrary.Utils.isEmailValid
import it.lamba.utilslibrary.Utils.isNameValid
import it.lamba.utilslibrary.Utils.showError
import it.lamba.utilslibrary.inflate
import it.unito.lamba.projector.*
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import kotlinx.android.synthetic.main.fragment_edit_profile.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import it.unito.lamba.projector.FragmentInteractionEvent.OpenProfile

class EditProfileFragment: ProjectorFragment() {

    companion object {
        val TAG = "EditProfileFragment"

        fun createBundle(): Bundle{
            return Bundle().apply {
                this.putBoolean(TAG, true)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return container!!.inflate(R.layout.fragment_edit_profile).apply {
            profile_image_overlay.setOnClickListener {
                launchSelectPictureEvent()
            }
            profile_image_icon.setOnClickListener {
                launchSelectPictureEvent()
            }
            save_button.setOnClickListener {
                attemptSave()
            }
            Database.getCurrentUser( {
                if(it.name != null) name.text = SpannableStringBuilder(it.name)
                if(it.surname != null)surname.text = SpannableStringBuilder(it.surname)
                if(it.email != null && it.email != "") {
                    email.text = SpannableStringBuilder(it.email)
                    email.isEnabled = false
                }
                if(it.imgUri != null && it.imgUri != ""){
                    Database.loadUserProfilePictureToImageView(it, context, profile_image, {},{
                        Log.d(TAG, it.message)
                    })
                }
                if(it.badgeNumber != null) badge.text = SpannableStringBuilder(it.badgeNumber)
            }, true)
        }
    }

    private fun attemptSave() {
        progress_bar.visibility = View.VISIBLE
        // Reset errors.
        email.error = null
        name.error = null
        surname.error = null
        badge.error = null

        // Store values at the time of the login attempt.
        val emailStr = email.text.toString()
        val nameStr = name.text.toString()
        val surnameStr = surname.text.toString()
        val badgeStr = badge.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid email address.
        if (TextUtils.isEmpty(nameStr)) {
            name.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isNameValid(nameStr)) {
            name.error = getString(R.string.at_least_two_chars)
            focusView = name
            cancel = true
        }

        if (TextUtils.isEmpty(surnameStr)) {
            surname.error = getString(R.string.error_field_required)
            focusView = surname
            cancel = true
        } else if (!isNameValid(surnameStr)) {
            surname.error = getString(R.string.at_least_two_chars)
            focusView = surname
            cancel = true
        }

        if (TextUtils.isEmpty(badgeStr)) {
            badge.error = getString(R.string.error_field_required)
            focusView = badge
            cancel = true
        } else if (!isNameValid(badgeStr)) {
            badge.error = getString(R.string.at_least_two_chars)
            focusView = badge
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
            progress_bar.visibility = GONE
        } else {
            updateCurrentUser()
        }
    }

    private fun updateCurrentUser() {
        Database.getCurrentUser ({
            it.badgeNumber = badge.text.toString()
            it.name = name.text.toString()
            it.surname = surname.text.toString()
            it.email = email.text.toString()
            Database.updateCurrentUserProfile(it, {
                if(arguments != null && arguments!!.getBoolean(TAG))
                    EventBus.getDefault().post(SetUpNavViewEvent())
                else EventBus.getDefault().post(FragmentInteractionEvent(OpenProfile.apply { data = Database.getCurrentAuthUserID() }))
            },{
                Toast.makeText(context, "Qualcosa è andato storto ¯\\_(ツ)_/¯", Toast.LENGTH_SHORT).show()
                Log.e(TAG, it.message)
            })
            progress_bar.visibility = View.GONE
        })
    }

    private fun launchSelectPictureEvent(){
        progress_bar.visibility = View.VISIBLE
        EventBus.getDefault().post(SelectImageEvent(fragmentClass = this@EditProfileFragment.javaClass))
        profile_image_overlay.isClickable = false
        profile_image_icon.isClickable = false
    }

    @Subscribe(sticky = true)
    fun setProfileImage(event: SetProfileImageEvent){
        EventBus.getDefault().removeStickyEvent(SetProfileImageEvent::class.java)
        Database.updateCurrentUserProfilePicture(event.image, {
            profile_image_icon.isClickable = true
            progress_bar.visibility = View.GONE
        }, {
            showError(it, context!!)
        })
    }
}