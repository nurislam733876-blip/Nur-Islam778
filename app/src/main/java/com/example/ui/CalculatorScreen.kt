package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.VaultViewModel

// Premium themed colors for the Calculator Decoy
val CalcBg = Color(0xFF13131A)
val CalcBtnNumber = Color(0xFF2C2C35)
val CalcBtnNumberText = Color.White
val CalcBtnOperator = Color(0xFFFF9F0A)
val CalcBtnOperatorText = Color.White
val CalcBtnFunction = Color(0xFF4E505F)
val CalcBtnFunctionText = Color.White

@Composable
fun CalculatorScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputState by viewModel.calcInput.collectAsState()
    val explanationState by viewModel.statusMessage.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalcBg)
            .padding(20.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Secure title/explanation banner at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .background(Color.White.copy(alpha = 0.04f), shape = CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = explanationState,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        
        // Calculator Math View Display
        Text(
            text = inputState,
            color = Color.White,
            fontSize = 62.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 20.dp)
                .testTag("calculator_display")
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Standard button layout
        val buttons = listOf(
            BtnUnit("AC", BtnType.Function), BtnUnit("C", BtnType.Function), BtnUnit("%", BtnType.Function), BtnUnit("÷", BtnType.Operator),
            BtnUnit("7", BtnType.Number), BtnUnit("8", BtnType.Number), BtnUnit("9", BtnType.Number), BtnUnit("×", BtnType.Operator),
            BtnUnit("4", BtnType.Number), BtnUnit("5", BtnType.Number), BtnUnit("6", BtnType.Number), BtnUnit("-", BtnType.Operator),
            BtnUnit("1", BtnType.Number), BtnUnit("2", BtnType.Number), BtnUnit("3", BtnType.Number), BtnUnit("+", BtnType.Operator),
            BtnUnit("0", BtnType.Number, true), BtnUnit(".", BtnType.Number), BtnUnit("=", BtnType.Operator)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                count = buttons.size,
                span = { index ->
                    val btn = buttons[index]
                    androidx.compose.foundation.lazy.grid.GridItemSpan(if (btn.isDoubleWidth) 2 else 1)
                }
            ) { index ->
                val btn = buttons[index]
                CalculatorButton(
                    unit = btn,
                    onClick = {
                        when (btn.text) {
                            "AC" -> viewModel.onAllClearPressed()
                            "C" -> viewModel.onClearPressed()
                            "%" -> viewModel.onOperatorPressed("%")
                            "÷" -> viewModel.onOperatorPressed("÷")
                            "×" -> viewModel.onOperatorPressed("×")
                            "-" -> viewModel.onOperatorPressed("-")
                            "+" -> viewModel.onOperatorPressed("+")
                            "." -> viewModel.onDotPressed()
                            "=" -> viewModel.onEqualsPressed(context)
                            else -> viewModel.onDigitPressed(btn.text)
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

enum class BtnType { Number, Operator, Function }
data class BtnUnit(
    val text: String,
    val type: BtnType,
    val isDoubleWidth: Boolean = false
)

@Composable
fun CalculatorButton(
    unit: BtnUnit,
    onClick: () -> Unit
) {
    val bgColor = when (unit.type) {
        BtnType.Number -> CalcBtnNumber
        BtnType.Operator -> CalcBtnOperator
        BtnType.Function -> CalcBtnFunction
    }
    
    val textColor = when (unit.type) {
        BtnType.Number -> CalcBtnNumberText
        BtnType.Operator -> CalcBtnOperatorText
        BtnType.Function -> CalcBtnFunctionText
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(if (unit.isDoubleWidth) 2.1f else 1f)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .testTag("btn_${unit.text}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = unit.text,
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
