package com.example.doantotnghiep.View.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.RoomAdapter
import com.example.doantotnghiep.View.Adapter.RoomItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var btnNotification: ImageView
    private lateinit var rvFeaturedRooms: RecyclerView
    private lateinit var rvNewRooms: RecyclerView
    private lateinit var tvNoFeatured: TextView
    private lateinit var tvNoNewRooms: TextView
    private lateinit var skeletonFeatured: View
    private lateinit var skeletonNewRooms: View

    private val db = FirebaseFirestore.getInstance()

    // 2 Adapter riêng biệt cho 2 danh sách
    private val featuredAdapter = RoomAdapter(
        viewType = RoomAdapter.VIEW_TYPE_HORIZONTAL
    )
    private val newRoomsAdapter = RoomAdapter(
        viewType = RoomAdapter.VIEW_TYPE_VERTICAL
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGreeting = view.findViewById(R.id.tvGreeting)
        btnNotification = view.findViewById(R.id.btnNotification)
        tvNoFeatured = view.findViewById(R.id.tvNoFeatured)
        tvNoNewRooms = view.findViewById(R.id.tvNoNewRooms)
        rvFeaturedRooms = view.findViewById(R.id.rvFeaturedRooms)
        rvNewRooms = view.findViewById(R.id.rvNewRooms)
        skeletonFeatured = view.findViewById(R.id.skeletonFeatured)
        skeletonNewRooms = view.findViewById(R.id.skeletonNewRooms)

        setupRecyclerViews()
        loadUserName()
        loadFeaturedRooms()
        loadNewRooms()

        btnNotification.setOnClickListener {
            com.example.doantotnghiep.Utils.MessageUtils.showInfoDialog(requireContext(), "Thông báo", "Chức năng thông báo đang được phát triển, vui lòng quay lại sau!")
        }
    }

    private fun setupRecyclerViews() {
        // Phòng nổi bật: cuộn ngang
        rvFeaturedRooms.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rvFeaturedRooms.adapter = featuredAdapter

        // Phòng mới đăng: cuộn dọc, tắt scroll vì đã nằm trong ScrollView
        rvNewRooms.layoutManager = LinearLayoutManager(requireContext())
        rvNewRooms.adapter = newRoomsAdapter
        rvNewRooms.isNestedScrollingEnabled = false
    }

    private fun loadUserName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded && doc.exists()) {
                    tvGreeting.text = "Chào, ${doc.getString("fullName") ?: "Bạn"}"
                }
            }
    }

    private fun loadFeaturedRooms() {
        skeletonFeatured.visibility = View.VISIBLE
        rvFeaturedRooms.visibility = View.GONE
        db.collection("rooms")
            .whereEqualTo("status", "approved")
            .whereEqualTo("isFeatured", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener
                skeletonFeatured.visibility = View.GONE

                if (documents.isEmpty) {
                    tvNoFeatured.visibility = View.VISIBLE
                    rvFeaturedRooms.visibility = View.GONE
                    return@addOnSuccessListener
                }

                tvNoFeatured.visibility = View.GONE
                rvFeaturedRooms.visibility = View.VISIBLE

                val list = documents.map { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val imageUrls = doc.get("imageUrls") as? List<String>
                    RoomItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        price = doc.getLong("price") ?: 0,
                        ward = doc.getString("ward") ?: "",
                        district = doc.getString("district") ?: "",
                        area = doc.getLong("area")?.toInt() ?: 0,
                        imageUrl = imageUrls?.firstOrNull()
                    )
                }
                featuredAdapter.submitList(list)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                skeletonFeatured.visibility = View.GONE
                tvNoFeatured.visibility = View.VISIBLE
                rvFeaturedRooms.visibility = View.GONE
            }
    }

    private fun loadNewRooms() {
        skeletonNewRooms.visibility = View.VISIBLE
        rvNewRooms.visibility = View.GONE
        db.collection("rooms")
            .whereEqualTo("status", "approved")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener
                skeletonNewRooms.visibility = View.GONE

                if (documents.isEmpty) {
                    tvNoNewRooms.visibility = View.VISIBLE
                    rvNewRooms.visibility = View.GONE
                    return@addOnSuccessListener
                }

                tvNoNewRooms.visibility = View.GONE
                rvNewRooms.visibility = View.VISIBLE

                val list = documents.map { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val imageUrls = doc.get("imageUrls") as? List<String>
                    RoomItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        price = doc.getLong("price") ?: 0,
                        ward = doc.getString("ward") ?: "",
                        district = doc.getString("district") ?: "",
                        area = doc.getLong("area")?.toInt() ?: 0,
                        imageUrl = imageUrls?.firstOrNull()
                    )
                }
                newRoomsAdapter.submitList(list)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                skeletonNewRooms.visibility = View.GONE
                tvNoNewRooms.visibility = View.VISIBLE
                rvNewRooms.visibility = View.GONE
            }
    }

    override fun onResume() {
        super.onResume()
        // Chỉ load lại nếu danh sách đang trống (tránh gọi Firebase mỗi khi chuyển tab)
        if (featuredAdapter.itemCount == 0) loadFeaturedRooms()
        if (newRoomsAdapter.itemCount == 0) loadNewRooms()
    }
}