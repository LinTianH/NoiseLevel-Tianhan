package com.example.noiselevel;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class SaveDataUtils {
    private static final String PREF_NAME = "NoiseHistory";
    private static final String KEY_HISTORY = "history";

    public static void saveNoiseData(Context context, NoiseData noiseData) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        List<NoiseData> history = getHistory(context);
        history.add(noiseData);

        Gson gson = new Gson();
        String historyJson = gson.toJson(history);
        editor.putString(KEY_HISTORY, historyJson);
        editor.apply();
    }

    public static List<NoiseData> getHistory(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String historyJson = preferences.getString(KEY_HISTORY, "");

        Gson gson = new Gson();
        NoiseData[] historyArray = gson.fromJson(historyJson, NoiseData[].class);

        List<NoiseData> history = new ArrayList<>();
        if (historyArray != null) {
            for (NoiseData data: historyArray) {
                history.add(data);
            }
        }

        return history;
    }
}
