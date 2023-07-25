package com.nesib.quotegram.ui.main.fragments.user

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.nesib.quotegram.R
import com.nesib.quotegram.adapters.HomeAdapter
import com.nesib.quotegram.databinding.FragmentUserProfileBinding
import com.nesib.quotegram.databinding.ReportDialogBinding
import com.nesib.quotegram.models.Quote
import com.nesib.quotegram.models.User
import com.nesib.quotegram.ui.main.MainActivity
import com.nesib.quotegram.ui.viewmodels.AuthViewModel
import com.nesib.quotegram.ui.viewmodels.QuoteViewModel
import com.nesib.quotegram.ui.viewmodels.ReportViewModel
import com.nesib.quotegram.ui.viewmodels.UserViewModel
import com.nesib.quotegram.utils.DataState
import com.nesib.quotegram.utils.showToast
import com.nesib.quotegram.utils.toFormattedNumber
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {
    private lateinit var binding: FragmentUserProfileBinding

    private val homeAdapter by lazy { HomeAdapter((activity as MainActivity).dialog) }
    private val userViewModel: UserViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels({ requireActivity() })
    private val quoteViewModel: QuoteViewModel by viewModels({ requireActivity() })
    private val reportViewModel: ReportViewModel by viewModels()
    private val args by navArgs<UserProfileFragmentArgs>()

    private var paginatingFinished = false
    private var paginationLoading = false
    private var currentPage = 1
    private var currentUserQuotes = mutableListOf<Quote>()
    private var currentUser: User? = null

    private var makeSureDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentUserProfileBinding.bind(view)

        setupClickListeners()
        setupRecyclerView()
        subscribeObservers()
        setHasOptionsMenu(true)

        userViewModel.getUser(args.userId)
    }


    private fun bindData(user: User) {
        binding.usernameTextView.text = user.username
        binding.bioTextView.text = if (user.bio!!.isNotEmpty()) user.bio else "No bio"
        binding.followerCountTextView.text = user.followers!!.size.toFormattedNumber()
        binding.followingCountTextView.text =
            (user.followingUsers!!.size + user.followingBooks!!.size).toFormattedNumber()
        binding.quoteCountTextView.text = (user.totalQuoteCount ?: 0).toFormattedNumber()
        if (user.profileImage != null && user.profileImage != "") {
            binding.userPhotoImageView.load(user.profileImage) {
                error(R.drawable.user)
            }
        } else {
            binding.userPhotoImageView.load(R.drawable.user)
        }
        if (user.quotes!!.isEmpty()) {
            binding.noQuoteFoundContainer.visibility = View.VISIBLE
        }
        toggleFollowButtonStyle(user.followers!!.contains(authViewModel.currentUserId))

        homeAdapter.setData(user.quotes!!)
    }

    private fun toggleFollowButtonStyle(following: Boolean) {
        binding.apply {
            followButton.setBackgroundResource(if (following) R.drawable.add_quote_from_this_book_bg else R.drawable.follow_button_bg)
            followButtonTextView.text = if (following) "Following" else "Follow"
            followButtonTextView.setTextColor(
                if (following) ContextCompat.getColor(
                    requireContext(),
                    R.color.blue
                ) else ContextCompat.getColor(requireContext(), R.color.white)
            )

        }
    }

    private fun subscribeObservers() {
        userViewModel.user.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    if (binding.failContainer.visibility == View.VISIBLE) {
                        binding.failContainer.visibility = View.GONE
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.profileContent.visibility = View.VISIBLE
                    val user = it.data!!.user!!
                    currentUser = user
                    bindData(user)
                    currentUserQuotes = user.quotes!!.toMutableList()
                }
                is DataState.Fail -> {
                    binding.failContainer.visibility = View.VISIBLE
                    binding.failMessage.text = it.message
                    binding.progressBar.visibility = View.GONE
                    binding.profileContent.visibility = View.INVISIBLE
                }
                is DataState.Loading -> {
                    if (binding.failContainer.visibility == View.VISIBLE) {
                        binding.failContainer.visibility = View.GONE
                    }
                    binding.progressBar.visibility = View.VISIBLE
                    binding.profileContent.visibility = View.GONE
                }
            }
        }
        userViewModel.userQuotes.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    if (currentUserQuotes.size == it.data!!.quotes.size) {
                        paginatingFinished = true
                    }
                    binding.paginationProgressBar.visibility = View.INVISIBLE
                    paginationLoading = false
                    homeAdapter.setData(it.data.quotes)
                    currentUserQuotes = it.data.quotes.toMutableList()

                }
                is DataState.Fail -> {
                    binding.paginationProgressBar.visibility = View.INVISIBLE
                    showToast(it.message!!)
                    paginationLoading = false
                }
                is DataState.Loading -> {
                    if (paginationLoading) {
                        binding.paginationProgressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
        userViewModel.userFollow.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Fail -> {
                    showToast(it.message)
                }
            }
        }
        reportViewModel.report.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    makeSureDialog?.dismiss()
                    showToast(it.data!!.message)
                }
                is DataState.Fail -> {
                    makeSureDialog?.dismiss()
                    showToast(it.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.followButton.setOnClickListener {
            if (authViewModel.currentUserId == null) {
                (activity as MainActivity).showAuthenticationDialog()
                return@setOnClickListener
            }
            currentUser?.let { user ->
                val followers = user.followers!!.toMutableList()
                if (!user.followers!!.contains(authViewModel.currentUserId)) {
                    followers.add(authViewModel.currentUserId!!)
                    toggleFollowButtonStyle(true)
                    binding.followerCountTextView.text =
                        (binding.followerCountTextView.text.toString()
                            .toInt() + 1).toString()
                } else {
                    followers.remove(authViewModel.currentUserId!!)
                    toggleFollowButtonStyle(false)
                    binding.followerCountTextView.text =
                        (binding.followerCountTextView.text.toString()
                            .toInt() - 1).toString()
                }
                user.followers = followers.toList()
                userViewModel.followOrUnFollowUser(user)
            }

        }
        binding.tryAgainButton.setOnClickListener {
            userViewModel.getUser(args.userId, forced = true)
        }
    }

    private fun setupRecyclerView() {
        homeAdapter.currentUserId = authViewModel.currentUserId
        homeAdapter.onLikeClickListener = { quote ->
            quoteViewModel.toggleLike(quote)
        }
        homeAdapter.onQuoteOptionsClickListener = { quote ->
            val action = UserProfileFragmentDirections.actionGlobalQuoteOptionsFragment(quote)
            findNavController().navigate(action)
        }
        homeAdapter.onDownloadClickListener = { quote ->
            val action = UserProfileFragmentDirections.actionGlobalDownloadQuoteFragment(
                quote.quote,
                quote.book?.name
            )
            findNavController().navigate(action)
        }
        homeAdapter.onShareClickListener = { quote ->
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, quote.quote + "\n\n#Quotegram App")
            val shareIntent = Intent.createChooser(intent,"Share Quote")
            startActivity(shareIntent)

        }
        binding.userQuotesRecyclerView.adapter = homeAdapter
        binding.userQuotesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.profileContent.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
                val notReachedBottom = v.canScrollVertically(1)

                if (!notReachedBottom && !paginationLoading && currentUserQuotes.size >= 2 && !paginatingFinished) {
                    currentPage++
                    paginationLoading = true
                    userViewModel.getMoreUserQuotes(args.userId, currentPage)
                }
            }
        })
    }

    private fun showMakeSureDialogForReport() {
        val view = layoutInflater.inflate(R.layout.report_dialog, binding.root, false)
        val binding = ReportDialogBinding.bind(view)
        makeSureDialog = AlertDialog.Builder(requireContext()).setView(binding.root).create()
        makeSureDialog!!.show()

        binding.apply {
            notNowButton.setOnClickListener { makeSureDialog!!.dismiss() }
            reportButton.setOnClickListener {
                makeSureDialog!!.setCancelable(false)
                binding.reportProgressBar.visibility = View.VISIBLE
                binding.reportButton.visibility = View.INVISIBLE
                binding.notNowButton.isEnabled = false
                reportViewModel.reportUser(authViewModel.currentUserId ?: "", args.userId)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.user_profile_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.report_user_menu_item) {
            showMakeSureDialogForReport()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}
