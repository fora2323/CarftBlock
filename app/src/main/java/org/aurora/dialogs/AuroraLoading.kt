package org.aurora.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.aurora.R
import com.google.android.material.loadingindicator.LoadingIndicator

class AuroraLoading : DialogFragment() {

    private var loadingIndicator: LoadingIndicator? = null

    override fun getTheme(): Int {
        return R.style.Base_Theme_AuroraDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        isCancelable = false
    }

    fun updateProgress(progress: Int) {
        activity?.runOnUiThread {
            loadingIndicator?.apply {
                if (isIndeterminate) {
                    isIndeterminate = false
                }
                setProgress(progress, true)
            }
        }
    }

    fun setIndeterminate(isIndeterminate: Boolean) {
        activity?.runOnUiThread {
            loadingIndicator?.isIndeterminate = isIndeterminate
        }
    }

    @JvmOverloads
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