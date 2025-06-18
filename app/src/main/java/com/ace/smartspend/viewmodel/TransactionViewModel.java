package com.ace.smartspend.viewmodel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ace.smartspend.model.Transaction;
import com.ace.smartspend.repo.TransactionRepository;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class TransactionViewModel extends ViewModel {

    private final TransactionRepository transactionRepository;

    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> balance = new MutableLiveData<>(0.0);

    private ListenerRegistration transactionListener;

    public TransactionViewModel() {
        transactionRepository = new TransactionRepository();
        listenForTransactions();
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }
    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }
    public LiveData<Double> getBalance() {
        return balance;
    }

    private void listenForTransactions() {
        Query query = transactionRepository.getTransactionsQuery();
        if (query == null) return;

        transactionListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) { return; }
            double income = 0.0;
            double expense = 0.0;
            for (QueryDocumentSnapshot doc : snapshots) {
                Transaction transaction = doc.toObject(Transaction.class);
                if (transaction.getAmount() == null) continue;
                if ("income".equals(transaction.getType())) {
                    income += transaction.getAmount();
                } else {
                    expense += transaction.getAmount();
                }
            }
            totalIncome.postValue(income);
            totalExpense.postValue(expense);
            balance.postValue(income - expense);
        });
    }

    public Query getFilteredQuery(String category) {
        Query query = transactionRepository.getTransactionsQuery();
        if (query != null && category != null && !category.equals("All Categories")) {
            query = query.whereEqualTo("category", category);
        }
        return query;
    }

    public void addTransactionsBatch(List<Transaction> transactions, TransactionRepository.OnBatchCompleteListener listener) {
        transactionRepository.addTransactionsBatchCheckingDuplicates(transactions, listener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (transactionListener != null) {
            transactionListener.remove();
        }
    }
}