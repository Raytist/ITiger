package com.example.itiger.ui.dashboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.itiger.R;
import com.example.itiger.Tetromino;
import com.example.itiger.TetrominoWithDate;
import com.example.itiger.databinding.FragmentDashboardBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ListView listView;
    private ArrayList<TetrominoWithDate> archivedTetrominos;
    private TetrominoAdapter adapter;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "TetrisPrefs";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        listView = binding.listViewArchive; // Используем binding для доступа к ListView
        if (listView == null) {
            Log.e("DashboardFragment", "ListView with id 'listViewArchive' not found in layout");
            return root;
        }

        archivedTetrominos = new ArrayList<>();
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, requireActivity().MODE_PRIVATE);

        loadArchivedTetrominos();
        adapter = new TetrominoAdapter();
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged(); // Уведомляем адаптер о данных

        // Добавляем обработчик кликов для показа диалога удаления
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d("DashboardFragment", "Clicked item at position: " + position);
            showDeleteConfirmationDialog(position);
        });

        return root;
    }

    private void loadArchivedTetrominos() {
        Set<String> datesWithData = prefs.getStringSet("DatesWithData", new HashSet<>());
        Log.d("DashboardFragment", "Dates with data: " + datesWithData.toString());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        archivedTetrominos.clear(); // Очищаем список перед загрузкой
        for (String date : datesWithData) {
            String datePrefix = date + "_";
            int completedTetrominoCount = prefs.getInt(datePrefix + "CompletedTetrominoCount", 0);
            Log.d("DashboardFragment", "Date: " + date + ", Completed count: " + completedTetrominoCount);
            for (int i = 0; i < completedTetrominoCount; i++) {
                int position = prefs.getInt(datePrefix + "CompletedTetrominoPosition_" + i, 0);
                int typeIndex = prefs.getInt(datePrefix + "CompletedTetrominoType_" + i, 0);
                int rotation = prefs.getInt(datePrefix + "CompletedTetrominoRotation_" + i, 0);
                int color = prefs.getInt(datePrefix + "CompletedTetrominoColor_" + i, ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));
                String title = prefs.getString(datePrefix + "CompletedTetrominoTitle_" + i, "");
                String description = prefs.getString(datePrefix + "CompletedTetrominoDescription_" + i, "");
                String category = prefs.getString(datePrefix + "CompletedTetrominoCategory_" + i, "");
                int difficulty = prefs.getInt(datePrefix + "CompletedTetrominoDifficulty_" + i, 1);
                int timeToComplete = prefs.getInt(datePrefix + "CompletedTetrominoTime_" + i, 0);

                int[] shape = generateShapeFromTimeAndDifficulty(timeToComplete, difficulty, rotation);
                Tetromino tetromino = new Tetromino(position, shape, color, typeIndex, rotation,
                        title, description, category, difficulty, timeToComplete);
                archivedTetrominos.add(new TetrominoWithDate(tetromino, date));
                Log.d("DashboardFragment", "Added tetromino: " + title + " for date: " + date);
            }
        }
        Log.d("DashboardFragment", "Total archived tetrominos: " + archivedTetrominos.size());
    }

    private int[] generateShapeFromTimeAndDifficulty(int timeInSeconds, int difficulty, int rotation) {
        final int SECONDS_PER_COLUMN = 2 * 60 * 60;
        int columns = (int) Math.ceil((double) timeInSeconds / SECONDS_PER_COLUMN);
        columns = Math.min(columns, 8); // WIDTH из MainActivity
        int rows = Math.min(difficulty, 8); // HEIGHT из MainActivity

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
                shape[index++] = row * 8 + col; // WIDTH = 8
            }
        }
        return shape;
    }

    private void showDeleteConfirmationDialog(final int position) {
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            Log.d("DashboardFragment", "Cannot show dialog, activity is null or finishing");
            return;
        }
        Log.d("DashboardFragment", "Showing delete confirmation dialog for position: " + position);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Удалить элемент");
        builder.setMessage("Вы уверены, что хотите удалить этот элемент из архива?");
        builder.setPositiveButton("Да", (dialog, which) -> {
            Log.d("DashboardFragment", "Confirmed deletion at position: " + position);
            archivedTetrominos.remove(position);
            adapter.notifyDataSetChanged();
            Log.d("DashboardFragment", "List updated, new size: " + archivedTetrominos.size());
            saveUpdatedArchive();
            dialog.dismiss();
        });
        builder.setNegativeButton("Нет", (dialog, which) -> {
            Log.d("DashboardFragment", "Cancelled deletion");
            dialog.dismiss();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        Log.d("DashboardFragment", "Dialog should be visible now");
    }

    private void saveUpdatedArchive() {
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> datesWithData = new HashSet<>(prefs.getStringSet("DatesWithData", new HashSet<>()));
        Set<String> modifiedDates = new HashSet<>(prefs.getStringSet("ModifiedDates", new HashSet<>()));

        // Очищаем данные по датам
        for (String date : datesWithData) {
            String datePrefix = date + "_";
            editor.putInt(datePrefix + "CompletedTetrominoCount", 0);
        }

        // Сохраняем обновлённые данные
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

        // Обновляем список дат с данными
        Set<String> updatedDatesWithData = new HashSet<>();
        for (TetrominoWithDate item : archivedTetrominos) {
            updatedDatesWithData.add(item.date);
        }
        editor.putStringSet("DatesWithData", updatedDatesWithData);
        editor.putStringSet("ModifiedDates", modifiedDates);

        editor.apply();
        Log.d("DashboardFragment", "Archive saved, DatesWithData: " + updatedDatesWithData.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
                convertView = LayoutInflater.from(requireContext())
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
}