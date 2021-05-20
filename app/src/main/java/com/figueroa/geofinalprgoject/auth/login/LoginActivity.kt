package com.figueroa.geofinalprgoject.auth.login

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.utils.isValidEmail
import com.google.android.material.textfield.TextInputLayout
import kotlin.properties.Delegates.observable
import kotlin.reflect.KProperty

class LoginActivity : AppCompatActivity() {

    // Form stuff
    private lateinit var formEmailLayout: TextInputLayout
    private lateinit var formPasswordLayout: TextInputLayout
    private lateinit var formSubmitButton: Button

    private var isEmailReady: Boolean by observable(false) {_,_,_ -> checkForm()}
    private var isPassReady: Boolean by observable(false) {_,_,_ -> checkForm()}

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
                if (isValidEmail(text.toString()))
                    isEmailReady = true
            }
        )

        formPasswordLayout = findViewById(R.id.form_login_password)
        formPasswordLayout.editText?.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                Log.w("ONTEXT", text.toString())
            }
        )

        formSubmitButton = findViewById(R.id.form_login_submit)
    }

    private fun checkForm() {

    }
}