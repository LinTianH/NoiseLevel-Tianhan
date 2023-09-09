package com.example.noiselevel;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class HistoricalDataActivity extends AppCompatActivity {

    private ListView listView;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_data);

        listView = findViewById(R.id.listView);
        backButton = findViewById(R.id.backButton);

        // Fetch historical data and populate the ListView
        displayHistoricalData();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // Retrieve and display historical data in the ListView
    private void displayHistoricalData() {
        // Retrieve historical data from the database
        List<String> data = new ArrayList<>(); // Modify this to hold your data

        // Create an ArrayAdapter to display the data in the ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);
    }
}
