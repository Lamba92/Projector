package it.unito.lamba.projector

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.StorageReference
import android.net.Uri
import android.os.Build
import android.support.customtabs.CustomTabsIntent
import android.text.Html
import android.widget.*
import it.lamba.utilslibrary.inflate
import java.io.*

@GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Register FirebaseImageLoader to handle StorageReference
        registry.append(StorageReference::class.java, InputStream::class.java,
                FirebaseImageLoader.Factory())
    }
}

fun LinearLayout.addDependencyView(name: String, link: String) {
    val tv = this.inflate(R.layout.about_item) as TextView
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        tv.text = Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY)
    } else tv.text = Html.fromHtml(name)
    tv.setOnClickListener {
        val builder = CustomTabsIntent.Builder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setToolbarColor(context.resources.getColor(R.color.colorPrimary, context.theme))
        } else builder.setToolbarColor(context.resources.getColor(R.color.colorPrimary))
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(link))
    }
    this.addView(tv)
}