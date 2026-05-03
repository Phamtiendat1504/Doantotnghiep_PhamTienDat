package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.UserSearchAdapter
import com.example.doantotnghiep.ViewModel.SearchProfileViewModel

class SearchProfileActivity : AppCompatActivity() {

    private val viewModel: SearchProfileViewModel by viewModels()

    private lateinit var edtSearch: EditText
    private lateinit var btnBack: ImageView
    private lateinit var rvResults: RecyclerView
    private lateinit var progressSearch: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: UserSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_profile)

        initViews()
        setupAdapter()
        observeViewModel()
        setupSearch()
    }

    private fun initViews() {
        edtSearch = findViewById(R.id.edtSearchProfile)
        btnBack = findViewById(R.id.btnBackSearch)
        rvResults = findViewById(R.id.rvSearchProfile)
        progressSearch = findViewById(R.id.progressSearch)
        layoutEmpty = findViewById(R.id.layoutEmptySearch)

        btnBack.setOnClickListener { finish() }
    }

    private fun setupAdapter() {
        adapter = UserSearchAdapter { user ->
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("userId", user.uid)
            intent.putExtra("userName", user.fullName)
            startActivity(intent)
        }
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressSearch.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.users.observe(this) { users ->
            adapter.submitList(users)

            if (users.isEmpty() && edtSearch.text.isNotEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                (layoutEmpty.getChildAt(1) as? android.widget.TextView)?.text = "Không tìm thấy người dùng nào"
            } else if (users.isNotEmpty()) {
                layoutEmpty.visibility = View.GONE
            }
        }
    }

    private var searchRunnable: Runnable? = null
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun setupSearch() {
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                if (query.length >= 2) {
                    layoutEmpty.visibility = View.GONE
                    
                    searchRunnable = Runnable {
                        viewModel.searchUsers(query)
                    }
                    searchHandler.postDelayed(searchRunnable!!, 400)
                } else if (query.isEmpty()) {
                    adapter.submitList(emptyList())
                    layoutEmpty.visibility = View.VISIBLE
                    (layoutEmpty.getChildAt(1) as? android.widget.TextView)?.text = "Gõ tên để tìm người dùng"
                }
            }
        })

        edtSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)
            ) {
                // Ẩn bàn phím
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                edtSearch.clearFocus()
                true
            } else {
                false
            }
        }

        edtSearch.requestFocus()
    }
}
