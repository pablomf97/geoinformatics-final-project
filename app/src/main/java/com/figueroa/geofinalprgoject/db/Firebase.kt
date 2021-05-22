package com.figueroa.geofinalprgoject.db

import android.content.Context
import android.util.Log
import com.figueroa.geofinalprgoject.models.Models
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseDB {
    private var db: FirebaseFirestore = Firebase.firestore
    private var tag = "FIRESTORE"

    fun getUserGeoMarkers(
        userId: String,
        onSuccess: (List<Models.GeoMarker>) -> Unit,
        onFailure: (Exception?) -> Unit) {
        db.collection("geo-markers").whereEqualTo("uid", userId)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val documents = snapshot.documents
                    onSuccess(documents as List<Models.GeoMarker>)
                } else onFailure(java.lang.Exception("More than one user was returned."))

            }.addOnFailureListener {
                onFailure(java.lang.Exception("More than one user was returned."))
            }
    }

    fun getUserGeoMarkersQuery(userId: String): Query {
        return db.collection("geo-markers").whereEqualTo("uid", userId)
            .orderBy("createdOn", Query.Direction.DESCENDING)
    }

    /**
     * Creates a geo-marker object in the database. Returns true
     * if the operation was completed successfully, false otherwise.
     *
     * @param geoMarker     The geo-marker to save.
     */
    fun createGeoMarker(geoMarker: Models.GeoMarker,
                        onSuccess: (DocumentReference) -> Unit,
                        onFailure: (Exception?) -> Unit) {
        db.collection("geo-markers")
            .add(geoMarker)
            .addOnSuccessListener { documentReference ->
                onSuccess(documentReference)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }

    }
}

class FirebaseAuth(appContext: Context) {
    private var auth: FirebaseAuth
    private val tag = "AUTH"

    init {
        FirebaseApp.initializeApp(appContext)
        auth = Firebase.auth
    }

    /**
     * Returns the currently logged in user.
     *
     * @return  FirebaseUser:   The user that is currently logged in. Can
     *                          be null there is no user logged in.
     */
    fun currentUser(): FirebaseUser? = auth.currentUser

    fun logout() = auth.signOut()

    /**
     * Creates an user in the database.
     *
     * @param   email       The email of the user to create.
     * @param   password    The password of the user to create.
     * @return  If the operation finished successfully, it will return
     *          the created user and it will directly log in the newly
     *          created user. It will return null otherwise.
     */
    fun createUser(email: String, password: String,
                   onSuccess: (Models.User) -> Unit,
                   onFailure: (Exception?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(tag, "Firebase.kt::FirebaseAuth::createUser " +
                            "-> Successfully created user.")
                    val currUser = currentUser()
                    if (currUser != null) {
                        val user = Models.User(currUser, listOf())

                        Firebase.firestore.collection("users")
                            .add(user).addOnSuccessListener { onSuccess(user) }
                            .addOnFailureListener { e -> onFailure(e) }

                    } else onFailure(task.exception)
                } else {
                    Log.d(tag, "Firebase.kt::FirebaseAuth::createUser " +
                            "-> Could not create user.")
                    onFailure(task.exception)
                }
            }
    }

    /**
     * Logs in an existing user.
     *
     * @param   email       The email of the user.
     * @param   password    The password of the user.
     * @return  If the operation finished successfully, it will return
     *          the logged user. It will return null otherwise.
     */
    fun signInUser(email: String, password: String,
                   onSuccess: (Models.User) -> Unit,
                   onFailure: (Exception?) -> Unit) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(tag, "Firebase.kt::FirebaseAuth::signInUser " +
                            "-> Successfully logged in user.")
                    val currUser = currentUser()
                    if (currUser != null) {
                        val user = Models.User(currUser, listOf())
                        onSuccess(user)
                    } else onFailure(task.exception)
                } else {
                    Log.d(tag, "Firebase.kt::FirebaseAuth::signInUser " +
                            "-> Could not create user.")
                    onFailure(task.exception)
                }
            }
    }
}