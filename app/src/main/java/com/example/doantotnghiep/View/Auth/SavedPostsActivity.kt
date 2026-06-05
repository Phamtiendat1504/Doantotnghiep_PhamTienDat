package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Adapter.SavedPostsAdapter
import com.example.doantotnghiep.ViewModel.SavedPostsViewModel
import com.example.doantotnghiep.databinding.ActivitySavedPostsBinding

class SavedPostsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedPostsBinding
    private val viewModel: SavedPostsViewModel by viewModels()
    private lateinit var adapter: SavedPostsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadSavedPosts()
    }

    private fun setupRecyclerView() {
        adapter = SavedPostsAdapter(
            onItemClick = { post ->
                if (post.roomId.isEmpty()) {
                    MessageUtils.showErrorDialog(this, "Lỗi", "Không tìm thấy thông tin phòng.")
                    return@SavedPostsAdapter
                }
                binding.progressBar.visibility = View.VISIBLE
                viewModel.checkRoomExists(post.savedDocId, post.roomId)
            },
            onRemoveClick = { post ->
                viewModel.deleteSavedPost(post.savedDocId)
            }
        )

        binding.rvSavedPosts.apply {
            layoutManager = LinearLayoutManager(this@SavedPostsActivity)
            adapter = this@SavedPostsActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener { finish() }
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadSavedPosts()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.savedPosts.observe(this) { posts ->
            binding.swipeRefreshLayout.isRefreshing = false
            adapter.submitList(posts)
            binding.tvEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                binding.tvEmpty.text = "Lỗi tải dữ liệu"
                binding.tvEmpty.visibility = View.VISIBLE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.deleteResult.observe(this) { _ ->
            binding.swipeRefreshLayout.isRefreshing = false
            viewModel.loadSavedPosts()
        }

        viewModel.roomCheckResult.observe(this) { result ->
            if (result == null) return@observe
            val (savedDocId, exists) = result
            
            viewModel.clearRoomCheckResult()
            binding.progressBar.visibility = View.GONE
            
            val post = viewModel.savedPosts.value?.find { it.savedDocId == savedDocId } ?: return@observe
            
            if (exists) {
                startActivity(Intent(this, RoomDetailActivity::class.java).apply {
                    putExtra("roomId", post.roomId)
                })
            } else {
                viewModel.autoDeleteSavedPost(savedDocId)
                MessageUtils.showInfoDialog(
                    this,
                    "Phòng không còn tồn tại",
                    "Bài đăng này đã bị gỡ bởi chủ trọ và đã được xóa khỏi danh sách yêu thích của bạn."
                ) { viewModel.loadSavedPosts() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSavedPosts()
    }
}