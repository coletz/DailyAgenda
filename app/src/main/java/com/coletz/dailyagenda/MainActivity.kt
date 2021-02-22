package com.coletz.dailyagenda

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper

import com.samsung.android.sdk.penremote.ButtonEvent
import com.samsung.android.sdk.penremote.SpenUnit

import kotlinx.coroutines.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class MainActivity : SpenActivity(), CoroutineScope {

    private lateinit var canvas: CanvasView

    private var lastSaveMs = System.currentTimeMillis()

    private val titleDateFormatter = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.getDefault())
    private val idDateFormatter = SimpleDateFormat("yyyy_MM_dd", Locale.ROOT)

    private var currentDay: Calendar = Calendar.getInstance()
        set(value) {
            fun change() {
                field = value
                supportActionBar?.title = titleDateFormatter.format(value.time)
                supportActionBar?.subtitle = calculateSubtitleByDate(value)
                tryLoadNotesForDate(value)
            }

            if (lastSaveMs < canvas.lastEditMs) {
                saveCanvas { saveToast(); change() }
            } else {
                change()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        canvas = findViewById(R.id.main_canvas)

        currentDay = Calendar.getInstance()

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_calendar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val eraser = menu?.findItem(R.id.menu_eraser) ?: return super.onPrepareOptionsMenu(menu)
        with(eraser) {
            when(canvas.isErasing) {
                true -> {
                    setIcon(R.drawable.ic_write)
                    setTitle(R.string.switch_to_write)
                }
                false -> {
                    setIcon(R.drawable.ic_eraser)
                    setTitle(R.string.switch_to_eraser)
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> openCalendarPicker()
            R.id.menu_save -> saveCanvas { saveToast() }
            R.id.menu_eraser -> toggleEraser()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun tryLoadNotesForDate(date: Calendar) = launch {
        runCatching {
            withContext(Dispatchers.IO) {
                // Will throw an exception catched by runCatching if file doesn't exists
                openFileInput(date.id).use {
                    ObjectMapper().readValue(it, Array<Drawee>::class.java).toMutableList()
                }
            }
        }.getOrNull().runCatching {
            if (this == null) {
                canvas.clear()
            } else {
                canvas.loadDrawees(this)
            }
        }
    }

    private fun saveCanvas(onSuccess: () -> Unit = {}) = launch {
        runCatching {
            val json = canvas.jsonByteArray()
            val outStream = openFileOutput(currentDay.id, Context.MODE_PRIVATE)
            withContext(Dispatchers.IO) {
                android.util.Log.e("savingJson", json.toString(Charset.defaultCharset()))
                json.inputStream().use { input ->
                    outStream.use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
            .onFailure { it.printStackTrace() }
            .onSuccess { lastSaveMs = System.currentTimeMillis(); onSuccess() }
    }

    private fun toggleEraser() {
        canvas.isErasing = !canvas.isErasing
        invalidateOptionsMenu()
    }

    private fun setEraserMode() {
        canvas.isErasing = true
        invalidateOptionsMenu()
    }

    private fun setDrawingMode() {
        canvas.isErasing = false
        invalidateOptionsMenu()
    }

    private fun openCalendarPicker() {
        DatePickerDialog(this,
            { _, year, month, dayOfMonth ->
                currentDay = Calendar.getInstance().apply {
                    this[Calendar.YEAR] = year
                    this[Calendar.MONTH] = month
                    this[Calendar.DAY_OF_MONTH] = dayOfMonth
                }
            },
            currentDay[Calendar.YEAR],
            currentDay[Calendar.MONTH],
            currentDay[Calendar.DAY_OF_MONTH]
        ).show()
    }

    override fun onSpenConnected() {
        super.onSpenConnected()
        val spenButton = spenUnitManager?.getUnit(SpenUnit.TYPE_BUTTON)
        spenUnitManager?.registerSpenEventListener({
            when (ButtonEvent(it).action) {
                ButtonEvent.ACTION_DOWN -> setEraserMode()
                ButtonEvent.ACTION_UP -> setDrawingMode()
            }
        }, spenButton)
    }

    override fun onPause() {
        super.onPause()
        val spenButton = spenUnitManager?.getUnit(SpenUnit.TYPE_BUTTON)
        spenUnitManager?.unregisterSpenEventListener(spenButton)

        if (lastSaveMs < canvas.lastEditMs) {
            saveCanvas()
        }
    }

    private fun saveToast() {
        Toast.makeText(this, R.string.saved_successfully, Toast.LENGTH_SHORT).show()
    }

    private fun calculateSubtitleByDate(calendar: Calendar): String {
        val today = Calendar.getInstance()
        var isToday = true
        if (calendar[Calendar.YEAR] != today[Calendar.YEAR]) { isToday = false }
        if (calendar[Calendar.MONTH] != today[Calendar.MONTH]) { isToday = false }
        if (calendar[Calendar.DAY_OF_MONTH] != today[Calendar.DAY_OF_MONTH]) { isToday = false }
        return if (isToday) {
            getString(R.string.subtitle_today)
        } else {
            val daysDiff = abs(floor((today.time.time - calendar.time.time) / 1000F / 60F / 60F / 24F)).roundToInt()
            if (today.time > calendar.time) {
                // Past
                resources.getQuantityString(R.plurals.subtitle_days_ago, daysDiff, daysDiff)
            } else {
                // Future
                resources.getQuantityString(R.plurals.subtitle_days_next, daysDiff, daysDiff)
            }
        }
    }

    private val Calendar.id
        get() = idDateFormatter.format(time)
}