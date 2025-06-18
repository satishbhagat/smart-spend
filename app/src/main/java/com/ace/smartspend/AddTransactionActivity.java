package com.ace.smartspend;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.ace.smartspend.model.Transaction;
import com.ace.smartspend.repo.TransactionRepository;
public class AddTransactionActivity extends AppCompatActivity {

    private EditText etAmount, etDescription;
    private Spinner spinnerCategory;
    private RadioGroup radioGroupType;
    private Button btnSaveTransaction;

    private TransactionRepository transactionRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Set the title for the activity
        setTitle("Add Transaction");

        transactionRepository = new TransactionRepository();

        // Initialize UI components
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        radioGroupType = findViewById(R.id.radio_group_type);
        btnSaveTransaction = findViewById(R.id.btn_save_transaction);

        setupCategorySpinner();

        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    /**
     * Sets up the category spinner with a predefined list of categories.
     */
    private void setupCategorySpinner() {
        // Create a list of categories
        String[] categories = new String[]{
                "Food", "Transport", "Shopping", "Bills", "Entertainment",
                "Health", "Education", "Salary", "Gifts", "Other"
        };

        // Create an adapter and set it to the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    /**
     * Validates user input and saves the new transaction to Firestore.
     */
    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        // --- Input Validation ---
        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Amount cannot be empty");
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description cannot be empty");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            return;
        }

        if (amount <= 0) {
            etAmount.setError("Amount must be positive");
            return;
        }
        // --- End Validation ---

        // Determine transaction type from the RadioGroup
        int selectedTypeId = radioGroupType.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedTypeId);
        String type = selectedRadioButton.getText().toString().equalsIgnoreCase("income") ? "income" : "expense";

        // --- CORRECTED LINE ---
        // For manual entries, we use the current time as the smsDate.
        long manualEntryTime = System.currentTimeMillis();
        Transaction newTransaction = new Transaction(null, description, amount, type, category, manualEntryTime);

        // Use the repository to add the transaction
        transactionRepository.addTransaction(newTransaction);

        Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show();

        // Finish the activity and return to the MainActivity
        finish();
    }
}