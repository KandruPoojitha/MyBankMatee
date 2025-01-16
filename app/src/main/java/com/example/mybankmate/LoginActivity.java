package com.example.mybankmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailField, passwordField;
    private Button loginButton;
    private FirebaseAuth auth;
    private DatabaseReference adminsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        adminsRef = FirebaseDatabase.getInstance().getReference("admins");

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        TextView forgotPassword = findViewById(R.id.forgotPassword);

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                checkIfAdmin(email, password);
            }
        });

        forgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void checkIfAdmin(String email, String password) {
        Log.d(TAG, "Checking if admin: " + email);

        adminsRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot adminSnapshot : snapshot.getChildren()) {
                        String storedPassword = adminSnapshot.child("password").getValue(String.class);
                        if (storedPassword != null && storedPassword.equals(password)) {
                            Log.d(TAG, "Admin login successful.");
                            Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }
                    Toast.makeText(LoginActivity.this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Not an admin. Proceeding with user login...");
                    loginUserWithFirebaseAuth(email, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Admin check failed: " + error.getMessage());
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUserWithFirebaseAuth(String email, String password) {
        Log.d(TAG, "Attempting user login with Firebase Authentication...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User login successful.");
                        checkIfUserNeedsPasswordReset();
                    } else {
                        Log.e(TAG, "User login failed: " + task.getException().getMessage());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfUserNeedsPasswordReset() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found.");
            Toast.makeText(LoginActivity.this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isFirstLogin = snapshot.child("isFirstLogin").getValue(Boolean.class);
                    Log.d(TAG, "isFirstLogin value: " + isFirstLogin);
                    if (Boolean.TRUE.equals(isFirstLogin)) {
                        Log.d(TAG, "First login detected. Redirecting to ResetPasswordActivity.");
                        Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
                        startActivity(intent);
                    } else {
                        Log.d(TAG, "Regular user login successful. Redirecting to HomeActivity.");
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    finish();
                } else {
                    Log.e(TAG, "User data not found for UID: " + uid);
                    Toast.makeText(LoginActivity.this, "User data not found. Please contact support.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking user data: " + error.getMessage());
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetPassword() {
        String email = emailField.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent to " + email, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
