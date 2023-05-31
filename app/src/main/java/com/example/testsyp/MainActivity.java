package com.example.testsyp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private EditText phoneEditText;
    private EditText timeIntervalEditText;
    private Button startButton;
    private TextView timerTextView;

    private CountDownTimer countDownTimer;
    private long timeInterval;
    private String phoneNumber;

    private boolean smsSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneEditText = findViewById(R.id.phoneEditText);
        timeIntervalEditText = findViewById(R.id.timeIntervalEditText);
        startButton = findViewById(R.id.startButton);
        timerTextView = findViewById(R.id.timerTextView);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();
            }
        });
    }

    private void startTimer() {
        String intervalText = timeIntervalEditText.getText().toString().trim();
        String phoneText = phoneEditText.getText().toString().trim();

        if (intervalText.isEmpty() || phoneText.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number and time interval", Toast.LENGTH_SHORT).show();
            return;
        }

        timeInterval = Long.parseLong(intervalText) * 60 * 1000; // Convert minutes to milliseconds
        phoneNumber = phoneText;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(timeInterval, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 1000 / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                showAlert();
            }
        };

        countDownTimer.start();
    }

    private void showAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Time's Up!")
                .setMessage("Do you want to snooze?")
                .setPositiveButton("Snooze", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startTimer();
                    }
                })
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing, SMS will be sent after 15 seconds automatically
                    }
                })
                .setCancelable(false)
                .show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sendSms();
            }
        }, 15000); // Delay of 15 seconds (15000 milliseconds)
    }

    private void sendSms() {
        if (smsSent) {
            return; // SMS has already been sent, no need to send again
        }

        smsSent = true;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, "Your message", null, null);
            Toast.makeText(this, "SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSms();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
