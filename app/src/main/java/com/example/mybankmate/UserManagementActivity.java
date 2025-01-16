package com.example.mybankmate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class
UserManagementActivity extends AppCompatActivity {

    private EditText newUserEmail, newUserPassword, newUserMobile, newUserLegalName, searchUser;
    private AutoCompleteTextView newUserAddress;
    private Spinner pronounsSpinner;
    private RadioGroup genderGroup;
    private Button addUserButton;
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private ImageView backButton;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    private PlacesClient placesClient;
    private List<String> addressSuggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCQsIJqnQs7sbv6gr-LT1amFavJoPFvCpQ");
        }
        placesClient = Places.createClient(this);

        newUserEmail = findViewById(R.id.newUserEmail);
        newUserPassword = findViewById(R.id.newUserPassword);
        newUserMobile = findViewById(R.id.newUserMobile);
        newUserLegalName = findViewById(R.id.newUserLegalName);
        newUserAddress = findViewById(R.id.newUserAddress);
        pronounsSpinner = findViewById(R.id.pronounsSpinner);
        genderGroup = findViewById(R.id.genderGroup);
        addUserButton = findViewById(R.id.addUserButton);
        searchUser = findViewById(R.id.searchUser);
        userRecyclerView = findViewById(R.id.userRecyclerView);
        backButton = findViewById(R.id.back_button);

        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, usersRef);
        userRecyclerView.setAdapter(userAdapter);

        ArrayAdapter<String> pronounsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"She/Her/Hers", "He/Him/His"}
        );
        pronounsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pronounsSpinner.setAdapter(pronounsAdapter);

        addressSuggestions = new ArrayList<>();
        newUserAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    fetchAddressSuggestions(s.toString());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        backButton.setOnClickListener(v -> onBackPressed());

        addUserButton.setOnClickListener(v -> {
            String email = newUserEmail.getText().toString().trim();
            String password = newUserPassword.getText().toString().trim();
            String mobile = newUserMobile.getText().toString().trim();
            String address = newUserAddress.getText().toString().trim();
            String legalName = newUserLegalName.getText().toString().trim();
            String pronouns = pronounsSpinner.getSelectedItem().toString();
            int genderId = genderGroup.getCheckedRadioButtonId();
            RadioButton genderButton = findViewById(genderId);
            String gender = genderButton == null ? "" : genderButton.getText().toString();

            if (validateInput(email, password, mobile, address, legalName, gender)) {
                addUser(email, password, mobile, address, legalName, pronouns, gender);
            }
        });

        searchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userAdapter.filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadUsers();
    }

    private boolean validateInput(String email, String password, String mobile, String address, String legalName, String gender) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(mobile) || mobile.length() != 10 || !mobile.matches("\\d+")) {
            Toast.makeText(this, "Enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Address cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(legalName)) {
            Toast.makeText(this, "Legal name cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(gender)) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void addUser(String email, String password, String mobile, String address, String legalName, String pronouns, String gender) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = task.getResult().getUser().getUid();
                        String checkingAccountNumber = generateAccountNumber("CHK");
                        String savingsAccountNumber = generateAccountNumber("SAV");

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", userId);
                        userData.put("email", email);
                        userData.put("password", password);
                        userData.put("mobile", mobile);
                        userData.put("address", address);
                        userData.put("legalName", legalName);
                        userData.put("pronouns", pronouns);
                        userData.put("gender", gender);
                        userData.put("checkingAccountNumber", checkingAccountNumber);
                        userData.put("savingsAccountNumber", savingsAccountNumber);
                        userData.put("checkingBalance", "0.00");
                        userData.put("savingsBalance", "0.00");
                        userData.put("isFirstLogin", true);
                        userData.put("isActive", true);

                        usersRef.child(userId).setValue(userData)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(this, "User added successfully!", Toast.LENGTH_LONG).show();
                                        clearFields();
                                    } else {
                                        Toast.makeText(this, "Failed to add user data", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Failed to create user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearFields() {
        newUserEmail.setText("");
        newUserPassword.setText("");
        newUserMobile.setText("");
        newUserLegalName.setText("");
        newUserAddress.setText("");
        pronounsSpinner.setSelection(0);
        genderGroup.clearCheck();
    }

    private String generateAccountNumber(String prefix) {
        int randomNum = 100000 + new Random().nextInt(900000);
        return prefix + randomNum;
    }

    private void loadUsers() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    User user = data.getValue(User.class);
                    if (user != null) userList.add(user);
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserManagementActivity.this, "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAddressSuggestions(String query) {
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            addressSuggestions.clear();
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                addressSuggestions.add(prediction.getFullText(null).toString());
            }
            newUserAddress.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, addressSuggestions));
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error fetching address suggestions", Toast.LENGTH_SHORT).show();
        });
    }
}
