package it.unito.lamba.projector

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import it.lamba.utilslibrary.Utils.createTemporalFile
import it.lamba.utilslibrary.Utils.encodeEmail
import it.unito.lamba.projector.data.Project
import it.unito.lamba.projector.data.User
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Suppress("FunctionName")
object Database {

    const val USERS = "users"
    const val TAG = "Database"
    const val PROFILE_PICTURES_PATH = "profiles"
    const val ADD_PROJECT_REF = "add_projects"
    const val PROJECT_IMAGES_PATH = "project/images"
    const val YEARS_INDEXED = "indexes/projects-by-years"
    const val PROJECTS = "projects"

    private val db = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val hookMap = HashMap<DatabaseReference, ValueEventListener>()
    private val storage = FirebaseStorage.getInstance()

    fun getCurrentUser(onResult: (user: User) -> Unit, hook: Boolean = false){
        getCurrentAuthUserID().apply {
            val userRef = db.reference.child("$USERS/$this")
            val valueEventListener = object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    p0.apply {
                        getValue(User::class.java)?.apply {
                            uid = p0.key!!
                            isUserAdmin(this.uid) {
                                this.isAdmin = it
                                onResult(this)
                            }
                        }?:onResult(User())
                    }
                }
            }
            if(hook){
                hookMap[userRef] = userRef.addValueEventListener(valueEventListener)
            } else userRef.addListenerForSingleValueEvent(valueEventListener)
        }
    }

    fun getCurrentAuthUserID(): String {
        return auth.currentUser!!.uid
    }


    fun getUserByUid(uid: String, onResult: (user: User?) -> Unit, onFailure: (e: Exception) -> Unit){
        db.reference.child(USERS).child(uid).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                p0.toException()?.let { onFailure(it) }
            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.getValue(User::class.java)?.apply {
                    this.apply{
                        this.uid = p0.key!!
                        isUserAdmin(this.uid) {
                            this.isAdmin = it
                            onResult(this)
                        }
                    }
                } == null) onResult(null)
            }
        })
    }

    fun getUserByEmail(email: String, onResult: (user: User?) -> Unit, onFailure: (e: Exception) -> Unit){
        lookupEmails(
                ArrayList<String>().apply {
            add(email)
        }, {
            if(!it.isEmpty()){
                it[0].apply {
                    getUserByUid(this, onResult, onFailure)
                }
            } else onResult(null)
        })
    }

    fun getYears(onResult: (years: Array<String>?) -> Unit){
        db.reference.child(YEARS_INDEXED).apply {
            hookMap[this] = addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    Log.e(TAG, p0.toString())
                }

                override fun onDataChange(p0: DataSnapshot) {
                    p0.let {
                        val array = it.getValue(object : GenericTypeIndicator<HashMap<String, HashMap<String, Boolean>>>(){})?.keys?.toTypedArray()
                        onResult(array)
                    }
                }
            })
        }
    }

    fun isLoggedIn(): Boolean{
        return auth.currentUser != null
    }

    fun isUserAdmin(user: User, onResult: (result: Boolean) -> Unit){
        isUserAdmin(user.uid, onResult)
    }

    fun isUserAdmin(userId: String, onResult: (result: Boolean) -> Unit) {
        db.reference.child("indexes/admins/$userId").addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.getValue(Boolean::class.java)?.apply{
                    onResult(this)
                } == null) onResult(false)
            }
        })
    }

    fun checkCurrentUserIntegrity(onResult: (isComplete: Boolean) -> Unit) {
        getCurrentUser ({ user ->
            if(user.name == null || user.surname == null ||
               user.name == ""   || user.surname == "") onResult(false)
            else if(!user.isAdmin && user.badgeNumber == null || user.badgeNumber == "") onResult(false)
            else onResult(true)
        })
    }

    fun updateCurrentUserProfilePicture(image: Uri, onSuccess: () -> Unit, onFailure: (e: Exception) -> Unit){
        val uid: String = getCurrentAuthUserID()
        val imgName = UUID.randomUUID().toString()
        FirebaseStorage
                .getInstance().reference
                .child(PROFILE_PICTURES_PATH).child(imgName)
                .putFile(image)
                .addOnSuccessListener {
                    db.getReference(USERS)
                            .child(uid).child("imgUri")
                            .setValue("profiles/$imgName")
                            .addOnSuccessListener {
                                onSuccess()
                            }
                            .addOnFailureListener {
                                onFailure(it)
                            }
                }.addOnFailureListener {
                    onFailure(it)
                }

    }

    fun loadUserProfilePictureToImageView(user: User, context: Context, view: ImageView,
                                          onSuccess: () -> Unit = {}, onFailure: (e: Exception) -> Unit) =
        db.getReference(USERS).child(user.uid).child("imgUri")
                .addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                _loadUserProfilePictureToImageView(p0, context, view, onSuccess, onFailure)
            }

        })


    private fun _loadUserProfilePictureToImageView(doc: DataSnapshot?, context: Context, view: ImageView,
                                                   onSuccess: () -> Unit, onFailure: (e: Exception) -> Unit){
        doc?.getValue(String::class.java)?.apply {
                FirebaseStorage
                        .getInstance()
                        .reference
                        .child(this)
                        .apply {
                            GlideApp.with(context).load(this)
                                    .listener(object : RequestListener<Drawable> {
                                        override fun onLoadFailed(e: GlideException?, model: Any?,
                                                                  target: Target<Drawable>?,
                                                                  isFirstResource: Boolean): Boolean {
                                            if (e != null) {
                                                onFailure(e)
                                            }
                                            return false
                                        }

                                        override fun onResourceReady(resource: Drawable?, model: Any?,
                                                                     target: Target<Drawable>?,
                                                                     dataSource: DataSource?,
                                                                     isFirstResource: Boolean): Boolean {
                                            onSuccess()
                                            return false
                                        }

                                    })
                                    .into(view)
                        }
        }

    }

    fun updateCurrentUserProfile(user: User, onSuccess: () -> Unit, onFailure: (e: Exception) -> Unit) {
        db.getReference(USERS).child(auth.currentUser!!.uid).setValue(user)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener {
                    onFailure(it)
                }
    }

    fun logOut(context: Context, onResult: () -> Unit) {
        for((key, value) in hookMap){
            key.removeEventListener(value)
        }
        AuthUI.getInstance()
                .signOut(context)
                .addOnCompleteListener {
                    onResult()
                }
    }

    private fun lookupEmails(emails: List<String>, onResult: (uids: List<String>) -> Unit) {
        val ref = db.reference.child("indexes/email-uid")
        val uids = ArrayList<String>()
        var i = 0
        if(emails.isEmpty()) onResult(uids)
        else for(email in emails){
            ref.child(encodeEmail(email)).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(p0: DataSnapshot) {
                    p0.getValue(String::class.java)?.apply {
                        uids.add(this)
                        i++
                    }
                    if(i == emails.size) onResult(uids)
                }

                override fun onCancelled(p0: DatabaseError) {
                    Log.e(TAG, p0.toString())
                }

            })
        }
    }

    private fun uploadImages(files: List<File>, onResult: (paths: List<String>) -> Unit){
        val paths = ArrayList<String>()
        var i = 0
        if(files.isEmpty()) onResult(paths)
        else for(file in files) {
            val imgName = UUID.randomUUID().toString()
            storage.reference
                    .child(PROJECT_IMAGES_PATH).child(imgName)
                    .putFile(Uri.fromFile(file)).addOnSuccessListener {
                            paths.add(imgName)
                            i++
                            if(i == files.size) onResult(paths)
                        }
        }
    }

    fun addProject(title: String, description: String, uids: List<String>, images: List<File>,
                   onSuccess: () -> Unit, onFailure: (e: Exception) -> Unit){
        uploadImages(images, {
            db.reference
                    .child(ADD_PROJECT_REF).child(getCurrentAuthUserID())
                    .setValue(Project(null, title, description, buildMap(ArrayList<String>().apply {
                        if(!this.contains(getCurrentAuthUserID())) add(getCurrentAuthUserID())
                        addAll(uids)
                    }), buildMap(it)))
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener {
                        onFailure(it)
                    }

        })
    }

    fun getProject(projectId: String, onResult: (project: Project) -> Unit, hook: Boolean = false): DatabaseReference {
        return db.reference.child(PROJECTS).child(projectId).apply {
            val listener = object: ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    p0.apply {
                        val prj = this.getValue(Project::class.java)!!
                        prj.id = this.key
                        onResult(prj)
                    }
                }
            }
            if(hook){
                hookMap[this] = listener
                addValueEventListener(listener)
            } else addListenerForSingleValueEvent(listener)
        }
    }

    fun getFile(path: String, onResult:(file: File) -> Unit){
        val tmpFile = createTemporalFile()
        storage.reference.child(path).getFile(tmpFile).addOnSuccessListener {
            onResult(tmpFile)
        }.addOnFailureListener {
            Log.e(TAG, it.message)
        }
    }

    fun buildProjectsUiOptions(year: String): FirebaseRecyclerOptions<Project> {
        val query = db.reference
                .child(PROJECTS)
                .orderByChild("year").equalTo(year)
                .limitToLast(50)
        return FirebaseRecyclerOptions.Builder<Project>()
                .setQuery(query, Project::class.java)
                .build()
    }

    fun buildUsersUiOptions(): FirebaseRecyclerOptions<User>{
        val query = db.reference
                .child(USERS)
                .limitToLast(50)
        return FirebaseRecyclerOptions.Builder<User>()
                .setQuery(query, User::class.java)
                .build()
    }

    private fun buildMap(list: List<String>): HashMap<String, Boolean> {
        return HashMap<String, Boolean>().apply {
            for(item in list){
                this[item] = true
            }
        }
    }

    fun updateProject(previousProject: Project, title: String, description: String, owners: HashMap<String, Boolean>,
                      oldImages: HashMap<String, Boolean>, newImages: ArrayList<File>,
                      documents: HashMap<String, Boolean>, repos: HashMap<String, Boolean>, onComplete: ()->Unit) {
        uploadImages(newImages) {
            db.reference.child(PROJECTS).child(previousProject.id!!).setValue(
                    Project(previousProject.id, title, description, owners, oldImages.apply {
                        it.forEach {
                            this[it] = true
                        }
                    }, previousProject.year, repos, documents)
            ).addOnCompleteListener {
                onComplete()
            }
        }
    }

    fun getTotalUsers(onResult: (result: Int) -> Unit){
        db.reference.child("indexes/total-users").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.getValue(Int::class.java)?.apply {
                    onResult(this)
                }==null) onResult(0)
            }
        })
    }

    fun getTotalProjects(onResult: (result: Int) -> Unit){
        db.reference.child("indexes/total-projects").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.getValue(Int::class.java)?.apply {
                            onResult(this)
                        }==null) onResult(0)
            }
        })
    }
}