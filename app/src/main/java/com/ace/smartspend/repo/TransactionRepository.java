package com.ace.smartspend.repo;

// In a new file TransactionRepository.java
// In your TransactionRepository.java file


import com.ace.smartspend.model.Transaction;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionRepository {

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference transactionRef = db.collection("transactions");

    public void addTransaction(Transaction transaction) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            transaction.setUserId(currentUser.getUid());
            transactionRef.add(transaction);
        }
    }

    public Query getTransactionsQuery() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            return transactionRef
                    .whereEqualTo("userId", currentUser.getUid())
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }
        return null;
    }

    public void addTransactionsBatchCheckingDuplicates(List<Transaction> transactions, OnBatchCompleteListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || transactions.isEmpty()) {
            listener.onComplete(0);
            return;
        }

        String userId = currentUser.getUid();

        // Get start of month to only check recent transactions for duplicates
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        long startOfMonth = cal.getTimeInMillis();

        transactionRef.whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("smsDate", startOfMonth)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> existingTransactionKeys = new HashSet<>();
                    for (Transaction t : queryDocumentSnapshots.toObjects(Transaction.class)) {
                        // Create a unique key for each existing transaction
                        existingTransactionKeys.add(t.getSmsDate() + "|" + t.getAmount());
                    }

                    WriteBatch batch = db.batch();
                    int newTransactionsCount = 0;

                    for (Transaction newTransaction : transactions) {
                        String newKey = newTransaction.getSmsDate() + "|" + newTransaction.getAmount();
                        if (!existingTransactionKeys.contains(newKey)) {
                            newTransaction.setUserId(userId);
                            batch.set(transactionRef.document(), newTransaction);
                            newTransactionsCount++;
                        }
                    }

                    if (newTransactionsCount > 0) {
                        int finalNewTransactionsCount = newTransactionsCount;
                        batch.commit()
                                .addOnSuccessListener(aVoid -> listener.onComplete(finalNewTransactionsCount))
                                .addOnFailureListener(e -> listener.onComplete(0));
                    } else {
                        listener.onComplete(0);
                    }
                })
                .addOnFailureListener(e -> listener.onComplete(0));
    }

    public interface OnBatchCompleteListener {
        void onComplete(int count);
    }
}