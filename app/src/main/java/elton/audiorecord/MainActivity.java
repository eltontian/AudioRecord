package elton.audiorecord;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.Hound.HoundJSON.ConversationStateJSON;
import com.Hound.HoundJSON.HoundPartialTranscriptJSON;
import com.Hound.HoundJSON.HoundServerJSON;
import com.Hound.HoundJSON.RequestInfoJSON;
import com.Hound.HoundRequester.HoundCloudRequester;
import com.Hound.HoundRequester.HoundRequester;


public class MainActivity extends Activity {

    AudioRecord ar = null;
    int buffsize = 0;

    int blockSize = 256;
    boolean isRecording = false;
    private Thread recordingThread = null;
    private TextView textView;
    private HoundCloudRequester requester;
    private HoundRequester.PartialHandler partialHandler;
    private HoundRequester.VoiceRequest request;
    private HoundServerJSON result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String client_id = "kOcKxPPh-SXYEqeLdGq3RQ==";
        String client_key = "0qvu3p6bv8SOZtgZJUwo5Nc6RJ0SLJzXxV4QdnWJ65VEyu7C9mIfZwW_r-7hgJgJPvGL6sZ_3UyHtSzBWvrztQ==";
        String user_id = "eltontian2@gmail.com";

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button start = (Button) findViewById(R.id.btnStart);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baslat();
            }
        });

        Button stop = (Button) findViewById(R.id.btnStop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                durdur();
            }
        });

        textView = (TextView) findViewById(R.id.text);

        requester = new HoundCloudRequester(client_id,
                client_key, user_id);
        partialHandler = new HoundRequester.PartialHandler(){
            public void handle(HoundPartialTranscriptJSON partial) {
                if (partial.hasSafeToStopAudio()) {
                    if (partial.getSafeToStopAudio()) {
                        durdur();
                    }
                }

                Log.v("partial",partial.getPartialTranscript());
                textView.setText("partial\n" + partial.getPartialTranscript());
            }
        };
    }

    public void baslat() {
        // when click to START
        buffsize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffsize);

        ar.startRecording();

        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

        ConversationStateJSON conversation_state = new ConversationStateJSON();
        RequestInfoJSON request_info = new RequestInfoJSON();
        try {
            request = requester.start_voice_request(conversation_state,
                    request_info, partialHandler);
        } catch(Exception e) {
            Log.e("start_voice_request",e.getMessage());
            this.finish();
            System.exit(0);
        }
    }

    public void durdur() {
        // When click to STOP
        ar.stop();
        isRecording = false;
        try {
            result = request.finish();
            String response = result.getAllResults().firstElement().getWrittenResponse();
            String transcription = result.getDisambiguation().getChoiceData().get(0).getTranscription();
            String fixedTranscription = result.getDisambiguation().getChoiceData().get(0).getFixedTranscription();
            textView.setText("Written response:\n" + response);
        } catch(Exception e) {
            Log.e("finish", result.getErrorMessage());
            this.finish();
            System.exit(0);
        }
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        short sData[] = new short[buffsize / 2];


        while (isRecording) {
            // gets the voice output from microphone to byte format

            ar.read(sData, 0, buffsize / 2);
            Log.d("eray", "Short writing to file" + sData.toString());
            // // writes the data to file from buffer
            // // stores the voice buffer
            byte bData[] = short2byte(sData);
            try {
                request.add_audio(buffsize, bData);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                //sending the actual Thread of execution to sleep X milliseconds
                Thread.sleep(100);
            } catch(InterruptedException ie) {}
        }
        durdur();
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }
}