package com.example.mybankmate;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class ContactUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        setupExpandableContact(R.id.contact1_layout, R.id.contact1_details_layout, R.id.toggle_contact1_icon);
        setupExpandableContact(R.id.contact2_layout, R.id.contact2_details_layout, R.id.toggle_contact2_icon);
    }

    private void setupExpandableContact(int contactLayoutId, int contactDetailsLayoutId, int toggleIconId) {
        LinearLayout contactLayout = findViewById(contactLayoutId);
        LinearLayout contactDetailsLayout = findViewById(contactDetailsLayoutId);
        ImageView toggleIcon = findViewById(toggleIconId);

        contactLayout.setOnClickListener(v -> {
            if (contactDetailsLayout.getVisibility() == View.GONE) {
                contactDetailsLayout.setVisibility(View.VISIBLE);
                toggleIcon.setImageResource(R.drawable.ic_minus);
            } else {
                contactDetailsLayout.setVisibility(View.GONE);
                toggleIcon.setImageResource(R.drawable.ic_plus);
            }
        });
    }
}
