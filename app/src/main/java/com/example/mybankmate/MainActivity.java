package com.example.mybankmate;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_container, new HomeFragment())
                    .commit();
        }
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_accounts) {
                selectedFragment = new AccountsFragment();
            } else if (itemId == R.id.nav_move_money) {
                selectedFragment = new MoveMoneyFragment();
            } else if (itemId == R.id.nav_more) {
                selectedFragment = new MoreFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }
}
