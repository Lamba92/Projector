package it.unito.lamba.projector.data

import com.google.firebase.database.Exclude

data class User(@get:Exclude @set:Exclude var uid: String = "", var name: String? = null, var surname: String? = null,
                @get:Exclude @set:Exclude var isAdmin: Boolean = false, var imgUri: String? = null, var email: String? = null,
                var projectId: String? = null, var badgeNumber: String? = null){

    @Exclude
    fun getDisplayName(): String {
        return "$name $surname"
    }
}