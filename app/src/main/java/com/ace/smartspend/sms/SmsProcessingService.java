package com.ace.smartspend.sms;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import com.ace.smartspend.model.Transaction;
import com.ace.smartspend.parser.SmsParser;
import com.ace.smartspend.repo.TransactionRepository;

public class SmsProcessingService extends JobIntentService {

    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SmsProcessingService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String smsBody = intent.getStringExtra("sms_body");
        if (smsBody != null) {
            Log.d("SmsProcessingService", "Processing SMS: " + smsBody);
            Transaction transaction = SmsParser.parse(smsBody);
            if (transaction != null) {
                // Found a financial transaction. Save it.
                TransactionRepository repository = new TransactionRepository();
                repository.addTransaction(transaction);
                Log.d("SmsProcessingService", "Financial transaction found and saved!");
                // Optionally, create a notification to inform the user.
            }
        }
    }
}

