package com.figueroa.geofinalprgoject.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.figueroa.geofinalprgoject.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import java.util.regex.Pattern

fun isValidEmail(email: String): Boolean {
    return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun isValidLoginPassword(pass: String): Boolean = pass.trim().length >= 6

fun isValidPassword(pass: String): Pair<Boolean, Int> {
    var valid = false
    var strength = 0

    // Password should be minimum minimum 5 characters long
    if (pass.trim().length >= 6) {
        strength += 1
        valid = true
    }
    // If the password contains at least one number
    var exp = ".*[0-9].*"
    var pattern = Pattern.compile(exp, Pattern.CASE_INSENSITIVE)
    var matcher = pattern.matcher(pass)
    if (matcher.matches()) {
        strength += 1
    }

    // If the password contains at least one capital letter
    exp = ".*[A-Z].*"
    pattern = Pattern.compile(exp)
    matcher = pattern.matcher(pass)
    if (matcher.matches()) {
        strength += 1
    }

    // If the password contains at least one small letter
    exp = ".*[a-z].*"
    pattern = Pattern.compile(exp)
    matcher = pattern.matcher(pass)
    if (matcher.matches()) {
        strength += 1
    }

    // If the password contains at least one special character
    // Allowed special characters : "~!@#$%^&*()-_=+|/,."';:{}[]<>?"
    exp = ".*[~!@#\$%\\^&*()\\-_=+\\|\\[{\\]};:'\",<.>/?].*"
    pattern = Pattern.compile(exp)
    matcher = pattern.matcher(pass)
    if (matcher.matches()) {
        strength += 1
    }

    return Pair(valid, strength)
}

fun hideKeyboard(activity: Activity) {
    val imm: InputMethodManager =
        activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = activity.currentFocus
    if (view == null) {
        view = View(activity)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun getBitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    // Get the drawable.
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
    DrawableCompat.setTint(vectorDrawable!!, context.resources.getColor(R.color.primaryColor))

    // Setting bounds to our vector drawable.
    vectorDrawable.setBounds(
        0,
        0,
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight
    )

    // Creating a bitmap for our drawable.
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )

    // Adding bitmap in our canvas.
    val canvas = Canvas(bitmap)

    // Adding the vector drawable to the canvas.
    vectorDrawable.draw(canvas)

    //  Returning our bitmap.
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun calculateDistance(from: LatLng, to: LatLng): Float {
    val latTo = to.latitude
    val lngTo = to.longitude

    val latFrom = from.latitude
    val lngFrom = from.longitude

    val locationFrom = Location("")
    locationFrom.latitude = latFrom
    locationFrom.longitude = lngFrom

    val locationTo = Location("")
    locationTo.latitude = latTo
    locationTo.longitude = lngTo

    return locationFrom.distanceTo(locationTo)
}
