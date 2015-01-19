package may.speechcalculator;

import java.text.DecimalFormat;
import java.util.Locale;
import java.io.IOException;
import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.os.AsyncTask;
import java.util.ArrayList;
import java.util.HashMap;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.content.Intent;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class MainActivity extends Activity {

    private enum RecognizerType {
        GOOGLE, SPHINX
    }

    private RecognizerType recognizerType = RecognizerType.GOOGLE;

    private static final int REQ_CODE_GOOGLE_RECOGNIZER = 100;

    private boolean ttsReady = false;
    private TextToSpeech tts;

    // Sphinx Related
    private class Sphinx implements RecognitionListener {
        public SpeechRecognizer recognizer;
        public static final String EQ_SEARCH = "equation";
        public static final String SIGNAL_LISTEN = "Listening";

        public void setupRecognizer(File assetsDir) {
            File modelsDir = new File(assetsDir, "models");
            recognizer = defaultSetup()
                    .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                    .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                    .setRawLogDir(assetsDir).setKeywordThreshold(1e20f)
                    .getRecognizer();

            recognizer.addListener(this);

            // Create grammarbased searches.
            File equationGrammer = new File(modelsDir, "grammar/equation.gram");
            recognizer.addGrammarSearch(EQ_SEARCH, equationGrammer);
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
            if(hypothesis != null) {
                String text = hypothesis.getHypstr();

                FormulaFilter filter = new FormulaFilter();
                Log.d("Speech Recogn", text);
                String formula = filter.speechResultToFormula(text);
                Log.d("Formula", formula);
                double res = filter.evaluateFormula(formula);
                speakText("Calculate " + text);
                speakText("Result is " + new DecimalFormat("#0.#######").format(res));
            }
        }

        @Override
        public void onEndOfSpeech() {
            recognizer.stop();
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {}

        @Override
        public void onBeginningOfSpeech() {}
    }
    private Sphinx sphinx = new Sphinx();

    private UtteranceProgressListener ttsListener = new UtteranceProgressListener() {
        @Override
        public void onDone(String id) {
            if(recognizerType == RecognizerType.SPHINX && id.equals(Sphinx.SIGNAL_LISTEN)) {
                sphinx.recognizer.startListening(Sphinx.EQ_SEARCH);
            }
        }

        @Override
        public void onStart(String id) {}

        @Override
        public void onError(String id) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    sphinx.setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Toast.makeText(getApplicationContext(), "Failed to init recognizer", Toast.LENGTH_LONG).show();
                } else {
                    speakText("Ready");
                }
            }
        }.execute();
    }

    @Override
    public void onResume() {
        if(tts == null) {
            tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status == TextToSpeech.SUCCESS) {
                        tts.setOnUtteranceProgressListener(ttsListener);
                        int result = tts.setLanguage(Locale.US);

                        if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Toast.makeText(getApplicationContext(), "Language not supported", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent();
                            intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                            startActivity(intent);
                        } else {
                            ttsReady = true;
                            Toast.makeText(getApplicationContext(), "TTS inited", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        if(tts != null) {
            ttsReady = false;
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onStop();
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
        if (id == R.id.action_switch_recognizer) {
            RecognizerType[] types = RecognizerType.values();
            final String[] recognizerNames = new String[types.length];
            for(int i = 0; i < types.length; i++) {
                recognizerNames[i] = types[i].name();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.pick_recognizer);
            builder.setItems(recognizerNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    recognizerType = RecognizerType.valueOf(recognizerNames[which]);
                }
            });
            builder.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startCalculate(View view) {
        if(recognizerType == RecognizerType.GOOGLE) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening");

            try {
                startActivityForResult(intent, REQ_CODE_GOOGLE_RECOGNIZER);
            } catch (ActivityNotFoundException e) {
                speakText("Speech Recognizer Not Supported");
            }
        } else if(recognizerType == RecognizerType.SPHINX) {
            speakText(Sphinx.SIGNAL_LISTEN);
        }

    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch(reqCode) {
            case REQ_CODE_GOOGLE_RECOGNIZER:
                if(resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    FormulaFilter filter = new FormulaFilter();

                    boolean recognized = false;
                    for(String attempt: result) {
                        Log.d("Speech Recogn", attempt);
                        String formula = filter.speechResultToFormula(attempt);
                        Log.d("Formula", formula);
                        try {
                            double res = filter.evaluateFormula(formula);
                            speakText("Calculate " + formula);
                            speakText("Result is " + new DecimalFormat("#0.#######").format(res));
                            recognized = true;
                            break;
                        } catch(IllegalArgumentException e) {

                        }
                    }
                    if(!recognized) speakText("Failed to calculate");
                }
        }
    }

    private void speakText(String speech) {
        Toast.makeText(getApplicationContext(), speech, Toast.LENGTH_SHORT).show();
        if(!ttsReady) return;

        HashMap<String, String> speakHash = new HashMap<>();
        speakHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, speech);
        tts.speak(speech, TextToSpeech.QUEUE_ADD, speakHash);
        //tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, speech);
    }
}
