package com.dodamsoft.fivembti

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.dodamsoft.fivembti.ui.theme.FiveMbtiTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import com.google.gson.GsonBuilder
import okhttp3.ResponseBody
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

// Data class for MBTI result response
data class MbtiResponse(
    val id: Int,
    val typeEnum: String,
    val typeNickName: String,
    val description: String,
    val similarCelebrities: String,
    val famousCelebrities: String,
    val historicalFigures: String
)

// Retrofit API interface
interface MbtiApiService {
    @GET("question/EI")
    fun getEIQuestion(): Call<ResponseBody>

    @GET("question/NS")
    fun getNSQuestion(): Call<ResponseBody>

    @GET("question/FT")
    fun getFTQuestion(): Call<ResponseBody>

    @GET("question/PJ")
    fun getPJQuestion(): Call<ResponseBody>

    @GET("results/{type}")
    fun getMbtiResult(@Path("type") type: String): Call<MbtiResponse>
}

// Retrofit client setup with lenient Gson parsing
object RetrofitClient {
   // private const val BASE_URL = "http://192.168.0.12:8085/" // Use 10.0.2.2 for emulator to access localhost
    private const val BASE_URL = "http://152.67.209.165:9081/fivembti/" //prod
    val apiService: MbtiApiService by lazy {
        val gson = GsonBuilder()
            .setLenient() // Enable lenient JSON parsing for /results/{type}
            .create()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MbtiApiService::class.java)
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FiveMbtiTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarHostState) {
                    delay(1000)
                    snackbarHostState.showSnackbar(
                        message = "👤 5초만에 나의 성격유형, 지금 바로 확인해보세요!",
                        duration = SnackbarDuration.Short
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Scaffold 내부 콘텐츠
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text(
                                    "\uD83D\uDC64 5초MBTI",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif // 원하는 폰트로 변경 가능
                                ) },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        MbtiTestScreen(modifier = Modifier.padding(innerPadding))
                    }

                    // 상단에 표시할 SnackbarHost
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 130.dp) // TopAppBar 높이만큼 띄움
                            .align(Alignment.TopCenter)
                            .zIndex(1f) // 위에 표시되도록 zIndex 지정
                    ) {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                }
            }
        }


    }
}


