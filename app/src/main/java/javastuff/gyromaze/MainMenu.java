package javastuff.gyromaze;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;

public class MainMenu extends AppCompatActivity {
    public enum Difficulty {Easy, Medium, Hard}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        fullscreen();
        refreshScore();
    }

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
        scoreField.setText(Integer.toString(score));
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
