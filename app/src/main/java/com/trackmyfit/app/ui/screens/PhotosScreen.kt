package com.trackmyfit.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.ProgressPhoto
import com.trackmyfit.app.ui.MainViewModel
import com.trackmyfit.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

@Composable
fun PhotoThumb(vm: MainViewModel, p: ProgressPhoto, modifier: Modifier = Modifier, sample: Int = 4) {
    var img by remember(p.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(p.id) {
        img = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(vm.photoFile(p).path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap() }.getOrNull()
        }
    }
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } ?: Icon(Icons.Filled.Image, null)
    }
}

@Composable
fun PhotosScreen(vm: MainViewModel, onBack: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var date by remember { mutableStateOf(LocalDate.now()) }
    var viewing by remember { mutableStateOf<ProgressPhoto?>(null) }
    var compare by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ProgressPhoto?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var addMenu by remember { mutableStateOf(false) }

    val done: (Boolean) -> Unit = { ok -> if (!ok) Toast.makeText(ctx, "Couldn't save photo", Toast.LENGTH_SHORT).show() }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.addPhoto(uri, date, done) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = cameraUri
        if (ok && u != null) vm.addPhoto(u, date, done)
    }
    fun launchCamera() {
        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
        val f = File(dir, "capture.jpg")
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        cameraUri = uri
        runCatching { cameraLauncher.launch(uri) }.onFailure { Toast.makeText(ctx, "No camera app found", Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress photos") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { if (data.photos.size >= 2) TextButton(onClick = { compare = true }) { Icon(Icons.Filled.Compare, null); Spacer(Modifier.width(4.dp)); Text("Compare") } },
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(onClick = { addMenu = true }, icon = { Icon(Icons.Filled.AddAPhoto, null) }, text = { Text("Add photo") })
                DropdownMenu(addMenu, { addMenu = false }) {
                    DropdownMenuItem(text = { Text("Take photo") }, leadingIcon = { Icon(Icons.Filled.PhotoCamera, null) }, onClick = { addMenu = false; date = LocalDate.now(); launchCamera() })
                    DropdownMenuItem(text = { Text("Choose from gallery") }, leadingIcon = { Icon(Icons.Filled.PhotoLibrary, null) }, onClick = {
                        addMenu = false; date = LocalDate.now()
                        pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                }
            }
        },
    ) { padding ->
        if (data.photos.isEmpty()) {
            EmptyState(Icons.Filled.PhotoCamera, "No photos yet",
                "Progress photos often show changes the scale doesn't. Photos are stored privately inside the app and are not added to your gallery.",
                Modifier.padding(padding))
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), contentPadding = PaddingValues(12.dp, padding.calculateTopPadding(), 12.dp, 96.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(data.photos.reversed(), key = { it.id }) { p ->
                Column(Modifier.clickable { viewing = p }) {
                    PhotoThumb(vm, p, Modifier.fillMaxWidth().aspectRatio(3f / 4f))
                    Text(fmtShortDate(LocalDate.parse(p.date)) + " " + LocalDate.parse(p.date).year, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    viewing?.let { p ->
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = Color.Black) {
                Column(Modifier.systemBarsPadding()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        IconButton(onClick = { viewing = null }) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
                        Text(fmtLongDate(LocalDate.parse(p.date)), color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { confirmDelete = p }) { Icon(Icons.Filled.Delete, "Delete", tint = Color.White) }
                    }
                    PhotoThumb(vm, p, Modifier.fillMaxSize(), sample = 1)
                }
            }
        }
    }
    if (compare) {
        var a by remember { mutableStateOf(data.photos.first()) }
        var b by remember { mutableStateOf(data.photos.last()) }
        Dialog(onDismissRequest = { compare = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.systemBarsPadding().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { compare = false }) { Icon(Icons.Filled.Close, "Close") }
                        Text("Compare", style = MaterialTheme.typography.titleLarge)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(a, b).forEachIndexed { i, p ->
                            Column(Modifier.weight(1f)) {
                                PhotoThumb(vm, p, Modifier.fillMaxWidth().aspectRatio(3f / 4f), sample = 2)
                                Text(fmtLongDate(LocalDate.parse(p.date)), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text(if (i == 0) "Before" else "After", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(a.date), LocalDate.parse(b.date))
                    Text("$days days apart", modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary)
                    Text("Tap a photo below to choose Before, long-press to choose After", style = MaterialTheme.typography.labelSmall)
                    LazyVerticalGrid(GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        items(data.photos, key = { it.id }) { p ->
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                                    .then(if (p == a || p == b) Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)).padding(3.dp) else Modifier)
                                    .combinedClickableCompat(onClick = { a = p }, onLongClick = { b = p }),
                            ) { PhotoThumb(vm, p, Modifier.fillMaxSize(), sample = 8) }
                        }
                    }
                }
            }
        }
    }
    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete photo?") },
            text = { Text("Photo from ${fmtLongDate(LocalDate.parse(p.date))} will be permanently deleted.") },
            confirmButton = { TextButton(onClick = { vm.deletePhoto(p); confirmDelete = null; viewing = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    this.then(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick))
