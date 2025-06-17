package com.ace.smartspend;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ace.smartspend.adapter.TransactionAdapter;
import com.ace.smartspend.model.Transaction;
import com.ace.smartspend.viewmodel.TransactionViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Query;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

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

        // --- NEW, MORE ROBUST SETUP ---

        // 1. Setup the RecyclerView and create the adapter once with an initial query
        RecyclerView recyclerView = findViewById(R.id.recycler_view_transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Query initialQuery = transactionViewModel.getFilteredQuery("All Categories");
        FirestoreRecyclerOptions<Transaction> initialOptions = new FirestoreRecyclerOptions.Builder<Transaction>()
                .setQuery(initialQuery, Transaction.class)
                .build();
        adapter = new TransactionAdapter(initialOptions);
        recyclerView.setAdapter(adapter);

        // 2. Setup the spinner. Its listener is now the only thing that updates the adapter.
        setupCategorySpinner();

        // 3. Observe the ViewModel for balance changes
        observeViewModel();

        FloatingActionButton fab = findViewById(R.id.fab_add_transaction);
        fab.setOnClickListener(view -> {
            startActivity(new Intent(this, AddTransactionActivity.class));
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

        // This listener is now the single source of truth for updating the list
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

    // This method is now the only place where the adapter's data source is changed.
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