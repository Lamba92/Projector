package it.unito.lamba.projector.fragments

import android.os.Bundle
import android.support.v4.app.Fragment

object CacheableFragmentsTagRegister {

    val TAG = "____TAG____"
    private val cacheableFragments = HashMap<Class<out Fragment>, Boolean>()

    init {
        cacheableFragments[ProjectorFragment::class.java] = true
    }

    fun isCacheable(clazz: Class<out Fragment>): Boolean {
        val b = cacheableFragments[clazz]
        return b != null && b
    }

    fun getCacheTag(clazz: Class<out Fragment>, bundle: Bundle?): String? {
        return if (bundle?.getString(TAG) != null)
                    clazz.simpleName + bundle.getString(TAG)!!
               else null
    }
}