package com.creditchek.approval_android.core.session

import com.creditchek.approval_android.core.theme.DynamicThemeConfig
import com.creditchek.approval_android.core.theme.parseHexColor
import com.creditchek.approval_android.features.identity.data.IdentityRepository

suspend fun loadTheme(
    repository: IdentityRepository,
    widgetId: String,
): DynamicThemeConfig {
    return repository.getConfigData(widgetId).fold(
        onSuccess = { configData ->
            DynamicThemeConfig(
                primaryColor = parseHexColor(configData.color),
                businessName = configData.displayName?.takeIf { it.isNotBlank() }
                    ?: configData.appId.appName
                    ?: "CreditChek",
                logoUrl = configData.logo
            )
        },
        onFailure = {
            DynamicThemeConfig()
        }
    )
}
