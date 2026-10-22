package one.only.player

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import one.only.player.core.common.AppThemeModeManager
import one.only.player.core.common.Logger
import one.only.player.core.common.PredictiveBackSupport
import one.only.player.crash.CRASH_PROCESS_SUFFIX
import one.only.player.crash.GlobalExceptionHandler
import one.only.player.crash.StartupRecovery
import one.only.player.crash.StartupStage

@HiltAndroidApp
class OnlyPlayerApplication :
    Application(),
    SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: Lazy<ImageLoader>

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (Application.getProcessName().endsWith(CRASH_PROCESS_SUFFIX)) return
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(this))
        StartupRecovery.begin(this, StartupStage.APPLICATION)
        Logger.initialize(this)
    }

    override fun onCreate() {
        // 崩溃进程不初始化 Hilt 业务图，避免恢复页依赖主业务启动。
        if (Application.getProcessName().endsWith(CRASH_PROCESS_SUFFIX)) return
        super.onCreate()
        AppForegroundTracker.register(this)
        val startupPreferences = StartupPreferencesCache.initialize(context = this)
        AppThemeModeManager.applyPlatformToCurrent(
            context = applicationContext,
            mode = startupPreferences.themeConfig.toAppThemeMode(),
        )
        PredictiveBackSupport.setEnabled(
            applicationInfo = applicationInfo,
            isEnabled = startupPreferences.shouldEnablePredictiveBack,
        )
        // 后台组件也会启动应用，初始化完成后不等待 Activity 首帧。
        StartupRecovery.markReady()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()
}
