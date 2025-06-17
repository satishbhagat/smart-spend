package com.ace.smartspend.parser;

import com.ace.smartspend.model.Transaction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {
    // Regex to find amounts like Rs. 5,000.00, INR 500, 2500.00
    private static final Pattern amountPattern = Pattern.compile("(?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)");
    // Regex to find debited/spent keywords
    private static final Pattern debitPattern = Pattern.compile("(?i)(?:debited|spent|paid|charged)");
    // Regex to find credited keywords
    private static final Pattern creditPattern = Pattern.compile("(?i)(?:credited|received)");
    // Regex to identify UPI transactions
    private static final Pattern upiPattern = Pattern.compile("(?i)UPI");

    public static Transaction parse(String smsBody) {
        Matcher amountMatcher = amountPattern.matcher(smsBody);
        if (!amountMatcher.find()) {
            return null; // Not a financial SMS if no amount is found
        }

        double amount;
        try {
            String amountStr = amountMatcher.group(1).replace(",", "");
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            return null;
        }

        String type;
        if (debitPattern.matcher(smsBody).find()) {
            type = "expense";
        } else if (creditPattern.matcher(smsBody).find()) {
            type = "income";
        } else {
            return null; // Not clearly debit or credit
        }

        // Simple AI/Rule-based categorization
        String description;
        String category;

        if (upiPattern.matcher(smsBody).find()) {
            description = "UPI Payment";
            category = "UPI";
        } else if (smsBody.toLowerCase().contains("zomato")) {
            description = "Zomato Order";
            category = "Food";
        } else if (smsBody.toLowerCase().contains("swiggy")) {
            description = "Swiggy Order";
            category = "Food";
        } else if (smsBody.toLowerCase().contains("amazon")) {
            description = "Amazon Purchase";
            category = "Shopping";
        } else if (smsBody.toLowerCase().contains("flipkart")) {
            description = "Flipkart Purchase";
            category = "Shopping";
        } else {
            description = "Bank Transaction";
            category = "Other";
        }

        return new Transaction(null, description, amount, type, category);
    }
}
