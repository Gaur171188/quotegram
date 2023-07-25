package com.nesib.quotegram.ui.main.fragments.add

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nesib.quotegram.R
import com.nesib.quotegram.adapters.SearchBooksAdapter
import com.nesib.quotegram.databinding.FragmentSelectBookBinding
import com.nesib.quotegram.models.Book
import com.nesib.quotegram.ui.viewmodels.BookViewModel
import com.nesib.quotegram.utils.DataState
import com.nesib.quotegram.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectBookFragment : Fragment(R.layout.fragment_select_book) {
    private lateinit var binding: FragmentSelectBookBinding
    private val bookViewModel: BookViewModel by viewModels()
    private val searchAdapter by lazy { SearchBooksAdapter(true) }
    private var searchViewTextChanged = false
    private var loadedBooks = emptyList<Book>()
    private var currentPage = 1
    private var paginationLoading = false
    private var currentSearchText = ""
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSelectBookBinding.bind(view)
        setOnClickListeners()
        subscribeObservers()
        setupRecyclerView()


        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                searchViewTextChanged = true
                Handler(Looper.getMainLooper()).postDelayed({
                    currentSearchText = s.toString()
                    if (currentSearchText.isNotEmpty()) {
                        currentPage = 1
                        bookViewModel.getBooks(currentSearchText,currentPage,true)
                    }
                }, 300)
            }

        })
    }

    private fun setOnClickListeners() {
        binding.addBookYourselfBtn.setOnClickListener {
            findNavController().navigate(R.id.action_selectBookFragment_to_addBookFragment)
        }
        searchAdapter.onBookClickListener = { book ->
            val action = SelectBookFragmentDirections.actionSelectBookFragmentToAddQuoteFragment()
            action.book = book
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() {
        val mLayoutManager = LinearLayoutManager(requireContext())
        binding.selectBooksRecyclerView.apply {
            itemAnimator = null
            adapter = searchAdapter
            layoutManager = mLayoutManager
            this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        if (mLayoutManager.findLastCompletelyVisibleItemPosition() == (loadedBooks.size - 1) && !paginationLoading) {
                            currentPage++
                            if(currentSearchText.isNotEmpty()){
                                paginationLoading = true
                                bookViewModel.getBooks(currentSearchText,currentPage)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun subscribeObservers() {
        bookViewModel.books.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    paginationLoading = false
                    loadedBooks = it.data!!.books
                    searchAdapter.setData(loadedBooks)
                    toggleProgressBar(false)
                    binding.paginationProgressBar.visibility = View.INVISIBLE

                }
                is DataState.Fail -> {
                    showToast(it.message!!)
                    paginationLoading = false
                    toggleProgressBar(false)
                    binding.paginationProgressBar.visibility = View.INVISIBLE
                }
                is DataState.Loading -> {
                    if(paginationLoading){
                        binding.paginationProgressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun toggleProgressBar(loading: Boolean) {
        if (loading || loadedBooks.isNotEmpty()) {
            binding.noBookFoundContainer.visibility = View.INVISIBLE
        } else if (!loading && loadedBooks.isEmpty()) {
            binding.noBookFoundContainer.visibility = View.VISIBLE
        }

        binding.progressBar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        binding.selectBooksRecyclerView.visibility =
            if (loading && loadedBooks.isNotEmpty()) View.INVISIBLE else View.VISIBLE
    }

}