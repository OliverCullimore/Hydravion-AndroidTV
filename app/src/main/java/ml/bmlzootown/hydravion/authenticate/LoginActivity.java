package ml.bmlzootown.hydravion.authenticate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.volley.VolleyError;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.UUID;

import ml.bmlzootown.hydravion.R;
import ml.bmlzootown.hydravion.browse.MainFragment;
import ml.bmlzootown.hydravion.models.LoginResponse;

public class LoginActivity extends Activity {

    private TextInputEditText username;
    private TextInputEditText password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
    }

    public void login(@Nullable View view) {
        if (username != null && password != null) {
            if (username.length() > 0 || password.length() > 0) {
                String user = username.getText().toString();
                String pass = password.getText().toString();
                hideSoftKeyboard(view);
                doLogin(user, pass);
            } else {
                Toast.makeText(this, "Incorrect username/password!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Incorrect username/password!", Toast.LENGTH_SHORT).show();
        }
    }

    private void doLogin(String username, String password) {
        LoginRequestTask rt = new LoginRequestTask(this.getApplicationContext());
        rt.sendRequest(username, password, new LoginRequestTask.VolleyCallback() {
            @Override
            public void onSuccess(ArrayList<String> cookies, String response) {
                if (response.length() < 19) {
                    MainFragment.dLog("RESPONSE", "Possible 2FA");
                    Gson gson = new Gson();
                    LoginResponse res = gson.fromJson(response, LoginResponse.class);
                    if (res.getNeeds2FA()) {
                        MainFragment.dLog("RESPONSE", "NEEDS 2FA");
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        builder.setTitle("2FA Token");
                        final EditText input = new EditText(LoginActivity.this);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String token = input.getText().toString();
                                LoginRequestTask twoFA = new LoginRequestTask(LoginActivity.this);
                                twoFA.sendRequest(token, cookies, new LoginRequestTask.TwoFACallback() {
                                    @Override
                                    public void onSuccess(ArrayList<String> string) {
                                        String sailssid = string.get(0);
                                        ArrayList<String> newCookies = new ArrayList<>();
                                        for (String c : cookies) {
                                            if (!c.contains("sails.sid")) {
                                                newCookies.add(c);
                                            }
                                        }
                                        newCookies.add(sailssid);
                                        Intent intent = new Intent();
                                        intent.putStringArrayListExtra("cookies", newCookies);
                                        setResult(1, intent);
                                        finish();
                                    }

                                    @Override
                                    public void onError(VolleyError ve) {
                                        Toast.makeText(LoginActivity.this, "Incorrect 2FA token!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                        builder.show();
                    }
                } else {
                    Intent intent = new Intent();
                    intent.putStringArrayListExtra("cookies", cookies);
                    setResult(1, intent);
                    finish();
                }
                MainFragment.dLog("response", response);
            }

            @Override
            public void onError() {
                Toast.makeText(getApplicationContext(), "Incorrect username/password!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}