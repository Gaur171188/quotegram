package com.nesib.quotegram.ui.on_boarding.fragments.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.nesib.quotegram.R
import com.nesib.quotegram.databinding.FragmentSignupBinding
import com.nesib.quotegram.ui.viewmodels.AuthViewModel
import com.nesib.quotegram.utils.DataState
import com.nesib.quotegram.utils.isPassword
import com.nesib.quotegram.utils.isUsername
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class SignupFragment : Fragment(R.layout.fragment_signup) {
    @Inject
    lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInActivityLauncher: ActivityResultLauncher<Intent>

    private lateinit var binding: FragmentSignupBinding
    private val authViewModel: AuthViewModel by viewModels({ requireActivity() })

    private var signingWithGoogle = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignupBinding.bind(view)

        registerActivityResult()
        subscribeObserver()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.signupBtn.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            if (!username.isUsername()) {
                binding.signupErrorTextView.visibility = View.VISIBLE
                binding.signupErrorTextView.text = "Username should at least be 5 characters"
            } else if (!password.isPassword()) {
                binding.signupErrorTextView.visibility = View.VISIBLE
                binding.signupErrorTextView.text = "Password should at least be 5 characters"
            } else {
                authViewModel.signup(username, password)
            }
        }
        binding.signupToLoginBtn.setOnClickListener {
            // Check if it is coming from login then pop back
            findNavController().popBackStack()
        }
        binding.signUpWithGoogleButton.setOnClickListener {
            val googleSignInIntent = googleSignInClient.signInIntent
            googleSignInActivityLauncher.launch(googleSignInIntent)
        }
    }

    private fun registerActivityResult() {
        googleSignInActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                try{
                    val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                    if (task.isSuccessful) {
                        val account = task.getResult(ApiException::class.java)
                        val username = account?.email?.split("@")?.get(0) + Random.nextInt(0, 9999)
                        val fullName = account?.displayName
                        val email = account?.email
                        val profileImage = account?.photoUrl?.toString()
                        // do signup operation here
                        if (email != null && fullName != null && username != null) {
                            signingWithGoogle = true
                            googleSignInClient.signOut()
                            authViewModel.signupWithGoogle(
                                email,
                                fullName,
                                username,
                                profileImage ?: ""
                            )
                        } else {
                            signingWithGoogle = false
                            binding.signupErrorTextView.visibility = View.VISIBLE
                            binding.signupErrorTextView.text = "Something went wrong, please try again"
                            // show something useful for user
                        }

                    } else {
                        binding.signupErrorTextView.visibility = View.VISIBLE
                        binding.signupErrorTextView.text = "Something went wrong, please try again"
                    }
                }
                catch (e:Exception){
                    binding.signupErrorTextView.visibility = View.VISIBLE
                    binding.signupErrorTextView.text = "Something went wrong, please try again later"
                }


            }
    }

    private fun subscribeObserver() {
        authViewModel.auth.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    binding.signupErrorTextView.visibility = View.INVISIBLE
                    authViewModel.saveUser()
                    val action =
                        SignupFragmentDirections.actionSignupFragmentToSelectCategoriesFragment(
                            it.data!!.userId,
                            it.data.username
                        )
                    findNavController().navigate(action)
                }
                is DataState.Fail -> {
                    signingWithGoogle = false
                    if (authViewModel.hasSignupError) {
                        binding.apply {
                            signupErrorTextView.visibility = View.VISIBLE
                            signupErrorTextView.text = it.message
                            signupBtn.isEnabled = true
                            signupBtnProgressBar.visibility = View.GONE
                            signupBtnTextView.visibility = View.VISIBLE
                            signupGoogleProgressBar.visibility = View.INVISIBLE
                            signUpGoogleTextView.visibility = View.VISIBLE
                        }
                    }
                }
                is DataState.Loading -> {
                    binding.signupBtn.isEnabled = false
                    if (!signingWithGoogle) {
                        binding.signupBtnProgressBar.visibility = View.VISIBLE
                        binding.signupBtnTextView.visibility = View.GONE
                    } else {
                        binding.signupGoogleProgressBar.visibility = View.VISIBLE
                        binding.signUpGoogleTextView.visibility = View.GONE
                    }

                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        authViewModel.hasSignupError = false
    }
}