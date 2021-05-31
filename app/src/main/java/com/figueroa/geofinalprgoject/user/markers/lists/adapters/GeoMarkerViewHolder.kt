package com.figueroa.geofinalprgoject.user.markers.lists.adapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.models.Models
import com.figueroa.geofinalprgoject.user.markers.details.MarkerDetailsBottomSheet
import com.google.android.material.card.MaterialCardView

// This is the view holder for the recycler view
class GeoMarkerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val card: MaterialCardView = view.findViewById(R.id.markers_list_recycler_view_item)
    private val markerType: ImageView = view.findViewById(R.id.list_item_marker_type)
    private val markerTitle: TextView = view.findViewById(R.id.list_item_title)
    private val markerDescription: TextView = view.findViewById(R.id.list_item_description)

    fun setMarkerCard(drawable: Int, title: String, description: String) {
        markerType.setImageResource(drawable)
        markerTitle.text = title
        markerDescription.text = description
    }

    fun setListener(marker: Models.GeoMarker, supportFragmentManager: FragmentManager) {
        card.setOnClickListener {
            MarkerDetailsBottomSheet(marker)
                .show(supportFragmentManager, "Details")
        }
    }

    fun setOnLongClickListener(onClick: () -> Unit) {
        card.setOnLongClickListener cardListener@{
            card.isChecked = !card.isChecked
            onClick()
            return@cardListener true
        }
    }
}