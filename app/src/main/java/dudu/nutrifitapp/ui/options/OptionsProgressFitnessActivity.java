package dudu.nutrifitapp.ui.options;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dudu.nutrifitapp.databinding.OptionsProgressFitnessBinding;

public class OptionsProgressFitnessActivity extends AppCompatActivity {

    private OptionsProgressFitnessBinding binding;
    private DatabaseReference logsDatabase;
    private FirebaseUser currentUser;
    private static final String[] DAYS_OF_WEEK = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = OptionsProgressFitnessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        logsDatabase = FirebaseDatabase.getInstance().getReference().child("DailyStats");

        setupBackButton();
        setupTitle();
        fetchDataAndPopulateChart();
    }

    private void setupBackButton() {
        binding.backButton.setOnClickListener(v -> finish());
    }

    private void setupTitle() {
        binding.title.setText("Calories burned in the last week:");
    }

    private void fetchDataAndPopulateChart() {
        logsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Entry> entries = new ArrayList<>();

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (int i = 6; i >= 0; i--) {
                    calendar.add(Calendar.DAY_OF_YEAR, -i);
                    String date = sdf.format(calendar.getTime());
                    calendar.add(Calendar.DAY_OF_YEAR, i); // Reset the day adjustment

                    DataSnapshot dateSnapshot = dataSnapshot.child(date);
                    if (dateSnapshot.exists()) {
                        float caloriesBurned = dateSnapshot.child("caloriesBurnt").getValue(Float.class);
                        entries.add(new Entry(6 - i, caloriesBurned));
                    } else {
                        entries.add(new Entry(6 - i, 0));
                    }
                }

                LineDataSet dataSet = new LineDataSet(entries, "Calories Burned");
                dataSet.setLineWidth(3f); // Make the line thicker
                dataSet.setCircleRadius(5f); // Make the circles bigger
                dataSet.setCircleHoleRadius(2.5f); // Make the circle holes bigger
                dataSet.setValueTextSize(12f); // Make the values bigger

                LineData lineData = new LineData(dataSet);

                LineChart lineChart = binding.lineChart;
                lineChart.setData(lineData);

                XAxis xAxis = lineChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setGranularity(1f); // Minimum axis-step (interval) is 1
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value >= 0 && value < DAYS_OF_WEEK.length) {
                            return DAYS_OF_WEEK[(int) value];
                        }
                        return "";
                    }
                });

                Description description = new Description();
                description.setText("Calories burned in the past week");
                lineChart.setDescription(description);

                lineChart.invalidate(); // Refresh chart
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(OptionsProgressFitnessActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
