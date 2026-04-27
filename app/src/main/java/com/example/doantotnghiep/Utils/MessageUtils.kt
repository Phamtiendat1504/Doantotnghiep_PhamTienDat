package com.example.doantotnghiep.Utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.doantotnghiep.R

object MessageUtils {

    private enum class DialogType {
        SUCCESS,
        ERROR,
        INFO
    }

    fun showSuccessDialog(
        context: Context,
        title: String,
        message: String,
        onConfirm: (() -> Unit)? = null
    ) {
        val dialog = createStateDialogBuilder(
            context = context,
            type = DialogType.SUCCESS,
            title = title,
            message = message
        )
            .setCancelable(false)
            .setPositiveButton("X\u00e1c nh\u1eadn") { _, _ -> onConfirm?.invoke() }
            .create()

        dialog.show()
        applyDialogWindowStyle(dialog, context, useTransparentBackground = false)
    }

    fun showErrorDialog(context: Context, title: String, message: String) {
        val dialog = createStateDialogBuilder(
            context = context,
            type = DialogType.ERROR,
            title = title,
            message = message
        )
            .setPositiveButton("\u0110\u00f3ng", null)
            .create()

        dialog.show()
        applyDialogWindowStyle(dialog, context, useTransparentBackground = false)
    }

    fun showInfoDialog(
        context: Context,
        title: String,
        message: String,
        onConfirm: (() -> Unit)? = null
    ) {
        val dialog = createStateDialogBuilder(
            context = context,
            type = DialogType.INFO,
            title = title,
            message = message
        )
            .setPositiveButton("OK") { _, _ -> onConfirm?.invoke() }
            .create()

        dialog.show()
        applyDialogWindowStyle(dialog, context, useTransparentBackground = false)
    }

    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "Xác nhận",
        negativeText: String = "Hủy",
        onConfirm: () -> Unit
    ) {
        val dialog = createStateDialogBuilder(
            context = context,
            type = DialogType.INFO,
            title = title,
            message = message
        )
            .setNegativeButton(negativeText, null)
            .setPositiveButton(positiveText) { _, _ -> onConfirm.invoke() }
            .create()

        dialog.show()
        applyDialogWindowStyle(dialog, context, useTransparentBackground = false)
    }

    fun showLoadingDialog(
        context: Context,
        message: String,
        title: String = "\u0110ang x\u1eed l\u00fd"
    ): AlertDialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_loading_state, null)
        dialogView.findViewById<TextView>(R.id.tvLoadingTitle).text = title
        dialogView.findViewById<TextView>(R.id.tvLoadingMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        applyDialogWindowStyle(dialog, context, useTransparentBackground = true)
        return dialog
    }

    private fun createStateDialogBuilder(
        context: Context,
        type: DialogType,
        title: String,
        message: String
    ): AlertDialog.Builder {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_message_state, null)
        val iconContainer = dialogView.findViewById<FrameLayout>(R.id.iconContainer)
        val iconView = dialogView.findViewById<ImageView>(R.id.imgStateIcon)
        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = title
        dialogView.findViewById<TextView>(R.id.tvDialogMessage).text = message

        when (type) {
            DialogType.SUCCESS -> {
                iconContainer.setBackgroundResource(R.drawable.bg_success_circle)
                iconView.setImageResource(R.drawable.ic_check_circle)
                iconView.imageTintList = null
            }
            DialogType.ERROR -> {
                iconContainer.setBackgroundResource(R.drawable.bg_dialog_icon_error)
                iconView.setImageResource(R.drawable.ic_cancel)
                iconView.imageTintList = null
            }
            DialogType.INFO -> {
                iconContainer.setBackgroundResource(R.drawable.bg_icon_circle_light_blue)
                iconView.setImageResource(R.drawable.ic_info)
                iconView.setColorFilter(ContextCompat.getColor(context, R.color.primary))
            }
        }

        return AlertDialog.Builder(context).setView(dialogView)
    }

    private fun applyDialogWindowStyle(
        dialog: AlertDialog,
        context: Context,
        useTransparentBackground: Boolean
    ) {
        dialog.window?.apply {
            if (useTransparentBackground) {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            } else {
                setBackgroundDrawableResource(R.drawable.bg_dialog_surface)
            }
            val width = (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
