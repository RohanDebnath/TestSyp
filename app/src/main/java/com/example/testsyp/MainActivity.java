package com.example.testsyp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CONTACT = 1;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 2;

    private EditText etPhoneNumber;
    private EditText etInterval;
    private Button btnSelectContact;
    private Button btnStartTimer;
    private TextView tvCountdownTimer;

    private CountDownTimer countDownTimer;
    private long timeRemaining;
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etInterval = findViewById(R.id.etInterval);
        btnSelectContact = findViewById(R.id.btnSelectContact);
        btnStartTimer = findViewById(R.id.btnStartTimer);
        tvCountdownTimer = findViewById(R.id.tvCountdownTimer);

        btnSelectContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectContact();
            }
        });

        btnStartTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();
            }
        });
    }

    private void selectContact() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT && resultCode == RESULT_OK) {
            Cursor cursor = null;
            try {
                Uri contactUri = data.getData();
                cursor = getContentResolver().query(contactUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String phoneNumber = cursor.getString(numberIndex);
                    etPhoneNumber.setText(phoneNumber);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void startTimer() {
        if (!timerRunning) {
            String intervalString = etInterval.getText().toString().trim();
            if (!intervalString.isEmpty()) {
                int interval = Integer.parseInt(intervalString);
                long millisInterval = interval * 60 * 1000; // Convert minutes to milliseconds

                timeRemaining = millisInterval;
                countDownTimer = new CountDownTimer(millisInterval, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        timeRemaining = millisUntilFinished;
                        updateCountdownTimer();
                    }

                    @Override
                    public void onFinish() {
                        showAlertDialog();
                    }
                };

                countDownTimer.start();
                timerRunning = true;
                btnStartTimer.setText("Stop Timer");
                etInterval.setEnabled(false);
                btnSelectContact.setEnabled(false);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a valid time interval.", Toast.LENGTH_SHORT).show();
            }
        } else {
            stopTimer();
        }
    }

    private void stopTimer() {
        if (timerRunning) {
            countDownTimer.cancel();
            timerRunning = false;
            btnStartTimer.setText("Start Timer");
            etInterval.setEnabled(true);
            btnSelectContact.setEnabled(true);
        }
    }

    private void updateCountdownTimer() {
        int minutes = (int) (timeRemaining / 1000) / 60;
        int seconds = (int) (timeRemaining / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvCountdownTimer.setText(timeLeftFormatted);
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Timer Expired");
        builder.setMessage("Snooze or Send SMS?");
        builder.setPositiveButton("Snooze", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopTimer();
                Toast.makeText(MainActivity.this, "Snooze clicked", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Send SMS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopTimer();
                Toast.makeText(MainActivity.this, "Send SMS clicked", Toast.LENGTH_SHORT).show();
                getCurrentLocationAndSendSMS();
            }
        });
        builder.setCancelable(false); // Prevent dialog from being dismissed by pressing outside
        AlertDialog dialog = builder.create();
        dialog.show();

        // Schedule sending SMS after 10 seconds if no button is clicked
        new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (dialog.isShowing()) {
                    getCurrentLocationAndSendSMS();
                    dialog.dismiss();
                }
            }
        }.start();
    }

    private void getCurrentLocationAndSendSMS() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        if (!phoneNumber.isEmpty()) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_SEND_SMS);
            } else {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        String locationText = getLocationText(location.getLatitude(), location.getLongitude());
                        sendSMS(phoneNumber, locationText);
                    } else {
                        Toast.makeText(MainActivity.this, "Unable to retrieve location.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "GPS is disabled.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(MainActivity.this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(MainActivity.this, "SMS Sent", Toast.LENGTH_SHORT).show();
    }

    private String getLocationText(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                sb.append(address.getAddressLine(0)).append(", ");
                sb.append(address.getLocality()).append(", ");
                sb.append(address.getAdminArea()).append(", ");
                sb.append(address.getCountryName());
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Location";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndSendSMS();
            } else {
                Toast.makeText(MainActivity.this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
