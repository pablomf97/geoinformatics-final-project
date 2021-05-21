package com.figueroa.geofinalprgoject.models

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

class Models {

    /**
     * GeoMarker POJO.
     */
    @IgnoreExtraProperties
    data class GeoMarker(
            var uid: String? = null,
            var title: String? = null,
            var description: String? = null,
            var latLng: GeoPoint? = null,
            var type: String? = null,
            var createdOn: Timestamp? = null
            ) {
        constructor(data: Map<String, Any>): this() {
            this.uid = data["uid"] as String
            this.title = data["title"] as String
            this.description = data["description"] as String
            this.latLng = data["latLng"] as GeoPoint
            this.type = data["type"] as String
            this.createdOn = data["createdOn"] as Timestamp
        }

    }

    data class User(
            var userId: String? = null,
            var userEmail: String? = null,
            var geoMarkers: List<GeoMarker>? = null,
            ) {
        constructor(user: FirebaseUser, geoMarkers: List<GeoMarker>): this() {
            this.userId = user.uid
            this.userEmail = user.email
            this.geoMarkers = geoMarkers
        }
        constructor(data: Map<String, Any>): this() {
            this.userId = data["userId"] as String
            this.userEmail = data["userEmail"] as String
            this.geoMarkers = data["geoMarkers"] as List<GeoMarker>
        }
    }
}