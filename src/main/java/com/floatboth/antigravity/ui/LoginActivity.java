package com.floatboth.antigravity.ui;

import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.net.Uri;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import net.app.adnlogin.ADNPassportUtility;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import org.androidannotations.annotations.*;
import org.androidannotations.annotations.sharedpreferences.*;
import org.androidannotations.annotations.res.StringRes;

import com.floatboth.antigravity.*;
import com.floatboth.antigravity.data.*;
import com.floatboth.antigravity.net.*;

@EActivity(R.layout.login_activity)
public class LoginActivity extends BaseActivity {
  @StringRes String empty_field;
  @StringRes String username_hint;
  @StringRes String password_hint;
  @StringRes String must_not_be_empty;
  @StringRes String adn_info_text;
  @StringRes String client_id;
  @StringRes String password_secret;
  @StringRes String login_progress;

  @SystemService InputMethodManager inputMethodManager;
  @Pref ADNPrefs_ adnPrefs;
  @ViewById EditText username_field;
  @ViewById EditText password_field;
  @ViewById Button login_with_passport;
  @ViewById Button install_passport;
  @ViewById TextView adn_info;
  @ViewById TextView or_label;
  ProgressDialog loginProgress;

  private static final int REQUEST_CODE_AUTHORIZE = 1;
  private static final String AUTHORIZE_ACTION = "net.app.adnpassport.authorize";
  private static final String SCOPES = "basic,write_post,files";

  // Event listeners

  public void onLoginSuccess(String token) {
    adnPrefs.accessToken().put(token);
    MainActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
    finish();
  }

  @OnActivityResult(REQUEST_CODE_AUTHORIZE)
  public void onLoginWithPassportSuccess(int resultCode, Intent data) {
    if (resultCode == 1) onLoginSuccess(data.getStringExtra("accessToken"));
  }

  private final BroadcastReceiver passportInstallReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) &&
          intent.getDataString().equals(String.format("package:%s", ADNPassportUtility.APP_PACKAGE)))
        loginWithPassport();
    }
  };

  // UI helpers

  public void showError(String title, String message) {
    new AlertDialog.Builder(this).setTitle(title).setMessage(message)
      .setPositiveButton(R.string.ok, null).show();
  }

  public void showPasswordADNError(ADNAuthError ex) {
    showError(ex.title, ex.text);
  }

  public void showUnknownError(Throwable ex) {
    showError("Unknown error", ex.getMessage());
  }

  // Lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
    filter.addDataScheme("package");
    registerReceiver(passportInstallReceiver, filter);
  }

  @AfterViews
  public void setUpViews() {
    password_field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
          loginWithPassword();
        }
        return false;
      }
    });
    adn_info.setText(Html.fromHtml(adn_info_text));
    adn_info.setMovementMethod(LinkMovementMethod.getInstance());
    if (ADNPassportUtility.isPassportAuthorizationAvailable(this)) {
      login_with_passport.setVisibility(View.VISIBLE);
    } else if (hasMarketInstalled()) {
      install_passport.setVisibility(View.VISIBLE);
    } else {
      or_label.setVisibility(View.GONE);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(passportInstallReceiver);
  }

  // Actions

  @Click(R.id.install_passport)
  public void installPassport() {
    ADNPassportUtility.launchPassportInstallation(this);
  }

  @Click(R.id.login_with_passport)
  public void loginWithPassport() {
    Intent i = ADNPassportUtility.getAuthorizationIntent(client_id, SCOPES);
    startActivityForResult(i, REQUEST_CODE_AUTHORIZE);
  }

  @Click(R.id.login_with_password)
  public void loginWithPassword() {
    String username = username_field.getText().toString().trim();
    String password = password_field.getText().toString();
    if (username.matches("")) {
      showError(empty_field, username_hint + " " + must_not_be_empty);
    } else if (password.matches("")) {
      showError(empty_field, password_hint + " " + must_not_be_empty);
    } else {
      inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
      loginProgress = ProgressDialog.show(this, null, login_progress, true, false);
      getSpiceManager().execute(new LoginRequest(client_id, password_secret, username, password, SCOPES),
          new LoginListener());
    }
  }

  public class LoginListener implements RequestListener<String> {
    @Override
    public void onRequestFailure(SpiceException spiceException) {
      loginProgress.dismiss();
      spiceException.printStackTrace();
      try {
        throw spiceException.getCause();
      } catch (ADNAuthError ex) {
        showPasswordADNError(ex);
      } catch (Throwable ex) {
        showUnknownError(ex);
      }
    }

    @Override
    public void onRequestSuccess(final String token) {
      loginProgress.dismiss();
      onLoginSuccess(token);
    }
  }

  // http://www.thiagorosa.com.br/how-to/check-if-market-is-installed
  public boolean hasMarketInstalled() {
    Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=dummy"));
    List<ResolveInfo> list = getPackageManager().queryIntentActivities(market, 0);
    if (list != null && list.size() > 0)
      for (int i = 0; i < list.size(); i++)
        if (list.get(i).activityInfo.packageName.startsWith("com.android.vending") == true)
          return true;
    return false;
  }
}
