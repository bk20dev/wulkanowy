package io.github.wulkanowy.ui.base

import android.app.ActivityManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import io.github.wulkanowy.R
import io.github.wulkanowy.ui.modules.auth.AuthDialog
import io.github.wulkanowy.ui.modules.login.LoginActivity
import io.github.wulkanowy.utils.FragmentLifecycleLogger
import io.github.wulkanowy.utils.getThemeAttrColor
import io.github.wulkanowy.utils.lifecycleAwareVariable
import io.github.wulkanowy.utils.openInternetBrowser
import javax.inject.Inject

abstract class BaseActivity<T : BasePresenter<out BaseView>, VB : ViewBinding> :
    AppCompatActivity(), BaseView {

    protected var binding: VB by lifecycleAwareVariable()

    @Inject
    lateinit var fragmentLifecycleLogger: FragmentLifecycleLogger

    @Inject
    lateinit var themeManager: ThemeManager

    protected var messageContainer: View? = null

    protected var messageAnchor: View? = null

    abstract var presenter: T

    override fun onCreate(savedInstanceState: Bundle?) {
        inject()
        themeManager.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleLogger, true)

        @Suppress("DEPRECATION")
        setTaskDescription(
            ActivityManager.TaskDescription(null, null, getThemeAttrColor(R.attr.colorSurface))
        )
    }

    override fun showError(text: String, error: Throwable) {
        if (messageContainer != null) {
            Snackbar.make(messageContainer!!, text, LENGTH_LONG)
                .setAction(R.string.all_details) { showErrorDetailsDialog(error) }
                .apply { messageAnchor?.let { anchorView = it } }
                .show()
        } else showMessage(text)
    }

    override fun showErrorDetailsDialog(error: Throwable) {
        ErrorDialog.newInstance(error).show(supportFragmentManager, error.toString())
    }

    override fun showMessage(text: String) {
        if (messageContainer != null) {
            Snackbar.make(messageContainer!!, text, LENGTH_LONG)
                .apply { messageAnchor?.let { anchorView = it } }
                .show()
        } else Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    override fun showExpiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.main_session_expired)
            .setMessage(R.string.main_session_relogin)
            .setPositiveButton(R.string.main_log_in) { _, _ -> presenter.onExpiredLoginSelected() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    override fun showAuthDialog() {
        AuthDialog.newInstance().show(supportFragmentManager, "auth_dialog")
    }

    override fun showChangePasswordSnackbar(redirectUrl: String) {
        messageContainer?.let {
            Snackbar.make(it, R.string.error_password_change_required, LENGTH_LONG)
                .setAction(R.string.all_change) { openInternetBrowser(redirectUrl) }
                .apply { messageAnchor?.let { anchorView = it } }
                .show()
        }
    }

    override fun openClearLoginView() {
        startActivity(LoginActivity.getStartIntent(this))
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        invalidateOptionsMenu()
        presenter.onDetachView()
    }

    //https://github.com/google/dagger/releases/tag/dagger-2.33
    protected open fun inject() {
        throw UnsupportedOperationException()
    }
}
