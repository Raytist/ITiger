package com.example.itiger;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.itiger.databinding.ActivityBottomNuvBinding;

public class BottomNuv extends AppCompatActivity {

    private ActivityBottomNuvBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using View Binding
        binding = ActivityBottomNuvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the ActionBar (required for NavigationUI)
        setSupportActionBar(binding.toolbar); // Replace 'toolbar' with your Toolbar ID if you have one

        // Set up Navigation
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_bottom_nuv);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}