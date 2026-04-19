package com.example.doantotnghiep.Utils

import android.content.Context
import androidx.appcompat.app.AlertDialog

object MessageUtils {

    fun showSuccessDialog(context: Context, title: String, message: String, onConfirm: (() -> Unit)? = null) {
        AlertDialog.Builder(context)
            .setTitle("✅ $title")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Xác nhận") { dialog, _ ->
                dialog.dismiss()
                onConfirm?.invoke()
            }
            .show()
    }

    fun showErrorDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle("❌ $title")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .show()
    }

    fun showInfoDialog(context: Context, title: String, message: String, onConfirm: (() -> Unit)? = null) {
        AlertDialog.Builder(context)
            .setTitle("ℹ️ $title")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onConfirm?.invoke()
            }
            .show()
    }

    fun showLoadingDialog(context: Context, message: String): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
        return dialog
    }
}
