package it.unito.lamba.projector.data

import com.google.firebase.database.Exclude

data class Project(@get:Exclude @set:Exclude var id: String? = null,
                   var title: String? = null,
                   var description: String? = null,
                   var owners: HashMap<String, Boolean>? = null,
                   var images: HashMap<String, Boolean>? = null,
                   var year: String? = null,
                   var repos: HashMap<String, Boolean>? = null,
                   var documents: HashMap<String, Boolean>? = null)