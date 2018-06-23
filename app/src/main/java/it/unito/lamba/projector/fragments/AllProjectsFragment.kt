package it.unito.lamba.projector.fragments

import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import it.unito.lamba.projector.BindAdapterInAllProjectsEvent
import it.unito.lamba.projector.model.ProjectsAdapter
import org.greenrobot.eventbus.Subscribe

const val MAIN_RV_ID = 123456

class AllProjectsFragment: ProjectorFragment() {

    private lateinit var rv: RecyclerView
    private var adapter: ProjectsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rv = RecyclerView(this.context)
        rv.layoutParams = CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        rv.layoutManager = LinearLayoutManager(context)
        rv.id = MAIN_RV_ID
        return rv
    }

    @Subscribe(sticky = true)
    fun bindAdapter(event: BindAdapterInAllProjectsEvent){
        adapter = event.adapter
        rv.adapter = adapter
        adapter!!.startListening()
    }

    override fun onPause() {
        adapter?.stopListening()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adapter?.startListening()
    }
}