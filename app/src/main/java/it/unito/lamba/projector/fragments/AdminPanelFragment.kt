package it.unito.lamba.projector.fragments

import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import it.lamba.utilslibrary.inflate
import it.unito.lamba.projector.Database
import it.unito.lamba.projector.R
import org.greenrobot.eventbus.EventBus
import it.unito.lamba.projector.FragmentInteractionEvent
import it.unito.lamba.projector.FragmentInteractionEvent.*
import it.unito.lamba.projector.model.UsersAdapter
import kotlinx.android.synthetic.main.fragment_admin_panel.view.*

class AdminPanelFragment: ProjectorFragment() {

    private var usersAdapter: UsersAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container!!.inflate(R.layout.fragment_admin_panel).apply {
            Database.getCurrentUser({
                Database.isUserAdmin(it, {isAdmin ->
                    if(!isAdmin) EventBus.getDefault().post(FragmentInteractionEvent(AllProjects))
                    else{
                        Database.getTotalProjects {
                            total_projects_tv.text = it.toString()
                        }
                        Database.getTotalUsers {
                            total_users_tv.text = it.toString()
                        }
                    }
                })
            })
            show_users_button.setOnClickListener {
                show_users_button.visibility = GONE
                loadUsers(this)
            }
        }
    }

    private fun loadUsers(v: View) {
        usersAdapter = UsersAdapter(Database.buildUsersUiOptions(), context!!).apply { startListening() }
        v.users_rv.adapter = usersAdapter
        v.users_rv.visibility = VISIBLE
    }

    override fun onResume() {
        super.onResume()
        usersAdapter?.startListening()
    }

    override fun onPause() {
        usersAdapter?.stopListening()
        super.onPause()
    }
}