package com.figueroa.geofinalprgoject.user.markers.lists

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.db.FirebaseDB
import com.figueroa.geofinalprgoject.models.Models
import com.figueroa.geofinalprgoject.user.markers.lists.adapters.GeoMarkerViewHolder
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlin.properties.Delegates

class SavedGeoMarkersActivity : AppCompatActivity() {

    private val db = FirebaseDB()
    private lateinit var userId: String
    private lateinit var userDocumentId: String

    private var shouldStartListening by Delegates.observable(false) { _, _, _ -> checkAdapter()}

    private fun checkAdapter() {
        if(this::adapter.isInitialized) adapter.startListening()
    }

    private lateinit var adapter: FirestoreRecyclerAdapter<Models.GeoMarker, GeoMarkerViewHolder>

    private lateinit var markersListRecyclerView: RecyclerView
    private var checkItems: Boolean? by Delegates.observable(null) { _, _, _ -> updateUI() }

    private lateinit var loadingScreen: RelativeLayout
    private lateinit var emptyTextView: TextView

    private var selectedMarkers: List<String> by Delegates.observable(listOf()) { _, _, _ -> updateUI() }
    private lateinit var deleteFab: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_geo_markers)

        userId = intent.getStringExtra("userId") ?: ""
        if (userId.isBlank()) {
            Toast.makeText(
                applicationContext,
                "There was an error trying to load the markers.",
                Toast.LENGTH_SHORT
            ).show()
            this.finish()
        }

        loadingScreen = findViewById(R.id.saved_markers_loading)
        emptyTextView = findViewById(R.id.saved_markers_empty)

        db.getUserByUid(userId,
            onSuccess = { id, user ->
                if (user.geoMarkers != null && user.geoMarkers!!.isNotEmpty()) {
                    userDocumentId = id
                    setUpRecyclerView(user)
                }
                else {
                    emptyTextView.visibility = View.VISIBLE
                    loadingScreen.visibility = View.GONE
                }
            },
            onFailure = {
                Toast.makeText(
                    applicationContext,
                    "There was an error trying to load the markers.",
                    Toast.LENGTH_SHORT
                ).show()
                this.finish()
            }
        )

        deleteFab = findViewById(R.id.saved_markers_fab)
        deleteFab.setOnClickListener {
            val alert = AlertDialog.Builder(this)
            alert.setTitle("Delete markers")
            alert.setMessage("Are you sure you want to delete ${if (selectedMarkers.size > 1) "these markers" else "this marker"}?")
            alert.setNegativeButton("No I'm not, go back!", null)
            alert.setPositiveButton("Yes, I am!") { _, _ ->
                db.removeGeoMarkerFromSaved(userDocumentId, selectedMarkers,
                    onSuccess = { deletedIds ->
                        selectedMarkers = selectedMarkers - deletedIds
                        Toast.makeText(
                            applicationContext,
                            "Successfully deleted", Toast.LENGTH_SHORT
                        ).show()

                        finish()
                        overridePendingTransition(0, 0);
                        startActivity(intent)
                        overridePendingTransition(0, 0);
                    }, onFailure = {
                        Toast.makeText(
                            applicationContext,
                            "Could not delete marker(s)", Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            val dialog = alert.create()
            dialog.show()
        }

    }

    override fun onStop() {
        super.onStop()
        if (this::adapter.isInitialized) adapter.stopListening()
    }

    private fun clearAllChecks() {
        markersListRecyclerView.children.forEach { view ->
            if (view is MaterialCardView) view.isChecked = false
        }
    }

    private fun setUpRecyclerView(user: Models.User) {
        markersListRecyclerView = findViewById(R.id.saved_markers_recycler_view)
        markersListRecyclerView.layoutManager = LinearLayoutManager(applicationContext)

        val query = db.getSavedGeoMarkersQuery(user.geoMarkers!!)
        val options: FirestoreRecyclerOptions<Models.GeoMarker> =
            FirestoreRecyclerOptions.Builder<Models.GeoMarker>()
                .setQuery(query, Models.GeoMarker::class.java)
                .build()

        adapter =
            object : FirestoreRecyclerAdapter<Models.GeoMarker, GeoMarkerViewHolder>(options) {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): GeoMarkerViewHolder {
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
                    holder.setOnLongClickListener {
                        val id = snapshots.getSnapshot(position).id
                        selectedMarkers = if (selectedMarkers.contains(id))
                            selectedMarkers - id
                        else
                            selectedMarkers + id
                    }
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
        shouldStartListening = true
    }

    private fun updateUI() {
        if (adapter.itemCount <= 0)
            emptyTextView.visibility = View.VISIBLE
        else
            emptyTextView.visibility = View.GONE

        loadingScreen.visibility = View.GONE

        if (selectedMarkers.isNotEmpty()) {
            deleteFab.show()
        } else {
            deleteFab.hide()
        }
    }

}