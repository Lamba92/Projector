package it.unito.lamba.projector.fragments

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.robertlevonyan.views.chip.Chip
import it.lamba.imagesadapter.ImagesAdapter
import it.lamba.utilslibrary.Utils.DpToPx
import it.lamba.utilslibrary.Utils.createChipBitmap
import it.lamba.utilslibrary.inflate
import it.unito.lamba.projector.*
import org.greenrobot.eventbus.EventBus
import it.unito.lamba.projector.FragmentInteractionEvent.*
import it.unito.lamba.projector.data.Project
import kotlinx.android.synthetic.main.fragment_edit_project.*
import kotlinx.android.synthetic.main.fragment_edit_project.view.*
import java.io.File
import org.greenrobot.eventbus.Subscribe

class EditProjectFragment: ProjectorFragment() {

    companion object {
        val TAG = "EditProjectFragment"
    }

    private val newOwners = HashMap<String, String>()
    private val oldImagesNamesMap = HashMap<File, String>()
    private lateinit var imagesAdapter: ImagesAdapter
    private lateinit var previousProject: Project

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container!!.inflate(R.layout.fragment_edit_project).apply {
            Database.getCurrentUser({currentUser ->
                if(currentUser.projectId == null){
                    Toast.makeText(context, "Tu non hai un progetto! Come sei arrivato fin qui??", Toast.LENGTH_LONG).show()
                    EventBus.getDefault().post(FragmentInteractionEvent(AllProjects))
                } else {
                    Database.getProject(currentUser.projectId!!, {project ->
                        previousProject = project
                        project_title_textview.text = SpannableStringBuilder(project.title)
                        project_description_textview.text = SpannableStringBuilder(project.description)
                        project.owners!!.keys.forEach { ownerId ->
                            buildChip(ownerId, this)
                        }
                        buildImageAdapter(project.images)
                        addResources(project.documents, project.repos)
                        add_friend_button.setOnClickListener {
                            addChipFromEmail(project_emails_textview.text.toString(), this, project)
                            project_emails_textview.text = SpannableStringBuilder("")
                        }
                    })
                }
            })
            save_button.setOnClickListener {
                saveChanges()
            }
            add_images_button.setOnClickListener {
                EventBus.getDefault().post(SelectImageEvent(true, this@EditProjectFragment.javaClass))
            }
            add_document_button.setOnClickListener {
                document_linear_layout.addView(buildEditText(hintResourceId = R.string.document_link))
            }
            add_repo_button.setOnClickListener {
                repo_linear_layout.addView(buildEditText(hintResourceId = R.string.repo_link))
            }
        }
    }

    @Subscribe(sticky = true)
    fun storeImagesUris(event: SetProjectImagesEvent){
        EventBus.getDefault().removeStickyEvent(SetProjectImagesEvent::class.java)
        this.imagesAdapter.addAll(event.list)
    }

    private fun addChipFromEmail(email: String, v: View, project: Project) {

        if(email == "" || previousProject.owners!!.containsKey(email)) return
        Database.getUserByEmail(email, {
            if(!project.owners!!.keys.contains(it!!.uid)) {
                newOwners[it.uid] = email
                buildChip(it.uid, v, true, {
                    newOwners.remove(it.uid)
                })
            }
        }, {
            Log.e(TAG, it.message)
        })
    }

    private fun saveChanges() {
        val owners = HashMap<String, Boolean>()
        newOwners.keys.forEach {
            owners[it] = true
        }
        previousProject.owners!!.keys.forEach {
            owners[it] = true
        }
        val oldImages = HashMap<String, Boolean>().apply {
            imagesAdapter.getImages(ImagesAdapter.ImagesType.OldImages).forEach {
                this[oldImagesNamesMap[it]!!] = true
            }
        }
        val documents = HashMap<String, Boolean>().apply {
            for(i in 0 until document_linear_layout.childCount step 1){
                if((document_linear_layout.getChildAt(i) as EditText).text.toString() != "")
                    this[(document_linear_layout.getChildAt(i) as EditText).text.toString()] = true
            }
        }
        val repos = HashMap<String, Boolean>().apply {
            for(i in 0 until repo_linear_layout.childCount step 1){
                if((repo_linear_layout.getChildAt(i) as EditText).text.toString() != "")
                    this[(repo_linear_layout.getChildAt(i) as EditText).text.toString()] = true
            }
        }

        Database.updateProject(previousProject, project_title_textview.text.toString(), project_description_textview.text.toString(),
                owners, oldImages, imagesAdapter.getImages(), documents, repos, {
            EventBus.getDefault().post(FragmentInteractionEvent(OpenProject.apply {
                this.data = previousProject.id
            }))
        })
    }

    private fun addResources(documents: HashMap<String, Boolean>?, repos: HashMap<String, Boolean>?) {
        documents?.keys?.forEach {
            document_linear_layout.addView(buildEditText(it))
        }
        repos?.keys?.forEach {
            repo_linear_layout.addView(buildEditText(it))
        }
    }

    private fun buildEditText(text: String? = null, hintResourceId: Int = R.string.link): EditText{
        return EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = DpToPx(resources, 8f).toInt() }
            this.text = SpannableStringBuilder(text?:"")
            hint = resources.getString(hintResourceId)
        }
    }

    private fun buildImageAdapter(images: HashMap<String, Boolean>?) {
        if(images != null){
            imagesAdapter = ImagesAdapter(context!!, true)
            for(imagePath in images.keys){
                Database.getFile("project/images/$imagePath", {
                    oldImagesNamesMap[it] = imagePath
                    imagesAdapter.add(it, false)
                })
            }
            images_rv.adapter = imagesAdapter
        }
    }

    private fun buildChip(ownerId: String, v: View, closable: Boolean = false, onDismiss: () -> Unit = {}) {
        Database.getUserByUid(ownerId, {user ->
            if(true) {
                val chip = Chip(context)
                chip.isClosable = closable
                chip.chipText = user!!.getDisplayName()
                chip.setOnCloseClickListener {
                    v.flowLayout.removeView(chip)
                    onDismiss()
                }
                if (user.imgUri != null) {
                    Database.getFile(user.imgUri!!, {
                        chip.isHasIcon = true
                        chip.setChipIcon(createChipBitmap(it, this.context!!))
                        v.flowLayout.addView(chip)
                    })
                } else {
                    v.flowLayout.addView(chip)
                }
            }
        }, {
            Log.e(TAG, it.message)
        })
    }
}