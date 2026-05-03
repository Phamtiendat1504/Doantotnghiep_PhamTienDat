package com.example.doantotnghiep.View.Auth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CccdCameraActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET_SIDE = "extra_target_side"
        const val EXTRA_OUTPUT_URI = "extra_output_uri"
        const val SIDE_FRONT = "front"
        const val SIDE_BACK = "back"

        private val FRONT_KEYWORDS = listOf(
            "CAN CUOC",
            "CAN CUOC CONG DAN",
            "IDENTITY CARD",
            "SOCIALIST REPUBLIC OF VIET NAM",
            "HO VA TEN",
            "DATE OF BIRTH",
            "GIOI TINH",
            "QUOC TICH"
        )
        private val BACK_KEYWORDS = listOf(
            "DAC DIEM NHAN DANG",
            "NGAY CAP",
            "NOI CAP",
            "CO GIA TRI DEN"
        )
        private val FRONT_HEADER_KEYWORDS = listOf(
            "CAN CUOC",
            "CAN CUOC CONG DAN",
            "SOCIALIST REPUBLIC OF VIET NAM",
            "IDENTITY CARD"
        )
        private const val MIN_MEAN_LUMA = 30.0
        private const val MAX_MEAN_LUMA = 230.0
        private const val MAX_DARK_RATIO = 0.70
        private const val MAX_BRIGHT_RATIO = 0.45
        private const val MIN_CONTRAST_STD = 15.0
        private const val MIN_SHARPNESS = 10.0
        private const val MIN_TEXT_BLOCKS_FRONT = 2
        private const val MIN_TEXT_BLOCKS_BACK = 1
        private const val MIN_TEXT_CHARS_FRONT = 20
        private const val MIN_TEXT_CHARS_BACK = 15
        private const val MIN_TEXT_SPREAD_X_FRONT = 0.40
        private const val MIN_TEXT_SPREAD_X_BACK = 0.40
        private const val MIN_TEXT_SPREAD_Y = 0.20
        private const val MIN_TEXT_COVERAGE = 0.10
        private const val AUTO_CAPTURE_MIN_STABLE_FRAMES = 3
        private const val AUTO_CAPTURE_ANALYZE_INTERVAL_MS = 700L
        private const val AUTO_CAPTURE_REARM_DELAY_MS = 1200L
    }

    private lateinit var previewView: PreviewView
    private lateinit var guideFrame: FrameLayout
    private lateinit var tvGuide: TextView
    private lateinit var tvGuideSub: TextView
    private lateinit var btnCapture: MaterialButton
    private lateinit var btnClose: ImageButton

    private var cameraController: LifecycleCameraController? = null
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var captureSide: String = SIDE_FRONT
    private val mainHandler = Handler(Looper.getMainLooper())
    private val analysisExecutor by lazy { Executors.newSingleThreadExecutor() }
    @Volatile private var isAnalyzingPreviewFrame = false
    @Volatile private var captureInProgress = false
    @Volatile private var autoCaptureLocked = false
    @Volatile private var isDestroyed = false
    private var stableValidFrameCount = 0
    private var lastPreviewAnalyzeAt = 0L

    // Cache tọa độ guide frame tại thời điểm bấm chụp để tránh đọc View trên wrong thread
    private var cachedGuideRect: android.graphics.Rect? = null
    private var cachedPreviewRect: android.graphics.Rect? = null

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            MessageUtils.showErrorDialog(
                this,
                "Lỗi camera",
                "Bạn chưa cấp quyền Camera. Vui lòng bật quyền Camera trong Cài đặt."
            )
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cccd_camera)

        captureSide = intent.getStringExtra(EXTRA_TARGET_SIDE) ?: SIDE_FRONT
        initViews()
        setupGuideText()
        setupActions()
        ensureCameraPermissionAndStart()
    }

    override fun onDestroy() {
        isDestroyed = true
        super.onDestroy()
        cameraController?.clearImageAnalysisAnalyzer()
        analysisExecutor.shutdown()
        textRecognizer.close()
    }

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        guideFrame = findViewById(R.id.guideFrame)
        tvGuide = findViewById(R.id.tvGuide)
        tvGuideSub = findViewById(R.id.tvGuideSub)
        btnCapture = findViewById(R.id.btnCaptureCccd)
        btnClose = findViewById(R.id.btnCloseCamera)
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }

    private fun setupGuideText() {
        if (captureSide == SIDE_BACK) {
            tvGuide.text = "Chụp MẶT SAU CCCD trong khung"
        } else {
            tvGuide.text = "Chụp MẶT TRƯỚC CCCD trong khung"
        }
        tvGuideSub.text = "Cầm máy theo chiều dọc, đặt thẻ nằm ngang và rõ nét. Giữ ổn định để tự động chụp."
    }

    private fun setupActions() {
        btnClose.setOnClickListener { finish() }
        btnCapture.setOnClickListener { capturePhoto() }
    }

    private fun ensureCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        try {
            val controller = LifecycleCameraController(this).apply {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
                setImageAnalysisBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                setImageAnalysisAnalyzer(analysisExecutor) { imageProxy ->
                    analyzePreviewFrame(imageProxy)
                }
                bindToLifecycle(this@CccdCameraActivity)
            }
            previewView.controller = controller
            cameraController = controller
        } catch (e: Exception) {
            MessageUtils.showErrorDialog(this, "Lỗi camera", "Không khởi tạo được camera: ${e.message}")
        }
    }

    private fun capturePhoto(fromAutoCapture: Boolean = false) {
        if (captureInProgress) return
        val controller = cameraController
        if (controller == null) {
            MessageUtils.showErrorDialog(this, "Lỗi camera", "Camera chưa sẵn sàng. Vui lòng thử lại.")
            return
        }

        captureInProgress = true
        autoCaptureLocked = true
        stableValidFrameCount = 0
        if (fromAutoCapture) {
            tvGuideSub.text = "Đang tự động nhận diện ảnh, vui lòng giữ máy ổn định..."
        }
        btnCapture.isEnabled = false

        // Cache vị trí guide và preview trước khi chụp để dùng an toàn trong callback
        cachedGuideRect = android.graphics.Rect(
            guideFrame.x.toInt(),
            guideFrame.y.toInt(),
            (guideFrame.x + guideFrame.width).toInt(),
            (guideFrame.y + guideFrame.height).toInt()
        )
        cachedPreviewRect = android.graphics.Rect(
            previewView.x.toInt(),
            previewView.y.toInt(),
            (previewView.x + previewView.width).toInt(),
            (previewView.y + previewView.height).toInt()
        )

        val outputFile = createOutputFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        controller.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    handleCapturedFile(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    resetCaptureState()
                    MessageUtils.showErrorDialog(
                        this@CccdCameraActivity,
                        "Lỗi chụp ảnh",
                        "Không thể chụp ảnh: ${exception.message}"
                    )
                }
            }
        )
    }

    private fun handleCapturedFile(file: File) {
        val bitmap = decodeBitmapWithExif(file)
        if (bitmap == null) {
            resetCaptureState()
            MessageUtils.showErrorDialog(this, "Lỗi ảnh", "Không đọc được ảnh vừa chụp. Vui lòng chụp lại.")
            return
        }

        val cropped = cropToGuide(bitmap)
        if (cropped == null) {
            resetCaptureState()
            bitmap.recycle()
            file.delete()
            MessageUtils.showInfoDialog(
                this,
                "Ảnh chưa đúng khung",
                "Không xác định được đúng vùng khung chụp. Vui lòng đặt lại CCCD vào khung và chụp lại."
            )
            return
        }

        if (cropped.width <= cropped.height) {
            resetCaptureState()
            if (cropped != bitmap) cropped.recycle()
            bitmap.recycle()
            file.delete()
            MessageUtils.showInfoDialog(
                this,
                "Ảnh chưa đúng khung",
                "Vui lòng đặt CCCD nằm ngang trong khung và chụp lại."
            )
            return
        }

        val qualityError = validateImageQuality(cropped)
        if (qualityError != null) {
            resetCaptureState()
            if (cropped != bitmap) cropped.recycle()
            bitmap.recycle()
            file.delete()
            MessageUtils.showInfoDialog(
                this,
                "Ảnh chưa hợp lệ",
                qualityError
            )
            return
        }

        validateCccdFromFrame(cropped) { valid, errorMessage ->
            if (!valid) {
                resetCaptureState()
                if (cropped != bitmap) cropped.recycle()
                bitmap.recycle()
                file.delete()
                MessageUtils.showInfoDialog(
                    this,
                    "Ảnh chưa hợp lệ",
                    errorMessage
                )
                return@validateCccdFromFrame
            }

            saveBitmapToFile(cropped, file)
            if (cropped != bitmap) cropped.recycle()
            bitmap.recycle()
            val resultIntent = android.content.Intent().putExtra(EXTRA_OUTPUT_URI, Uri.fromFile(file).toString())
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun resetCaptureState() {
        if (isDestroyed) return
        captureInProgress = false
        btnCapture.isEnabled = true
        setupGuideText()
        mainHandler.postDelayed({
            if (!isDestroyed) {
                autoCaptureLocked = false
                stableValidFrameCount = 0
            }
        }, AUTO_CAPTURE_REARM_DELAY_MS)
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyzePreviewFrame(imageProxy: ImageProxy) {
        if (isDestroyed || captureInProgress || autoCaptureLocked) {
            imageProxy.close()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastPreviewAnalyzeAt < AUTO_CAPTURE_ANALYZE_INTERVAL_MS || isAnalyzingPreviewFrame) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isAnalyzingPreviewFrame = true
        lastPreviewAnalyzeAt = now
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val sideSignals = analyzeSignals(
                    visionText = visionText,
                    imageWidth = inputImage.width.toFloat(),
                    imageHeight = inputImage.height.toFloat()
                )

                val sideValid = if (captureSide == SIDE_BACK) {
                    isBackSide(sideSignals)
                } else {
                    isFrontSide(sideSignals)
                }

                // Dùng điều kiện đơn giản hơn cho preview (không dùng completenessOk hoặc isGuideCenterAligned):
                // Chỉ cần nhận ra đúng mặt thẻ + có đủ text cơ bản trong frame.
                // Ngưỡng này phù hợp với full camera frame (không phải ảnh đã crop).
                val hasEnoughText = visionText.textBlocks.size >= 2 &&
                    visionText.text.replace("\\s+".toRegex(), "").length >= 15
                val stable = sideValid && hasEnoughText

                stableValidFrameCount = if (stable) {
                    val newCount = stableValidFrameCount + 1
                    // Hiển thị feedback cho user khi đếm đếm gần đủ
                    mainHandler.post {
                        if (!isDestroyed && !captureInProgress) {
                            when (newCount) {
                                1 -> tvGuideSub.text = "Đã nhận ra thẻ... giữ nguyên"
                                2 -> tvGuideSub.text = "Đã nhận ra thẻ... giữ nguyên..."
                                else -> tvGuideSub.text = "Đang tự động chụp..."
                            }
                        }
                    }
                    newCount
                } else {
                    if (stableValidFrameCount > 0) {
                        mainHandler.post {
                            if (!isDestroyed && !captureInProgress) setupGuideText()
                        }
                    }
                    0
                }

                if (stableValidFrameCount >= AUTO_CAPTURE_MIN_STABLE_FRAMES && !captureInProgress && !autoCaptureLocked) {
                    autoCaptureLocked = true
                    stableValidFrameCount = 0
                    mainHandler.post {
                        capturePhoto(fromAutoCapture = true)
                    }
                }
            }
            .addOnFailureListener {
                stableValidFrameCount = 0
            }
            .addOnCompleteListener {
                isAnalyzingPreviewFrame = false
                imageProxy.close()
            }
    }

    private fun validateCccdFromFrame(bitmap: Bitmap, onDone: (Boolean, String) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val signals = analyzeSignals(
                    visionText = visionText,
                    imageWidth = bitmap.width.toFloat(),
                    imageHeight = bitmap.height.toFloat()
                )
                val frameMetrics = analyzeOcrFrameMetrics(visionText, bitmap.width, bitmap.height)

                val isValid = if (captureSide == SIDE_BACK) {
                    isBackSide(signals)
                } else {
                    isFrontSide(signals)
                }

                if (isValid) {
                    val completenessError = validateCardCompleteness(frameMetrics)
                    if (completenessError != null) {
                        onDone(false, completenessError)
                    } else {
                        onDone(true, "")
                    }
                } else {
                    val wrongSideMessage = when {
                        captureSide == SIDE_FRONT && isBackSide(signals) ->
                            "Bạn đang chụp nhầm MẶT SAU vào ô MẶT TRƯỚC. Vui lòng lật lại đúng MẶT TRƯỚC CCCD."
                        captureSide == SIDE_BACK && isFrontSide(signals) ->
                            "Bạn đang chụp nhầm MẶT TRƯỚC vào ô MẶT SAU. Vui lòng lật lại đúng MẶT SAU CCCD."
                        else -> null
                    }
                    onDone(
                        false,
                        wrongSideMessage
                            ?: "Hệ thống chưa nhận ra đúng mặt CCCD trong khung. Đặt lại thẻ vào khung, giữ tay vững và tránh chói sáng."
                    )
                }
            }
            .addOnFailureListener {
                onDone(false, "Không đọc được thông tin CCCD trong ảnh. Vui lòng chụp lại rõ nét hơn.")
            }
    }

    private data class ImageQualityMetrics(
        val meanLuma: Double,
        val darkRatio: Double,
        val brightRatio: Double,
        val contrastStd: Double,
        val sharpness: Double
    )

    private data class OcrFrameMetrics(
        val textBlockCount: Int,
        val textCharCount: Int,
        val spreadX: Double,
        val spreadY: Double,
        val coverageRatio: Double,
        val hasCccdCandidate: Boolean
    )

    private fun validateImageQuality(bitmap: Bitmap): String? {
        val metrics = analyzeImageQuality(bitmap)

        if (metrics.meanLuma < MIN_MEAN_LUMA || metrics.darkRatio > MAX_DARK_RATIO) {
            return "Ảnh quá tối hoặc thiếu sáng. Vui lòng tăng ánh sáng và chụp lại."
        }
        if (metrics.meanLuma > MAX_MEAN_LUMA || metrics.brightRatio > MAX_BRIGHT_RATIO) {
            return "Ảnh bị chói sáng. Vui lòng tránh đèn chiếu trực tiếp và chụp lại."
        }
        if (metrics.contrastStd < MIN_CONTRAST_STD) {
            return "Ảnh thiếu tương phản. Vui lòng đặt thẻ rõ nét, tránh lóa và chụp lại."
        }
        if (metrics.sharpness < MIN_SHARPNESS) {
            return "Ảnh bị mờ. Vui lòng giữ tay vững, lấy nét rõ hơn rồi chụp lại."
        }
        return null
    }

    private fun analyzeImageQuality(bitmap: Bitmap): ImageQualityMetrics {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 2 || height <= 2) {
            return ImageQualityMetrics(
                meanLuma = 0.0,
                darkRatio = 1.0,
                brightRatio = 0.0,
                contrastStd = 0.0,
                sharpness = 0.0
            )
        }

        val targetSamples = 24000.0
        val step = maxOf(1, sqrt((width.toDouble() * height.toDouble()) / targetSamples).toInt())
        var count = 0
        var darkCount = 0
        var brightCount = 0
        var sum = 0.0
        var sumSq = 0.0
        var gradSum = 0.0

        var y = step
        while (y < height - step) {
            var x = step
            while (x < width - step) {
                val l = luma(bitmap.getPixel(x, y))
                val lLeft = luma(bitmap.getPixel(x - step, y))
                val lRight = luma(bitmap.getPixel(x + step, y))
                val lTop = luma(bitmap.getPixel(x, y - step))
                val lBottom = luma(bitmap.getPixel(x, y + step))

                val gx = lRight - lLeft
                val gy = lBottom - lTop
                gradSum += sqrt(gx * gx + gy * gy)

                sum += l
                sumSq += (l * l)
                if (l < 35.0) darkCount++
                if (l > 235.0) brightCount++
                count++
                x += step
            }
            y += step
        }

        if (count <= 0) {
            return ImageQualityMetrics(
                meanLuma = 0.0,
                darkRatio = 1.0,
                brightRatio = 0.0,
                contrastStd = 0.0,
                sharpness = 0.0
            )
        }

        val mean = sum / count
        val variance = ((sumSq / count) - (mean * mean)).coerceAtLeast(0.0)
        return ImageQualityMetrics(
            meanLuma = mean,
            darkRatio = darkCount.toDouble() / count.toDouble(),
            brightRatio = brightCount.toDouble() / count.toDouble(),
            contrastStd = sqrt(variance),
            sharpness = gradSum / count.toDouble()
        )
    }

    private fun luma(pixel: Int): Double {
        val r = (pixel shr 16) and 0xff
        val g = (pixel shr 8) and 0xff
        val b = pixel and 0xff
        return (0.299 * r) + (0.587 * g) + (0.114 * b)
    }

    private fun analyzeOcrFrameMetrics(visionText: Text, width: Int, height: Int): OcrFrameMetrics {
        var blockCount = 0
        var charCount = 0
        var minLeft = Int.MAX_VALUE
        var minTop = Int.MAX_VALUE
        var maxRight = 0
        var maxBottom = 0

        visionText.textBlocks.forEach { block ->
            val box = block.boundingBox ?: return@forEach
            val normalized = block.text.replace("\\s+".toRegex(), "")
            if (normalized.isBlank()) return@forEach

            blockCount++
            charCount += normalized.length
            minLeft = minOf(minLeft, box.left)
            minTop = minOf(minTop, box.top)
            maxRight = maxOf(maxRight, box.right)
            maxBottom = maxOf(maxBottom, box.bottom)
        }

        if (blockCount == 0 || minLeft == Int.MAX_VALUE) {
            return OcrFrameMetrics(
                textBlockCount = 0,
                textCharCount = 0,
                spreadX = 0.0,
                spreadY = 0.0,
                coverageRatio = 0.0,
                hasCccdCandidate = false
            )
        }

        val frameWidth = width.coerceAtLeast(1).toDouble()
        val frameHeight = height.coerceAtLeast(1).toDouble()
        val spanW = (maxRight - minLeft).coerceAtLeast(1).toDouble()
        val spanH = (maxBottom - minTop).coerceAtLeast(1).toDouble()
        val cccdRegex = Regex("(?:\\d[\\s.\\-]*){12}")
        val compactCccdRegex = Regex("\\d{12}")

        return OcrFrameMetrics(
            textBlockCount = blockCount,
            textCharCount = charCount,
            spreadX = spanW / frameWidth,
            spreadY = spanH / frameHeight,
            coverageRatio = (spanW * spanH) / (frameWidth * frameHeight),
            hasCccdCandidate = cccdRegex.containsMatchIn(visionText.text) || compactCccdRegex.containsMatchIn(visionText.text)
        )
    }

    private fun validateCardCompleteness(metrics: OcrFrameMetrics): String? {
        val minBlocks = if (captureSide == SIDE_BACK) MIN_TEXT_BLOCKS_BACK else MIN_TEXT_BLOCKS_FRONT
        val minChars = if (captureSide == SIDE_BACK) MIN_TEXT_CHARS_BACK else MIN_TEXT_CHARS_FRONT
        val minSpreadX = if (captureSide == SIDE_BACK) MIN_TEXT_SPREAD_X_BACK else MIN_TEXT_SPREAD_X_FRONT

        if (metrics.textBlockCount < minBlocks || metrics.textCharCount < minChars) {
            return "Ảnh có vẻ bị thiếu nội dung thẻ (chỉ chụp một phần). Vui lòng đặt toàn bộ thẻ vào khung."
        }
        if (metrics.spreadX < minSpreadX || metrics.spreadY < MIN_TEXT_SPREAD_Y || metrics.coverageRatio < MIN_TEXT_COVERAGE) {
            return "Ảnh có vẻ chỉ chụp một góc hoặc nửa thẻ. Vui lòng đặt toàn bộ CCCD vào khung và chụp lại."
        }
        if (captureSide == SIDE_FRONT && !metrics.hasCccdCandidate) {
            return "Mặt trước CCCD chưa đọc được dãy số. Vui lòng chụp rõ nét hơn và tránh lóa sáng."
        }
        return null
    }

    private fun isGuideCenterAligned(visionText: Text, width: Int, height: Int): Boolean {
        var minLeft = Int.MAX_VALUE
        var minTop = Int.MAX_VALUE
        var maxRight = 0
        var maxBottom = 0
        var hasText = false

        visionText.textBlocks.forEach { block ->
            val box = block.boundingBox ?: return@forEach
            val normalized = block.text.replace("\\s+".toRegex(), "")
            if (normalized.isBlank()) return@forEach

            hasText = true
            minLeft = minOf(minLeft, box.left)
            minTop = minOf(minTop, box.top)
            maxRight = maxOf(maxRight, box.right)
            maxBottom = maxOf(maxBottom, box.bottom)
        }

        if (!hasText || minLeft == Int.MAX_VALUE) return false

        val frameW = width.coerceAtLeast(1).toFloat()
        val frameH = height.coerceAtLeast(1).toFloat()
        val centerX = (minLeft + maxRight) / 2f
        val centerY = (minTop + maxBottom) / 2f
        val dxRatio = abs(centerX - frameW / 2f) / frameW
        val dyRatio = abs(centerY - frameH / 2f) / frameH
        val spreadX = (maxRight - minLeft).coerceAtLeast(1) / frameW
        val spreadY = (maxBottom - minTop).coerceAtLeast(1) / frameH

        return dxRatio <= 0.12f &&
            dyRatio <= 0.12f &&
            spreadX >= 0.68f &&
            spreadY >= 0.35f
    }

    private data class SideSignals(
        val frontScore: Int,
        val backScore: Int,
        val hasMrz: Boolean,
        val frontHeaderYRatio: Float?,
        val mrzYRatio: Float?,
        val verticalTextBlockRatio: Float
    )

    private fun analyzeSignals(
        visionText: Text,
        imageWidth: Float,
        imageHeight: Float
    ): SideSignals {
        val normalizedText = normalizeNoAccentUpper(visionText.text)
        val frontScore = FRONT_KEYWORDS.count { normalizedText.contains(it) }
        val backScore = BACK_KEYWORDS.count { normalizedText.contains(it) }
        val hasMrz = Regex("[A-Z0-9<]{20,}").containsMatchIn(normalizedText) || normalizedText.count { it == '<' } >= 8

        val safeWidth = imageWidth.coerceAtLeast(1f)
        val safeHeight = imageHeight.coerceAtLeast(1f)
        val frontHeaderRatios = mutableListOf<Float>()
        val mrzRatios = mutableListOf<Float>()
        var textBlockCount = 0
        var verticalTextBlockCount = 0

        visionText.textBlocks.forEach { block ->
            val blockBox = block.boundingBox ?: return@forEach
            val blockText = normalizeNoAccentUpper(block.text)
            val blockWidth = (blockBox.right - blockBox.left).coerceAtLeast(1)
            val blockHeight = (blockBox.bottom - blockBox.top).coerceAtLeast(1)
            val centerYRatio = ((blockBox.top + blockBox.bottom) / 2f) / safeHeight
            textBlockCount++
            if (blockHeight > blockWidth * 1.20f) {
                verticalTextBlockCount++
            }

            if (FRONT_HEADER_KEYWORDS.any { blockText.contains(it) }) {
                frontHeaderRatios.add(centerYRatio)
            }

            val hasMrzInBlock = Regex("[A-Z0-9<]{20,}").containsMatchIn(blockText) || blockText.count { it == '<' } >= 8
            if (hasMrzInBlock) {
                mrzRatios.add(centerYRatio)
            }
        }

        val frontHeaderYRatio = frontHeaderRatios.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val mrzYRatio = mrzRatios.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val verticalTextBlockRatio = if (textBlockCount > 0) {
            verticalTextBlockCount.toFloat() / textBlockCount.toFloat()
        } else {
            0f
        }

        return SideSignals(
            frontScore = frontScore,
            backScore = backScore,
            hasMrz = hasMrz,
            frontHeaderYRatio = frontHeaderYRatio,
            mrzYRatio = mrzYRatio,
            verticalTextBlockRatio = verticalTextBlockRatio
        )
    }

    private fun isFrontSide(signals: SideSignals): Boolean {
        return signals.frontScore >= 1
    }

    private fun isBackSide(signals: SideSignals): Boolean {
        return signals.backScore >= 1 || signals.hasMrz
    }

    private fun isLikelySideRotated(signals: SideSignals): Boolean {
        return false // Removed strict rotation checks to avoid false negatives
    }

    private fun isLikelyUpsideDownFront(signals: SideSignals): Boolean {
        return false // Removed strict upside down checks
    }

    private fun isLikelyUpsideDownBack(signals: SideSignals): Boolean {
        return false // Removed strict upside down checks
    }

    private fun normalizeNoAccentUpper(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return normalized.uppercase(Locale.ROOT)
    }

    private fun cropToGuide(bitmap: Bitmap): Bitmap? {
        // Dùng các giá trị đã cache từ lúc bấm chụp để tránh đọc View khi có vấn đề layout
        val guide = cachedGuideRect
        val preview = cachedPreviewRect

        val previewWidth: Float
        val previewHeight: Float
        val guideLeft: Float
        val guideTop: Float
        val guideRight: Float
        val guideBottom: Float

        if (guide != null && preview != null && guide.width() > 0 && guide.height() > 0) {
            previewWidth = (preview.right - preview.left).toFloat().coerceAtLeast(1f)
            previewHeight = (preview.bottom - preview.top).toFloat().coerceAtLeast(1f)
            guideLeft = (guide.left - preview.left).toFloat().coerceIn(0f, previewWidth)
            guideTop = (guide.top - preview.top).toFloat().coerceIn(0f, previewHeight)
            guideRight = (guide.right - preview.left).toFloat().coerceIn(0f, previewWidth)
            guideBottom = (guide.bottom - preview.top).toFloat().coerceIn(0f, previewHeight)
        } else {
            // Fallback: đọc trực tiếp từ View
            previewWidth = previewView.width.toFloat().coerceAtLeast(1f)
            previewHeight = previewView.height.toFloat().coerceAtLeast(1f)
            guideLeft = (guideFrame.x - previewView.x).coerceIn(0f, previewWidth)
            guideTop = (guideFrame.y - previewView.y).coerceIn(0f, previewHeight)
            guideRight = (guideFrame.x + guideFrame.width - previewView.x).coerceIn(0f, previewWidth)
            guideBottom = (guideFrame.y + guideFrame.height - previewView.y).coerceIn(0f, previewHeight)
        }

        if (guideRight <= guideLeft || guideBottom <= guideTop) return null

        val displayScale = min(previewWidth / bitmap.width.toFloat(), previewHeight / bitmap.height.toFloat())
        val displayedWidth = bitmap.width * displayScale
        val displayedHeight = bitmap.height * displayScale
        val displayLeft = (previewWidth - displayedWidth) / 2f
        val displayTop = (previewHeight - displayedHeight) / 2f

        val intersectLeft = maxOf(guideLeft, displayLeft)
        val intersectTop = maxOf(guideTop, displayTop)
        val intersectRight = minOf(guideRight, displayLeft + displayedWidth)
        val intersectBottom = minOf(guideBottom, displayTop + displayedHeight)

        if (intersectRight <= intersectLeft || intersectBottom <= intersectTop) return null

        val leftPct = ((intersectLeft - displayLeft) / displayedWidth).coerceIn(0f, 1f)
        val topPct = ((intersectTop - displayTop) / displayedHeight).coerceIn(0f, 1f)
        val rightPct = ((intersectRight - displayLeft) / displayedWidth).coerceIn(0f, 1f)
        val bottomPct = ((intersectBottom - displayTop) / displayedHeight).coerceIn(0f, 1f)

        val left = (leftPct * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (topPct * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (rightPct * bitmap.width).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bottomPct * bitmap.height).roundToInt().coerceIn(top + 1, bitmap.height)

        val cropWidth = (right - left).coerceAtLeast(1)
        val cropHeight = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    private fun decodeBitmapWithExif(file: File): Bitmap? {
        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val orientation = try {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            else -> Unit
        }

        if (matrix.isIdentity) return raw

        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        raw.recycle()
        return rotated
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            output.flush()
        }
    }

    private fun createOutputFile(): File {
        val dir = File(filesDir, "verification_camera").apply { mkdirs() }
        val sidePrefix = if (captureSide == SIDE_BACK) "back" else "front"
        return File(dir, "cccd_${sidePrefix}_${System.currentTimeMillis()}.jpg")
    }
}
