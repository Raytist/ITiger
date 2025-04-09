package com.example.itiger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class ArchiveActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<MainActivity.Tetromino> completedTetrominos;
    @SuppressWarnings("FieldCanBeLocal")
    private TetrominoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        // Получаем данные из Intent
        Object serializableExtra = getIntent().getSerializableExtra("completedTetrominos");
        if (serializableExtra instanceof ArrayList) {
            completedTetrominos = (ArrayList<MainActivity.Tetromino>) serializableExtra;
        } else {
            completedTetrominos = new ArrayList<>();
        }

        // Инициализируем ListView
        listView = findViewById(R.id.listViewArchive);

        // Создаём адаптер
        adapter = new TetrominoAdapter();
        listView.setAdapter(adapter);

        // Добавляем обработчик нажатий на элементы списка
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Показываем диалог подтверждения удаления
            showDeleteConfirmationDialog(position);
        });
    }

    // Метод для показа диалога подтверждения удаления
    private void showDeleteConfirmationDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удалить элемент");
        builder.setMessage("Вы уверены, что хотите удалить этот элемент из архива?");
        builder.setPositiveButton("Да", (dialog, which) -> {
            // Удаляем элемент из списка
            completedTetrominos.remove(position);
            // Обновляем адаптер
            adapter.notifyDataSetChanged();
            // Сохраняем изменения в SharedPreferences
            saveCompletedTetrominos();
            dialog.dismiss();
        });
        builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Метод для сохранения завершённых тетромино в SharedPreferences
    private void saveCompletedTetrominos() {
        // Используем те же ключи, что в MainActivity
        SharedPreferences prefs = getSharedPreferences("TetrisPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Сохраняем количество завершённых тетромино
        editor.putInt("CompletedTetrominoCount", completedTetrominos.size());

        // Сохраняем данные каждого завершённого тетромино
        for (int i = 0; i < completedTetrominos.size(); i++) {
            MainActivity.Tetromino tetromino = completedTetrominos.get(i);
            editor.putInt("CompletedTetrominoPosition_" + i, tetromino.position);
            editor.putInt("CompletedTetrominoType_" + i, tetromino.typeIndex);
            editor.putInt("CompletedTetrominoRotation_" + i, tetromino.rotation);
            editor.putInt("CompletedTetrominoColor_" + i, tetromino.originalColor);
            editor.putString("CompletedTetrominoTitle_" + i, tetromino.title);
            editor.putString("CompletedTetrominoDescription_" + i, tetromino.description);
            editor.putString("CompletedTetrominoCategory_" + i, tetromino.category);
            editor.putInt("CompletedTetrominoDifficulty_" + i, tetromino.difficulty);
            editor.putInt("CompletedTetrominoTime_" + i, tetromino.timeToComplete);
        }

        editor.apply();
    }

    // Переопределяем onBackPressed для возврата результата
    @Override
    public void onBackPressed() {
        // Возвращаем обновлённый список completedTetrominos
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updatedCompletedTetrominos", completedTetrominos);
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }

    // Пользовательский адаптер для ListView
    private class TetrominoAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return completedTetrominos != null ? completedTetrominos.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return completedTetrominos.get(position);
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

            MainActivity.Tetromino tetromino = completedTetrominos.get(position);

            // Инициализируем элементы
            TextView textTitle = convertView.findViewById(R.id.text_title);
            TextView textDescription = convertView.findViewById(R.id.text_description);
            TextView textCategory = convertView.findViewById(R.id.text_category);
            TextView textDifficulty = convertView.findViewById(R.id.text_difficulty);
            TextView textTime = convertView.findViewById(R.id.text_time);

            // Заполняем данными
            textTitle.setText(tetromino.title);
            textDescription.setText(tetromino.description);
            textCategory.setText("Категория: " + tetromino.category);
            textDifficulty.setText("Сложность: " + tetromino.difficulty);
            int timeInMinutes = tetromino.timeToComplete / 60;
            textTime.setText("Время: " + timeInMinutes + " минут");

            return convertView;
        }
    }
}