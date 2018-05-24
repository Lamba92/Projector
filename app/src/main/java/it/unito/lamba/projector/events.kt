package it.unito.lamba.projector

import android.net.Uri
import android.widget.AdapterView
import it.unito.lamba.projector.fragments.ProjectorFragment
import it.unito.lamba.projector.model.ProjectsAdapter
import java.io.File

class SelectImageEvent(val multiple: Boolean = false, val fragmentClass: Class<ProjectorFragment>)

class BindSpinnerEvent(val onItemSelectedListener: AdapterView.OnItemSelectedListener)

class ChangeToolbarElevationEvent(val elevation: Float)

class CloseKeyBoardEvent

class FragmentInteractionEvent(val interaction: Interaction){
    abstract class Interaction(var data: String? = null)
    object EditProfile: Interaction()
    object OpenProfile: Interaction()
    object AllProjects: Interaction()
    object OpenProject: Interaction()
    object EditProject: Interaction()
}

class SetProfileImageEvent(val image: Uri)

class SetUpNavViewEvent

class SetProjectImagesEvent(val list: List<File>)

class BindAdapterInAllProjectsEvent(val adapter: ProjectsAdapter)