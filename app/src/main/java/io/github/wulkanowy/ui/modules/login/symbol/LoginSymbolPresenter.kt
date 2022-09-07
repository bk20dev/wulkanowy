package io.github.wulkanowy.ui.modules.login.symbol

import io.github.wulkanowy.data.Resource
import io.github.wulkanowy.data.onResourceNotLoading
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.data.resourceFlow
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.modules.login.LoginData
import io.github.wulkanowy.ui.modules.login.LoginErrorHandler
import io.github.wulkanowy.utils.AnalyticsHelper
import io.github.wulkanowy.utils.ifNullOrBlank
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

class LoginSymbolPresenter @Inject constructor(
    studentRepository: StudentRepository,
    private val loginErrorHandler: LoginErrorHandler,
    private val analytics: AnalyticsHelper
) : BasePresenter<LoginSymbolView>(loginErrorHandler, studentRepository) {

    private var lastError: Throwable? = null

    lateinit var loginData: LoginData

    fun onAttachView(view: LoginSymbolView, loginData: LoginData) {
        super.onAttachView(view)
        this.loginData = loginData
        with(view) {
            initView()
            showContact(false)
            setLoginToHeading(loginData.login)
            clearAndFocusSymbol()
            showSoftKeyboard()
        }
    }

    fun onSymbolTextChanged() {
        view?.apply { if (symbolNameError != null) clearSymbolError() }
    }

    fun attemptLogin(symbol: String) {
        if (symbol.isBlank()) {
            view?.setErrorSymbolRequire()
            return
        }

        resourceFlow {
            studentRepository.getStudentsScrapper(
                email = loginData.login,
                password = loginData.password,
                scrapperBaseUrl = loginData.baseUrl,
                symbol = symbol,
            )
        }.onEach {
            when (it) {
                is Resource.Loading -> view?.run {
                    Timber.i("Login with symbol started")
                    hideSoftKeyboard()
                    showProgress(true)
                    showContent(false)
                }
                is Resource.Success -> {
                    when (it.data.size) {
                        0 -> {
                            Timber.i("Login with symbol result: Empty student list")
                            view?.run {
                                setErrorSymbolIncorrect()
                                showContact(true)
                            }
                        }
                        else -> {
                            Timber.i("Login with symbol result: Success")
                            view?.navigateToStudentSelect(requireNotNull(it.data))
                        }
                    }
                    analytics.logEvent(
                        "registration_symbol",
                        "success" to true,
                        "students" to it.data.size,
                        "scrapperBaseUrl" to loginData.baseUrl,
                        "symbol" to symbol,
                        "error" to "No error"
                    )
                }
                is Resource.Error -> {
                    Timber.i("Login with symbol result: An exception occurred")
                    analytics.logEvent(
                        "registration_symbol",
                        "success" to false,
                        "students" to -1,
                        "scrapperBaseUrl" to loginData.baseUrl,
                        "symbol" to symbol,
                        "error" to it.error.message.ifNullOrBlank { "No message" }
                    )
                    loginErrorHandler.dispatch(it.error)
                    lastError = it.error
                    view?.showContact(true)
                }
            }
        }.onResourceNotLoading {
            view?.apply {
                showProgress(false)
                showContent(true)
            }
        }.launch("login")
    }

    fun onFaqClick() {
        view?.openFaqPage()
    }

    fun onEmailClick() {
        view?.openEmail(loginData.baseUrl, lastError?.message.ifNullOrBlank { "empty" })
    }
}
