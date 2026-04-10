package com.hangout.app.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hangout.app.R
import com.hangout.app.databinding.ActivityAuthBinding
import com.hangout.app.ui.main.MainActivity
import com.hangout.app.utils.SessionManager
import java.util.Calendar

class AuthActivity : AppCompatActivity(), AuthContract.View {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var presenter: AuthContract.Presenter
    private lateinit var session: SessionManager
    private var selectedBirthdate: String = ""
    private var dotIndex = 0
    private val dotHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val dotRunnable = object : Runnable {
        override fun run() {
            cycleDots()
            dotHandler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        if (!::presenter.isInitialized) {
            presenter = AuthPresenter(this, this) // pass context here
        }

        if (session.isLoggedIn()) { goToMain(); return }

        showSignIn()
        dotHandler.post(dotRunnable)
        setupTabSwitching()
        setupDatePicker()
        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        dotHandler.removeCallbacks(dotRunnable)
        presenter.detachView()
    }

    // ── AuthContract.View ──────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.visibility     = if (show) View.VISIBLE else View.GONE
        binding.btnSignIn.isEnabled        = !show
        binding.btnCreateAccount.isEnabled = !show
    }

    override fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.setTextColor(ContextCompat.getColor(this, R.color.red_accent))
        binding.tvError.visibility = View.VISIBLE
    }

    override fun onLoginSuccess(token: String, email: String, firstname: String) {
        session.saveSession(token, email, firstname)
        goToMain()
    }

    override fun onRegisterSuccess() {
        binding.tvError.text = "Account created! Please sign in."
        binding.tvError.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        binding.tvError.visibility = View.VISIBLE
        binding.root.postDelayed({
            showSignIn()
            binding.etSignInEmail.setText(binding.etRegisterEmail.text)
        }, 1500)
    }

    // ── UI Helpers ─────────────────────────────────────────────────────────

    private fun showSignIn() {
        binding.layoutSignInForm.visibility   = View.VISIBLE
        binding.layoutRegisterForm.visibility = View.GONE
        binding.tvError.visibility            = View.GONE
        binding.btnTabSignIn.setBackgroundResource(R.drawable.tab_active_bg)
        binding.btnTabSignIn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        binding.btnTabRegister.setBackgroundResource(android.R.color.transparent)
        binding.btnTabRegister.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
    }

    private fun showRegister() {
        binding.layoutRegisterForm.visibility = View.VISIBLE
        binding.layoutSignInForm.visibility   = View.GONE
        binding.tvError.visibility            = View.GONE
        binding.btnTabRegister.setBackgroundResource(R.drawable.tab_active_bg)
        binding.btnTabRegister.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        binding.btnTabSignIn.setBackgroundResource(android.R.color.transparent)
        binding.btnTabSignIn.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
    }

    private fun setupTabSwitching() {
        binding.btnTabSignIn.setOnClickListener   { showSignIn()   }
        binding.btnTabRegister.setOnClickListener { showRegister() }
    }

    private fun setupDatePicker() {
        val showPicker = {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, -18)
            DatePickerDialog(this,
                { _, year, month, day ->
                    val mm = String.format("%02d", month + 1)
                    val dd = String.format("%02d", day)
                    binding.etBirthdate.setText("$mm/$dd/$year")
                    selectedBirthdate = "$year-$mm-$dd"
                    binding.tilBirthdate.error = null
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }
        binding.etBirthdate.setOnClickListener         { showPicker() }
        binding.tilBirthdate.setEndIconOnClickListener { showPicker() }
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            binding.tvError.visibility = View.GONE
            val email    = binding.etSignInEmail.text?.toString()?.trim() ?: ""
            val password = binding.etSignInPassword.text?.toString() ?: ""
            var valid    = true
            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilSignInEmail.error = "Valid email required"; valid = false
            } else binding.tilSignInEmail.error = null
            if (password.isBlank()) {
                binding.tilSignInPassword.error = "Password required"; valid = false
            } else binding.tilSignInPassword.error = null
            if (valid) presenter.login(email, password)
        }

        binding.btnCreateAccount.setOnClickListener {
            binding.tvError.visibility = View.GONE
            if (validateRegisterForm()) {
                presenter.register(
                    firstname = binding.etFirstName.text?.toString()?.trim() ?: "",
                    lastname  = binding.etLastName.text?.toString()?.trim() ?: "",
                    email     = binding.etRegisterEmail.text?.toString()?.trim() ?: "",
                    password  = binding.etRegisterPassword.text?.toString() ?: "",
                    birthdate = selectedBirthdate
                )
            }
        }
    }

    private fun validateRegisterForm(): Boolean {
        var valid     = true
        val firstname = binding.etFirstName.text?.toString()?.trim() ?: ""
        val lastname  = binding.etLastName.text?.toString()?.trim() ?: ""
        val email     = binding.etRegisterEmail.text?.toString()?.trim() ?: ""
        val password  = binding.etRegisterPassword.text?.toString() ?: ""
        val confirm   = binding.etConfirmPassword.text?.toString() ?: ""

        if (firstname.isBlank()) { binding.etFirstName.error = "Required"; valid = false }
        if (lastname.isBlank())  { binding.etLastName.error  = "Required"; valid = false }
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etRegisterEmail.error = "Valid email required"; valid = false
        }
        if (selectedBirthdate.isBlank()) {
            binding.tilBirthdate.error = "Required"; valid = false
        }
        val pwRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$")
        if (!pwRegex.matches(password)) {
            showError("Password must be 8+ chars with upper, lower, number & special char")
            valid = false
        }
        if (confirm != password) {
            binding.etConfirmPassword.error = "Passwords do not match"; valid = false
        }
        return valid
    }

    private fun cycleDots() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEachIndexed { index, view ->
            view.animate().cancel()
            if (index == dotIndex) {
                view.setBackgroundResource(R.drawable.dot_active)
                view.alpha = 1f
                view.animate().scaleX(1.5f).scaleY(1.5f).setDuration(900).withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(900).start()
                }.start()
            } else {
                view.setBackgroundResource(R.drawable.dot_inactive)
                view.animate().scaleX(1f).scaleY(1f).alpha(0.35f).setDuration(500).start()
            }
        }
        dotIndex = (dotIndex + 1) % 3
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}