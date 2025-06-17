package com.ace.smartspend.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ace.smartspend.R;
import com.ace.smartspend.model.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParsedTransactionAdapter extends RecyclerView.Adapter<ParsedTransactionAdapter.ViewHolder> {

    private final List<Transaction> transactionList;
    private final List<Boolean> checkedState;

    public ParsedTransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
        this.checkedState = new ArrayList<>();
        // Initialize checked state based on the initial list
        resetCheckedState();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parsed_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.bind(transaction, position);
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    // This is the new public method to reset the checked state.
    // It replaces the incorrect override of notifyDataSetChanged().
    public void resetCheckedState() {
        checkedState.clear();
        for (int i = 0; i < transactionList.size(); i++) {
            checkedState.add(true); // Default all to checked
        }
    }

    public List<Transaction> getSelectedTransactions() {
        List<Transaction> selected = new ArrayList<>();
        for (int i = 0; i < transactionList.size(); i++) {
            if (i < checkedState.size() && checkedState.get(i)) {
                selected.add(transactionList.get(i));
            }
        }
        return selected;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount;
        CheckBox cbSelect;

        ViewHolder(View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tv_parsed_description);
            tvAmount = itemView.findViewById(R.id.tv_parsed_amount);
            cbSelect = itemView.findViewById(R.id.cb_parsed_select);
        }

        void bind(Transaction transaction, int position) {
            tvDescription.setText(transaction.getDescription() + " (" + transaction.getCategory() + ")");
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            String formattedAmount = currencyFormat.format(transaction.getAmount());
            tvAmount.setText(formattedAmount);

            // Ensure checkedState has been populated for this position
            if (position < checkedState.size()) {
                cbSelect.setChecked(checkedState.get(position));
            }

            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    checkedState.set(getAdapterPosition(), isChecked);
                }
            });
        }
    }
}