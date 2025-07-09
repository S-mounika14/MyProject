package com.example.myproject

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: EmployeeViewModel
    private lateinit var adapter: EmployeeAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tverrormessage: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupRecyclerView()
        setupViewModel()
        setupObservers()
        setupSearchFunctionality()
        setupSwipeRefresh()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.rv_employees)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        progressBar = findViewById(R.id.progress_bar)
        errorTextView = findViewById(R.id.tv_error)
        searchEditText = findViewById(R.id.et_search)
        emptyStateLayout = findViewById(R.id.empty_state_layout)
        tverrormessage=findViewById(R.id.tv_empty_message)

    }

    private fun setupRecyclerView() {
        adapter = EmployeeAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupViewModel() {
        val repository = EmployeeRepository(this)
        val factory = EmployeeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[EmployeeViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                if (error.contains("429")) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    swipeRefreshLayout.isEnabled = false
                    swipeRefreshLayout.postDelayed({ swipeRefreshLayout.isEnabled = true }, 10000)
                } else {
                    showError(error)
                }
            } else {
                hideError()
            }
        }

        viewModel.filteredEmployees.observe(this) { employees ->
            if (employees.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
                adapter.submitList(employees)
            }
        }

        viewModel.invalidInput.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            if (swipeRefreshLayout.isEnabled) {
                viewModel.loadEmployees()
            }
        }
    }
    private fun setupSearchFunctionality() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchEmployees(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

//    private fun setupSwipeRefresh() {
//        swipeRefreshLayout.setOnRefreshListener {
//            viewModel.loadEmployees()
//        }
//    }

    private fun showError(error: String) {
        errorTextView.text = error
        errorTextView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
    }

    private fun hideError() {
        errorTextView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        val searchQuery = searchEditText.text.toString()
        emptyStateLayout.findViewById<TextView>(R.id.tv_empty_message).text =
            if (searchQuery.isEmpty()) "No employees available. Try refreshing." else "No employees match your search."
        emptyStateLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateLayout.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}