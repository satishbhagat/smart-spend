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

public class TransactionRepository {

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference transactionRef = db.collection("transactions");

    // --- THIS IS THE CORRECTED METHOD ---
    public void addTransaction(Transaction transaction) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            transaction.setUserId(userId);
            transactionRef.add(transaction);
        } else {
            // Handle the case where the user is somehow null
            // This prevents the crash. You could log an error here.
            System.err.println("Error: User is not logged in. Cannot add transaction.");
        }
    }

    public Query getTransactionsQuery() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // This query fetches transactions for the current user and orders them by date
            return transactionRef
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }
        // Return null or an empty query if the user is not logged in
        return null;
    }
}