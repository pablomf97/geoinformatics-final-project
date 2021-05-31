package com.figueroa.geofinalprgoject.user.markers.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat.getColor
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.db.FirebaseDB
import com.figueroa.geofinalprgoject.models.Models
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator

// This is the detailed view of a geomarker
class MarkerDetailsBottomSheet(
    private val marker: Models.GeoMarker,
    private val markerId: String? = null,
    private val userId: String? = null
) : BottomSheetDialogFragment() {

    // Views
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var latLngTextView: TextView
    private lateinit var typeImageView: ImageView
    private lateinit var typeTextView: TextView
    private lateinit var saveButton: CardView

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.marker_details_sheet, container, false)

        // Finding and assigning views
        titleTextView = view.findViewById(R.id.bottom_sheet_title)
        titleTextView.text = marker.title

        descriptionTextView = view.findViewById(R.id.bottom_sheet_description)
        descriptionTextView.text = marker.description

        latLngTextView = view.findViewById(R.id.bottom_sheet_latlng)
        latLngTextView.text = "${marker.latLng?.latitude}, ${marker.latLng?.longitude}"

        typeImageView = view.findViewById(R.id.bottom_sheet_type_image)
        val drawable = if (marker.type == "WARNING") R.drawable.ic_warning
        else R.drawable.ic_interest
        typeImageView.setImageResource(drawable)

        typeTextView = view.findViewById(R.id.bottom_sheet_type_text)
        typeTextView.text = marker.type

        // If the user is logged in, the marker is not already on its list
        // and the type is not warning, show the button to add the marker to its saved markers.
        val shouldShow = (userId != null && markerId != null && marker.uid != userId)
                && marker.type.equals("interest point", true)
        if (shouldShow) {
            saveButton = view.findViewById(R.id.bottom_sheet_save_button) as CardView
            saveButton.visibility = View.VISIBLE
            saveButton.setOnClickListener {
                val loadingProgressBar =
                    saveButton.findViewById<CircularProgressIndicator>(R.id.save_button_progress)
                loadingProgressBar.visibility = View.VISIBLE

                val textView = saveButton.findViewById<TextView>(R.id.save_button_text)
                textView.text = "Saving"

                FirebaseDB().addGeoMarkerToUser(
                    userId = userId!!,
                    markerId = markerId!!,
                    onSuccess = {
                        Toast.makeText(
                            view.context, "Marker saved!",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadingProgressBar.visibility = View.GONE
                        textView.text = "Saved"
                        textView.setTextColor(getColor(resources, R.color.primaryColor, null))

                        saveButton.setCardBackgroundColor(
                            getColor(resources, R.color.primaryLightColor, null)
                        )
                        saveButton.isEnabled = false
                    },
                    onFailure = {
                        Toast.makeText(
                            view.context, it?.message ?: "Error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }

        return view
    }
}