@Composable
fun MbtiTestScreen(modifier: Modifier = Modifier) {
    // State to track current question, answers, and UI states
    var currentQuestion by remember { mutableStateOf<String?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var answers by remember { mutableStateOf(mutableListOf<Boolean>()) }
    var result by remember { mutableStateOf<MbtiResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch question when index changes or on initial load
    LaunchedEffect(currentQuestionIndex) {
        if (currentQuestionIndex < 4) {
            isLoading = true
            fetchQuestion(currentQuestionIndex) { question, error ->
                currentQuestion = question
                errorMessage = error
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val context = LocalContext.current

        when {
            isLoading -> {
                // Show loading state
                CircularProgressIndicator()
                Text("Loading...", modifier = Modifier.padding(top = 16.dp))
            }
            errorMessage != null -> {
                // Show error state
                Text(
                    text = errorMessage ?: "An error occurred",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = {
                        // Retry fetching current question
                        isLoading = true
                        errorMessage = null
                        fetchQuestion(currentQuestionIndex) { question, error ->
                            currentQuestion = question
                            errorMessage = error
                            isLoading = false
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Try Again")
                }
            }
            result != null -> {
                // Display result
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "당신의 MBTI : ${result!!.typeEnum}",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = result!!.typeNickName,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = result!!.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                }

                // 기존 다른 텍스트들과 분리된 영역으로 따로 Column 생성
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "· 연예인: ${result!!.similarCelebrities}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "· 유명인: ${result!!.famousCelebrities}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    Text(
                        text = "· 역사인물: ${result!!.historicalFigures}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {


                    Button(
                        onClick = {
                            // Reset quiz
                            currentQuestionIndex = 0
                            answers = mutableListOf()
                            result = null
                            errorMessage = null
                            isLoading = true
                            fetchQuestion(currentQuestionIndex) { question, error ->
                                currentQuestion = question
                                errorMessage = error
                                isLoading = false
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(24.dp)
                            )
                            Text("다시하기")
                        }
                    }

                    Button(
                        onClick = {
                            result?.let {
                                shareMbtiResult(context, it)
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(24.dp)
                            )
                            Text("공유하기")
                        }
                    }


                }


            }
            currentQuestion != null && currentQuestionIndex < 4 -> {
                // Display current question
                Text(
                    text = "Question ${currentQuestionIndex + 1}.\n$currentQuestion",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(onClick = {
                        answers.add(true) // true maps to E, N, F, or P
                        Log.d("MbtiTest", "Current MBTI Type: ${computeMbtiType(answers)}")
                        currentQuestionIndex++
                        if (currentQuestionIndex == 4) {
                            val mbtiType = computeMbtiType(answers)
                            fetchMbtiResult(mbtiType) { resultResponse, error ->
                                result = resultResponse
                                errorMessage = error
                                isLoading = false
                            }
                            isLoading = true
                        }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Check icon",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "예"
                                , fontWeight = FontWeight.Bold
                                , fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                    Button(onClick = {
                        answers.add(false) // false maps to I, S, T, or J
                        Log.d("MbtiTest", "Current MBTI Type: ${computeMbtiType(answers)}")
                        currentQuestionIndex++
                        if (currentQuestionIndex == 4) {
                            val mbtiType = computeMbtiType(answers)
                            fetchMbtiResult(mbtiType) { resultResponse, error ->
                                result = resultResponse
                                errorMessage = error
                                isLoading = false
                            }
                            isLoading = true
                        }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Check icon",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "아니오"
                                , fontWeight = FontWeight.Bold
                                , fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }
    }
}

fun shareMbtiResult(context: Context, result: MbtiResponse) {
    val googlePlayStoreUrl = "https://play.google.com/store/apps/details?id=com.dodamsoft.fivembti"
    val message = """
        [5초MBTI 테스트 결과]
        
        나의 MBTI는 ${result.typeEnum} (${result.typeNickName})!
        
        ${result.description}
        
        연예인: ${result.similarCelebrities}
        유명인: ${result.famousCelebrities}
        역사인물: ${result.historicalFigures}
        
        👉 [5초만에 MBTI 확인하기]  
        - Android (구글 플레이 스토어): ${googlePlayStoreUrl}  
        - iOS (앱스토어): 아직 지원하지 않아요
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    val chooser = Intent.createChooser(intent, "결과 공유하기")
    context.startActivity(chooser)
}

// Function to compute MBTI type from answers
private fun computeMbtiType(answers: List<Boolean>): String {
    val ei = if (answers.getOrNull(0) == true) "E" else if (answers.getOrNull(0) == false) "I" else "-"
    val ns = if (answers.getOrNull(1) == true) "N" else if (answers.getOrNull(1) == false) "S" else "-"
    val ft = if (answers.getOrNull(2) == true) "F" else if (answers.getOrNull(2) == false) "T" else "-"
    val pj = if (answers.getOrNull(3) == true) "P" else if (answers.getOrNull(3) == false) "J" else "-"
    return "$ei$ns$ft$pj" // e.g., "INTP" or "E---" for partial
}

// Function to fetch a single question based on index
private fun fetchQuestion(index: Int, callback: (String?, String?) -> Unit) {
    val call = when (index) {
        0 -> RetrofitClient.apiService.getEIQuestion()
        1 -> RetrofitClient.apiService.getNSQuestion()
        2 -> RetrofitClient.apiService.getFTQuestion()
        3 -> RetrofitClient.apiService.getPJQuestion()
        else -> return callback(null, "Invalid question index")
    }

    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                val question = response.body()?.string()?.trim()
                if (!question.isNullOrEmpty()) {
                    callback(question, null)
                } else {
                    callback(null, "Empty question data")
                }
            } else {
                callback(null, "Failed to fetch question: ${response.message()}")
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            callback(null, "Network error: ${t.message}")
        }
    })
}

// Function to fetch MBTI result from API
private fun fetchMbtiResult(mbtiType: String, callback: (MbtiResponse?, String?) -> Unit) {
    RetrofitClient.apiService.getMbtiResult(mbtiType).enqueue(object : Callback<MbtiResponse> {
        override fun onResponse(call: Call<MbtiResponse>, response: Response<MbtiResponse>) {
            if (response.isSuccessful) {
                callback(response.body(), null)
            } else {
                callback(null, "Failed to fetch result: ${response.message()}")
            }
        }

        override fun onFailure(call: Call<MbtiResponse>, t: Throwable) {
            callback(null, "Network error: ${t.message}")
        }
    })
}

@Composable
fun MbtiTestPreview() {
    FiveMbtiTheme {
        MbtiTestScreen()
    }
}