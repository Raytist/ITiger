package com.example.itiger;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ArchiveActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<TetrominoWithDate> archivedTetrominos;
    private TetrominoAdapter adapter;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "TetrisPrefs";
    private static final int ARCHIVE_REQUEST_CODE = 1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Используем правильную разметку для активити, а не фрагмента
        setContentView(R.layout.activity_archive); // Предполагается, что вы создадите activity_archive.xml

        listView = findViewById(R.id.listViewArchive);
        if (listView == null) {
            Log.e("ArchiveActivity", "ListView with id 'listViewArchive' not found in layout");
            Toast.makeText(this, "Ошибка: ListView не найден", Toast.LENGTH_LONG).show();
            return;
        }

        archivedTetrominos = new ArrayList<>();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loadArchivedTetrominos();
        adapter = new TetrominoAdapter();
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d("ArchiveActivity", "Item clicked at position: " + position + ", id: " + id);
            showDeleteConfirmationDialog(position);
        });
    }

    private void loadArchivedTetrominos() {
        Set<String> datesWithData = prefs.getStringSet("DatesWithData", new HashSet<>());
        Log.d("ArchiveActivity", "Dates with data: " + datesWithData.toString());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        archivedTetrominos.clear();
        for (String date : datesWithData) {
            String datePrefix = date + "_";
            int completedTetrominoCount = prefs.getInt(datePrefix + "CompletedTetrominoCount", 0);
            Log.d("ArchiveActivity", "Date: " + date + ", Completed count: " + completedTetrominoCount);
            for (int i = 0; i < completedTetrominoCount; i++) {
                int position = prefs.getInt(datePrefix + "CompletedTetrominoPosition_" + i, 0);
                int typeIndex = prefs.getInt(datePrefix + "CompletedTetrominoType_" + i, 0);
                int rotation = prefs.getInt(datePrefix + "CompletedTetrominoRotation_" + i, 0);
                int color = prefs.getInt(datePrefix + "CompletedTetrominoColor_" + i, ContextCompat.getColor(this, android.R.color.holo_blue_light));
                String title = prefs.getString(datePrefix + "CompletedTetrominoTitle_" + i, "");
                String description = prefs.getString(datePrefix + "CompletedTetrominoDescription_" + i, "");
                String category = prefs.getString(datePrefix + "CompletedTetrominoCategory_" + i, "");
                int difficulty = prefs.getInt(datePrefix + "CompletedTetrominoDifficulty_" + i, 1);
                int timeToComplete = prefs.getInt(datePrefix + "CompletedTetrominoTime_" + i, 0);

                int[] shape = generateShapeFromTimeAndDifficulty(timeToComplete, difficulty, rotation);
                Tetromino tetromino = new Tetromino(position, shape, color, typeIndex, rotation,
                        title, description, category, difficulty, timeToComplete);
                archivedTetrominos.add(new TetrominoWithDate(tetromino, date));
                Log.d("ArchiveActivity", "Added tetromino: " + title + " for date: " + date);
            }
        }
        Log.d("ArchiveActivity", "Total archived tetrominos: " + archivedTetrominos.size());
    }

    private int[] generateShapeFromTimeAndDifficulty(int timeInSeconds, int difficulty, int rotation) {
        final int SECONDS_PER_COLUMN = 2 * 60 * 60;
        int columns = (int) Math.ceil((double) timeInSeconds / SECONDS_PER_COLUMN);
        columns = Math.min(columns, 8);
        int rows = Math.min(difficulty, 8);

        if (rotation % 2 == 1) {
            int temp = rows;
            rows = columns;
            columns = temp;
        }

        rows = Math.min(rows, 8);
        columns = Math.min(columns, 8);

        if (rows == 0) rows = 1;
        if (columns == 0) columns = 1;

        int[] shape = new int[rows * columns];
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                shape[index++] = row * 8 + col;
            }
        }
        return shape;
    }

    private void showDeleteConfirmationDialog(final int position) {
        if (isFinishing() || isDestroyed()) {
            Log.d("ArchiveActivity", "Cannot show dialog, activity is finishing or destroyed");
            return;
        }
        Log.d("ArchiveActivity", "Showing delete confirmation dialog for position: " + position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удалить элемент");
        builder.setMessage("Вы уверены, что хотите удалить этот элемент из архива?");
        builder.setPositiveButton("Да", (dialog, which) -> {
            Log.d("ArchiveActivity", "Confirmed deletion at position: " + position);
            archivedTetrominos.remove(position);
            adapter.notifyDataSetChanged();
            Log.d("ArchiveActivity", "List updated, new size: " + archivedTetrominos.size());
            saveUpdatedArchive();
            dialog.dismiss();
        });
        builder.setNegativeButton("Нет", (dialog, which) -> {
            Log.d("ArchiveActivity", "Cancelled deletion");
            dialog.dismiss();
        });
        runOnUiThread(() -> {
            AlertDialog dialog = builder.create();
            dialog.show();
            Log.d("ArchiveActivity", "Dialog should be visible now");
        });
    }

    private void saveUpdatedArchive() {
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> datesWithData = new HashSet<>(prefs.getStringSet("DatesWithData", new HashSet<>()));
        Set<String> modifiedDates = new HashSet<>(prefs.getStringSet("ModifiedDates", new HashSet<>()));

        for (String date : datesWithData) {
            String datePrefix = date + "_";
            editor.putInt(datePrefix + "CompletedTetrominoCount", 0);
        }

        for (TetrominoWithDate item : archivedTetrominos) {
            String datePrefix = item.date + "_";
            int count = prefs.getInt(datePrefix + "CompletedTetrominoCount", 0);
            editor.putInt(datePrefix + "CompletedTetrominoPosition_" + count, item.tetromino.position);
            editor.putInt(datePrefix + "CompletedTetrominoType_" + count, item.tetromino.typeIndex);
            editor.putInt(datePrefix + "CompletedTetrominoRotation_" + count, item.tetromino.rotation);
            editor.putInt(datePrefix + "CompletedTetrominoColor_" + count, item.tetromino.originalColor);
            editor.putString(datePrefix + "CompletedTetrominoTitle_" + count, item.tetromino.title);
            editor.putString(datePrefix + "CompletedTetrominoDescription_" + count, item.tetromino.description);
            editor.putString(datePrefix + "CompletedTetrominoCategory_" + count, item.tetromino.category);
            editor.putInt(datePrefix + "CompletedTetrominoDifficulty_" + count, item.tetromino.difficulty);
            editor.putInt(datePrefix + "CompletedTetrominoTime_" + count, item.tetromino.timeToComplete);
            editor.putInt(datePrefix + "CompletedTetrominoCount", count + 1);
            modifiedDates.add(item.date);
        }

        Set<String> updatedDatesWithData = new HashSet<>();
        for (TetrominoWithDate item : archivedTetrominos) {
            updatedDatesWithData.add(item.date);
        }
        for (String date : datesWithData) {
            String datePrefix = date + "_";
            if (prefs.getInt(datePrefix + "TetrominoCount", 0) > 0) {
                updatedDatesWithData.add(date);
            }
        }
        editor.putStringSet("DatesWithData", updatedDatesWithData);
        editor.putStringSet("ModifiedDates", modifiedDates);

        editor.apply();
        Log.d("ArchiveActivity", "Archive saved, DatesWithData: " + updatedDatesWithData.toString());
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updatedCompletedTetrominos", archivedTetrominos);
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }

    private class TetrominoAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return archivedTetrominos != null ? archivedTetrominos.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return archivedTetrominos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ArchiveActivity.this)
                        .inflate(R.layout.item_tetromino, parent, false);
            }

            TetrominoWithDate item = archivedTetrominos.get(position);
            Tetromino tetromino = item.tetromino;

            TextView textTitle = convertView.findViewById(R.id.text_title);
            TextView textDescription = convertView.findViewById(R.id.text_description);
            TextView textCategory = convertView.findViewById(R.id.text_category);
            TextView textDifficulty = convertView.findViewById(R.id.text_difficulty);
            TextView textTime = convertView.findViewById(R.id.text_time);

            textTitle.setText(tetromino.title + " (" + item.date + ")");
            textDescription.setText(tetromino.description);
            textCategory.setText("Категория: " + tetromino.category);
            textDifficulty.setText("Сложность: " + tetromino.difficulty);
            int timeInMinutes = tetromino.timeToComplete / 60;
            textTime.setText("Время: " + timeInMinutes + " минут");

            return convertView;
        }
    }

    // Вспомогательный класс для сериализации
    public static class TetrominoWithDate implements java.io.Serializable {
        Tetromino tetromino;
        String date;

        TetrominoWithDate(Tetromino tetromino, String date) {
            this.tetromino = tetromino;
            this.date = date;
        }
    }
}