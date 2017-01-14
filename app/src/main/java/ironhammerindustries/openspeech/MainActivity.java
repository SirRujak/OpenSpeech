package ironhammerindustries.openspeech;

import android.app.ActionBar;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private String[][] keys1Array,keys2Array;
    private ArrayList<ArrayList<String>> keys1, keys2;
    private JSONObject keysObject, keysObject2;

    private boolean isShifted;
    private boolean isShiftedOnce;
    private boolean isOnLeftPane;



    private ArrayList<String> activeStringList;
    private String activeString;
    private String currentWordString;

    private int rightSet, rightRow, rightColumn;
    private int leftRow;

    private ArrayList<LinearLayout> upperLayout, lowerLayout;

    private Runnable runnable;
    private Handler handler;
    private int updateDelay;
    private int updateJustTappedDelay;

    private long lastTime;
    private long currentTime;
    private ArrayList<ArrayList<String>> keys1Type, keys2Type;

    // Right pane:
    // 1: At the set level.
    // 2: At the row level.
    // 3: At the column level.
    private enum Level {
        SET, ROW, COLUMN
    }

    private Level level;

    private LinearLayout rightSet1, rightSet2;

    private boolean type;

    private TextView mainText;

    private TextToSpeech textToSpeech;

    private TextPredictor textPredictor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        this.setupKeys();
        this.addKeysToRightPane();
        this.setupScroller();
        this.setupPredictor();
        this.testFunc();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.textPredictor.saveUserDict(this.getBaseContext());
    }

    private void setupPredictor() {
        this.textPredictor = new TextPredictor(this);
    }

    private void setupScroller() {
        this.isShifted = false;
        this.isShiftedOnce = false;
        this.isOnLeftPane = false;
        this.activeStringList = new ArrayList<>();
        this.activeString = "";
        this.currentWordString = "hello";
        this.rightSet = 1;
        this.rightRow = 0;
        this.rightColumn = 0;
        this.leftRow = 0;
        this.updateDelay = 1000;
        this.updateJustTappedDelay = 1300;
        this.level = Level.SET;
        this.type = true;
        this.lastTime = 0L;
        this.currentTime = 0L;

        this.updateActiveString();

        this.textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });

        this.mainText = (TextView) findViewById(R.id.upper_text_region);
        this.mainText.setText(this.activeString);

        this.rightSet1 = (LinearLayout) findViewById(R.id.upper_keys);
        this.rightSet2 = (LinearLayout) findViewById(R.id.lower_keys);

        this.handler = new Handler();

        this.runnable = new Runnable() {
            @Override
            public void run() {
                updateScroller(true);
                handler.postDelayed(this, updateDelay);

            }
        };
        handler.post(runnable);
    }

    private void updateScroller(boolean updateNumbers) {
        if (this.isOnLeftPane) {
            // Here is for the left pane stuff.
        } else {
            // Here is for the right pane stuff.
            switch (level) {
                case SET:
                    if (updateNumbers) {
                        if (this.rightSet == 0) {
                            this.rightSet = 1;
                        } else {
                            this.rightSet = 0;
                        }
                    }

                    this.resetVisualizations();

                    this.updateCurrentSetVisualization();

                    break;
                case ROW:
                    if (updateNumbers) {
                        this.rightRow += 1;
                    }
                    if (this.rightRow >= this.upperLayout.size()) {
                        this.rightRow = 0;
                        this.level = Level.SET;
                        this.resetVisualizations();
                        this.updateScroller(false);
                    } else {
                        this.updateCurrentRowVisualization();
                    }
                    break;
                case COLUMN:
                    if (updateNumbers) {
                        this.rightColumn += 1;
                    }
                    if (this.rightColumn > this.upperLayout.get(0).getChildCount()) {
                        this.rightColumn = 0;
                        this.level = Level.ROW;
                        this.resetVisualizations();
                        this.updateScroller(false);
                    } else {
                        this.updateCurrentColumnVisualization();
                    }
                    break;
            }
        }
    }

    private void resetVisualizations() {
        this.rightSet1.setBackgroundColor(Color.WHITE);
        this.rightSet2.setBackgroundColor(Color.WHITE);
        if (this.rightSet == 0) {
            for (int i = 0; i < this.upperLayout.size(); i++) {
                this.upperLayout.get(i).setBackgroundColor(Color.parseColor("#d2e0f1"));
                for (int j = 0; j < this.upperLayout.get(i).getChildCount(); j++) {
                    this.upperLayout.get(i).getChildAt(j).setBackgroundColor(Color.WHITE);
                }
            }
        } else {
            for (int i = 0; i < this.lowerLayout.size(); i++) {
                this.lowerLayout.get(i).setBackgroundColor(Color.parseColor("#d2e0f1"));
                for (int j = 0; j < this.lowerLayout.get(i).getChildCount(); j++) {
                    this.lowerLayout.get(i).getChildAt(j).setBackgroundColor(Color.WHITE);
                }
            }
        }
        this.testFunc();
    }

    private void updateCurrentColumnVisualization() {
        if (this.rightSet == 0) {
            this.upperLayout.get(this.rightRow).setBackgroundColor(Color.parseColor("#d2e0f1"));
            for (int j = 0; j < this.upperLayout.get(this.rightRow).getChildCount(); j++) {
                if (j == this.rightColumn) {
                    this.upperLayout.get(this.rightRow).getChildAt(j).setBackgroundColor(Color.parseColor("#ff9dd2a5"));
                } else {
                    this.upperLayout.get(this.rightRow).getChildAt(j).setBackgroundColor(Color.WHITE);
                }
            }
        } else {
            this.lowerLayout.get(this.rightRow).setBackgroundColor(Color.parseColor("#d2e0f1"));
            for (int j = 0; j < this.lowerLayout.get(this.rightRow).getChildCount(); j++) {
                if (j == this.rightColumn) {
                    this.lowerLayout.get(this.rightRow).getChildAt(j).setBackgroundColor(Color.parseColor("#ff9dd2a5"));
                } else {
                    this.lowerLayout.get(this.rightRow).getChildAt(j).setBackgroundColor(Color.WHITE);
                }
            }

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.handler.removeCallbacksAndMessages(null);
                this.keyActivated();
                this.updateScroller(false);
                this.handler.postDelayed(runnable,this.updateJustTappedDelay);
        }
        return true;
    }

    private void keyActivated() {
        switch (level) {
            case SET:
                this.level = Level.ROW;
                this.rightRow = 0;
                this.rightColumn = 0;
                this.resetVisualizations();
                break;
            case ROW:
                this.level = Level.COLUMN;
                this.rightColumn = 0;
                this.resetVisualizations();
                break;
            case COLUMN:
                this.resetVisualizations();
                this.applyKey();
                this.level = Level.SET;
                this.rightSet = 0;
                this.rightColumn = 0;
                break;
        }
    }

    private void updateActiveString() {
        this.activeString = "";
        for (String item:activeStringList) {
            this.activeString += item;
        }
        this.activeString += this.currentWordString;
    }

    private void applyKey() {
        if (this.rightSet == 0) {
            if (this.keys1Type.get(this.rightRow).get(rightColumn).equals("standard")) {
                if (this.isShifted && !this.isShiftedOnce ||
                        this.isShiftedOnce && !this.isShifted) {
                    this.currentWordString += this.keys1
                            .get(this.rightRow)
                            .get(rightColumn)
                            .toUpperCase();
                } else {
                    this.currentWordString += this.keys1.get(this.rightRow).get(rightColumn);
                }
                this.updateActiveString();
                //this.activeString += this.keys1.get(this.rightRow).get(rightColumn);
                this.isShiftedOnce = false;
            } else if (this.keys1Type.get(this.rightRow).get(rightColumn).equals("return")) {
                this.activeStringList.add(this.currentWordString);
                try {
                    this.textPredictor.insertUserDict(activeStringList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                this.synthSpeech();
                this.activeString = "";

                this.currentWordString = "";
                this.activeStringList.clear();
                this.isShiftedOnce = true;
            } else if (this.keys1Type.get(this.rightRow).get(this.rightColumn).equals("space")) {
                this.activeStringList.add(this.currentWordString);
                this.activeStringList.add(" ");
                this.currentWordString = "";
                this.updateActiveString();
            } else if (this.keys1Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("delete_letter")) {
                // Backspace.
                if (this.currentWordString.length() != 0) {
                    this.currentWordString = this.currentWordString
                            .substring(0, this.currentWordString.length() - 1);
                } else {
                    if (this.activeStringList.size() > 0) {
                        this.activeStringList.remove(this.activeStringList.size() - 1);
                        this.currentWordString =
                                this.activeStringList.get(this.activeStringList.size() - 1);
                        this.activeStringList.remove(this.activeStringList.size() - 1);
                    }
                }
            } else if (this.keys1Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("delete_word")) {
                if (this.currentWordString.length() != 0) {
                    this.currentWordString = "";
                } else {
                    if (this.activeStringList.size() > 0) {
                        this.activeStringList.remove(this.activeStringList.size() - 1);
                        this.activeStringList.remove(this.activeStringList.size() - 1);
                    }
                }
            } else if (this.keys1Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("standard+space")) {
                this.currentWordString += this.keys1.get(this.rightRow).get(this.rightColumn);
                this.activeStringList.add(this.currentWordString);
                this.activeStringList.add(" ");
                this.currentWordString = "";
                this.updateActiveString();
                this.isShiftedOnce = false;
            } else if (this.keys1Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("tab")) {
                this.currentWordString += "    ";
                this.updateActiveString();
                this.isShiftedOnce = false;
            } else if (this.keys1Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("toggle_shift_once")) {
                if (this.isShiftedOnce) {
                    // If it is shifted unshift.
                    this.isShiftedOnce = false;
                } else {
                    // If it is unshifted, shift.
                    this.isShiftedOnce = true;
                }

            } else if (this.keys1Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("toggle_shift")) {
                if (this.isShifted) {
                    this.isShifted = false;
                } else {
                    this.isShifted = true;
                }
            }
        } else {
            if (this.keys2Type.get(this.rightRow).get(rightColumn).equals("standard")) {
                //this.activeString += this.keys2.get(this.rightRow).get(rightColumn);
                this.currentWordString += this.keys2.get(this.rightRow).get(rightColumn);
                this.updateActiveString();
                this.isShiftedOnce = false;
            } else if (this.keys2Type.get(this.rightRow).get(rightColumn).equals("return")) {
                this.activeStringList.add(this.currentWordString);
                try {
                    this.textPredictor.insertUserDict(activeStringList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                this.synthSpeech();
                this.activeString = "";
                this.currentWordString = "";
                this.activeStringList.clear();
                this.isShiftedOnce = true;
            } else if (this.keys2Type.get(this.rightRow).get(this.rightColumn).equals("space")) {
                //this.activeString += " ";
                this.activeStringList.add(this.currentWordString);
                this.activeStringList.add(" ");
                this.currentWordString = "";
                this.updateActiveString();
            } else if (this.keys2Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("delete_letter")) {
                // Backspace.
                if (this.currentWordString.length() != 0) {
                    this.currentWordString = this.currentWordString
                            .substring(0, this.currentWordString.length() - 1);
                    this.updateActiveString();
                } else {
                    if (this.activeStringList.size() > 0) {
                        this.activeStringList.remove(this.activeStringList.size() - 1);
                        this.currentWordString =
                                this.activeStringList.get(this.activeStringList.size() - 1);
                        this.activeStringList.remove(this.activeStringList.size() - 1);
                        this.updateActiveString();
                    }
                }
            } else if (this.keys2Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("delete_word")) {
                if (this.currentWordString.length() != 0) {
                    this.currentWordString = "";
                    this.updateActiveString();
                } else {
                    if (this.activeStringList.size() > 0) {
                        this.activeStringList.remove(this.activeStringList.size() - 1);
                        this.activeStringList.remove(this.activeStringList.size() - 1);
                        this.updateActiveString();
                    }
                }
            } else if (this.keys2Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("standard+space")) {
                this.currentWordString += this.keys2.get(this.rightRow).get(this.rightColumn);
                this.activeStringList.add(this.currentWordString);
                this.activeStringList.add(" ");
                this.currentWordString = "";
                this.updateActiveString();
                this.isShiftedOnce = false;
                this.testFunc();
            } else if (this.keys2Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("tab")) {
                this.currentWordString += "    ";
                this.updateActiveString();
                this.isShiftedOnce = false;
            } else if (this.keys2Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("toggle_shift_once")) {
                if (this.isShiftedOnce) {
                    // If it is shifted unshift.
                    this.isShiftedOnce = false;
                } else {
                    // If it is unshifted, shift.
                    this.isShiftedOnce = true;
                }

            } else if (this.keys2Type
                    .get(this.rightRow)
                    .get(this.rightColumn)
                    .equals("toggle_shift")) {
                if (this.isShifted) {
                    this.isShifted = false;
                } else {
                    this.isShifted = true;
                }
            }
        }
        this.mainText.setText(this.activeString);
        this.rightSet = 0;
        this.rightRow = 0;
        this.rightColumn = 0;
    }

    private void synthSpeech() {
        textToSpeech.speak(this.activeString, TextToSpeech.QUEUE_ADD, null);
        //textToSpeech.speak(this.activeString, TextToSpeech.QUEUE_ADD, null, this.activeString);
    }

    private void updateCurrentRowVisualization() {
        if (this.rightSet == 0) {
            this.rightSet1.setBackgroundColor(Color.parseColor("#ffffff"));
            for (int i = 0; i < this.upperLayout.size(); i++) {
                if (i == this.rightRow) {
                    this.upperLayout.get(i).setBackgroundColor(Color.parseColor("#ff9dd2a5"));
                } else {
                    this.upperLayout.get(i).setBackgroundColor(Color.parseColor("#d2e0f1"));
                }
            }
        } else {
            this.rightSet2.setBackgroundColor(Color.parseColor("#ffffff"));
            for (int i = 0; i < this.lowerLayout.size(); i++) {
                if (i == this.rightRow) {
                    this.lowerLayout.get(i).setBackgroundColor(Color.parseColor("#ff9dd2a5"));
                } else {
                    this.lowerLayout.get(i).setBackgroundColor(Color.parseColor("#d2e0f1"));
                }
            }
        }
    }

    private void updateCurrentSetVisualization() {
        if (this.rightSet == 0) {
            this.rightSet1.setBackgroundColor(Color.parseColor("#ff9dd2a5"));
            this.rightSet2.setBackgroundColor(Color.parseColor("#ffffff"));
        } else {
            this.rightSet1.setBackgroundColor(Color.parseColor("#ffffff"));
            this.rightSet2.setBackgroundColor(Color.parseColor("#ff9dd2a5"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupKeys() {
        this.loadKeysJson();
        this.loadKeysTypeJson();
        this.keys1 = new ArrayList<>();
        this.keys2 = new ArrayList<>();
        this.keys1Type = new ArrayList<>();
        this.keys2Type = new ArrayList<>();
        try {
            JSONArray jsonArray1 = (JSONArray) this.keysObject.get("keys1");
            JSONArray jsonArray2 = (JSONArray) this.keysObject.get("keys2");

            for (int i = 0; i < jsonArray1.length(); i++) {
                JSONArray tempArray = (JSONArray) jsonArray1.get(i);
                this.keys1.add(new ArrayList<String>());
                for (int j = 0; j < tempArray.length(); j++) {
                    this.keys1.get(i).add((String) tempArray.get(j));
                }
            }

            for (int i = 0; i < jsonArray2.length(); i++) {
                JSONArray tempArray = (JSONArray) jsonArray2.get(i);
                this.keys2.add(new ArrayList<String>());
                for (int j = 0; j < tempArray.length(); j++) {
                    this.keys2.get(i).add((String) tempArray.get(j));
                }
            }
            this.keys1Array = new String[this.keys1.size()][this.keys1.get(0).size()];
            this.keys2Array = new String[this.keys2.size()][this.keys2.get(0).size()];
            for (int i = 0; i < this.keys1.size(); i++) {
                for (int j = 0; j < this.keys1.get(0).size(); j++) {
                    this.keys1Array[i][j] = this.keys1.get(i).get(j);
                }
            }


            for (int i = 0; i < this.keys2.size(); i++) {
                for (int j = 0; j < this.keys2.get(0).size(); j++) {
                    this.keys2Array[i][j] = this.keys2.get(i).get(j);
                }
            }


            // For the type information.

            JSONArray jsonArray1Type = (JSONArray) this.keysObject2.get("keys1");
            JSONArray jsonArray2Type = (JSONArray) this.keysObject2.get("keys2");

            for (int i = 0; i < jsonArray1Type.length(); i++) {
                JSONArray tempArray = (JSONArray) jsonArray1Type.get(i);
                this.keys1Type.add(new ArrayList<String>());
                for (int j = 0; j < tempArray.length(); j++) {
                    this.keys1Type.get(i).add((String) tempArray.get(j));
                }
            }

            for (int i = 0; i < jsonArray2Type.length(); i++) {
                JSONArray tempArray = (JSONArray) jsonArray2Type.get(i);
                this.keys2Type.add(new ArrayList<String>());
                for (int j = 0; j < tempArray.length(); j++) {
                    this.keys2Type.get(i).add((String) tempArray.get(j));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadKeysJson() {
        InputStream inputStream = this.getResources().openRawResource(R.raw.keys);
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseBuilder = new StringBuilder();

            String inputString;
            while ((inputString = streamReader.readLine()) != null) {
                responseBuilder.append(inputString);
            }
            this.keysObject = new JSONObject(responseBuilder.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadKeysTypeJson() {
        InputStream inputStream = this.getResources().openRawResource(R.raw.keytypes);
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseBuilder = new StringBuilder();

            String inputString;
            while ((inputString = streamReader.readLine()) != null) {
                responseBuilder.append(inputString);
            }
            this.keysObject2 = new JSONObject(responseBuilder.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addKeysToRightPane() {

        /*
        LinearLayout tempLayout = (LinearLayout) findViewById(R.id.u_k_1);
        TextView textView = new TextView(this);
        textView.setText("0");
        textView.setId(100 + 0*10 + 0);
        textView.setLayoutParams(new LinearLayout.LayoutParams(10, 100, 1));
        tempLayout.addView(textView);
        */

        this.upperLayout = new ArrayList<>();
        this.upperLayout.add((LinearLayout) findViewById(R.id.u_k_1));
        this.upperLayout.add((LinearLayout) findViewById(R.id.u_k_2));
        this.upperLayout.add((LinearLayout) findViewById(R.id.u_k_3));
        this.upperLayout.add((LinearLayout) findViewById(R.id.u_k_4));
        for (int i = 0; i < this.keys1.size(); i++) {
            for (int j = 0; j < this.keys1.get(0).size(); j++) {
                TextView textView = new TextView(this);
                textView.setText(this.keys1.get(i).get(j));
                textView.setId(100 + i * 10 + j);
                textView.setBackgroundColor(Color.WHITE);
                textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                textView.setPadding(2, 2, 2, 2);
                textView.setTextSize(22);

                LinearLayout.LayoutParams tempParams =
                        new LinearLayout.LayoutParams(50,
                                50,1);
                tempParams.setMargins(10,0,10,10);
                textView.setLayoutParams(tempParams);

                this.upperLayout.get(i).addView(textView);
            }
        }

        this.lowerLayout = new ArrayList<>();
        this.lowerLayout.add((LinearLayout) findViewById(R.id.l_k_1));
        this.lowerLayout.add((LinearLayout) findViewById(R.id.l_k_2));
        this.lowerLayout.add((LinearLayout) findViewById(R.id.l_k_3));
        this.lowerLayout.add((LinearLayout) findViewById(R.id.l_k_4));
        for (int i = 0; i < this.keys2.size(); i++) {
            for (int j = 0; j < this.keys2.get(0).size(); j++) {
                TextView textView = new TextView(this);
                textView.setText(this.keys2.get(i).get(j));
                textView.setId(100 + i * 10 + j);
                textView.setBackgroundColor(Color.WHITE);
                textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                textView.setPadding(2, 2, 2, 2);
                textView.setTextSize(22);
                LinearLayout.LayoutParams tempParams =
                        new LinearLayout.LayoutParams(50,
                                50,1);
                tempParams.setMargins(10, 0, 10, 10);
                textView.setLayoutParams(tempParams);
                this.lowerLayout.get(i).addView(textView);
            }
        }

        /*
        this.rightGrid = (GridView) findViewById(R.id.key_box);

        this.upperGrids = new ArrayList<>();
        this.upperGrids.add((GridView) findViewById(R.id.u_k_1));
        this.upperGrids.add((GridView) findViewById(R.id.u_k_2));
        this.upperGrids.add((GridView) findViewById(R.id.u_k_3));
        this.upperGrids.add((GridView) findViewById(R.id.u_k_4));

        this.lowerGrids = new ArrayList<>();
        this.lowerGrids.add((GridView) findViewById(R.id.l_k_1));
        this.lowerGrids.add((GridView) findViewById(R.id.l_k_2));
        this.lowerGrids.add((GridView) findViewById(R.id.l_k_3));
        this.lowerGrids.add((GridView) findViewById(R.id.l_k_4));
        */


        //this.mainGrid = (GridView) findViewById(R.id.main_area);


        /*
        for (int i = 0; i<this.upperGrids.size(); i++) {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,this.keys1.get(i));
            this.upperGrids.get(i).setAdapter(arrayAdapter);
        }
        */

    }

    private void testFunc() {

    }
}
