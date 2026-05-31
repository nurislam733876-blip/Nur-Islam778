package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class AppInfo(
    val packageName: String,
    val label: String,
    val isVaulted: Boolean = false,
    val isSystem: Boolean = false
)

data class DrawPathPoint(
    val x: Float,
    val y: Float
)

data class DrawStroke(
    val points: List<DrawPathPoint>,
    val color: Int, // RGB Color Int
    val brushWidth: Float
)

enum class VaultScreen {
    Calculator,
    VaultSetupConfirm,
    VaultDashboard,
    PhotoViewer,
    PhotoEditor,
    VideoPlayer,
    HiddenAppManager
}

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {

    // --- State Navigation ---
    val currentScreen = MutableStateFlow(VaultScreen.Calculator)
    val backStack = java.util.Stack<VaultScreen>()

    fun navigateTo(screen: VaultScreen) {
        backStack.push(currentScreen.value)
        currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (!backStack.isEmpty()) {
            currentScreen.value = backStack.pop()
            return true
        }
        return false
    }

    // --- Calculator State ---
    val calcInput = MutableStateFlow("0")
    val calcRawExpression = StringBuilder()

    // --- Vault PIN Configuration & Unlock State ---
    val isPasscodeSet = MutableStateFlow(false)
    val isUnlocked = MutableStateFlow(false)
    val passcodeText = MutableStateFlow("") // Temp passcode typing
    val draftPasscode = MutableStateFlow("") // Holds first choice
    val statusMessage = MutableStateFlow("যোগ, বিয়োগ, গুণ, ভাগ করুন। ভল্ট খুলতে পাসকোড দিয়ে '=' চাপুন।")
    val passcodeSetupStep = MutableStateFlow(0) // 0 = normal, 1 = first pin set, 2 = confirm pin

    // --- Active media viewer states ---
    val selectedItem = MutableStateFlow<VaultedItem?>(null)

    // --- Photo Editor State ---
    val editedPhotoBitmapState = MutableStateFlow<Bitmap?>(null)
    val originalPhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val activeFilter = MutableStateFlow("None") // "None", "Grayscale", "Sepia", "Vintage", "Warm", "Cool"
    val rotationDegrees = MutableStateFlow(0f)
    val drawStrokesList = MutableStateFlow<List<DrawStroke>>(emptyList())
    val drawStrokesCount = drawStrokesList.map { it.size }
    val brushColor = MutableStateFlow(android.graphics.Color.RED)
    val brushWidth = MutableStateFlow(12f)
    val isSavingEdited = MutableStateFlow(false)

    // --- App Hider State ---
    val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val appHiderSearchText = MutableStateFlow("")
    val queryHiddenOnly = MutableStateFlow(false)
    val isLoadingApps = MutableStateFlow(false)

    // --- Flows from Database ---
    val allItems: StateFlow<List<VaultedItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photosList: StateFlow<List<VaultedItem>> = repository.getItemsByType("PHOTO")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosList: StateFlow<List<VaultedItem>> = repository.getItemsByType("VIDEO")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filesList: StateFlow<List<VaultedItem>> = repository.getItemsByType("FILE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenAppsList: StateFlow<List<HiddenApp>> = repository.hiddenApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Fetch setting to check if passcode already exists
        viewModelScope.launch {
            val pass = repository.getSetting("vault_passcode")
            if (pass != null && pass.isNotEmpty()) {
                isPasscodeSet.value = true
                statusMessage.value = "পাসকোড সেট করা আছে। পাসকোড এন্টার করে '=' চাপুন।"
            } else {
                isPasscodeSet.value = false
                statusMessage.value = "ভল্ট সেটআপ করতে ৪ সংখ্যার পাসকোড দিয়ে '=' চাপুন।"
            }
        }
    }

    // --- Calculator Buttons Action ---
    fun onDigitPressed(digit: String) {
        if (calcInput.value == "0" || calcInput.value == "Error") {
            calcInput.value = digit
        } else {
            calcInput.value += digit
        }
        calcRawExpression.append(digit)
    }

    fun onOperatorPressed(op: String) {
        val lastChar = calcInput.value.trim().lastOrNull()
        if (lastChar != null && "+-×÷".contains(lastChar)) {
            // Replace operator
            calcInput.value = calcInput.value.dropLast(1) + op
            calcRawExpression.setLength(calcRawExpression.length - 1)
            calcRawExpression.append(op)
        } else {
            calcInput.value += op
            calcRawExpression.append(op)
        }
    }

    fun onDotPressed() {
        val parts = calcInput.value.split("+", "-", "×", "÷")
        val currentPart = parts.last()
        if (!currentPart.contains(".")) {
            calcInput.value += "."
            calcRawExpression.append(".")
        }
    }

    fun onClearPressed() {
        if (calcInput.value.isNotEmpty() && calcInput.value != "0") {
            calcInput.value = calcInput.value.dropLast(1)
            if (calcInput.value.isEmpty()) {
                calcInput.value = "0"
            }
            if (calcRawExpression.isNotEmpty()) {
                calcRawExpression.setLength(calcRawExpression.length - 1)
            }
        }
    }

    fun onAllClearPressed() {
        calcInput.value = "0"
        calcRawExpression.setLength(0)
    }

    fun onEqualsPressed(context: Context) {
        val input = calcInput.value
        // Check PIN Trigger: It should be a purely numeric passcode (usually 4-8 digits) typed directly
        val numericPIN = input.filter { it.isDigit() }
        
        if (numericPIN.isNotEmpty() && numericPIN == input && input.length in 4..8) {
            handlePinValidation(numericPIN)
            return
        }

        // Standard calculation
        try {
            val resultValue = evaluateExpression(calcInput.value)
            // format output decently
            val formatted = if (resultValue % 1.0 == 0.0) {
                resultValue.toLong().toString()
            } else {
                String.format("%.4f", resultValue).trimEnd('0').trimEnd('.')
            }
            calcInput.value = formatted
            calcRawExpression.setLength(0)
            calcRawExpression.append(formatted)
        } catch (e: Exception) {
            calcInput.value = "Error"
        }
    }

    private fun handlePinValidation(pin: String) {
        viewModelScope.launch {
            val isSet = isPasscodeSet.value
            if (isSet) {
                // Verify passcode
                val savedPass = repository.getSetting("vault_passcode")
                if (pin == savedPass) {
                    isUnlocked.value = true
                    statusMessage.value = "ভল্ট আনলকড!"
                    onAllClearPressed()
                    navigateTo(VaultScreen.VaultDashboard)
                } else {
                    statusMessage.value = "ভুল পাসকোড! সঠিক পাসকোড দিয়ে '=' চাপুন।"
                    onAllClearPressed()
                }
            } else {
                // Passcode Setting Flow (first time)
                if (passcodeSetupStep.value == 0) {
                    draftPasscode.value = pin
                    passcodeSetupStep.value = 1
                    statusMessage.value = "পাসকোড নিশ্চিত করতে আবার টাইপ করে '=' চাপুন।"
                    onAllClearPressed()
                } else if (passcodeSetupStep.value == 1) {
                    if (pin == draftPasscode.value) {
                        repository.saveSetting("vault_passcode", pin)
                        isPasscodeSet.value = true
                        isUnlocked.value = true
                        passcodeSetupStep.value = 2
                        statusMessage.value = "পাসকোড সফলভাবে সেট হয়েছে! ভল্ট আনলকড।"
                        onAllClearPressed()
                        navigateTo(VaultScreen.VaultDashboard)
                    } else {
                        passcodeSetupStep.value = 0
                        statusMessage.value = "পাসকোড মেলেনি! ৪ অঙ্কের পাসকোড দিয়ে পুনরায় শুরু করুন।"
                        onAllClearPressed()
                    }
                }
            }
        }
    }

    // Lock Vault
    fun lockVault() {
        isUnlocked.value = false
        passcodeSetupStep.value = 0
        currentScreen.value = VaultScreen.Calculator
        backStack.clear()
        onAllClearPressed()
    }

    // --- File Import / Export Core Operations ---
    fun importSelectedFile(context: Context, uri: Uri, originalName: String, typeString: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.importFileToVault(context, uri, originalName, typeString)
            onFinished(result != null)
        }
    }

    fun exportItemFromVault(context: Context, item: VaultedItem, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.exportFileFromVault(context, item)
            onFinished(success)
        }
    }

    fun deleteItemPermanently(item: VaultedItem, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteItemFromVault(item)
            onFinished(success)
        }
    }

    // --- App Hider Functions ---
    fun loadApps(context: Context) {
        viewModelScope.launch {
            isLoadingApps.value = true
            val pm = context.packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val rawApps = withContext(Dispatchers.Default) {
                pm.queryIntentActivities(intent, 0)
            }

            // Get currently hidden from db
            val hiddenSet = hiddenAppsList.value.map { it.packageName }.toSet()

            val resolvedList = withContext(Dispatchers.Default) {
                rawApps.mapNotNull { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    if (packageName == context.packageName) return@mapNotNull null // ignore this scanner
                    val label = resolveInfo.loadLabel(pm).toString()
                    val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    AppInfo(
                        packageName = packageName,
                        label = label,
                        isVaulted = hiddenSet.contains(packageName),
                        isSystem = isSystem
                    )
                }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
            }

            installedApps.value = resolvedList
            isLoadingApps.value = false
        }
    }

    fun toggleAppHideState(packageName: String, label: String) {
        viewModelScope.launch {
            val isCurrentlyHidden = repository.isAppHidden(packageName)
            if (isCurrentlyHidden) {
                repository.removeHiddenApp(packageName)
            } else {
                repository.addHiddenApp(packageName, label)
            }
            
            // Re-map localized list
            installedApps.value = installedApps.value.map {
                if (it.packageName == packageName) {
                    it.copy(isVaulted = !isCurrentlyHidden)
                } else it
            }
        }
    }

    // --- Photo Editor Engine ---
    fun setupPhotoEditor(item: VaultedItem) {
        selectedItem.value = item
        editedPhotoBitmapState.value = null
        originalPhotoBitmap.value = null
        activeFilter.value = "None"
        rotationDegrees.value = 0f
        drawStrokesList.value = emptyList()

        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(item.path)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                originalPhotoBitmap.value = bitmap
                editedPhotoBitmapState.value = bitmap
            }
        }
    }

    fun applyFilter(filterName: String) {
        activeFilter.value = filterName
        recalculateEditorBitmap()
    }

    fun rotate90Degrees() {
        rotationDegrees.value = (rotationDegrees.value + 90f) % 360f
        recalculateEditorBitmap()
    }

    fun addDrawStroke(stroke: DrawStroke) {
        drawStrokesList.value += stroke
        recalculateEditorBitmap()
    }

    fun undoLastDrawStroke() {
        if (drawStrokesList.value.isNotEmpty()) {
            drawStrokesList.value = drawStrokesList.value.dropLast(1)
            recalculateEditorBitmap()
        }
    }

    private fun recalculateEditorBitmap() {
        val original = originalPhotoBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            // Apply Filters
            var result = applyFilterToBitmap(original, activeFilter.value)
            // Apply Rotations
            result = rotateBitmap(result, rotationDegrees.value)
            // Apply Ink drawings
            result = applyDrawingToBitmap(result, drawStrokesList.value)
            
            editedPhotoBitmapState.value = result
        }
    }

    fun saveEditedImg(context: Context, onFinished: (Boolean) -> Unit) {
        val item = selectedItem.value ?: return
        val currentBitmap = editedPhotoBitmapState.value ?: return
        isSavingEdited.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(item.path)
                FileOutputStream(file).use { outStream ->
                    currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                }
                
                repository.saveEditedPhoto(item, item.path, file.length())
                
                // Success! Force reload
                withContext(Dispatchers.Main) {
                    selectedItem.value = repository.getItemById(item.id)
                    isSavingEdited.value = false
                    onFinished(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isSavingEdited.value = false
                    onFinished(false)
                }
            }
        }
    }

    // Internal Image processing utilities
    private fun applyFilterToBitmap(src: Bitmap, filterName: String): Bitmap {
        if (filterName == "None") return src
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(dest)
        val paint = android.graphics.Paint()
        
        val colorMatrix = android.graphics.ColorMatrix()
        when (filterName) {
            "Grayscale" -> {
                colorMatrix.setSaturation(0f)
            }
            "Sepia" -> {
                colorMatrix.setSaturation(0f)
                val sepiaMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(sepiaMatrix)
            }
            "Vintage" -> {
                colorMatrix.setScale(0.9f, 0.82f, 0.7f, 1f)
            }
            "Warm" -> {
                colorMatrix.setScale(1.15f, 1.05f, 0.9f, 1f)
            }
            "Cool" -> {
                colorMatrix.setScale(0.9f, 1.05f, 1.2f, 1f)
            }
        }
        
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return src
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun applyDrawingToBitmap(src: Bitmap, strokes: List<DrawStroke>): Bitmap {
        if (strokes.isEmpty()) return src
        val dest = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(dest)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = android.graphics.Paint.Cap.ROUND
        }
        
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            paint.color = stroke.color
            paint.strokeWidth = stroke.brushWidth
            val path = android.graphics.Path()
            path.moveTo(stroke.points[0].x, stroke.points[0].y)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x, stroke.points[i].y)
            }
            canvas.drawPath(path, paint)
        }
        return dest
    }

    // --- Expression parsing recursive evaluation ---
    private fun evaluateExpression(expr: String): Double {
        val clean = expr.trim().replace("×", "*").replace("÷", "/")
        if (clean.isEmpty()) return 0.0
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < clean.length) clean[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < clean.length) throw RuntimeException("Unexpected: " + clean[pos])
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = clean.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected character")
                }
                return x
            }
        }.parse()
    }
}

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = VaultDatabase.getDatabase(context)
        val repo = VaultRepository(db.vaultDao())
        @Suppress("UNCHECKED_CAST")
        return VaultViewModel(repo) as T
    }
}
