package com.figueroa.geofinalprgoject.user.markers

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.models.Models
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MarkerDetailsBottomSheet(private val marker: Models.GeoMarker): BottomSheetDialogFragment() {

    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var latLngTextView: TextView
    private lateinit var typeImageView: ImageView
    private lateinit var typeTextView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.marker_details_sheet, container, false)

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

        return view
    }

}