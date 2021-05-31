package com.figueroa.geofinalprgoject.user.markers.lists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.db.FirebaseDB
import com.figueroa.geofinalprgoject.models.Models
import com.figueroa.geofinalprgoject.user.markers.lists.adapters.GeoMarkerViewHolder
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlin.properties.Delegates

class SavedGeoMarkersActivity : AppCompatActivity() {

    // DB stuff
    private val db = FirebaseDB()
    private lateinit var userId: String
    private lateinit var userDocumentId: String

    // When should the adapter start listening
    private var shouldStartListening by Delegates.observable(false) { _, _, _ -> checkAdapter() }

    // The adapter for the recycler view
    private lateinit var adapter: FirestoreRecyclerAdapter<Models.GeoMarker, GeoMarkerViewHolder>

    // The recycler view
    private lateinit var markersListRecyclerView: RecyclerView

    // Observed variable that will trigger a function when it changes its value
    private var checkItems: Boolean? by Delegates.observable(null) { _, _, _ -> updateUI() }

    // Other views
    private lateinit var loadingScreen: RelativeLayout
    private lateinit var emptyTextView: TextView

    private var selectedMarkers: List<String> by Delegates.observable(listOf()) { _, _, _ -> updateUI() }
    private lateinit var deleteFab: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_geo_markers)

        // Checking if there is any user logged in
        userId = intent.getStringExtra("userId") ?: ""
        if (userId.isBlank()) {
            // If not, we finish the activity
            Toast.makeText(
                applicationContext,
                "There was an error trying to load the markers.",
                Toast.LENGTH_SHORT
            ).show()
            this.finish()
        }

        // Initialising some views
        loadingScreen = findViewById(R.id.saved_markers_loading)
        emptyTextView = findViewById(R.id.saved_markers_empty)

        // Get the user's saved geomarkers
        db.getUserByUid(userId,
            onSuccess = { id, user ->
                if (user.geoMarkers != null && user.geoMarkers!!.isNotEmpty()) {
                    userDocumentId = id
                    setUpRecyclerView(user)
                } else {
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

        // Adding a listener to the delete marker button
        deleteFab = findViewById(R.id.saved_markers_fab)
        deleteFab.setOnClickListener {

            // Creating an alert to ensure that the user want delete the marker
            val alert = AlertDialog.Builder(this)
            alert.setTitle("Delete markers")
            alert.setMessage("Are you sure you want to delete ${if (selectedMarkers.size > 1) "these markers" else "this marker"}?")
            alert.setNegativeButton("No I'm not, go back!", null)

            // If the user confirms, we delete the marker
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
        // If the adapter as initialised, stop listening to updates from the DB
        if (this::adapter.isInitialized) adapter.stopListening()
    }

    /**
     * Sets up the recycler view given a user.
     *
     * @param   user    The user from which to retrieve the markers
     */
    private fun setUpRecyclerView(user: Models.User) {
        // Initialising the recycler view
        markersListRecyclerView = findViewById(R.id.saved_markers_recycler_view)
        markersListRecyclerView.layoutManager = LinearLayoutManager(applicationContext)

        // The query that will retrieve the data from firebase
        val query = db.getSavedGeoMarkersQuery(user.geoMarkers!!)
        val options: FirestoreRecyclerOptions<Models.GeoMarker> =
            FirestoreRecyclerOptions.Builder<Models.GeoMarker>()
                .setQuery(query, Models.GeoMarker::class.java)
                .build()

        // Creating the Firestore adapter
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

                // For each object we retrieve, create a cardview
                override fun onBindViewHolder(
                    holder: GeoMarkerViewHolder,
                    position: Int,
                    model: Models.GeoMarker
                ) {
                    val drawable = if (model.type == "WARNING")
                        R.drawable.ic_warning else R.drawable.ic_interest

                    // Creating the cards
                    holder.setMarkerCard(
                        drawable = drawable,
                        title = model.title!!,
                        description = model.description!!
                    )

                    // Setting listeners to the cards
                    holder.setListener(model, supportFragmentManager)
                    holder.setOnLongClickListener {
                        val id = snapshots.getSnapshot(position).id
                        selectedMarkers = if (selectedMarkers.contains(id))
                            selectedMarkers - id
                        else
                            selectedMarkers + id
                    }
                }

                // When anything changes in the dataset, notify
                // it so the registered listeners can know
                override fun onDataChanged() {
                    super.onDataChanged()
                    notifyDataSetChanged()
                }
            }

        // Registering a listener to know when an item in the dataset is changed
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkItems = true
            }
        })

        markersListRecyclerView.adapter = adapter
        shouldStartListening = true
    }

    /**
     * If the adapter is initialised, start listening to the DB
     */
    private fun checkAdapter() {
        if (this::adapter.isInitialized) adapter.startListening()
    }

    /**
     * Updates the views
     */
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