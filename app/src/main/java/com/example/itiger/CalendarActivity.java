package com.example.itiger;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CalendarView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "TetrisPrefs";
    private static final String KEY_DATES_WITH_DATA = "DatesWithData";
    private static final String KEY_MODIFIED_DATES = "ModifiedDates";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_home);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        calendarView = findViewById(R.id.calendarView);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
            intent.putExtra("selectedDate", selectedDate);
            startActivity(intent);
        });

        // Пометка изменённых дней недоступна в стандартном CalendarView напрямую,
        // поэтому можно использовать визуальные эффекты через MainActivity при выборе даты
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Здесь можно обновить визуализацию, но стандартный CalendarView не поддерживает кастомные метки
        // Рекомендуется перейти на com.applandeo.materialcalendarview.CalendarView для полной функциональности
    }
}