package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.model.AssetEntity
import com.example.model.AssetWithLogs
import com.example.model.MaintenanceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SopDao {
    @Query("SELECT * FROM assets ORDER BY id ASC")
    fun observeAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY id ASC")
    suspend fun getAllAssetsOnce(): List<AssetEntity>

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getAssetCount(): Int

    @Query("SELECT COUNT(*) FROM assets WHERE pending_offline_sync = 1")
    fun observePendingOfflineSyncCount(): Flow<Int>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    fun observeAssetById(id: String): Flow<AssetEntity?>

    @Transaction
    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    fun observeAssetWithLogs(id: String): Flow<AssetWithLogs?>

    @Transaction
    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetWithLogsOnce(id: String): AssetWithLogs?

    @Query("SELECT * FROM assets WHERE maintenance_status = :status ORDER BY id ASC")
    fun observeAssetsByStatus(status: String): Flow<List<AssetEntity>>

    @Query(
        """
        SELECT * FROM assets 
        WHERE (:system = 'All' OR system = :system)
          AND (
            :query = '' 
            OR id LIKE '%' || :query || '%' 
            OR name LIKE '%' || :query || '%' 
            OR location LIKE '%' || :query || '%'
            OR maintenance_status LIKE '%' || :query || '%'
          )
        ORDER BY id ASC
        """
    )
    fun searchAssetsOffline(query: String, system: String): Flow<List<AssetEntity>>

    @Query(
        """
        SELECT * FROM assets
        WHERE :query = ''
           OR name LIKE '%' || :query || '%'
           OR location LIKE '%' || :query || '%'
           OR id LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    fun searchAssetsByNameOrLocation(query: String): Flow<List<AssetEntity>>

    @Upsert
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Upsert
    suspend fun upsertAsset(asset: AssetEntity)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Query(
        """
        UPDATE assets 
        SET maintenance_status = :status, 
            location = :location, 
            notes = :notes, 
            last_updated_at = :updatedAt, 
            pending_offline_sync = 1,
            rev = rev + 1
        WHERE id = :assetId
        """
    )
    suspend fun updateAssetStatusAndLocationOffline(
        assetId: String,
        status: String,
        location: String,
        notes: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE assets SET pending_offline_sync = 0")
    suspend fun clearAllPendingOfflineSyncFlags()

    @Query(
        """
        UPDATE assets
        SET next_due_date_iso = :dueDateIso,
            next_due_freq_code = :freqCode,
            next_due_task_note = :taskNote,
            reminder_lead_hours = :reminderLeadHours,
            notifications_enabled = :notificationsEnabled,
            dueByFreqJson = :dueByFreqJson,
            last_updated_at = :updatedAt,
            pending_offline_sync = 1,
            rev = rev + 1
        WHERE id = :assetId
        """
    )
    suspend fun updateAssetDueDateOffline(
        assetId: String,
        dueDateIso: String,
        freqCode: String,
        taskNote: String,
        reminderLeadHours: Int,
        notificationsEnabled: Boolean,
        dueByFreqJson: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM assets WHERE next_due_date_iso != '' ORDER BY next_due_date_iso ASC")
    fun observeAssetsWithScheduledDueDates(): Flow<List<AssetEntity>>

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: String)

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()

    @Query("SELECT * FROM maintenance_logs WHERE assetId = :assetId ORDER BY at DESC, logId DESC LIMIT 300")
    fun observeLogsForAsset(assetId: String): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs WHERE assetId = :assetId ORDER BY at DESC, logId DESC LIMIT 300")
    suspend fun getLogsForAssetOnce(assetId: String): List<MaintenanceLogEntity>

    @Query("SELECT * FROM maintenance_logs ORDER BY at DESC, logId DESC")
    fun observeAllLogs(): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs ORDER BY at DESC, logId DESC")
    suspend fun getAllLogsOnce(): List<MaintenanceLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MaintenanceLogEntity)
}

@Database(
    entities = [AssetEntity::class, MaintenanceLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SopDatabase : RoomDatabase() {
    abstract fun sopDao(): SopDao

    companion object {
        @Volatile
        private var INSTANCE: SopDatabase? = null

        fun getInstance(context: Context): SopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SopDatabase::class.java,
                    "akah_sop_register.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
