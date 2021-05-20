package com.figueroa.geofinalprgoject.models

import android.text.TextUtils
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.type.LatLng

class Models {

    /**
     * GeoMarker POJO.
     */
    @IgnoreExtraProperties
    data class GeoMarker(
            var id: String? = null,
            var userId: String? = null,
            var title: String? = null,
            var description: String? = null,
            var latLng: LatLng? = null,
            var type: GeoType? = null
            ) {
        companion object {
            const val FIELD_USER_ID = "userId"
            const val FIELD_TITLE = "title"
            const val FIELD_DESCRIPTION = "description"
            const val FIELD_LAT_LNG = "latLng"
            const val FIELD_TYPE = "type"
        }
    }

    data class User(
            var userId: String? = null,
            var userEmail: String? = null,
            var userName: String? = null,
            var geoMarkers: List<GeoMarker>? = null,
            ) {
        constructor(user: FirebaseUser, geoMarkers: List<GeoMarker>): this() {
            this.userId = user.uid
            this.userEmail = user.email
            if (TextUtils.isEmpty(this.userName)) {
                this.userName = user.email
            }

            this.geoMarkers = geoMarkers
        }
    }

    enum class GeoType {
        INTEREST_POINT, WARNING,
    }
}