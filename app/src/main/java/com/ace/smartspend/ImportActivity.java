package com.ace.smartspend;
import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ace.smartspend.adapter.ParsedTransactionAdapter;
import com.ace.smartspend.model.Transaction;
import com.ace.smartspend.parser.SmsParser;
import com.ace.smartspend.repo.TransactionRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportActivity extends AppCompatActivity {

    private static final int READ_SMS_PERMISSION_CODE = 101;

    private Button btnStartDate, btnEndDate, btnScanSms, btnImportSelected;
    private RecyclerView rvParsedTransactions;
    private ProgressBar pbImportProgress;

    private Calendar startDateCalendar = Calendar.getInstance();
    private Calendar endDateCalendar = Calendar.getInstance();
    private ParsedTransactionAdapter adapter;
    private final List<Transaction> parsedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);
        btnScanSms = findViewById(R.id.btn_scan_sms);
        btnImportSelected = findViewById(R.id.btn_import_selected);
        rvParsedTransactions = findViewById(R.id.rv_parsed_transactions);
        pbImportProgress = findViewById(R.id.pb_import_progress);

        setupDatePickers();
        setupRecyclerView();

        btnScanSms.setOnClickListener(v -> checkPermissionAndScan());
        btnImportSelected.setOnClickListener(v -> importSelectedTransactions());
    }

    private void setupDatePickers() {
        // Set default start date to beginning of the month
        startDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
        updateDateButtonText();

        btnStartDate.setOnClickListener(v -> showDatePickerDialog(startDateCalendar));
        btnEndDate.setOnClickListener(v -> showDatePickerDialog(endDateCalendar));
    }

    private void showDatePickerDialog(Calendar calendar) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            updateDateButtonText();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButtonText() {
        btnStartDate.setText(String.format("Start: %tF", startDateCalendar));
        btnEndDate.setText(String.format("End: %tF", endDateCalendar));
    }

    private void setupRecyclerView() {
        adapter = new ParsedTransactionAdapter(parsedList);
        rvParsedTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvParsedTransactions.setAdapter(adapter);
    }

    private void checkPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            scanSms();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanSms();
        } else {
            Toast.makeText(this, "Read SMS permission is required to import transactions.", Toast.LENGTH_LONG).show();
        }
    }

    private void scanSms() {
        pbImportProgress.setVisibility(View.VISIBLE);
        btnImportSelected.setVisibility(View.GONE);
        parsedList.clear();
        adapter.notifyDataSetChanged();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // Background work
            List<Transaction> foundTransactions = new ArrayList<>();
            Uri smsUri = Uri.parse("content://sms/inbox");
            String[] projection = {"body", "date"};

            // Set time to start of the start day and end of the end day
            Calendar queryStart = (Calendar) startDateCalendar.clone();
            queryStart.set(Calendar.HOUR_OF_DAY, 0);
            queryStart.set(Calendar.MINUTE, 0);

            Calendar queryEnd = (Calendar) endDateCalendar.clone();
            queryEnd.set(Calendar.HOUR_OF_DAY, 23);
            queryEnd.set(Calendar.MINUTE, 59);

            String selection = "date >= ? AND date <= ?";
            String[] selectionArgs = {String.valueOf(queryStart.getTimeInMillis()), String.valueOf(queryEnd.getTimeInMillis())};

            Cursor cursor = getContentResolver().query(smsUri, projection, selection, selectionArgs, "date DESC");

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    Transaction transaction = SmsParser.parse(body);
                    if (transaction != null) {
                        foundTransactions.add(transaction);
                    }
                }
                cursor.close();
            }

            // UI Thread work
            handler.post(() -> {
                pbImportProgress.setVisibility(View.GONE);
                if (foundTransactions.isEmpty()) {
                    Toast.makeText(this, "No financial transactions found in the selected date range.", Toast.LENGTH_SHORT).show();
                } else {
                    parsedList.addAll(foundTransactions);
                    // This is the corrected block
                    adapter.resetCheckedState();
                    adapter.notifyDataSetChanged();
                    btnImportSelected.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void importSelectedTransactions() {
        List<Transaction> selectedTransactions = adapter.getSelectedTransactions();
        if (selectedTransactions.isEmpty()) {
            Toast.makeText(this, "No transactions selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        TransactionRepository repository = new TransactionRepository();
        for (Transaction t : selectedTransactions) {
            repository.addTransaction(t);
        }

        Toast.makeText(this, selectedTransactions.size() + " transactions imported successfully!", Toast.LENGTH_SHORT).show();
        finish(); // Go back to MainActivity
    }
}