package org.aurora.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.aurora.R

class AuroraLoading : DialogFragment() {

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
        isCancelable = false
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