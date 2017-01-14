package ironhammerindustries.openspeech;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Rujak on 3/23/2016.
 */
public class TextPredictor {
    private ArrayList<String> defaultString;
    private JSONObject defaultTrieObject;

    private ArrayList<String> resultString;
    private JSONObject userTrieObject;

    private JSONObject trieObject;

    MainActivity mainActivity;

    public TextPredictor(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.defaultString = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            this.defaultString.add("");
        }
        this.defaultTrieObject = new JSONObject();
        try {
            InputStream inputStream = mainActivity
                    .getResources()
                    .openRawResource(R.raw.simplified_trie_dictionary);
                    //.openRawResource(R.raw.simplified_trie_dictionary_v2);
            int size = inputStream.available();
            byte[] byteBuffer = new byte[size];
            inputStream.read(byteBuffer);
            inputStream.close();
            //String jsonString = byteBuffer.toString();
            String jsonString = new String(byteBuffer, Charset.forName("UTF-8"));
            //String jsonString = "{\"h1\": {\"f\": {}, \"i\":{}}}";
            this.trieObject = new JSONObject(jsonString);
        } catch (Exception e){
            e.printStackTrace();
        }
        this.checkForUserDict();
        this.autoComplete("hel");
    }

    private ArrayList<String> getSuffixesDefault(String prefix,
                                                 JSONObject prefixObject,
                                                 int startingSum) {
        // Find all words starting with these.
        // Eventually make it just get the first eight based on A* or so.
        int secondSum = startingSum;
        int wordsFound = 0;
        ArrayList<String> resultStrings = new ArrayList<>();

        if (prefixObject.has("x1")) {
            resultStrings.add(prefix);
        }
        if (!prefixObject.has("h1")) {
            return resultStrings;
        }

        // Go through a breadth first search collecting all of the ones that have "x1".
        this.getSuffixesRecursive(prefixObject, secondSum);
        return resultStrings;
    }

    private ArrayList<String> getSuffixesRecursive(JSONObject prefixObject, int startingSum) {
        ArrayList<String> finalList = new ArrayList<>();
        ArrayList<TempItem> tempItems = new ArrayList<>();
        Iterator<?> iterator = prefixObject.keys();
        if (prefixObject.has("x1")) {

        }
        try {
            ArrayList<JSONObject> tempList = new ArrayList<>();
            while( iterator.hasNext() ) {
                String key = (String)iterator.next();
                if ( prefixObject.get(key) instanceof JSONObject ) {
                    this.testFunc();
                } else {

                }
            }
            /*
            for (Object key: prefixObject.keySet()) {
                tempList.add();
            }
            */
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return finalList;
    }

    private void getSuffixesUser(String prefix, JSONObject prefixObject, int startingSum) {
        // Find all words starting with these.
        // Eventually make it just get the first eight based on A* or so.

    }

    private boolean autoComplete(String prefix) {
        // Move through the tree until you are at the listing of the final character in the string.
        // Then call getSuffixesDefault and return that to getEightSuggestions().
        // Then do them both again for the user dictionary;
        JSONObject tempObject = null;
        int startingSum = 0;
        try {
            tempObject = this.trieObject;
            for (int i = 0; i < prefix.length(); i++) {
                try {
                    tempObject = tempObject
                            .getJSONObject("h1")
                            .getJSONObject(prefix.substring(i, i + 1));
                    startingSum += tempObject.getInt("f1");
                } catch (Exception e) {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        getSuffixesDefault(prefix, tempObject, startingSum);
        getSuffixesUser(prefix, tempObject, startingSum);

        return true;
    }

    public ArrayList<String> getEightSuggestions() {
        return this.defaultString;
    }

    private void checkForUserDict() {
        //this.userTrieObject = new JSONObject();

        if (!this.loadUserDict(this.mainActivity.getBaseContext())) {
            this.createUserDict(this.mainActivity.getBaseContext());
            try {
                this.userTrieObject = new JSONObject().put("h1",new JSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void insertUserDict(ArrayList<String> words) throws JSONException {
        for (int j = 0; j < words.size(); j++) {
            String word = words.get(j);
            if (!word.equals(" ")) {
                this.insertUserDict(word);
            }
        }

    }

    public void insertUserDict(String word) throws JSONException {
        JSONObject tempObject = this.userTrieObject.getJSONObject("h1");
        JSONObject tempObjectOld = this.userTrieObject.getJSONObject("h1");
        String lastChar = "";
        for (int i = 0; i < word.length(); i++) {
            lastChar = word.substring(i,i+1);
            if (!tempObject.has(lastChar)) {
                tempObject.put(lastChar,
                        new JSONObject().put("f1",1)
                                .put("h1", new JSONObject()));
            } else {
                tempObject.put("f1",tempObject.getInt("f1") + 1);
            }

            tempObjectOld = tempObject;

            try {
                tempObject = tempObject.getJSONObject(lastChar).getJSONObject("h1");
            } catch (Exception e) {
                tempObject.getJSONObject(lastChar).put("h1",new JSONObject());
                tempObject = tempObject.getJSONObject(lastChar).getJSONObject("h1");
            }
        }

        tempObjectOld.getJSONObject(lastChar).put("x",1);
        if (tempObjectOld.getJSONObject(lastChar).getJSONObject("h1").length() == 0) {
            tempObjectOld.getJSONObject(lastChar).remove("h1");
    }

        this.testFunc();
    }

    public void insertUserDict(String word, int frequency, String pos, int fileCount) {

    }

    private void createUserDict(Context context) {
        String fileName = "userdict";
        String string = "{'h1':{}}";
        FileOutputStream outputStream;

        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveUserDict(Context context) {
        String fileName = "userdict";
        String string = this.userTrieObject.toString();
        FileOutputStream outputStream;

        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean loadUserDict(Context context) {
        String fileName = "userdict";
        FileInputStream inputStream;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder;
        String inputString, finalString;

        try {
            inputStream = context.openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            stringBuilder = new StringBuilder();

            while ((inputString = bufferedReader.readLine()) != null) {
                stringBuilder.append(inputString);
            }

            finalString = stringBuilder.toString();
            this.userTrieObject = new JSONObject(finalString);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private class LetterNode {
        private boolean isWord;
        private ArrayList<LetterNode> sortedNodes;
        private float probability;
        private String letter;
    }

    private class TempItem {
        private String tempString;
        private int tempValue;
        private JSONObject jsonObject;
        private TempItem(String tempString, int tempValue, JSONObject jsonObject) {
            this.tempString = tempString;
            this.tempValue = tempValue;
            this.jsonObject = jsonObject;
        }

        public String getTempString() {
            return tempString;
        }

        public int getTempValue() {
            return tempValue;
        }
    }

    private void testFunc() {

    }
}
