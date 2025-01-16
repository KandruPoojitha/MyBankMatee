package com.example.mybankmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText newPasswordField;
    private Button resetPasswordButton;
    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        String userId = currentUser.getUid();

        newPasswordField = findViewById(R.id.newPasswordField);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);

        resetPasswordButton.setOnClickListener(v -> {
            String newPassword = newPasswordField.getText().toString().trim();
            if (isPasswordValid(newPassword)) {
                resetPassword(userId, newPassword);
            }
        });
    }

    private boolean isPasswordValid(String password) {
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void resetPassword(String userId, String newPassword) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateIsFirstLogin(userId);
                    } else {
                        Toast.makeText(this, "Password reset failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateIsFirstLogin(String userId) {
        usersRef.child(userId).child("isFirstLogin").setValue(false)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password reset successful. Please log in again.", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    } else {
                        Toast.makeText(this, "Failed to update user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
