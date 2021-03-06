package com.cueaudio.engine_consumer;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.ScrollingMovementMethod;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.cueaudio.engine.CUEEngine;
import com.cueaudio.engine.CUEReceiverCallbackInterface;
import com.cueaudio.engine.CUETrigger;
import com.cueaudio.engine.CUEEngineError;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AndroidConsumer";

    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final String API_KEY = "EH0GHbslb0pNWAxPf57qA6n23w4Zgu5U";
    private static final int NOTIFICATION_ID = 1;

    private TextView outputView;
    private View clearOutput;
    private Switch outputMode;
    private View sendButton;
    private Spinner spinner;
    private TextInputLayout messageLayout;
    private TextInputEditText messageInput;

    private boolean isShown = false;

    /**
     * Used to validate the input.
     */
    private Pattern inputMatcher = null;
    private String[] hints;
    private String[] regex;
    private String[] errors;

    private boolean restartListening = false;

    private int getModeByPosition(int position) {
        int realMode;
        switch( position ) {
            case 0:
            case 1:
                realMode = CUETrigger.MODE_TRIGGER;
                break;
            case 2:
                realMode = CUETrigger.MODE_LIVE;
                break;
            default:
                realMode = CUETrigger.MODE_ASCII;
                break;
        }

        return realMode;
    }

    private boolean getTriggerAsNumberByPosition(int position) {
        if( position == 1 )
            return true;
        else
            return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        messageLayout = findViewById(R.id.message_layout);
        messageInput = findViewById(R.id.message);
        sendButton = findViewById(R.id.send);
        outputView = findViewById(R.id.outputView);
        outputMode = findViewById(R.id.output_mode);
        clearOutput = findViewById(R.id.clear_output);

        hints = getResources().getStringArray(R.array.message_hints);
        regex = getResources().getStringArray(R.array.message_regex);
        errors = getResources().getStringArray(R.array.message_errors);
        spinner = findViewById(R.id.message_mode);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.message_modes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectMode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().length() != 0 ) {
                    validateInput(s.toString());
                }
            }
        });

        outputView.setMovementMethod(new ScrollingMovementMethod());
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                final String input = messageInput.getText().toString();
                final int position = spinner.getSelectedItemPosition();

                int mode = getModeByPosition( position );
                boolean triggerAsNumber = getTriggerAsNumberByPosition( position );

                Log.v(TAG, String.format("triggerAsNumber %b", triggerAsNumber));
                queueInput(input, mode, triggerAsNumber);
            }
        });

        clearOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outputView.setText(null);
                clearOutput.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        isShown = true;
        if (restartListening) {
            CUEEngine.getInstance().startListening();
        }
    }

    @Override
    protected void onStop() {
        restartListening = CUEEngine.getInstance().isListening();
        CUEEngine.getInstance().stopListening();
        isShown = false;
        super.onStop();
    }

    private void selectMode(int position) {
        refreshKeyboard(messageInput);

        messageInput.setHint(hints[position]);
        messageLayout.setHint(null);
        inputMatcher = Pattern.compile(regex[position]);

        int mode = getModeByPosition(position);
        switch (mode) {
            case CUETrigger.MODE_TRIGGER:
            case CUETrigger.MODE_LIVE:
                messageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                messageInput.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
                break;
            case CUETrigger.MODE_ASCII:
                messageInput.setInputType(InputType.TYPE_CLASS_TEXT);
                messageInput.setKeyListener(TextKeyListener.getInstance());
                break;
        }

        //noinspection ConstantConditions
        validateInput(messageInput.getText().toString());
    }

    private void validateInput(@NonNull String input) {
        final boolean matches = inputMatcher.matcher(input).matches();
        sendButton.setEnabled(matches);
        if (!matches) {
            final int position = spinner.getSelectedItemPosition();
            // HACK: to prevent error message to be cut https://stackoverflow.com/a/55468225/322955
            messageLayout.setError(null);
            messageLayout.setError(errors[position]);
        } else {
            messageLayout.setError(null);
        }
    }

    private void queueInput(@NonNull String input, int mode, boolean triggerAsNumber) {
        int result;

        switch (mode) {
            case CUETrigger.MODE_TRIGGER:
                if(triggerAsNumber) {
                    long number = Long.parseLong(input);
                    result = CUEEngine.getInstance().queueTriggerAsNumber(number);
                    if( result == CUEEngineError.G1_TRIGGER_AS_NUMBER_UNSUPPORTED ) {
                        messageLayout.setError(
                            "Triggers as number sending is unsupported for engine generation 1" );
                    } else if( result == CUEEngineError.TRIGGER_AS_NUMBER_MAX_NUMBER_EXCEEDED ) {
                        messageLayout.setError(
                            "Triggers us number can not exceed 98611127" );
                    } else if ( result < 0 ){
                        messageLayout.setError(
                                "Triggers us number sending: unknown error" );
                    }
                } else {
                    CUEEngine.getInstance().queueTrigger(input);
                }
                break;
   
            case CUETrigger.MODE_LIVE:
                result = CUEEngine.getInstance().queueLive(input);
                if ( result == CUEEngineError.G2_QUEUE_LIVE_UNSUPPORTED ) {
                    messageLayout.setError(
                        "Live triggers sending is unsupported for engine generation 2");
                }
                break;

            case CUETrigger.MODE_ASCII:
                result = CUEEngine.getInstance().queueMessage(input);
                if ( result == CUEEngineError.G1_NUMBER_OF_SYMBOLS_EXCEEDED ) {
                    ///!!! should be fixed some how (but how?)
                    messageLayout.setError("Ascii stream can't contain more then 10 symbols for G1");
                } else if ( result == CUEEngineError.G2_QUEUE_MESSAGE_STRING_SIZE_IN_BYTES_EXCEEDED ) {
                    messageLayout.setError("Test can't contain more then 512 bytes for G2");
                } else if( result < 0 ) {
                    messageLayout.setError("Queue message: unknown error");
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean listening = CUEEngine.getInstance().isListening();

        final Drawable startIcon = menu.findItem(R.id.menu_start).getIcon();
        final Drawable stopIcon = menu.findItem(R.id.menu_stop).getIcon();
        if (listening) {
            DrawableCompat.setTint(startIcon, getResources().getColor(R.color.menu_active));
            DrawableCompat.setTint(stopIcon, getResources().getColor(R.color.menu_inactive));
        } else {
            DrawableCompat.setTint(startIcon, getResources().getColor(R.color.menu_inactive));
            DrawableCompat.setTint(stopIcon, getResources().getColor(R.color.menu_active));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_start:
                enableListening(true);
                return true;
            case R.id.menu_stop:
                enableListening(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[] { android.Manifest.permission.RECORD_AUDIO },
                REQUEST_RECORD_AUDIO
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //check if permission was granted, and confirm that permission was mic access
        boolean permCondition = requestCode == REQUEST_RECORD_AUDIO &&
                                grantResults.length == 1 &&
                                grantResults[0] == PackageManager.PERMISSION_GRANTED;
        // permission is not granted yet
        if (!permCondition) {
            checkPermission();
            return;
        }

        CUEEngine.getInstance().setupWithAPIKey(this, API_KEY);
        CUEEngine.getInstance().setDefaultGeneration(2);

        CUEEngine.getInstance().setReceiverCallback(new OutputListener());
        enableListening(true);

        final String config = CUEEngine.getInstance().getConfig();
        Log.v(TAG, config);

        CUEEngine.getInstance().setTransmittingEnabled(true);
    }

    private void onTriggerHeard(CUETrigger model) {
        if (!isShown) {
            showNotification(model.getRawIndices());
        }

        if (outputMode.isChecked()) {
            outputView.append(model.toString());
        } else {
            outputView.append(model.toShortString());
        }
        outputView.append("\n");
        outputView.append("\n");
        clearOutput.setVisibility(View.VISIBLE);

        long triggerNum = model.getTriggerAsNumber();
        Log.i("triggerAsNumber: ", Long.toString(triggerNum));


        // scroll to end
        // https://stackoverflow.com/a/43290961
        final Editable editable = (Editable) outputView.getText();
        Selection.setSelection(editable, editable.length());
    }

    private void showNotification(@NonNull String message) {
        final NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final String channelId = getString(R.string.notification_channel_id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final CharSequence name = getString(R.string.notification_channel_name);
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            //noinspection ConstantConditions
            notificationManager.createNotificationChannel(channel);
        }

        final Intent intent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, 0
        );

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[] { 1000, 1000, 1000 })
                .setAutoCancel(true);
        //noinspection ConstantConditions
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void enableListening(boolean enable) {
        if (enable) {
            CUEEngine.getInstance().startListening();
        } else {
            CUEEngine.getInstance().stopListening();
        }
        supportInvalidateOptionsMenu();
    }

    private static void refreshKeyboard(@NonNull View view) {
        final InputMethodManager imm =
                (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        //noinspection ConstantConditions
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        imm.showSoftInput(view, 0);
    }

    private class OutputListener implements CUEReceiverCallbackInterface {
        @Override
        public void run(@NonNull String json) {
            final CUETrigger model = CUETrigger.parse(json);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onTriggerHeard(model);
                }
            });
        }
    }
}
