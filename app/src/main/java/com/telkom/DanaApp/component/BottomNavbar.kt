package com.telkom.DanaApp.component // Or your actual package

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape // For a circular "Add" button background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.telkom.DanaApp.R // Your R file
import com.telkom.DanaApp.ui.theme.Black
import com.telkom.DanaApp.ui.theme.DarkGreen // Make sure these are correctly defined
import com.telkom.DanaApp.ui.theme.LightGray
import com.telkom.DanaApp.ui.theme.Orange
import com.telkom.DanaApp.ui.theme.PoppinsFontFamily
import com.telkom.DanaApp.ui.theme.White
import com.telkom.DanaApp.view.ProfileInfoCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- Screen Definitions (Keep as is) ---
sealed class Screen(val route: String, val title: String, val icon: Int, val index: Int) {
    object Wallet : Screen("wallet", "Wallet", R.drawable.icon_wallet, 0)
    object Report : Screen("report", "Report", R.drawable.icon_report, 1)
    object Add : Screen("add", "Add", R.drawable.icon_plus, 2) // Center button
    object Target : Screen("target", "Target", R.drawable.icon_target, 3)
    object User : Screen("profile", "Account", R.drawable.icon_user, 4)
}

val bottomNavScreens = listOf(
    Screen.Wallet,
    Screen.Report,
    // Add is handled separately by the central button
    Screen.Target,
    Screen.User,
)

// Custom Ripple Theme to disable the ripple effect
private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Transparent

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
}

@Composable
fun MainScreen(
    onGoToAddBalance: () -> Unit
) {
    val navController = rememberNavController()

    // Back press handling for NavHost
    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.popBackStack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = { BottomNavigationBar(navController = navController) }

    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Wallet.route,
            Modifier.padding(innerPadding),
        ) {
            composable(Screen.Wallet.route) { WalletScreen() }
            composable(Screen.Report.route) { ReportScreen() }
            composable(Screen.Add.route) {
                LaunchedEffect(Unit) {
                    onGoToAddBalance()
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack() // Go back to the previous screen in this NavHost
                    } else {
                        navController.navigate(Screen.Wallet.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
            }
            composable(Screen.Target.route) { TargetScreen() }
            composable(Screen.User.route) { UserScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    // All screens including "Add" for consistent indexing if needed,
    // but "Add" is handled by the central button.
    val allScreens = listOf(
        Screen.Wallet,
        Screen.Report,
        Screen.Add, // For index reference if Add button changes selection
        Screen.Target,
        Screen.User,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine selected item based on current route
    // The central "Add" button won't be "selected" in the bottom bar items,
    // but we might want to update a general selected state if navigating via it.
    // For simplicity, let's manage selection based on the bottom bar items only.
    var selectedItemIndex by rememberSaveable {
        mutableIntStateOf(allScreens.indexOfFirst { it.route == Screen.Wallet.route })
    }

    // Update selectedItemIndex when currentRoute changes
    // This makes sure the correct item is highlighted even with programmatic navigation or back press
    currentRoute?.let { route ->
        val matchedScreen = allScreens.find { it.route == route }
        if (matchedScreen != null && matchedScreen != Screen.Add) { // Don't select "Add" as a bar item
            selectedItemIndex = matchedScreen.index
        } else if (route == Screen.Add.route) {
            // If "Add" screen is active, decide what to show as selected.
            // E.g., keep the previous selection or select nothing.
            // For now, let's assume "Add" doesn't change the bottom bar selection.
            // Or, if "Add" screen has its own indicator:
            // selectedItemIndex = Screen.Add.index // This would require Add to be in left/right screens
        }
    }


    CompositionLocalProvider(LocalRippleTheme provides NoRippleTheme) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp), // Height of the actual bar + part of the FAB
            contentAlignment = Alignment.BottomCenter,
        ) {
            // The actual NavigationBar
            NavigationBar(
                containerColor = DarkGreen, // Set background color directly
                modifier = Modifier
                    .height(60.dp) // Standard height for the bar itself
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter) // Ensure it's at the bottom of the Box
            ) {
                val leftScreens = allScreens.subList(0, 2) // Wallet, Report
                val rightScreens = allScreens.subList(3, allScreens.size) // Target, User

                // Left items
                Row(modifier = Modifier.weight(1.2f).graphicsLayer {
                    translationY = 68.dp.value
                }) {
                    leftScreens.forEach { screen ->
                        val isSelected = selectedItemIndex == screen.index
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painterResource(id = screen.icon),
                                    contentDescription = screen.title,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .alpha(if (isSelected) 1f else 0.5f), // Adjusted alpha
                                    tint = White
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = White.copy(alpha = if (isSelected) 1f else 0.7f) // Adjusted alpha
                                    )
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                selectedItemIndex = screen.index
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent, // No indicator circle
                                selectedIconColor = White,
                                unselectedIconColor = White.copy(alpha = 0.5f),
                                selectedTextColor = White,
                                unselectedTextColor = White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Spacer for the central button (takes up space equivalent to one item)
                Box(modifier = Modifier.weight(0.2f)) {} // Adjust weight as needed for spacing

                // Right items
                Row(modifier = Modifier.weight(1.2f).graphicsLayer {
                    translationY = 68.dp.value
                }
                ) {
                    rightScreens.forEach { screen ->
                        val isSelected = selectedItemIndex == screen.index
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painterResource(id = screen.icon),
                                    contentDescription = screen.title,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .alpha(if (isSelected) 1f else 0.5f),
                                    tint = White
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = White.copy(alpha = if (isSelected) 1f else 0.7f)
                                    )
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                selectedItemIndex = screen.index
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = White,
                                unselectedIconColor = White.copy(alpha = 0.5f),
                                selectedTextColor = White,
                                unselectedTextColor = White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }

            // Central "Add" button - overlaid on top
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter) // Align to the top center of the 80.dp Box
                    .size(70.dp) // Increased size for easier tapping and visual prominence
                    .clip(CircleShape) // Make it circular
                    .clickable {
                        // selectedItemIndex = Screen.Add.index // Optionally select "Add"
                        navController.navigate(Screen.Add.route) {
                            // Decide navigation behavior for "Add"
                            // popUpTo(navController.graph.startDestinationId) { saveState = true } // If it's a main tab
                            launchSingleTop = true
                            restoreState = true // If state should be restored
                        }
                    },
                color = Orange, // Example: Use Orange for the FAB
                shadowElevation = 6.dp // Add some shadow
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_plus),
                    contentDescription = "Add",
                    modifier = Modifier
                        .size(38.dp), // Size of the icon itself
                    // Consider tinting the icon if it's a vector and not the desired color
                    // colorFilter = ColorFilter.tint(White)
                )
            }
        }
    }
}


