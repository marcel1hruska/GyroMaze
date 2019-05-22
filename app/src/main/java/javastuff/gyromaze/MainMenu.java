package javastuff.gyromaze;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;

public class MainMenu extends AppCompatActivity {
    public enum Difficulty {Easy, Medium, Hard}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Spinner spinner = findViewById(R.id.difficulty_spinner);

        // Initializing a String Array
        String[] plants = new String[]{
                "Easy",
                "Medium",
                "Hard"
        };

        // Initializing an ArrayAdapter
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                this,R.layout.spinner_item,plants
        );
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(spinnerArrayAdapter);

        fullscreen();
        refreshScore();
    }

    /**
     * Show help dialog
     * @param view
     */
    public void displayHelp(View view) {
        Intent intent = new Intent(this, Help.class);
        startActivity(intent);
    }

    /**
     * End application
     * @param view
     */
    public void endGame(View view) {
        finish();
    }

    /**
     * Start maze game
     * @param view
     */
    public void startGame(View view) {
        //set difficulty
        Spinner mySpinner = findViewById(R.id.difficulty_spinner);
        String diff = mySpinner.getSelectedItem().toString();
        Difficulty difficulty = Difficulty.Easy;
        switch (diff)
        {
            case "Easy":
                difficulty = Difficulty.Easy;
                break;
            case "Medium":
                difficulty = Difficulty.Medium;
                break;
            case "Hard":
                difficulty = Difficulty.Hard;
                break;
        }

        Intent intent = new Intent(this, Game.class);
        intent.putExtra("difficulty", difficulty.ordinal());
        startActivity(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            refreshScore();
            fullscreen();
        }
    }

    /**
     * Refreshes score on in main menu
     */
    private void refreshScore() {
        // retrieve highscore
        SharedPreferences prefs = getSharedPreferences("score_value_unique_key", Context.MODE_PRIVATE);
        int score = prefs.getInt("score", 0);

        //show score
        TextView scoreField = findViewById(R.id.score);
        scoreField.setText("Highscore: " + Integer.toString(score));
    }

    /**
     * Makes game fullscreen
     */
    private void fullscreen()
    {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
