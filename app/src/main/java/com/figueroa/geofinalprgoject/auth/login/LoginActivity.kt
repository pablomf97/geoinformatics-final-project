package com.figueroa.geofinalprgoject.auth.login

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.db.FirebaseAuth
import com.figueroa.geofinalprgoject.utils.hideKeyboard
import com.figueroa.geofinalprgoject.utils.isValidEmail
import com.figueroa.geofinalprgoject.utils.isValidLoginPassword
import com.google.android.material.textfield.TextInputLayout
import kotlin.properties.Delegates.observable

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    // TextInputLayout to give the
    // app a more material design look
    private lateinit var formEmailLayout: TextInputLayout
    private lateinit var formPasswordLayout: TextInputLayout

    // Checking if the form is ready to commit
    // everytime these values change
    private var isEmailReady: Boolean by observable(false) { _, _, _ -> checkForm() }
    private var isPassReady: Boolean by observable(false) { _, _, _ -> checkForm() }

    // Submit button
    private lateinit var formSubmitButton: Button

    // Showed when the app is sending
    // the request to Firebase
    private lateinit var loadingLayout: RelativeLayout
    private var isLoading: Boolean by observable(false) { _, _, _ -> showHideLoadScreen() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        initComponents()
    }

    private fun initComponents() {
        formEmailLayout = findViewById(R.id.form_login_username)
        formEmailLayout.editText?.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                /**
                 * Checking the email...
                 */
                isEmailReady = isValidEmail(text.toString().trim())

                /**
                 * If it is NOT valid then we
                 * show a message to the user
                 */
                if (!isEmailReady)
                    formEmailLayout.error = "Invalid email! It must look something" +
                            "like 'example@email.com'"

                /**
                 * Otherwise, it means that the
                 * email was correct and we
                 * give feedback to the user
                 */
                else {
                    formEmailLayout.error = null
                    formEmailLayout.helperText = "Valid email!"
                    formEmailLayout.setHelperTextColor(
                        AppCompatResources
                            .getColorStateList(applicationContext, R.color.green)
                    )
                    formEmailLayout.boxStrokeColor = ContextCompat
                        .getColor(applicationContext, R.color.green)
                }
            }
        )

        formPasswordLayout = findViewById(R.id.form_login_password)
        formPasswordLayout.editText?.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                isPassReady = isValidLoginPassword(text.toString())

                /**
                 * If password is NOT ready, we tell what's
                 * wrong about it to the user.
                 */
                if (!isPassReady)
                    formPasswordLayout.error = "Password must be at least 6 characters long!"

                /**
                 * Otherwise, it means that the password
                 * was acceptable, and we tell it to the user.
                 */
                else {
                    formPasswordLayout.error = null
                    formPasswordLayout.helperText = "Valid password!"
                    formPasswordLayout.setHelperTextColor(
                        AppCompatResources
                            .getColorStateList(applicationContext, R.color.green)
                    )
                    formPasswordLayout.boxStrokeColor = ContextCompat
                        .getColor(applicationContext, R.color.green)
                }
            }
        )

        formSubmitButton = findViewById(R.id.form_login_submit)
        formSubmitButton.setOnClickListener(this)

        loadingLayout = findViewById(R.id.form_login_loading)
    }

    override fun onClick(v: View?) {
        if (v?.id == formSubmitButton.id) {

            /**
             * We hide the keyboard, disable thetouchscreen
             * and show a simple loading animation.
             */
            hideKeyboard(this)

            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            formSubmitButton.isEnabled = false
            isLoading = true

            val auth = FirebaseAuth(applicationContext)
            auth.signInUser(
                email = formEmailLayout.editText!!.text.toString().trim(),
                password = formPasswordLayout.editText!!.text.toString().trim(),

                /**
                 * If the operation completed successfully,
                 * we perform the following, that is in our
                 * case, show a toast and finish the activity.
                 */
                onSuccess = {
                    Toast.makeText(
                        applicationContext,
                        "Successfully logged in!",
                        Toast.LENGTH_SHORT
                    ).show()

                    this.finish()
                },
                onFailure = {
                    makeToast(
                        "Oops! Something went wrong... " +
                                "Please try again later."
                    )

                    /**
                     * We enable the touchscreen again.
                     */
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    formSubmitButton.isEnabled = true
                    isLoading = false
                }
            )
        }
    }

    /**
     * Shows a simple toast.
     */
    private fun makeToast(message: String) {
        Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Checks that the for is ready for submission.
     */
    private fun checkForm() {
        formSubmitButton.isEnabled = isEmailReady && isPassReady
    }

    /**
     * Shows/hides the loading screen.
     */
    private fun showHideLoadScreen() {
        if (isLoading) {
            loadingLayout.visibility = View.VISIBLE
        } else {
            loadingLayout.visibility = View.GONE
        }
    }
}