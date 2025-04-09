package com.example.itiger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "TetrisPrefs";
    private static final String KEY_DATES_WITH_DATA = "DatesWithData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        calendarView = findViewById(R.id.calendarView);

        // Добавляем декоратор для дней с данными
        updateCalendarDecorations();

        calendarView.setOnDateSelectedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
                String selectedDate = String.format(Locale.US, "%04d-%02d-%02d",
                        date.getYear(), date.getMonth() + 1, date.getDay());
                Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
                intent.putExtra("selectedDate", selectedDate);
                startActivity(intent);
            }
        });
    }

    private void updateCalendarDecorations() {
        Set<String> datesWithData = prefs.getStringSet(KEY_DATES_WITH_DATA, new HashSet<>());
        for (String dateStr : datesWithData) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date date = sdf.parse(dateStr);
                CalendarDay calendarDay = CalendarDay.from(date);
                calendarView.addDecorator(new EventDecorator(android.R.color.holo_orange_light, calendarDay));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Класс декоратора для отметки дней
    private static class EventDecorator implements com.prolificinteractive.materialcalendarview.DayViewDecorator {
        private final int color;
        private final CalendarDay date;

        EventDecorator(int color, CalendarDay date) {
            this.color = color;
            this.date = date;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return day.equals(date);
        }

        @Override
        public void decorate(com.prolificinteractive.materialcalendarview.DayViewFacade view) {
            view.addSpan(new com.prolificinteractive.materialcalendarview.spans.DotSpan(5, color));
        }
    }
}