package com.trackmyfit.app.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.Period

/** Reads step counts that the phone / watch already records, via Android Health Connect. */
class StepsRepository(private val context: Context) {

    enum class Availability { AVAILABLE, NEEDS_INSTALL_OR_UPDATE, UNSUPPORTED }

    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(context, PROVIDER)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_INSTALL_OR_UPDATE
        else -> if (android.os.Build.VERSION.SDK_INT < 34) Availability.NEEDS_INSTALL_OR_UPDATE else Availability.UNSUPPORTED
    }

    private val client: HealthConnectClient? by lazy {
        if (availability() == Availability.AVAILABLE) HealthConnectClient.getOrCreate(context) else null
    }

    suspend fun hasPermission(): Boolean = try {
        client?.permissionController?.getGrantedPermissions()?.containsAll(permissions) == true
    } catch (e: Exception) { false }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun stepsOn(date: LocalDate): Long? = try {
        val c = client ?: return null
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        c.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(start, end)))[StepsRecord.COUNT_TOTAL] ?: 0L
    } catch (e: Exception) { null }

    /** Steps per day for the last [days] days ending today (oldest first). */
    suspend fun dailySteps(days: Int, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Long>>? = try {
        val c = client ?: return null
        val start = today.minusDays((days - 1).toLong())
        val res = c.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            ),
        )
        val byDate = res.associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        (0 until days).map { i -> start.plusDays(i.toLong()).let { d -> d to (byDate[d] ?: 0L) } }
    } catch (e: Exception) { null }

    fun installIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
        setPackage("com.android.vending")
        data = Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding")
        putExtra("overlay", true)
        putExtra("callerId", context.packageName)
    }

    fun settingsIntent(): Intent =
        if (android.os.Build.VERSION.SDK_INT >= 34) Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS").putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
        else Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)

    companion object {
        const val PROVIDER = "com.google.android.apps.healthdata"
    }
}
