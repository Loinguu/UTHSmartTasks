package com.example.uthsmarttasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text


class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private var navigateToProfile: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val nav = rememberNavController()

                LaunchedEffect(nav) {
                    navigateToProfile = {
                        nav.navigate("profile") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                NavHost(navController = nav, startDestination = "login") {
                    composable("login") {
                        LoginScreen(
                            onGoogleClick = { signInLauncher.launch(googleClient.signInIntent) }
                        )
                    }
                    composable("profile") {
                        val user = auth.currentUser
                        ProfileScreen(
                            nameInit = user?.displayName.orEmpty(),
                            emailInit = user?.email.orEmpty(),
                            photoUrl = user?.photoUrl?.toString(),
                            onBack = {
                                // quay về login
                                if (!nav.popBackStack()) {
                                    nav.navigate("login") { launchSingleTop = true }
                                }
                            }
                        )
                    }
                }
            }
        }

    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (_: ApiException) {
            }
        }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener {
                navigateToProfile?.invoke()
            }
            .addOnFailureListener {
            }
    }

}

@Composable
fun LoginScreen(onGoogleClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.uth_logo),
                contentDescription = "UTH Logo",
                modifier = Modifier
                    .size(150.dp)
            )

            Spacer(Modifier.height(12.dp))
            Text("SmartTasks", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "A simple and efficient to-do app",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))
            Text(
                "Welcome",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Ready to explore? Log in to get started.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = onGoogleClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.`ic_google`),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "SIGN IN WITH GOOGLE",
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    nameInit: String,
    emailInit: String,
    photoUrl: String?,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(nameInit) }
    var email by remember { mutableStateOf(emailInit) }
    var dob by remember { mutableStateOf("23/05/1995") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { pv ->
        Column(
            Modifier
                .padding(pv)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                if (photoUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEAEAEA))
                    )
                } else {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text("Name", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Text("Email", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
            OutlinedTextField(
                value = email,
                onValueChange = {},
                enabled = false,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Text("Date of Birth", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                singleLine = true,
                trailingIcon = {
                    Icon(
                        painterResource(id = R.drawable.ic_chevron_down),
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Button(
                onClick = onBack,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Back")
            }
        }
    }
}
