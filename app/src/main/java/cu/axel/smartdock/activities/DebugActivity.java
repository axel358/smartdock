package cu.axel.smartdock.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cu.axel.smartdock.R;
import cu.axel.smartdock.utils.Utils;

public class DebugActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.something_wrong);
        final String report = getIntent().getStringExtra("report");
        dialog.setMessage(report);
        dialog.setPositiveButton(R.string.ok, (DialogInterface p1, int which) -> finish());

        dialog.setNeutralButton(R.string.save_log, (DialogInterface p1, int p2) -> {
            Utils.saveLog(DebugActivity.this, "crash_log", report);
            finish();
        });
        dialog.setNegativeButton(R.string.open_again, (DialogInterface p1, int p2) -> {
            startActivity(new Intent(DebugActivity.this, MainActivity.class));
            finish();
        });
        dialog.setCancelable(false);
        dialog.create().show();
    }
}
