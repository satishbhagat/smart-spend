package com.ace.smartspend.adapter;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ace.smartspend.R;
import com.ace.smartspend.model.Transaction;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import java.text.NumberFormat;
import java.util.Locale;

public class TransactionAdapter extends FirestoreRecyclerAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.
     * @param options The FirestoreRecyclerOptions which includes the query.
     */
    public TransactionAdapter(@NonNull FirestoreRecyclerOptions<Transaction> options) {
        super(options);
    }

    /**
     * Binds a Transaction object to the ViewHolder, setting the UI elements.
     */
    @Override
    protected void onBindViewHolder(@NonNull TransactionViewHolder holder, int position, @NonNull Transaction model) {
        holder.bind(model);
    }

    /**
     * Creates a new ViewHolder by inflating the item_transaction.xml layout.
     */
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    /**
     * The ViewHolder holds the views for a single item in the list.
     */
    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView description;
        private final TextView category;
        private final TextView amount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.tv_transaction_description);
            category = itemView.findViewById(R.id.tv_transaction_category);
            amount = itemView.findViewById(R.id.tv_transaction_amount);
        }

        // Helper method to set the data for a transaction
        public void bind(Transaction transaction) {
            description.setText(transaction.getDescription());
            category.setText(transaction.getCategory());

            // Format amount as Indian Rupee currency
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            String formattedAmount = currencyFormat.format(transaction.getAmount() != null ? transaction.getAmount() : 0.0);

            // Set amount text and color based on transaction type
            if ("income".equals(transaction.getType())) {
                amount.setText("+ " + formattedAmount);
                amount.setTextColor(Color.parseColor("#2E7D32")); // Dark Green
            } else {
                amount.setText("- " + formattedAmount);
                amount.setTextColor(Color.parseColor("#C62828")); // Dark Red
            }
        }
    }
}