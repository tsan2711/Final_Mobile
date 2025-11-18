package com.example.final_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.final_mobile.services.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;
    private SessionManager sessionManager;
    private BottomNavigationView.OnNavigationItemSelectedListener navigationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is logged in
        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            // Not logged in, redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);

        initViews();
        fragmentManager = getSupportFragmentManager();
        setupBottomNavigation();
        
        // Load default fragment based on user role
        if (savedInstanceState == null) {
            if (sessionManager.isBankOfficer()) {
                // Load officer dashboard for bank officers
                loadFragment(new OfficerHomeFragment());
            } else {
                // Load regular home for customers
                loadFragment(new HomeFragment());
            }
        }
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        navigationListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                
                int itemId = item.getItemId();
                
                // Check if user is bank officer
                if (sessionManager.isBankOfficer()) {
                    // Officer navigation
                    if (itemId == R.id.nav_home) {
                        selectedFragment = new OfficerHomeFragment();
                    } else if (itemId == R.id.nav_transactions) {
                        // Officers can still see transactions
                        selectedFragment = new TransactionFragment();
                    } else if (itemId == R.id.nav_utilities) {
                        // Officers use utilities tab for customer management
                        selectedFragment = new CustomerListFragment();
                    } else if (itemId == R.id.nav_profile) {
                        selectedFragment = new ProfileFragment();
                    }
                } else {
                    // Customer navigation
                    if (itemId == R.id.nav_home) {
                        selectedFragment = new HomeFragment();
                    } else if (itemId == R.id.nav_transactions) {
                        selectedFragment = new TransactionFragment();
                    } else if (itemId == R.id.nav_utilities) {
                        selectedFragment = new UtilitiesFragment();
                    } else if (itemId == R.id.nav_profile) {
                        selectedFragment = new ProfileFragment();
                    }
                }
                
                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }
                return false;
            }
        };
        bottomNavigation.setOnItemSelectedListener(navigationListener);
    }

    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
    
    public void setBottomNavigationSelection(int itemId) {
        if (bottomNavigation != null && navigationListener != null) {
            // Check if already selected to avoid unnecessary update
            if (bottomNavigation.getSelectedItemId() == itemId) {
                return;
            }
            
            // Temporarily remove listener to prevent recursive fragment loading
            bottomNavigation.setOnItemSelectedListener(null);
            bottomNavigation.setSelectedItemId(itemId);
            // Restore listener after setting selection
            bottomNavigation.setOnItemSelectedListener(navigationListener);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check session on resume
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            // Update last activity time
            sessionManager.updateLastActivity();
        }
    }
}