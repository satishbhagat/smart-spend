package com.ace.smartspend.model;
// In a new file Transaction.java

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Transaction {
    private String userId;
    private String description;
    private Double amount;
    private String type; // "income" or "expense"
    private String category;
    @ServerTimestamp // Automatically set by Firestore on creation
    private Date timestamp;

    public Transaction() {
        // Public no-arg constructor needed for Firestore
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Transaction(String userId, String description, Double amount, String type, String category) {
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
    }

    // --- Add Getters and Setters for all fields ---
    // (Right-click -> Generate -> Getters and Setters -> Select all)
}
