package com.challenge.vault;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/* loaded from: classes3.dex */
public class MainActivity extends AppCompatActivity {
    private CacheManager cacheManager;
    private MaterialCardView flagCard;
    private TextView flagText;
    private TextInputEditText passwordInput;
    private TextInputLayout passwordLayout;
    private TextView statusText;
    private MaterialButton unlockButton;

    private void attemptUnlock() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
        if (inputMethodManager != null && getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        final String obj = this.passwordInput.getText() != null ? this.passwordInput.getText().toString() : "";
        boolean isEmpty = obj.isEmpty();
        TextInputLayout textInputLayout = this.passwordLayout;
        if (isEmpty) {
            textInputLayout.setError("Enter a code");
            return;
        }
        textInputLayout.setError(null);
        this.unlockButton.setEnabled(false);
        this.unlockButton.setText("CHECKING...");
        new Thread(new Runnable() { // from class: com.challenge.vault.MainActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m124lambda$attemptUnlock$4$comchallengevaultMainActivity(obj);
            }
        }).start();
    }

    private void initViews() {
        this.passwordInput = (TextInputEditText) findViewById(R.id.passwordInput);
        this.passwordLayout = (TextInputLayout) findViewById(R.id.passwordLayout);
        this.unlockButton = (MaterialButton) findViewById(R.id.unlockButton);
        this.statusText = (TextView) findViewById(R.id.statusText);
        this.flagCard = (MaterialCardView) findViewById(R.id.flagCard);
        this.flagText = (TextView) findViewById(R.id.flagText);
        this.unlockButton.setOnClickListener(new View.OnClickListener() { // from class: com.challenge.vault.MainActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m125lambda$initViews$1$comchallengevaultMainActivity(view);
            }
        });
        this.passwordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.challenge.vault.MainActivity$$ExternalSyntheticLambda7
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return MainActivity.this.m126lambda$initViews$2$comchallengevaultMainActivity(textView, i, keyEvent);
            }
        });
    }

    static /* synthetic */ WindowInsetsCompat lambda$onCreate$0(View view, WindowInsetsCompat windowInsetsCompat) {
        Insets insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars());
        view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        return windowInsetsCompat;
    }

    private void onUnlockFailed() {
        this.statusText.setVisibility(0);
        this.statusText.setText(getString(R.string.wrong_code));
        this.statusText.setTextColor(getColor(R.color.vault_error));
        this.passwordLayout.setError(" ");
        this.unlockButton.setEnabled(true);
        this.unlockButton.setText(getString(R.string.unlock));
        this.passwordLayout.animate().translationX(10.0f).setDuration(50L).withEndAction(new Runnable() { // from class: com.challenge.vault.MainActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m128lambda$onUnlockFailed$6$comchallengevaultMainActivity();
            }
        }).start();
    }

    private void onUnlockSuccess() {
        this.statusText.setVisibility(8);
        this.flagCard.setVisibility(0);
        this.flagText.setText("FLAG{5_FR4GM3NT5_D3F34T3D}");
        this.unlockButton.setEnabled(false);
        this.unlockButton.setText("UNLOCKED");
        this.passwordInput.setEnabled(false);
    }

    private void showSecurityWarning() {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog).setTitle((CharSequence) "Security Alert").setMessage((CharSequence) getString(R.string.security_warning)).setCancelable(false).setPositiveButton((CharSequence) "Exit", new DialogInterface.OnClickListener() { // from class: com.challenge.vault.MainActivity$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m129lambda$showSecurityWarning$7$comchallengevaultMainActivity(dialogInterface, i);
            }
        }).show();
    }

    /* renamed from: lambda$attemptUnlock$3$com-challenge-vault-MainActivity, reason: not valid java name */
    /* synthetic */ void m123lambda$attemptUnlock$3$comchallengevaultMainActivity(boolean z) {
        if (z) {
            onUnlockSuccess();
        } else {
            onUnlockFailed();
        }
    }

    /* renamed from: lambda$attemptUnlock$4$com-challenge-vault-MainActivity, reason: not valid java name */
    /* synthetic */ void m124lambda$attemptUnlock$4$comchallengevaultMainActivity(String str) {
        final boolean invalidateCache = this.cacheManager.invalidateCache(str);
        runOnUiThread(new Runnable() { // from class: com.challenge.vault.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m123lambda$attemptUnlock$3$comchallengevaultMainActivity(invalidateCache);
            }
        });
    }

    /* renamed from: lambda$initViews$1$com-challenge-vault-MainActivity, reason: not valid java name */
    /* synthetic */ void m125lambda$initViews$1$comchallengevaultMainActivity(View view) {
        attemptUnlock();
    }

    /* renamed from: lambda$initViews$2$com-challenge-vault-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m126lambda$initViews$2$comchallengevaultMainActivity(TextView textView, int i, KeyEvent keyEvent) {
        if (i != 6) {
            return false;
        }
        attemptUnlock();
        return true;
    }

    /* renamed from: lambda$onUnlockFailed$5$com-challenge-vault-MainActivity, reason: not valid java name */
    /* synthetic */ void m127lambda$onUnlockFailed$5$comchallengevaultMainActivity() {
        this.passwordLayout.animate().translationX(0.0f).setDuration(50L).start();
    }

    /* renamed from: lambda$onUnlockFailed$6$com-challenge-vault-MainActivity, reason: not valid java name */
    /* synthetic */ void m128lambda$onUnlockFailed$6$comchallengevaultMainActivity() {
        this.passwordLayout.animate().translationX(-10.0f).setDuration(50L).withEndAction(new Runnable() { // from class: com.challenge.vault.MainActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m127lambda$onUnlockFailed$5$comchallengevaultMainActivity();
            }
        }).start();
    }

    /* renamed from: lambda$showSecurityWarning$7$com-challenge-vault-MainActivity, reason: not valid java name */
    /* synthetic */ void m129lambda$showSecurityWarning$7$comchallengevaultMainActivity(DialogInterface dialogInterface, int i) {
        finishAffinity();
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() { // from class: com.challenge.vault.MainActivity$$ExternalSyntheticLambda1
            @Override // androidx.core.view.OnApplyWindowInsetsListener
            public final WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsetsCompat) {
                return MainActivity.lambda$onCreate$0(view, windowInsetsCompat);
            }
        });
        if (NetworkHelper.isOfflineMode()) {
            showSecurityWarning();
        } else {
            initViews();
            this.cacheManager = new CacheManager(this);
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        if (NetworkHelper.isOfflineMode()) {
            showSecurityWarning();
        }
    }
}
