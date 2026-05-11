package com.learnon.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "learnon_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_NOME  = "nome";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void salvarToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void salvarTokens(String token, String refreshToken) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public void salvarNome(String nome) {
        prefs.edit().putString(KEY_NOME, nome).apply();
    }

    public String getNome() {
        return prefs.getString(KEY_NOME, "");
    }

    public boolean estaLogado() {
        return getToken() != null;
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
