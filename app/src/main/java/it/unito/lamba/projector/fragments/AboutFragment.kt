package it.unito.lamba.projector.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_about.view.*
import android.graphics.Point
import it.lamba.utilslibrary.Utils.DpToPx
import it.lamba.utilslibrary.inflate
import it.unito.lamba.projector.*


class AboutFragment: ProjectorFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container!!.inflate(R.layout.fragment_about).apply {
            val dependencyNames = resources.getStringArray(R.array.dependencyNames)
            val dependencyLinks = resources.getStringArray(R.array.dependencyLinks)
            for(i in 0 until dependencyLinks.size step 1){
                linear_layout.addDependencyView(dependencyNames[i], dependencyLinks[i])
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val size = Point()
        activity!!.windowManager.defaultDisplay.getSize(size)
        val width = size.x - DpToPx(resources, 32f)
        view.kotlin_logo.setImageBitmap(Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.kotlin_logo_wordmark),
                width.toInt(), DpToPx(resources, 90f).toInt(),
                true))
        view.firebase_logo.setImageBitmap(Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.firebase_logo_built_white),
                width.toInt(), DpToPx(resources, 150f).toInt(),
                true))
    }
}
