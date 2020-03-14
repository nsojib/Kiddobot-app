package nsojib.com.animatedkiddobot;

import android.content.Context;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by FridayLab on 8/26/2018.
 */

public class MyTTS implements TextToSpeech.OnInitListener {
    TextToSpeech tts;
    Context context;
    boolean isBusy=false;
    TTSListener listener;
    public MyTTS(Context context, TTSListener listener){
        this.context=context;
        this.listener=listener;
//        tts=new TextToSpeech(context,this);
        tts=new TextToSpeech(context,this , "com.google.android.tts");
    }

    //blocking call.
    public void speak_b(String txt){
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ribotts");
        tts.speak(txt, TextToSpeech.QUEUE_FLUSH,  map);
        while (tts.isSpeaking()){
            try{Thread.sleep(10);}catch (Exception e){}
        }
    }
    public void speak_nb(String txt) {
        isBusy=true;
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ribotts");
        tts.speak(txt, TextToSpeech.QUEUE_FLUSH,  map);
        new Waiter().execute();
//        Bundle params = new Bundle();
//        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ribotts");
//        tts.speak(txt, TextToSpeech.QUEUE_FLUSH, params, "ribotts");
//
    }
    class Waiter extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            while (tts.isSpeaking()){
                try{Thread.sleep(10);}catch (Exception e){}
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
//            Toast.makeText(context, "wait completed", Toast.LENGTH_LONG).show();
            listener.onTTSCompleted();
            isBusy=false;
        }
    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("bn_BD"));
        }
    }

}