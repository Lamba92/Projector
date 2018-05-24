package it.unito.lamba.projector.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.robertlevonyan.views.chip.Chip
import it.lamba.imagesadapter.ImagesAdapter
import it.lamba.utilslibrary.Utils.DpToPx
import it.lamba.utilslibrary.inflate
import it.lamba.utilslibrary.putString_
import it.unito.lamba.projector.Database
import it.unito.lamba.projector.R
import org.greenrobot.eventbus.EventBus
import it.unito.lamba.projector.FragmentInteractionEvent
import it.unito.lamba.projector.FragmentInteractionEvent.*
import kotlinx.android.synthetic.main.fragment_project.*
import kotlinx.android.synthetic.main.fragment_project.view.*

class ProjectFragment: ProjectorFragment() {

    private val TAG = "ProjectFragment"
    private lateinit var imagesAdapter: ImagesAdapter
    private var done = 0
    private var showFabEndedLoading = false

    companion object {

        const val BUNDLETAG = "yolo"

        fun createBundle(projectId: String): Bundle{
            return Bundle().putString_(BUNDLETAG, projectId)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container!!.inflate(R.layout.fragment_project).apply {
            val projectId = arguments!!.getString(BUNDLETAG)
            Database.getCurrentUser({
                if(it.projectId != null && it.projectId == projectId) {
                    showFabEndedLoading = true
                    edit_fab.setOnClickListener {
                        EventBus.getDefault().post(FragmentInteractionEvent(EditProject))
                    }
                }
            })
            imagesAdapter = ImagesAdapter(context!!, false)
            images_rv.adapter = imagesAdapter
            images_rv.setSlideOnFling(true)
            images_rv.addItemDecoration(object : RecyclerView.ItemDecoration(){
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                            state: RecyclerView.State) {
                    outRect.right = DpToPx(resources, 4f).toInt()
                    outRect.left = DpToPx(resources, 4f).toInt()
                }
            })
            Database.getProject(projectId, {
                progress_bar.visibility = VISIBLE
                project_title.text = it.title
                project_description.text = it.description
                flowLayout.removeAllViews()
                loadChipFromUserIds(it.owners!!.keys)
                loadImagesFromPaths(it.images!!.keys)
            })
        }
    }

    private fun loadImagesFromPaths(images: MutableSet<String>?) {
        if(images != null){
            done+=images.size
            for(imagePath in images){
                Database.getFile("project/images/$imagePath", {
                    imagesAdapter.add(it)
                    doneLoading()
                })
            }
        }
    }

    private fun loadChipFromUserIds(userIds: MutableSet<String>) {
        done+=userIds.size
        for(userId in userIds){
            Database.getUserByUid(userId, {
                val chip = Chip(context)
                chip.chipText = it!!.getDisplayName()
                if(it.imgUri != null) {
                    Database.getFile(it.imgUri!!, {
                        chip.isHasIcon = true
                        chip.setOnChipClickListener { _ ->
                            EventBus.getDefault().post(FragmentInteractionEvent(OpenProfile.apply { data = userId}))
                        }
                        chip.setChipIcon(
                                Bitmap.createScaledBitmap(
                                        BitmapFactory.decodeFile(it.path),
                                        DpToPx(resources, 35f).toInt(),
                                        DpToPx(resources, 35f).toInt(),
                                        false))
                        flowLayout.addView(chip)
                        doneLoading()
                    })
                } else {
                    flowLayout.addView(chip)
                    doneLoading()
                }
            }, {
                Log.e(TAG, it.message)
            })
        }
    }

    private fun doneLoading(){
        done--
        if(done == 0) {
            (progress_bar.parent as ViewGroup).removeView(progress_bar)
            if(showFabEndedLoading) edit_fab.show()
        }
    }

}