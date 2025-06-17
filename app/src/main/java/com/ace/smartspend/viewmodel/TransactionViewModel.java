package com.ace.smartspend.viewmodel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ace.smartspend.model.Transaction;
import com.ace.smartspend.repo.TransactionRepository;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.lifecycle.ViewModel;
public class TransactionViewModel extends ViewModel {

    private final TransactionRepository transactionRepository;

    // LiveData for the summary. The UI will "observe" these for changes.
    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> balance = new MutableLiveData<>(0.0);

    // This will hold the listener registration to remove it when the ViewModel is cleared.
    private ListenerRegistration transactionListener;

    public TransactionViewModel() {
        transactionRepository = new TransactionRepository();
        listenForTransactions();
    }

    // --- Public LiveData getters for the UI to observe ---
    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }

    public LiveData<Double> getBalance() {
        return balance;
    }
    // --- End of public getters ---

    /**
     * Attaches a real-time listener to the Firestore query.
     * Whenever the data changes on the server, this code runs automatically.
     */
    private void listenForTransactions() {
        Query query = transactionRepository.getTransactionsQuery();
        if (query == null) return; // In case user is not logged in yet

        transactionListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                // Handle the error, maybe log it
                return;
            }

            double income = 0.0;
            double expense = 0.0;
            // Loop through all documents in the result
            for (QueryDocumentSnapshot doc : snapshots) {
                Transaction transaction = doc.toObject(Transaction.class);
                if (transaction.getAmount() == null) continue;

                if ("income".equals(transaction.getType())) {
                    income += transaction.getAmount();
                } else {
                    expense += transaction.getAmount();
                }
            }

            // Post the new calculated values to the LiveData objects.
            // Any UI element observing them will update automatically.
            totalIncome.postValue(income);
            totalExpense.postValue(expense);
            balance.postValue(income - expense);
        });
    }

    public Query getTransactionsQuery() {
        return transactionRepository.getTransactionsQuery();
    }

    /**
     * Constructs a Firestore query based on a selected category.
     * @param category The category to filter by. If "All Categories", no filter is applied.
     * @return A Firestore Query object.
     */
    public Query getFilteredQuery(String category) {
        // This is the base query that gets all transactions for the user, sorted by time
        Query query = transactionRepository.getTransactionsQuery();

        if (query != null && category != null && !category.equals("All Categories")) {
            // Add the category filter if a specific category is chosen
            query = query.whereEqualTo("category", category);
        }

        return query;
    }


    /**
     * This method is called when the ViewModel is no longer used and will be destroyed.
     * It's crucial to remove the Firestore listener here to prevent memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (transactionListener != null) {
            transactionListener.remove();
        }
    }
}