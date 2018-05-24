package it.unito.lamba.projector

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import com.firebase.ui.auth.AuthUI
import it.unito.lamba.projector.fragments.*
import java.util.*
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.widget.AdapterView
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE
import com.theartofdev.edmodo.cropper.CropImageView
import id.zelory.compressor.Compressor
import it.lamba.utilslibrary.Utils.getImagePathFromInputStreamUri
import it.unito.lamba.projector.model.ProjectsAdapter
import kotlinx.android.synthetic.main.nav_header_main.*
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.net.URI
import kotlin.collections.ArrayList


const val PICK_IMAGE_FRAGMENT_CREATE_PROJECT = 999
const val PICK_IMAGE_FRAGMENT_EDIT_PROFILE = 998
const val RC_SIGN_IN = 997
const val ADMIN_MENU_ID = 123123
const val LOGIN_EXTRA = "LOGIN_EXTRA"


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemSelectedListener {

    private val TAG = "MainActivity"
    private var userProjectID: String? = null
    private var adminFlag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!Database.isLoggedIn()) {
            initiateLogin()
            return
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        Database.checkCurrentUserIntegrity { isComplete ->
            if (!isComplete) {
                launchFragment(EditProfileFragment::class.java, EditProfileFragment.createBundle())
            } else {
                setUpNavigationViewAndGoHome(SetUpNavViewEvent())
            }
        }
    }

    private fun initiateLogin() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(Arrays.asList(
                                AuthUI.IdpConfig.EmailBuilder().build()
                                //,AuthUI.IdpConfig.GoogleBuilder().build()
                                //,AuthUI.IdpConfig.FacebookBuilder().build()
                                //,AuthUI.IdpConfig.TwitterBuilder().build()
                                ))
                        .build(),
                RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PICK_IMAGE_FRAGMENT_CREATE_PROJECT -> {
                if(resultCode == Activity.RESULT_OK && data!!.clipData != null) {
                    val list = ArrayList<File>()
                    for (index in 0 until data.clipData.itemCount step 1) {
                        val file = getImagePathFromInputStreamUri(
                                data.clipData.getItemAt(index).uri, contentResolver)?:
                        File(data.clipData.getItemAt(index).uri.path)
                        list.add(File(compressImage(Uri.fromFile(file))))
                    }
                    EventBus.getDefault().postSticky(SetProjectImagesEvent(list))
                }
            }
            RC_SIGN_IN -> {
                recreate()
            }
            PICK_IMAGE_FRAGMENT_EDIT_PROFILE -> {
                if (resultCode == RESULT_OK && data != null) {
                    CropImage.activity(data.data)
                            .setCropShape(CropImageView.CropShape.OVAL)
                            .setAspectRatio(1,1)
                            .start(this)
                }
            }
            CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == RESULT_OK) {
                    compressImage(result.uri)
                            .apply {
                                EventBus.getDefault().postSticky(
                                        SetProfileImageEvent(
                                                Uri.parse(
                                                        this.toASCIIString()
                                                )
                                        )
                                )
                    }
                }
            }
        }
    }

    private fun compressImage(uri: Uri): URI {

        val sizes = getImageSize(uri)
        if(sizes.x >= sizes.y) {
            sizes.x = 1280
            sizes.y = 720
        } else {
            sizes.y = 1280
            sizes.x = 720
        }

        return Compressor(this)
                .setMaxWidth(sizes.x)
                .setMaxWidth(sizes.y)
                .setQuality(80)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .compressToFile(File(uri.path))
                .toURI()
    }

    private fun getImageSize(uri: Uri): Point{
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(File(uri.path).absolutePath, options)
        return Point(options.outWidth, options.outWidth)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            supportFragmentManager.backStackEntryCount > 1 -> supportFragmentManager.popBackStackImmediate()
            else -> super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_profile -> launchFragment(ProfileFragment::class.java, ProfileFragment.createBundle(Database.getCurrentAuthUserID()))
            R.id.nav_projects -> {
                launchFragment(AllProjectsFragment::class.java)
            }
            R.id.nav_my_project -> {
                if(userProjectID != null) launchFragment(ProjectFragment::class.java, ProjectFragment.createBundle(userProjectID as String))
                else launchFragment(CreateProjectFragment::class.java)
            }
            R.id.nav_logout -> {
                val progress = ProgressDialog(this)
                progress.setTitle(resources.getString(R.string.wait))
                progress.setMessage(resources.getString(R.string.logging_out))
                progress.setCancelable(false) // disable dismiss by tapping outside of the dialog
                progress.show()
                Database.logOut(this, {
                    progress.dismiss()
                    initiateLogin()
                })
            }
            R.id.nav_about -> {
                launchFragment(AboutFragment::class.java)
            }
            ADMIN_MENU_ID -> {
                launchFragment(AdminPanelFragment::class.java)
            }
            R.id.nav_crash -> {
                Crashlytics.getInstance().crash()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setUpToolbar(fragmentClass: Class<out Fragment>) {
        when (fragmentClass) {
            AllProjectsFragment::class.java -> {
                supportActionBar?.setDisplayShowTitleEnabled(false)
                toolbar_spinner.visibility = View.VISIBLE
            }
            else -> {
                supportActionBar?.setDisplayShowTitleEnabled(true)
                toolbar_spinner.visibility = View.GONE
            }
        }
    }

    private fun launchFragment(fragmentClass: Class<out Fragment>,
                               bundle: Bundle? = null) {
        var f = false
        if (CacheableFragmentsTagRegister.isCacheable(fragmentClass)) {
            val tag = CacheableFragmentsTagRegister
                    .getCacheTag(fragmentClass, bundle)
            if (tag != null && supportFragmentManager
                            .findFragmentByTag(tag) != null) {
                f = true
                supportFragmentManager.popBackStack(tag,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }
        if (!f) {
            var fragment: Fragment? = null
            try {
                fragment = fragmentClass.newInstance()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (bundle != null)
                fragment!!.arguments = bundle
            val transaction = supportFragmentManager.beginTransaction()
            transaction
                    .replace(R.id.main_content, fragment)
                    .addToBackStack(CacheableFragmentsTagRegister
                            .getCacheTag(fragmentClass, bundle))
                    .commitAllowingStateLoss()
        }
        setUpToolbar(fragmentClass)
    }

    @Suppress("unused")
    @Subscribe
    fun selectImages(event: SelectImageEvent) {
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "image/*"

        val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickIntent.type = "image/*"

        val chooserIntent = Intent.createChooser(getIntent, "Select Image")
        if (event.multiple) chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
        pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        when (event.fragmentClass) {
            CreateProjectFragment::class.java -> startActivityForResult(chooserIntent, PICK_IMAGE_FRAGMENT_CREATE_PROJECT)
            EditProfileFragment::class.java -> startActivityForResult(chooserIntent, PICK_IMAGE_FRAGMENT_EDIT_PROFILE)
            EditProjectFragment::class.java -> startActivityForResult(chooserIntent, PICK_IMAGE_FRAGMENT_CREATE_PROJECT)
        }
    }

    @Subscribe
    fun changeToolbarElevation(event: ChangeToolbarElevationEvent) {
        //TODO
    }

    @Subscribe
    fun onFragmentInteraction(event: FragmentInteractionEvent) {
        when(event.interaction){
            FragmentInteractionEvent.EditProfile -> {
                launchFragment(EditProfileFragment::class.java)
            }
            FragmentInteractionEvent.OpenProfile -> {
                launchFragment(ProfileFragment::class.java,
                        ProfileFragment.createBundle(
                                event.interaction.data as String))
            }
            FragmentInteractionEvent.AllProjects -> {
                launchFragment(AllProjectsFragment::class.java)
            }
            FragmentInteractionEvent.OpenProject -> {
                launchFragment(ProjectFragment::class.java,
                        ProjectFragment.createBundle(
                                event.interaction.data as String))
            }
            FragmentInteractionEvent.EditProject -> {
                launchFragment(EditProjectFragment::class.java)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun setUpNavigationViewAndGoHome(event: SetUpNavViewEvent) {
        val toggle = object : ActionBarDrawerToggle(this, drawer_layout,
                toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                EventBus.getDefault().post(CloseKeyBoardEvent())
                super.onDrawerSlide(drawerView, slideOffset)
            }

        }
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        launchFragment(AllProjectsFragment::class.java)
        Database.getCurrentUser({
            navigation_display_name.text = it.getDisplayName()
            navigation_email.text = it.email
            if (it.imgUri != null) {
                Database.loadUserProfilePictureToImageView(it,
                        this, navigation_profile_image,
                        {}, {it.printStackTrace()})
            }
            if(it.projectId != null){
                nav_view.menu.getItem(2)
                        .setIcon(R.drawable.ic_folder_shared_black_24dp)
                nav_view.menu.getItem(2)
                        .title = getString(R.string.my_project)
                userProjectID = it.projectId
            } else {
                nav_view.menu.getItem(2)
                        .setIcon(R.drawable.ic_add_black_24dp)
                nav_view.menu.getItem(2).title = getString(R.string.create_project)
                userProjectID = null
            }
            if(it.isAdmin && !adminFlag){
                nav_view.menu.add(R.id.secondary_menu, ADMIN_MENU_ID,
                        0, resources.getString(R.string.admin_menu))
                        .setIcon(R.drawable.ic_supervisor_account_black_24dp)
                adminFlag = true
            }
        }, true)
        Database.getYears {
            if(it!=null){
                ArrayAdapter<String>(this@MainActivity,
                        R.layout.simple_white_spinner_item, it).apply {
                    this.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item)
                    toolbar_spinner.adapter = this
                    toolbar_spinner.onItemSelectedListener = this@MainActivity
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val year = (view as TextView).text.toString()
        val adapter = ProjectsAdapter(Database.buildProjectsUiOptions(year), this)
        EventBus.getDefault().removeStickyEvent(BindAdapterInAllProjectsEvent::class.java)
        EventBus.getDefault().postSticky(BindAdapterInAllProjectsEvent(adapter))
    }


}

