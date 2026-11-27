package dev.neko.chatdemo.sheet;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.sidesheet.SideSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import dev.neko.chatdemo.R;

public class SidebarSheet extends SideSheetDialog {

    public SidebarSheet(@NonNull Context context) {
        super(context);
        setContentView(R.layout.sheet_sidebar);
        setSheetEdge(Gravity.START);
        setCanceledOnTouchOutside(true);

        View newSession = findViewById(R.id.sidebar_new_session);
        View settings = findViewById(R.id.sidebar_settings);
        if (newSession != null) {
            newSession.setOnClickListener(v -> demo("Demo: new session"));
        }
        if (settings != null) {
            settings.setOnClickListener(v -> demo("Demo: settings"));
        }
    }

    private void demo(String message) {
        Context context = getContext();
        dismiss();
        if (context instanceof Activity) {
            View root = ((Activity) context).findViewById(android.R.id.content);
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
        }
    }
}