// --- Placeholder Screens ---
@Composable
fun SimpleScreenContent(screenName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGray) // Use your theme color
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = screenName, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun WalletScreen() {
    SimpleScreenContent("Wallet Screen")
}

@Composable
fun ReportScreen() {
    SimpleScreenContent("Report Screen")
}



@Composable
fun TargetScreen() {
    SimpleScreenContent("Target Screen")
}

@Composable
fun UserScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    val orangeColor = Color(0xFFE5A547)
    val tealColor = Color(0xFF4A7C59)

    // Firebase Auth instance
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // States untuk menyimpan data user
    var userName by remember { mutableStateOf("Loading...") }
    var userEmail by remember { mutableStateOf("Loading...") }
    var userPhone by remember { mutableStateOf("Loading...") }
    var userLocation by remember { mutableStateOf("Loading...") }
    var userId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // State untuk Edit Profile Dialog
    var showEditDialog by remember { mutableStateOf(false) }

    // State untuk Logout Confirmation
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Load user data dari Firebase
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            userEmail = user.email ?: "No email"
            userId = user.uid

            // Load additional data dari Firestore
            val db = FirebaseFirestore.getInstance()
            try {
                val document = db.collection("users").document(user.uid).get().await()
                if (document.exists()) {
                    userName = document.getString("username") ?: user.displayName ?: "User"
                    userPhone = document.getString("phone") ?: "No phone number"
                    userLocation = document.getString("location") ?: "No location"
                } else {
                    userName = user.displayName ?: "User"
                    userPhone = user.phoneNumber ?: "No phone number"
                    userLocation = "No location"
                }
            } catch (e: Exception) {
                userName = user.displayName ?: "User"
                userPhone = user.phoneNumber ?: "No phone number"
                userLocation = "No location"
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    orangeColor,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Back button and Edit Profile button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* Handle back navigation */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Account",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // Profile section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        tint = orangeColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hello! $userName",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "ID: ${userId.take(9)}...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Information Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileInfoCard(
                icon = Icons.Default.Person,
                label = "Username",
                value = if (isLoading) "Loading..." else userName
            )

            ProfileInfoCard(
                icon = Icons.Default.Email,
                label = "Email",
                value = if (isLoading) "Loading..." else userEmail
            )

            ProfileInfoCard(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = if (isLoading) "Loading..." else userLocation
            )

            ProfileInfoCard(
                icon = Icons.Default.Phone,
                label = "Number",
                value = if (isLoading) "Loading..." else userPhone
            )

            // Tombol Logout
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Logout",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Edit Profile Dialog
    if (showEditDialog) {
        EditProfileDialog(
            currentUsername = userName,
            currentLocation = userLocation,
            currentPhone = userPhone,
            onDismiss = { showEditDialog = false },
            onSave = { newUsername, newLocation, newPhone ->
                // Update data ke Firestore
                currentUser?.let { user ->
                    val db = FirebaseFirestore.getInstance()
                    val userData = hashMapOf(
                        "username" to newUsername,
                        "location" to newLocation,
                        "phone" to newPhone,
                        "email" to user.email
                    )

                    db.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            // Update local state
                            userName = newUsername
                            userLocation = newLocation
                            userPhone = newPhone
                            showEditDialog = false
                        }
                        .addOnFailureListener {
                            // Handle error
                        }
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                FirebaseAuth.getInstance().signOut()
                showLogoutDialog = false
                onNavigateToLogin()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
fun EditProfileDialog(
    currentUsername: String,
    currentLocation: String,
    currentPhone: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var location by remember { mutableStateOf(currentLocation) }
    var phone by remember { mutableStateOf(currentPhone) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A7C59)
                )

                // Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Location Field
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Phone Field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(username, location, phone) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A7C59)
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Logout",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Are you sure you want to logout?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    val orangeColor = Color(0xFFE5A547)
    val tealColor = Color(0xFF4A7C59)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tealColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        orangeColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}



