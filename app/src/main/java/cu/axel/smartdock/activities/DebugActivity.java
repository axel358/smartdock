package cu.axel.smartdock.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import cu.axel.smartdock.R;
import cu.axel.smartdock.utils.Utils;

public class DebugActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.DialogTheme);
		dialog.setTitle(R.string.something_wrong);
		final String report = getIntent().getStringExtra("report");
        dialog.setMessage(report);
		dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();

                }
            });
        dialog.setNeutralButton(R.string.save_log, new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    Utils.saveLog(DebugActivity.this, "crash_log", report);
                    finish();
                }
            });
        dialog.setNegativeButton(R.string.open_again, new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    startActivity(new Intent(DebugActivity.this, MainActivity.class));
                    finish();
                }
            });
        dialog.setCancelable(false);
		dialog.create().show();
    }
}
