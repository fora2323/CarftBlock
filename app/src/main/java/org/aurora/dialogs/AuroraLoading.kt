package org.aurora.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

import com.google.android.material.progressindicator.CircularProgressIndicator

import org.aurora.dialogs.AuroraLoading

import com.aurora.R

class AuroraLoading : DialogFragment() {

    private var progressIndicator: CircularProgressIndicator? = null

    override fun getTheme(): Int {
        return R.style.Base_Theme_AuroraDialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressIndicator = view.findViewById(R.id.progressIndicator)
        isCancelable = false
    }

    fun updateProgress(progress: Int) {
        activity?.runOnUiThread {
            progressIndicator?.apply {
                if (isIndeterminate) {
                    isIndeterminate = false
                }
                setProgress(progress, true)
            }
        }
    }

    fun setIndeterminate(isIndeterminate: Boolean) {
        activity?.runOnUiThread {
            progressIndicator?.isIndeterminate = isIndeterminate
        }
    }

    fun showDialog(fragmentManager: FragmentManager, tag: String = "AuroraLoading") {
        if (!isAdded && !fragmentManager.isStateSaved) {
            show(fragmentManager, tag)
        }
    }

    fun dismissDialog() {
        if (isAdded) {
            dismissAllowingStateLoss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): AuroraLoading {
            return AuroraLoading()
        }
    }
}