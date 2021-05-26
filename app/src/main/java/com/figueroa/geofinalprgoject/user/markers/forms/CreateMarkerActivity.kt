package com.figueroa.geofinalprgoject.user.markers.forms

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.db.FirebaseDB
import com.figueroa.geofinalprgoject.models.Models
import com.figueroa.geofinalprgoject.utils.hideKeyboard
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.*
import kotlin.properties.Delegates.observable

class CreateMarkerActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var userId: String
    private lateinit var latLng: GeoPoint

    // TextInputLayout to give the
    // app a more material design look
    private lateinit var formTypeLayout: TextInputLayout
    private lateinit var formNameLayout: TextInputLayout
    private lateinit var formDescriptionLayout: TextInputLayout

    // Checking if the form is ready to commit
    // everytime these values change
    private var isTypeReady: Boolean by observable(false) { _, _, _ -> checkForm() }
    private var isNameReady: Boolean by observable(false) { _, _, _ -> checkForm() }
    private var isDescriptionReady: Boolean by observable(false) { _, _, _ -> checkForm() }

    // Submit button
    private lateinit var formSubmitButton: Button

    // Showed when the app is sending
    // the request to Firebase
    private lateinit var loadingLayout: RelativeLayout
    private var isLoading: Boolean by observable(false) { _, _, _ -> showHideLoadScreen() }

    private fun showHideLoadScreen() {
        if (isLoading)
            loadingLayout.visibility = View.VISIBLE
        else
            loadingLayout.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_marker)
        supportActionBar?.hide()

        userId = intent.getStringExtra("userId") ?: ""

        // -32.3291159,-58.0608368 -> Middle of the ocean
        val lat = intent.getDoubleExtra("lat", -32.3291159)
        val lng = intent.getDoubleExtra("lng", -58.0608368)

        Log.w("CREATE", lat.toString() + lng.toString())
        latLng = GeoPoint(lat, lng)
        if (userId.isBlank() || latLng == GeoPoint(-32.3291159, -58.0608368)) {
            Toast.makeText(
                applicationContext,
                "Oops! Something went wrong...", Toast.LENGTH_SHORT
            ).show()
            this.finish()
        }

        initComponents()
    }

    private fun initComponents() {
        formTypeLayout = findViewById(R.id.form_marker_type)

        val items = listOf("Warning", "Interest point")
        val adapter = ArrayAdapter(applicationContext, R.layout.marker_type_item, items)
        val autoComplete = formTypeLayout.editText as? AutoCompleteTextView
        autoComplete?.setAdapter(adapter)
        autoComplete?.setOnItemClickListener { _, _, _, _ -> isTypeReady = true }

        formNameLayout = findViewById(R.id.form_marker_name)
        formNameLayout.editText?.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                /**
                 * Checking the name...
                 */
                isNameReady = !text.toString().isBlank()

                /**
                 * If it is NOT valid then we
                 * show a message to the user
                 */
                if (!isNameReady)
                    formNameLayout.error = "You must provide a name for the marker!"

                /**
                 * Otherwise, it means that the
                 * name was correct and we
                 * give feedback to the user
                 */
                else {
                    formNameLayout.error = null
                    formNameLayout.boxStrokeColor = ContextCompat
                        .getColor(applicationContext, R.color.green)
                }
            }
        )

        formDescriptionLayout = findViewById(R.id.form_marker_description)
        formDescriptionLayout.editText?.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                /**
                 * Checking the name...
                 */
                isDescriptionReady = !text.toString().isBlank()

                /**
                 * If it is NOT valid then we
                 * show a message to the user
                 */
                if (!isDescriptionReady)
                    formDescriptionLayout.error = "You must provide a name for the marker!"

                /**
                 * Otherwise, it means that the
                 * name was correct and we
                 * give feedback to the user
                 */
                else {
                    formDescriptionLayout.error = null
                    formDescriptionLayout.boxStrokeColor = ContextCompat
                        .getColor(applicationContext, R.color.green)
                }
            }
        )

        formSubmitButton = findViewById(R.id.form_marker_submit)
        formSubmitButton.setOnClickListener(this)

        loadingLayout = findViewById(R.id.form_marker_loading)
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.form_marker_submit) {
            /**
             * We hide the keyboard, disable thetouchscreen
             * and show a simple loading animation.
             */
            hideKeyboard(this)

            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            formNameLayout.isEnabled = false
            formDescriptionLayout.isEnabled = false
            formSubmitButton.isEnabled = false
            isLoading = true

            val type = (formTypeLayout.editText as? AutoCompleteTextView)?.text.toString()
                .toUpperCase(Locale.ROOT)

            val marker = Models.GeoMarker(
                uid = userId,
                title = formNameLayout.editText?.text.toString(),
                description = formDescriptionLayout.editText?.text.toString(),
                type = type,
                createdOn = Timestamp.now(),
                latLng = latLng
            )

            val db = FirebaseDB()
            db.createGeoMarker(
                geoMarker = marker,
                onSuccess = {
                    Toast.makeText(
                        applicationContext,
                        "Successfully created marker!",
                        Toast.LENGTH_SHORT
                    ).show()
                    this.finish()
                },
                onFailure = {
                    Toast.makeText(
                        applicationContext,
                        "Oops! Something went wrong while trying to create the marker...",
                        Toast.LENGTH_SHORT
                    ).show()
                    /**
                     * We enable the touchscreen again.
                     */
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    formNameLayout.isEnabled = true
                    formDescriptionLayout.isEnabled = true
                    formSubmitButton.isEnabled = true
                    isLoading = false
                }
            )
        }
    }

    private fun checkForm() {
        formSubmitButton.isEnabled = isTypeReady && isNameReady && isDescriptionReady
    }
}