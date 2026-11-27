package dev.neko.chatdemo.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputEditText;

import dev.neko.chatdemo.R;

/** Full screen composer. Keeps the draft in sync when it closes. */
public class FullscreenInputDialog extends Dialog {

    public interface Listener {
        void onSend(String text);

        void onClosed(String text);
    }

    private final Listener listener;
    private final TextInputEditText input;

    public FullscreenInputDialog(@NonNull Context context, @NonNull String initial,
                                 @NonNull Listener listener) {
        super(DynamicColors.wrapContextIfAvailable(context), R.style.Theme_Neko_FullscreenInput);
        this.listener = listener;

        View root = LayoutInflater.from(context).inflate(R.layout.dialog_fullscreen_input, null);
        setContentView(root);
        setCanceledOnTouchOutside(false);

        input = root.findViewById(R.id.fullscreen_input);
        input.setText(initial);
        if (initial.length() > 0) {
            input.setSelection(initial.length());
        }

        MaterialButton close = root.findViewById(R.id.fullscreen_close);
        MaterialButton send = root.findViewById(R.id.fullscreen_send);
        close.setOnClickListener(v -> dismiss());
        send.setOnClickListener(v -> {
            String text = input.getText() == null ? "" : input.getText().toString().trim();
            dismiss();
            listener.onSend(text);
        });

        setOnDismissListener(d -> {
            String text = input.getText() == null ? "" : input.getText().toString();
            listener.onClosed(text);
        });

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(window, window.getDecorView());
            boolean night = (context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            controller.setAppearanceLightStatusBars(!night);
            controller.setAppearanceLightNavigationBars(!night);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets ime =
                    insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });

        setOnShowListener(d -> {
            input.requestFocus();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        });
    }

    public void showWithKeyboard() {
        show();
        input.requestFocus();
        if (getWindow() != null) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }
}
