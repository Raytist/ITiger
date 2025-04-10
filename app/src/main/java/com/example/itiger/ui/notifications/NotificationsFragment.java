package com.example.itiger.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.itiger.R;
import com.example.itiger.databinding.FragmentNotificationsBinding;

import java.util.ArrayList;
import java.util.Arrays;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private ListView listView;
    private ArrayList<String> tips;
    private TipsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        listView = binding.listViewTips;
        if (listView == null) {
            return root;
        }

        // Список советов
        tips = new ArrayList<>(Arrays.asList(
                "Планируйте свой день с утра, чтобы лучше организовать задачи.",
                "Используйте метод Помодоро: 25 минут работы, 5 минут отдыха.",
                "Ставьте реалистичные цели, чтобы избежать перегрузки.",
                "Делите крупные задачи на мелкие шаги для упрощения выполнения.",
                "Делайте перерывы, чтобы сохранять концентрацию.",
                "Используйте визуальные напоминания для важных задач.",
                "Отмечайте выполненные задачи, чтобы видеть прогресс.",
                "Сосредоточьтесь на одной задаче за раз для повышения эффективности."
        ));

        adapter = new TipsAdapter();
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class TipsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return tips != null ? tips.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return tips.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_tip, parent, false);
            }

            String tip = tips.get(position);
            TextView textTip = convertView.findViewById(R.id.text_tip);
            textTip.setText(tip);

            return convertView;
        }
    }
}