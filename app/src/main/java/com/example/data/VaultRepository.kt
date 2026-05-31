package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class VaultRepository(private val vaultDao: VaultDao) {

    // Settings
    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
        vaultDao.getSetting(key)
    }

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        vaultDao.saveSetting(VaultSetting(key, value))
    }

    // Items Flow
    val allItems: Flow<List<VaultedItem>> = vaultDao.getAllItemsFlow()
    fun getItemsByType(type: String): Flow<List<VaultedItem>> = vaultDao.getItemsByTypeFlow(type)
    
    suspend fun getItemById(id: Int): VaultedItem? = withContext(Dispatchers.IO) {
        vaultDao.getItemById(id)
    }

    // Apps Flow
    val hiddenApps: Flow<List<HiddenApp>> = vaultDao.getHiddenAppsFlow()
    suspend fun isAppHidden(packageName: String): Boolean = withContext(Dispatchers.IO) {
        vaultDao.isAppHidden(packageName)
    }
    suspend fun addHiddenApp(packageName: String, label: String) = withContext(Dispatchers.IO) {
        vaultDao.insertHiddenApp(HiddenApp(packageName, label))
    }
    suspend fun removeHiddenApp(packageName: String) = withContext(Dispatchers.IO) {
        vaultDao.deleteHiddenApp(packageName)
    }

    // Import File to Vault
    suspend fun importFileToVault(
        context: Context,
        uri: Uri,
        originalName: String,
        type: String // "PHOTO", "VIDEO", "FILE"
    ): VaultedItem? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) return@withContext null

            // Create private directory
            val vaultDir = File(context.filesDir, "vault_media")
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }

            // High security obfuscated private filename
            val extension = MimeTypeMap.getFileExtensionFromUrl(originalName) ?: "bin"
            val uniqueName = "sec_${UUID.randomUUID()}.$extension"
            val destinationFile = File(vaultDir, uniqueName)

            // Copy file securely to our app's sandbox
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            // Insert metadata into Room
            val item = VaultedItem(
                name = originalName,
                path = destinationFile.absolutePath,
                type = type,
                size = destinationFile.length()
            )
            val id = vaultDao.insertItem(item)

            // Attempt to clean up original from user storage
            try {
                contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                // If permission is lacking, it will log, but we keep our imported copy safe!
                e.printStackTrace()
            }

            return@withContext item.copy(id = id.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    // Export / Restore File back to public gallery/storage
    suspend fun exportFileFromVault(
        context: Context,
        item: VaultedItem
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(item.path)
            if (!sourceFile.exists()) return@withContext false

            val resolver = context.contentResolver
            val extension = MimeTypeMap.getFileExtensionFromUrl(item.name) ?: ""
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = when (item.type) {
                        "PHOTO" -> Environment.DIRECTORY_PICTURES + "/Restored"
                        "VIDEO" -> Environment.DIRECTORY_MOVIES + "/Restored"
                        else -> Environment.DIRECTORY_DOWNLOADS + "/Restored"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
            }

            val collectionUri = when (item.type) {
                "PHOTO" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "VIDEO" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    } else {
                        @Suppress("DEPRECATION")
                        Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    }
                }
            }

            val destUri = resolver.insert(collectionUri, contentValues) ?: return@withContext false

            resolver.openOutputStream(destUri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            // Delete private file copy & remove from Room database
            sourceFile.delete()
            vaultDao.deleteItem(item)

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // Delete item permanently from Vault
    suspend fun deleteItemFromVault(
        item: VaultedItem
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(item.path)
            if (file.exists()) {
                file.delete()
            }
            vaultDao.deleteItem(item)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // Save edited photo back to vault
    suspend fun saveEditedPhoto(
        item: VaultedItem,
        newFilePath: String,
        newSize: Long
    ) = withContext(Dispatchers.IO) {
        // Delete old file if it is stored in a different location
        if (item.path != newFilePath) {
            val oldFile = File(item.path)
            if (oldFile.exists()) {
                oldFile.delete()
            }
        }
        val updatedItem = item.copy(path = newFilePath, size = newSize, addedDate = System.currentTimeMillis())
        vaultDao.insertItem(updatedItem)
    }
}
