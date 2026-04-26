package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.Model.SupportTicket
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Adapter.SupportTicketAdapter
import com.example.doantotnghiep.ViewModel.SupportViewModel
import com.google.firebase.auth.FirebaseAuth

class SupportTicketsActivity : AppCompatActivity() {

    private lateinit var viewModel: SupportViewModel
    private lateinit var adapter: SupportTicketAdapter
    private lateinit var rvTickets: RecyclerView
    private lateinit var emptyView: View
    private lateinit var filterGroup: RadioGroup

    private var allTickets = listOf<SupportTicket>()
    private var currentFilter = "all"
    private var selectedTicketId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_tickets)

        if (FirebaseAuth.getInstance().currentUser == null) {
            MessageUtils.showErrorDialog(this, "Thông báo", "Bạn cần đăng nhập để sử dụng Trung tâm hỗ trợ.")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[SupportViewModel::class.java]
        initViews()
        setupRecyclerView()
        observeViewModel()
        viewModel.listenMyTickets()
    }

    private fun initViews() {
        rvTickets = findViewById(R.id.rvSupportTickets)
        emptyView = findViewById(R.id.layoutTicketEmpty)
        filterGroup = findViewById(R.id.rgTicketFilters)

        findViewById<View>(R.id.btnSupportBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnNewTicket).setOnClickListener { showCreateTicketDialog() }
        filterGroup.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                R.id.rbNewTickets -> "new"
                R.id.rbProgressTickets -> "in_progress"
                R.id.rbResolvedTickets -> "resolved"
                R.id.rbClosedTickets -> "closed"
                else -> "all"
            }
            renderTickets()
        }
    }

    private fun setupRecyclerView() {
        adapter = SupportTicketAdapter { ticket ->
            selectedTicketId = ticket.id
            startActivity(Intent(this, SupportTicketDetailActivity::class.java).apply {
                putExtra(SupportTicketDetailActivity.EXTRA_TICKET_ID, ticket.id)
                putExtra(SupportTicketDetailActivity.EXTRA_TICKET_TITLE, ticket.title)
                putExtra(SupportTicketDetailActivity.EXTRA_TICKET_STATUS, ticket.status)
            })
        }
        rvTickets.layoutManager = LinearLayoutManager(this)
        rvTickets.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.tickets.observe(this) {
            allTickets = it
            renderTickets()
        }
        viewModel.createdTicketId.observe(this) { ticketId ->
            if (ticketId.isNotBlank()) {
                startActivity(Intent(this, SupportTicketDetailActivity::class.java).apply {
                    putExtra(SupportTicketDetailActivity.EXTRA_TICKET_ID, ticketId)
                    putExtra(SupportTicketDetailActivity.EXTRA_TICKET_TITLE, "Yêu cầu hỗ trợ")
                    putExtra(SupportTicketDetailActivity.EXTRA_TICKET_STATUS, "new")
                })
                viewModel.clearCreatedTicketId()
            }
        }
    }

    private fun renderTickets() {
        val filtered = if (currentFilter == "all") {
            allTickets
        } else {
            allTickets.filter { it.status == currentFilter }
        }
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvTickets.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(filtered, selectedTicketId)
    }

    private fun showCreateTicketDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_support_ticket, null)
        val spCategory = view.findViewById<Spinner>(R.id.spSupportCategory)
        val etTitle = view.findViewById<EditText>(R.id.etSupportTitle)
        val etContent = view.findViewById<EditText>(R.id.etSupportContent)

        AlertDialog.Builder(this)
            .setTitle("Tạo yêu cầu hỗ trợ")
            .setView(view)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Gửi", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = etTitle.text.toString().trim()
                        val content = etContent.text.toString().trim()
                        val category = spCategory.selectedItem?.toString() ?: "Khác"
                        when {
                            title.length < 5 -> etTitle.error = "Tiêu đề tối thiểu 5 ký tự"
                            content.length < 10 -> etContent.error = "Nội dung tối thiểu 10 ký tự"
                            else -> {
                                val loading = MessageUtils.showLoadingDialog(
                                    context = this@SupportTicketsActivity,
                                    title = "Đang gửi hỗ trợ",
                                    message = "Vui lòng chờ trong giây lát."
                                )
                                viewModel.createTicket(
                                    category,
                                    title,
                                    content,
                                    null,
                                    onSuccess = {
                                        loading.dismiss()
                                        dismiss()
                                    },
                                    onFailure = {
                                        loading.dismiss()
                                        MessageUtils.showErrorDialog(this@SupportTicketsActivity, "Lỗi", it)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            .show()
    }
}
