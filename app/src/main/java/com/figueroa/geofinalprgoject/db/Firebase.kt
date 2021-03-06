package com.figueroa.geofinalprgoject.db

import android.content.Context
import android.util.Log
import com.figueroa.geofinalprgoject.models.Models
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseDB {

    /**
     * Access to the cloud DB.
     */
    private var db: FirebaseFirestore = Firebase.firestore

    /**
     * Gets the user by its unique identifier.
     *
     * @param   id          The id of the user to retrieve
     * @param   onSuccess   Executes the provided function when the operation completed successfully
     * @param   onFailure   Executes the provided function when the operation did no complete successfully
     */
    fun getUserByUid(
        id: String,
        onSuccess: (documentId: String, user: Models.User) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        db.collection("users").whereEqualTo("userId", id).get()
            .addOnSuccessListener { snapshots ->
                when {
                    snapshots.isEmpty -> onFailure(Exception("User not found"))
                    !snapshots.isEmpty -> {
                        val users = snapshots.toObjects(Models.User::class.java)
                        if (users.size > 1)
                            onFailure(Exception("Multiple users found"))
                        else
                            onSuccess(snapshots.documents[0].id, users[0])
                    }
                }
            }.addOnFailureListener { onFailure(it) }
    }

    /**
     * Adds a geomarker to the user's saved geomarkers.
     *
     * @param   userId      The id of the user
     * @param   markerId    The id of the marker to add
     * @param   onSuccess   Executes the provided function when the operation completed successfully
     * @param   onFailure   Executes the provided function when the operation did no complete successfully
     */
    fun addGeoMarkerToUser(
        userId: String, markerId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        db.collection("users").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { snapshot ->
                val id = snapshot.documents[0].id
                db.collection("users").document(id)
                    .update("geoMarkers", FieldValue.arrayUnion(markerId))
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }.addOnFailureListener { onFailure(it) }
    }

    /**
     * Removes a geomarker to the user's saved geomarkers.
     *
     * @param   userId      The id of the user
     * @param   ids         The id(s) of the marker(s) to remove
     * @param   onSuccess   Executes the provided function when the operation completed successfully
     * @param   onFailure   Executes the provided function when the operation did no complete successfully
     */
    fun removeGeoMarkerFromSaved(
        userId: String,
        ids: List<String>,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        when {
            ids.isNotEmpty() -> {
                db.collection("users")
                    .document(userId)
                    .update("geoMarkers", FieldValue.arrayRemove(*ids.toTypedArray()))
                    .addOnSuccessListener { onSuccess(ids) }
                    .addOnFailureListener { onFailure(it) }
            }
            else -> onFailure(Exception("Empty ID list"))
        }
    }

    /**
     * Gets the user by its unique identifier.
     *
     * @param   ids         The id(s) of the geomarker(s) to delete
     * @param   onSuccess   Executes the provided function when the operation completed successfully
     * @param   onFailure   Executes the provided function when the operation did no complete successfully
     */
    fun deleteGeoMarker(
        ids: List<String>, onSuccess: (List<String>) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        when {
            ids.size == 1 -> {
                db.collection("geo-markers")
                    .document(ids[0]).delete()
                    .addOnSuccessListener { onSuccess(ids) }
                    .addOnFailureListener { onFailure(it) }
            }
            ids.size > 1 -> {
                val batch = db.batch()
                ids.forEach { id ->
                    val docRef = db.collection("geo-markers").document(id)
                    batch.delete(docRef)
                }
                batch.commit().addOnSuccessListener { onSuccess(ids) }
                    .addOnFailureListener { onFailure(it) }
            }
            else -> {
                onFailure(Exception("Empty ID list"))
            }
        }
    }

    /**
     * Gets the nearby geomarker from a provided center.
     *
     * @param   center      The coordinates from which to make the calculations
     * @param   onSuccess   Executes the provided function when the operation completed successfully
     * @param   onFailure   Executes the provided function when the operation did no complete successfully
     */
    fun getNearbyMarkers(
        center: LatLng,
        onSuccess: (documents: List<DocumentSnapshot>) -> Unit,
        onFailure: (exception: Exception?) -> Unit
    ) {

        // Approximately 200 meters translated
        // into latitude and longitude
        val lat = 0.0144927536231884 * 0.1242742
        val lng = 0.0181818181818182 * 0.1242742

        // The lower latitude & longitude
        val lowerLat = center.latitude - lat
        val lowerLng = center.longitude - lng

        // The greater latitude & longitude
        val greaterLat = center.latitude + lat
        val greaterLng = center.longitude + lng

        // Converting them into GeoPoints
        val lesserGeopoint = GeoPoint(lowerLat, lowerLng)
        val greaterGeopoint = GeoPoint(greaterLat, greaterLng)

        // Building the query
        val docRef = Firebase.firestore.collection("geo-markers")
        val query = docRef.whereGreaterThan("latLng", lesserGeopoint)
            .whereLessThan("latLng", greaterGeopoint)

        // Requesting to Firebase Firestore
        query.get().addOnSuccessListener { snapshot ->
            onSuccess(snapshot.documents)
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }

    /**
     * Builds the query that retrieves the geomarkers of an specified user.
     *
     * @param   userId  The id of the user
     * @return  The query
     */
    fun getUserGeoMarkersQuery(userId: String): Query {
        return db.collection("geo-markers").whereEqualTo("uid", userId)
            .orderBy("createdOn", Query.Direction.DESCENDING)
    }

    /**
     * Builds the query that retrieves the saved geomarkers of an specified user.
     *
     * @param   ids  The ids of geomarkers to retrieve
     * @return  The query
     */
    fun getSavedGeoMarkersQuery(ids: List<String>): Query {
        return db.collection("geo-markers").whereIn(FieldPath.documentId(), ids)
            .orderBy("createdOn", Query.Direction.DESCENDING)
    }

    /**
     * Creates a geo-marker object in the database. Returns true
     * if the operation was completed successfully, false otherwise.
     *
     * @param geoMarker     The geo-marker to save.
     */
    fun createGeoMarker(
        geoMarker: Models.GeoMarker,
        onSuccess: (DocumentReference) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
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
    fun createUser(
        email: String, password: String,
        onSuccess: (Models.User) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(
                        tag, "Firebase.kt::FirebaseAuth::createUser " +
                                "-> Successfully created user."
                    )
                    val currUser = currentUser()
                    if (currUser != null) {
                        val user = Models.User(currUser, listOf())

                        Firebase.firestore.collection("users")
                            .add(user).addOnSuccessListener { onSuccess(user) }
                            .addOnFailureListener { e -> onFailure(e) }

                    } else onFailure(task.exception)
                } else {
                    Log.d(
                        tag, "Firebase.kt::FirebaseAuth::createUser " +
                                "-> Could not create user."
                    )
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
    fun signInUser(
        email: String, password: String,
        onSuccess: (Models.User) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(
                        tag, "Firebase.kt::FirebaseAuth::signInUser " +
                                "-> Successfully logged in user."
                    )
                    val currUser = currentUser()
                    if (currUser != null) {
                        val user = Models.User(currUser, listOf())
                        onSuccess(user)
                    } else onFailure(task.exception)
                } else {
                    Log.d(
                        tag, "Firebase.kt::FirebaseAuth::signInUser " +
                                "-> Could not create user."
                    )
                    onFailure(task.exception)
                }
            }
    }
}