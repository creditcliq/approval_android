package com.creditchek.approval_android.core.shared.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.R
import com.creditchek.approval_android.core.theme.ApprovalBlue
import com.creditchek.approval_android.core.theme.ApprovalTextSecondary

@Composable
fun PoweredByCreditChek(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Powered by ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = ApprovalTextSecondary
        )
        Spacer(modifier = Modifier.size(4.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(80.dp)
        )
//        Text(
//            text = "CreditChek",
//            fontSize = 13.sp,
//            fontWeight = FontWeight.Bold,
//            color = ApprovalBlue
//        )
    }
}