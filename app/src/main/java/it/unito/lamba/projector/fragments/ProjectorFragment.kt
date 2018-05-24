package it.unito.lamba.projector.fragments

import android.support.v4.app.Fragment
import android.widget.EditText
import it.unito.lamba.projector.CloseKeyBoardEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import it.lamba.utilslibrary.Utils.hideKeyboardFrom



abstract class ProjectorFragment: Fragment() {

    @Suppress("unused")
    @Subscribe
    fun closeKeyboard(event: CloseKeyBoardEvent){
        if(activity?.currentFocus is EditText) {
            hideKeyboardFrom(activity!!, activity!!.currentFocus as EditText)
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }
}