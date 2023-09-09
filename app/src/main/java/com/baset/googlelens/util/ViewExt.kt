package com.baset.googlelens.util

import android.view.View
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

fun CoordinatorLayout.showSnackBar(
    message: String? = null,
    @StringRes stringRes: Int? = null,
    duration: Int = Snackbar.LENGTH_SHORT,
    @StringRes
    actionStringRes: Int? = null,
    actionText: String? = null,
    actionClickListener: View.OnClickListener? = null
) {
    val safeMessage = when {
        message != null -> message
        stringRes != null -> context.getString(stringRes)
        else -> return
    }

    val safeActionText = when {
        actionStringRes != null -> context.getString(actionStringRes)
        actionText != null -> actionText
        else -> null
    }

    val snackBar = Snackbar.make(this, safeMessage, duration)
    if (actionText != null && actionClickListener != null) {
        snackBar.setAction(safeActionText) { actionClickListener.onClick(it) }
    }
    snackBar.show()
}