package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_settings")
data class VaultSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "vaulted_items")
data class VaultedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String, // Local private path inside app directory
    val type: String, // "PHOTO", "VIDEO", "FILE"
    val size: Long,
    val addedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_apps")
data class HiddenApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val isHidden: Boolean = true
)

@Dao
interface VaultDao {
    // Settings
    @Query("SELECT value FROM vault_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: VaultSetting)

    // Items
    @Query("SELECT * FROM vaulted_items ORDER BY addedDate DESC")
    fun getAllItemsFlow(): Flow<List<VaultedItem>>

    @Query("SELECT * FROM vaulted_items WHERE type = :type ORDER BY addedDate DESC")
    fun getItemsByTypeFlow(type: String): Flow<List<VaultedItem>>

    @Query("SELECT * FROM vaulted_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Int): VaultedItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultedItem): Long

    @Delete
    suspend fun deleteItem(item: VaultedItem)

    // Hidden Apps
    @Query("SELECT * FROM hidden_apps")
    fun getHiddenAppsFlow(): Flow<List<HiddenApp>>

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_apps WHERE packageName = :packageName LIMIT 1)")
    suspend fun isAppHidden(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenApp(app: HiddenApp)

    @Query("DELETE FROM hidden_apps WHERE packageName = :packageName")
    suspend fun deleteHiddenApp(packageName: String)
}

@Database(entities = [VaultSetting::class, VaultedItem::class, HiddenApp::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: android.content.Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
