package com.figueroa.geofinalprgoject.auth.registration

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.db.FirebaseAuth
import com.figueroa.geofinalprgoject.utils.hideKeyboard
import com.figueroa.geofinalprgoject.utils.isValidEmail
import com.figueroa.geofinalprgoject.utils.isValidPassword
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlin.properties.Delegates.observable


class RegisterActivity : AppCompatActivity(), View.OnClickListener {

    // TextInputLayout to give the
    // app a more material design look
    private lateinit var formEmailLayout: TextInputLayout
    private lateinit var formPasswordLayout: TextInputLayout

    // Some indicators to give the user
    // info about what they input
    private lateinit var formPassStrengthText: TextView
    private lateinit var formProgressIndicator: LinearProgressIndicator

    // Submit button
    private lateinit var formSubmitButton: Button

    // Checking if the form is ready to commit
    // everytime these values change
    private var isEmailReady: Boolean by observable(false) { _, _, _ -> checkForm() }
    private var isPassReady: Boolean by observable(false) { _, _, _ -> checkForm() }

    // Showed when the app is sending
    // the request to Firebase
    private lateinit var loadingLayout: RelativeLayout
    private var isLoading: Boolean by observable(false) { _, _, _ -> showHideLoadScreen() }

    /**
     * Overriding Android's onCreate function
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar?.hide()

        initComponents()
    }

    /**
     * Initializes the components of the view, assigning
     * the needed listeners and event catchers
     */
    private fun initComponents() {
        formEmailLayout = findViewById(R.id.form_register_username)
        formEmailLayout.editText?.addTextChangedListener(
            /**
             * Listener to check the email.
             */
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

        formPasswordLayout = findViewById(R.id.form_register_password)
        formPasswordLayout.editText?.addTextChangedListener(
            /**
             * Listener to check the password.
             */
            onTextChanged = { text, _, _, _ ->
                /**
                 * Checking if the password is valid...
                 */
                val passCheck = isValidPassword(text.toString().trim())
                isPassReady = passCheck.first

                /**
                 * This will give the user some feedback
                 * about the strength of their password.
                 */
                formProgressIndicator.visibility = View.VISIBLE
                formProgressIndicator.setProgressCompat((100 * passCheck.second) / 5, true)

                val color = decideBarColor(passCheck.second)
                formPassStrengthText.visibility = View.VISIBLE
                formProgressIndicator.setIndicatorColor(color)
                formPassStrengthText.setTextColor(color)
                formPassStrengthText.text = decidePassText(passCheck.second)

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

        /**
         * Initialising the rest of the components...
         */
        formProgressIndicator = findViewById(R.id.form_register_progress_bar)
        formPassStrengthText = findViewById(R.id.form_register_strength_indicator)

        formSubmitButton = findViewById(R.id.form_register_submit)
        formSubmitButton.setOnClickListener(this)

        loadingLayout = findViewById(R.id.form_register_loading)
    }

    /**
     * Given the strength of a password, it returns
     * a string that says it textually.
     */
    private fun decidePassText(strength: Int): String {
        return when (strength) {
            0 -> {
                "Extremely weak password"
            }
            1 -> {
                "Weak password"
            }
            2 -> {
                "Less weak password"
            }
            3 -> {
                "Average password"
            }
            4 -> {
                "Secure password"
            }
            5 -> {
                "Really secure password"
            }
            else -> {
                ""
            }
        }
    }

    /**
     * Given the strength of a password,
     * it returns a color.
     */
    private fun decideBarColor(strength: Int): Int {
        return when (strength) {
            0 -> {
                ContextCompat.getColor(applicationContext, R.color.primaryColor)
            }
            1 -> {
                ContextCompat.getColor(applicationContext, R.color.red)
            }
            2 -> {
                ContextCompat.getColor(applicationContext, R.color.orange)
            }
            3 -> {
                ContextCompat.getColor(applicationContext, R.color.yellow)
            }
            4 -> {
                ContextCompat.getColor(applicationContext, R.color.green)
            }
            5 -> {
                ContextCompat.getColor(applicationContext, R.color.blue)
            }
            else -> {
                ContextCompat.getColor(applicationContext, R.color.primaryColor)
            }
        }
    }

    /**
     * Overriding Android's onClick function.
     */
    override fun onClick(v: View?) {
        /**
         * If the pressed view was the submit button...
         */
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

            /**
             * We try to sign in the user...
             */
            val auth = FirebaseAuth(applicationContext)
            auth.createUser(
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
                        "Successfully created account!",
                        Toast.LENGTH_SHORT
                    ).show()

                    this.finish()
                },

                /**
                 * If there was some error, we inform the user.
                 */
                onFailure = { exception ->
                    when (exception?.message) {
                        "The email address is already in use by another account." -> {
                            makeToast("It looks like that email is already in use!")
                        }
                        "auth/invalid-email" -> {
                            makeToast("Oops! The email you provided is invalid...")
                        }
                        "The given password is invalid. " +
                                "[ Password should be at least 6 characters ]" -> {
                            makeToast("Woah there! Your password is too weak...")
                        }
                        else -> {
                            makeToast(
                                "Oops! Something went wrong... " +
                                        "Please try again later."
                            )
                        }
                    }

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