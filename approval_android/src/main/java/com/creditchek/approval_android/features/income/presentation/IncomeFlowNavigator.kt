package com.creditchek.approval_android.features.income.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.SessionResult
import com.creditchek.approval_android.core.shared.components.ComingSoonScreen
import com.creditchek.approval_android.core.theme.DynamicThemeConfig
import com.creditchek.approval_android.core.session.SharedSessionContext

@Composable
fun IncomeFlowNavigator(
    config: ApprovalConfig,
    context: SharedSessionContext,
    onThemeLoaded: (DynamicThemeConfig) -> Unit,
    onComplete: (SessionResult) -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler {
        onCancel()
    }

    ComingSoonScreen(
        moduleName = "Income Verification",
        description = "Income verification is coming soon. Tap below to continue to the next step.",
        isDevelopment = context.environment.isDevelopment,
        nextButtonLabel = "Next Module",
        onNext = {
            onComplete(SessionResult.Success(context.sessionId))
        },
        onDismiss = onCancel
    )
}