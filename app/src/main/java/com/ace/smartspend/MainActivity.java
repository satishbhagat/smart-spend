package com.ace.smartspend;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ace.smartspend.adapter.TransactionAdapter;
import com.ace.smartspend.model.Transaction;
import com.ace.smartspend.parser.SmsParser;
import com.ace.smartspend.viewmodel.TransactionViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Query;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int READ_SMS_PERMISSION_CODE = 101;
    private FirebaseAuth mAuth;
    private TransactionViewModel transactionViewModel;
    private TransactionAdapter adapter;
    private Spinner categorySpinner;
    private TextView tvBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        tvBalance = findViewById(R.id.tv_balance);
        categorySpinner = findViewById(R.id.category_spinner);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Query initialQuery = transactionViewModel.getFilteredQuery("All Categories");
        FirestoreRecyclerOptions<Transaction> initialOptions = new FirestoreRecyclerOptions.Builder<Transaction>()
                .setQuery(initialQuery, Transaction.class).build();
        adapter = new TransactionAdapter(initialOptions);
        recyclerView.setAdapter(adapter);

        setupCategorySpinner();
        observeViewModel();

        FloatingActionButton fab = findViewById(R.id.fab_add_transaction);
        fab.setOnClickListener(view -> startActivity(new Intent(this, AddTransactionActivity.class)));

        // --- NEW: AUTO-SCAN LOGIC ---
        requestSmsPermissionAndScan();
    }

    private void requestSmsPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            autoScanSmsForCurrentMonth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            autoScanSmsForCurrentMonth();
        } else {
            Toast.makeText(this, "Read SMS permission is needed for auto-import.", Toast.LENGTH_LONG).show();
        }
    }

    private void autoScanSmsForCurrentMonth() {
        Toast.makeText(this, "Scanning messages for current month...", Toast.LENGTH_SHORT).show();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<Transaction> smsTransactions = new ArrayList<>();
            Calendar startOfMonth = Calendar.getInstance();
            startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
            startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
            startOfMonth.set(Calendar.MINUTE, 0);

            Uri smsUri = Uri.parse("content://sms/inbox");
            String[] projection = {"body", "date"};
            String selection = "date >= ?";
            String[] selectionArgs = {String.valueOf(startOfMonth.getTimeInMillis())};

            Cursor cursor = getContentResolver().query(smsUri, projection, selection, selectionArgs, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                    Transaction transaction = SmsParser.parse(body, date); // Assumes SmsParser is updated
                    if (transaction != null) {
                        smsTransactions.add(transaction);
                    }
                }
                cursor.close();
            }

            if (!smsTransactions.isEmpty()) {
                transactionViewModel.addTransactionsBatch(smsTransactions, count -> {
                    handler.post(() -> {
                        if (count > 0) {
                            Toast.makeText(MainActivity.this, "Imported " + count + " new transaction(s).", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "No new transactions to import.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else {
                handler.post(() -> Toast.makeText(MainActivity.this, "No financial messages found.", Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void setupCategorySpinner() {
        String[] categories = new String[]{
                "All Categories", "Food", "Transport", "Shopping", "Bills", "Entertainment",
                "Health", "Education", "Salary", "Gifts", "Other"
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                updateRecyclerView(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateRecyclerView(String category) {
        Query query = transactionViewModel.getFilteredQuery(category);
        FirestoreRecyclerOptions<Transaction> newOptions = new FirestoreRecyclerOptions.Builder<Transaction>()
                .setQuery(query, Transaction.class)
                .build();

        adapter.updateOptions(newOptions);
    }


    private void observeViewModel() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        transactionViewModel.getBalance().observe(this, balance -> {
            if (balance != null) {
                tvBalance.setText(currencyFormat.format(balance));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_import_sms) {
            startActivity(new Intent(this, ImportActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}