package com.example.noiselevel;

public class NoiseData {
    private double kamparNoiseLevel;
    private long timestamp;

    public NoiseData(double kamparNoiseLevel, long timestamp) {
        this.kamparNoiseLevel = kamparNoiseLevel;
        this.timestamp = timestamp;
    }

    public double getKamparNoiseLevel(){
        return kamparNoiseLevel;
    }

    public long getTimestamp(){
        return timestamp;
    }
}
