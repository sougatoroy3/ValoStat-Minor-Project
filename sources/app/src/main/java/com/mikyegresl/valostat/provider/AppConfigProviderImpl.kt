package com.mikyegresl.valostat.provider

import com.mikyegresl.valostat.BuildConfig
import com.mikyegresl.valostat.base.config.AppConfigProvider
object AppConfigProviderImpl : AppConfigProvider {

    override val versionCode: Int =
        BuildConfig.VERSION_CODE

    override val versionName: String =
        BuildConfig.VERSION_NAME

    override val isDebug: Boolean =
        BuildConfig.DEBUG
}