package io.stipop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.stipop.api.StipopApi
import io.stipop.custom.StipopImageView
import io.stipop.data.ConfigRepository
import io.stipop.delegate.SPComponentLifeCycleDelegate
import io.stipop.delegate.StipopDelegate
import io.stipop.delegate.StipopKeyboardHeightDelegate
import io.stipop.event.SAuthDelegate
import io.stipop.models.SPSticker
import io.stipop.models.body.InitSdkBody
import io.stipop.models.body.TrackErrorBody
import io.stipop.models.enums.StipopApiEnum
import io.stipop.s_auth.SAuthManager
import io.stipop.s_auth.TrackUsingStickerEnum
import io.stipop.view.PackDetailFragment
import io.stipop.view.StickerSearchView
import io.stipop.view.pickerview.StickerPickerFragment
import io.stipop.view.pickerview.StickerPickerPopupView
import io.stipop.view.pickerview.StickerPickerViewClass
import io.stipop.view.pickerview.listener.VisibleStateListener
import io.stipop.view.viewmodel.*
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*


class Stipop(
    private val activity: FragmentActivity,
    private var stipopButton: StipopImageView? = null,
    val delegate: StipopDelegate
) : VisibleStateListener {

    companion object {

        private lateinit var rootView: View

        internal var stipopDelegate: StipopDelegate? = null
        internal var sAuthDelegate: SAuthDelegate? = null
        internal var keyboardHeightDelegate: StipopKeyboardHeightDelegate? = null
        internal var spComponentLifeCycleDelegate: SPComponentLifeCycleDelegate? = null

        internal val mainScope = CoroutineScope(Job() + Dispatchers.Main)

        internal var configRepository: ConfigRepository = ConfigRepository()

        internal var stickerPickerViewClass: StickerPickerViewClass? = null
        internal var stickerPickerPopupView: StickerPickerPopupView? = null
        internal var stickerPickerFragment: StickerPickerFragment? = null
        internal var stickerPickerViewModel: StickerPickerViewModel? = null
        internal var storeHomeViewModel: StoreHomeViewModel? = null
        internal var storeMyStickerViewModel: StoreMyStickerViewModel? = null
        internal var storeNewsViewModel: StoreNewsViewModel? = null
        internal var packDetailViewModel: PackDetailViewModel? = null

        internal lateinit var applicationContext: Context

        @SuppressLint("StaticFieldLeak")
        internal var instance: Stipop? = null

        var userId = "-1"
            private set

        var lang = "en"
            private set

        var countryCode = "US"
            private set

        internal var currentPickerViewHeight = 0
            private set

        internal var fromTopToVisibleFramePx = 0
            private set

        private var inputMode: WindowSoftInputModeAdjustEnum? = null

        private var canRetryIfConnectFailed = true

        private var pickerViewPopupWindowYValue = -1
        private var pickerViewPopupWindowHeight = -1

        fun setAccessToken(accessToken: String) {
            SAuthManager.setAccessToken(accessToken)
        }

        fun configure(context: Context, sAuthDelegate: SAuthDelegate? = null, callback: ((isSuccess: Boolean) -> Unit)? = null) {
            sAuthDelegate?.let {
                this.sAuthDelegate = it
            }
            Config.configure(context, callback = { result ->
                mainScope.launch {
                    configRepository.isConfigured = result
                    callback?.let { callback -> callback(result) }
                }
            })
        }

        fun configureWithConfigFileName(context: Context, sAuthDelegate: SAuthDelegate? = null, configFileName: String, callback: ((isSuccess: Boolean) -> Unit)? = null) {
            sAuthDelegate?.let {
                this.sAuthDelegate = it
            }
            Config.configure(context,
                configFileName = configFileName,
                callback = { result ->
                    mainScope.launch {
                        configRepository.isConfigured = result
                        callback?.let { callback -> callback(result) }
                    }
                })
        }

        fun connect(
            activity: FragmentActivity,
            userId: String,
            delegate: StipopDelegate,
            stipopButton: StipopImageView? = null,
            stickerPickerFragment: StickerPickerFragment? = null,
            locale: Locale = Locale.getDefault(),
            taskCallBack: ((isSuccess: Boolean) -> Unit)? = null
        ) {
            Stipop.userId = userId
            StipopUtils.controlLocale(locale).let {
                lang = it.language
                countryCode = it.country
            }
            if (!configRepository.isConfigured) {
                if (canRetryIfConnectFailed) {
                    Log.w("STIPOP-SDK", "Stipop SDK not connected. Because 'canRetryIfConnectFailed' is True, SDK calls 'configure(context)' automatically just once.")
                    configure(activity, callback = {
                        if (it) connect(activity, userId, delegate, stipopButton, stickerPickerFragment, locale, taskCallBack)
                        canRetryIfConnectFailed = false
                    })
                } else {
                    Log.e("STIPOP-SDK", "Stipop SDK connect failed. Please call Stipop.configure(context) first.")
                    taskCallBack?.let {
                        it(false)
                    }
                }
            } else {
                Log.v("STIPOP-SDK", "Stipop SDK connect succeeded. You can use SDK by calling Stipop.show() or Stipop.showSearch() and implementing StipopDelegate interface.")
                connectSuccessInit(activity, stickerPickerFragment, stipopButton, delegate, taskCallBack)
            }
        }

        private fun connectSuccessInit(
            activity: FragmentActivity,
            stickerPickerFragment: StickerPickerFragment?,
            stipopButton: StipopImageView?,
            stipopDelegate: StipopDelegate,
            taskCallBack: ((isSuccess: Boolean) -> Unit)?
        ) {
            applicationContext = activity.applicationContext
            this.stipopDelegate = stipopDelegate
            mainScope.launch {
                configRepository.postConfigSdk()
                postInitSDK()
                try {
                    Stipop(activity, stipopButton, stipopDelegate).apply {
                        initPickerView(this, stickerPickerFragment)
                        connectIcon()
                    }.run {
                        instance = this
                        taskCallBack?.let { it(true) }
                    }
                } catch (exception: Exception) {
                    trackError(exception)
                }
            }
        }

        suspend fun postInitSDK() {
            try {
                configRepository.postInitSdk(
                    initSdkBody = InitSdkBody(
                        userId = userId,
                        lang = lang
                    )
                )
            } catch (exception: HttpException) {
                when (exception.code()) {
                    401 -> sAuthDelegate?.httpException(StipopApiEnum.INIT_SDK, exception)
                }
            } catch (exception: Exception) {
                trackError(exception)
            }
        }

        private fun initPickerView(stipop: Stipop, stickerPickerFragment: StickerPickerFragment? = null) {
            stipop.apply {
                when (Config.getViewPickerViewType()) {
                    ViewPickerViewType.POPUP_WINDOW -> {
                        stickerPickerPopupView = StickerPickerPopupView(activity)
                        stickerPickerPopupView?.setDelegate(this)
                        setSpvHeight()
                    }
                    ViewPickerViewType.FRAGMENT -> {
                        Stipop.stickerPickerFragment = stickerPickerFragment
                        Stipop.stickerPickerFragment?.setDelegate(this)
                    }
                }
            }
        }

        /**
         * Use When Sticker Picker View Height Modifying is needed.
         */
        fun setKeyboardAdditionalHeightOffset(height: Int) {
            instance?.let {
                instance!!.spvAdditionalHeightOffset = height
            } ?: kotlin.run {
                throw InstantiationException("Stipop Instance Not Connected. Please call this method in callback of Stipop.connect(), or when Stipop.connect() is completed.")
            }
        }

        fun showSearch() = instance?.showSearch()

        fun show() = instance?.showAndHidePickerView()

        fun hide() = instance?.hidePickerView()

        fun showStickerPackage(fragmentManager: FragmentManager, packageId: Int) {
            instance?.showStickerPackage(fragmentManager, packageId)
        }

        internal fun send(
            trackUsingStickerEnum: TrackUsingStickerEnum,
            sticker: SPSticker,
            entrancePoint: String,
            completionHandler: (result: Boolean) -> Unit
        ) {
            mainScope.launch {
                try {
                    configRepository.postTrackUsingSticker(
                        stickerId = sticker.stickerId.toString(),
                        userId = userId,
                        query = sticker.keyword,
                        countryCode = countryCode,
                        lang = lang,
                        eventPoint = entrancePoint,
                        onSuccess = {
                            if (it.header.isSuccess()) {
                                completionHandler(true)
                            } else {
                                completionHandler(false)
                            }
                        })
                } catch (exception: HttpException) {
                    when (exception.code()) {
                        401 -> {
                            completionHandler(false)
                            SAuthManager.setPostTrackUsingStickerData(trackUsingStickerEnum, sticker)
                            sAuthDelegate?.httpException(StipopApiEnum.TRACK_USING_STICKER, exception)
                        }
                    }
                } catch (exception: Exception) {
                    trackError(exception)
                }
            }
        }

        fun setKeyboardHeightDelegate(keyboardHeightDelegate: StipopKeyboardHeightDelegate?) {
            Stipop.keyboardHeightDelegate = keyboardHeightDelegate
        }

        fun releaseDelegates() {
            setKeyboardHeightDelegate(null)
            Stipop.stipopDelegate = null
            Stipop.spComponentLifeCycleDelegate = null
            stickerPickerViewClass = null
            Stipop.stickerPickerPopupView = null
            Stipop.stickerPickerFragment = null
        }

        internal fun trackError(exception: Exception) {
            if (StipopUtils.getCurrentNetworkStatus()) {
                val stringWriter = StringWriter()
                exception.printStackTrace(PrintWriter(stringWriter))
                val exceptionAsString: String = stringWriter.toString()

                GlobalScope.launch {
                    try {
                        val response = StipopApi.create().trackError(userId = Stipop.userId, trackErrorBody = TrackErrorBody(exceptionAsString))
                        if (response.code() == 401) {
                            SAuthManager.setTrackErrorData(exception)
                            Stipop.sAuthDelegate?.httpException(StipopApiEnum.TRACK_ERROR, HttpException((response)))
                        }
                    } catch (exception: Exception) {
                        trackError(exception)
                    }
                }
            }
        }

        fun setCustomPopupWindowYAndHeightValue(y: Int, height: Int) {
            this.pickerViewPopupWindowYValue = y
            this.pickerViewPopupWindowHeight = height
        }

        fun setComponentLifeCycleDelegate(delegate: SPComponentLifeCycleDelegate) {
            spComponentLifeCycleDelegate = delegate
        }
    }

    private var spvAdditionalHeightOffset = 0

    private fun connectIcon() {
        stipopButton?.setImageResource(Config.getStickerIconResourceId(activity))
        stipopButton?.setIconDefaultsColor()
    }

    private fun enableStickerIcon() {
        stipopButton?.setTint()
    }

    private fun disableStickerIcon() {
        stipopButton?.clearTint()
    }

    /*
    private fun setInputMode(){
        if(inputMode == null) {
            val inputValue = activity.window.attributes.softInputMode
            inputMode = WindowSoftInputModeUtils().isInputSoftModeNothing(inputValue)
        }
    }
     */

    private fun setSpvHeight() {
        rootView = activity.window.decorView.findViewById(android.R.id.content) as View

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val fullSizeHeight = StipopUtils.getScreenHeight(activity)
            val visibleFrameRect = Rect()
            rootView.getWindowVisibleDisplayFrame(visibleFrameRect)
            fromTopToVisibleFramePx = visibleFrameRect.bottom

            val heightDifference =
                fullSizeHeight - fromTopToVisibleFramePx + spvAdditionalHeightOffset
            SPLogger.log("${heightDifference} > ${StipopUtils.pxToDp(100)}")
            if (heightDifference > StipopUtils.pxToDp(100)) {
                currentPickerViewHeight = heightDifference
                stickerPickerPopupView.let { spv ->
                    spv?.height = currentPickerViewHeight
                    Log.e("Stipop Log", "setSpvHeight = ${heightDifference}")
                    if ((spv?.wantShowing ?: false) && !(spv?.isShowing ?: false)) {
                        spv?.show(fromTopToVisibleFramePx)
                    }
                }
            } else {
                currentPickerViewHeight = 0
                Stipop.stickerPickerViewClass?.dismiss()
            }
        }
    }

    private fun getStickerPickerKeyboardViewHeightShow(fromTopToVisibleFramePx: Int, keyboardHeight: Int) {
        try {
            Stipop.fromTopToVisibleFramePx = fromTopToVisibleFramePx

            if (keyboardHeight > StipopUtils.pxToDp(100)) {
                currentPickerViewHeight = keyboardHeight
                stickerPickerPopupView.let { spv ->
                    spv?.let {
                        it.height = currentPickerViewHeight
                        if (it.wantShowing && !it.isShowing) {
                            it.show(fromTopToVisibleFramePx)
                        }
                    }
                }
            } else {
                currentPickerViewHeight = 0
                Stipop.stickerPickerViewClass?.dismiss()
            }
        } catch (exception: Exception) {
            trackError(exception)
        }
    }

    private fun setKeyboardHeightDelegateValue(keyboardHeight: Int) {
        try {
            if (keyboardHeight > StipopUtils.pxToDp(100)) {
                keyboardHeightDelegate?.onHeightChanged(currentPickerViewHeight)
            } else {
                keyboardHeightDelegate?.onHeightChanged(0)
            }
        } catch (exception: Exception) {
            trackError(exception)
        }
    }

    private fun showSearch() {
        mainScope.launch {
            if (Config.isPickerViewPopupWindow()) {
                Stipop.stickerPickerViewClass?.dismiss()
            }
            StickerSearchView.newInstance().show(activity.supportFragmentManager, Constants.Tag.SSV)
        }
    }

    private fun showAndHidePickerView() {
        mainScope.launch {
            try {
                when (Config.getViewPickerViewType()) {
                    ViewPickerViewType.POPUP_WINDOW -> {
                        when (Config.pickerViewLayoutOnKeyboard) {
                            true -> showAndHidePopupPickerKeyboardView()
                            false -> showAndHidePopupPickerCustomView()
                        }
                    }
                    ViewPickerViewType.FRAGMENT -> showAndHideFragmentPickerCustomView()
                }
            } catch (exception: Exception) {
                trackError(exception)
            }
        }
    }

    private fun showAndHidePopupPickerKeyboardView(isCurrentShowing: Boolean? = null) {
        val isCurrentShowing = isCurrentShowing ?: stickerPickerPopupView?.isShowing
        when (isCurrentShowing) {
            true -> Stipop.stickerPickerViewClass?.dismiss()
            false -> showPopupPickerKeyboardView()
        }
    }

    private fun showPopupPickerKeyboardView() {
        if (Stipop.stickerPickerPopupView?.isShowing ?: false) {
            Stipop.stickerPickerViewClass?.dismiss()
        } else {
            stickerPickerPopupView?.wantShowing = true
            if (currentPickerViewHeight == 0) {
                StipopUtils.showKeyboard(instance!!.activity)
            }
            stickerPickerPopupView?.show(fromTopToVisibleFramePx)
        }
    }

    private fun showAndHidePopupPickerCustomView(isCurrentShowing: Boolean? = null) {
        val isCurrentShowing = isCurrentShowing ?: stickerPickerPopupView?.isShowing
        when (isCurrentShowing) {
            true -> Stipop.stickerPickerViewClass?.dismiss()
            false -> showPopupPickerCustomView()
        }
    }

    private fun showPopupPickerCustomView() {
        SPLogger.log("showPopupPickerView -1 => 3")
        stickerPickerPopupView?.wantShowing = true
        getStickerPickerKeyboardViewHeightShow(pickerViewPopupWindowYValue, pickerViewPopupWindowHeight)
        stickerPickerPopupView?.show(fromTopToVisibleFramePx)
    }

    internal fun showAndHideFragmentPickerCustomView(isCurrentShowing: Boolean? = null) {
        val isCurrentShowing = isCurrentShowing ?: stickerPickerFragment?.isShowing()
        SPLogger.log("isCurrentShowing = ${isCurrentShowing}")
        when (isCurrentShowing) {
            true -> Stipop.stickerPickerViewClass?.dismiss()
            false -> Stipop.stickerPickerViewClass?.show()
        }
    }

    private fun hidePickerView() {
        try {
            Stipop.stickerPickerViewClass?.dismiss()
        } catch (exception: Exception) {
            trackError(exception)
        }
    }

    private fun showStickerPackage(fragmentManager: FragmentManager, packageId: Int) {
        StipopUtils.hideKeyboard(activity)
        PackDetailFragment.newInstance(packageId, Constants.Point.EXTERNAL).show(fragmentManager, Constants.Tag.EXTERNAL)
    }

    override fun onSpvVisibleState(isVisible: Boolean) {
        try {
            when (isVisible) {
                true -> {
                    enableStickerIcon()
                }
                false -> {
                    disableStickerIcon()
                }
            }
        } catch (exception: Exception) {
            trackError(exception)
        }
    }
}