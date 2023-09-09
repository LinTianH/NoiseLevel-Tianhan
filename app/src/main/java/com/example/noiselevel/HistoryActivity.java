package com.example.noiselevel;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView listView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);

        // Fetch historical data and populate the ListView
        displayHistoricalData();
    }

    // Retrieve and display historical data in the ListView
    private void displayHistoricalData() {
        // Retrieve historical data from your database or source
        List<String> data = new ArrayList<>(); // Replace with your historical data

        // Create an ArrayAdapter to display the data in the ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);
    }
}
