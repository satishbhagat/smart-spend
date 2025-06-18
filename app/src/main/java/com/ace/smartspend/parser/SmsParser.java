package com.ace.smartspend.parser;

import com.ace.smartspend.model.Transaction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    // This single regex captures amounts with Rs, INR, or no currency symbol.
    private static final Pattern amountPattern = Pattern.compile("(?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)|([\\d,]+\\.?\\d*)\\s*(?:Rs|INR)");
    private static final Pattern debitPattern = Pattern.compile("(?i)(?:debited|spent|paid|charged)");
    private static final Pattern creditPattern = Pattern.compile("(?i)(?:credited|received)");
    private static final Pattern upiPattern = Pattern.compile("(?i)UPI");

    public static Transaction parse(String smsBody, long smsDate) {
        Matcher amountMatcher = amountPattern.matcher(smsBody);
        if (!amountMatcher.find()) {
            return null;
        }

        double amount;
        try {
            // The regex has two capturing groups for amount. One will be null.
            String amountStr = amountMatcher.group(1) != null ? amountMatcher.group(1) : amountMatcher.group(2);
            if(amountStr == null) return null; // Should not happen if matcher.find() is true
            amount = Double.parseDouble(amountStr.replace(",", ""));
        } catch (Exception e) {
            return null; // Parsing failed
        }

        String type;
        if (debitPattern.matcher(smsBody).find()) {
            type = "expense";
        } else if (creditPattern.matcher(smsBody).find()) {
            type = "income";
        } else {
            return null; // Not clearly a debit or credit message
        }

        // Simple rule-based categorization
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

        // Create the transaction using the correct constructor
        return new Transaction(null, description, amount, type, category, smsDate);
    }
}
