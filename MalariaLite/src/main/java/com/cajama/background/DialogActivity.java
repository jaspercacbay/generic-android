package com.cajama.background;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.cajama.malarialite.R;

/**
 * Created by Jasper on 9/8/13.
 */
public class DialogActivity extends Activity {
    String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater li = LayoutInflater.from(this);
        View promptView = li.inflate(R.layout.prompt_password, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(promptView);

        final EditText password = (EditText) promptView.findViewById(R.id.dialogActivityPassword);
        final EditText username = (EditText) promptView.findViewById(R.id.dialogActivityEditText);

        String number = getIntent().getStringExtra("tries");

        alertDialogBuilder
                .setTitle("Retype credentials")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick (DialogInterface dialog, int id) {
                        result = username.getText().toString().trim();
                        result += "\n";
                        result += password.getText().toString().trim();
                        returnOk(result);
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();

        if (!number.equals("-1")) alertDialogBuilder.setMessage("Retries: " +number + " left");

        alertDialog.show();
    }

    private void returnOk(String ok) {
        Intent srcIntent = this.getIntent();
        Intent tgtIntent = new Intent();
        String className = srcIntent.getExtras().getString("passwd");
        Log.d("DialogActivity", "Service Class Name: " + className);
        ComponentName cn = new ComponentName(this.getApplicationContext(), className);
        tgtIntent.setComponent(cn);
        tgtIntent.putExtra("message", ok);
        this.startService(tgtIntent);
        this.finish();
    }
}
