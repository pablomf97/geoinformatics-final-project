package com.figueroa.geofinalprgoject.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.db.FirebaseDB
import com.figueroa.geofinalprgoject.models.Models
import com.figueroa.geofinalprgoject.user.markers.MarkerDetailsBottomSheet
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import kotlin.properties.Delegates.observable


class GeoMarkerListActivity : AppCompatActivity() {

    private lateinit var userId: String

    private lateinit var adapter: FirestoreRecyclerAdapter<Models.GeoMarker, GeoMarkerViewHolder>

    private lateinit var markersListRecyclerView: RecyclerView
    private var checkItems: Boolean? by observable(null) { _, _, _ -> updateUI() }

    private lateinit var loadingScreen: RelativeLayout
    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_marker_list)

        userId = intent.getStringExtra("userId") ?: ""
        if (userId.isBlank()) {
            Toast.makeText(applicationContext,
                "There was an error trying to load the markers.",
                Toast.LENGTH_SHORT
            ).show()
            this.finish()
        }

        loadingScreen = findViewById(R.id.markers_list_loading)
        emptyTextView = findViewById(R.id.markers_list_empty)

        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        markersListRecyclerView = findViewById(R.id.markers_list_recycler_view)
        markersListRecyclerView.layoutManager = LinearLayoutManager(applicationContext)

        val db = FirebaseDB()
        val query = db.getUserGeoMarkersQuery(userId)
        val options: FirestoreRecyclerOptions<Models.GeoMarker> =
            FirestoreRecyclerOptions.Builder<Models.GeoMarker>()
                .setQuery(query, Models.GeoMarker::class.java)
                .build()

        adapter = object : FirestoreRecyclerAdapter<Models.GeoMarker, GeoMarkerViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeoMarkerViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.markers_list_recycler_view_item, parent, false)
                return GeoMarkerViewHolder(view)
            }

            override fun onBindViewHolder(
                holder: GeoMarkerViewHolder,
                position: Int,
                model: Models.GeoMarker
            ) {
                val drawable = if (model.type == "WARNING")
                    R.drawable.ic_warning else R.drawable.ic_interest

                holder.setMarkerCard(
                    drawable = drawable,
                    title = model.title!!,
                    description = model.description!!
                )

                holder.setListener(model, supportFragmentManager)
            }

            override fun onDataChanged() {
                super.onDataChanged()
                notifyDataSetChanged()
            }
        }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkItems = true
            }
        })

        markersListRecyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    private fun updateUI() {
        if (adapter.itemCount <= 0)
            emptyTextView.visibility = View.VISIBLE
        else
            emptyTextView.visibility = View.GONE

        loadingScreen.visibility = View.GONE
    }

    internal class GeoMarkerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: CardView = view.findViewById(R.id.markers_list_recycler_view_item)
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
    }
}

