package com.learnon.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.learnon.app.BuildConfig;
import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.Student;
import com.learnon.app.instructor.InstructorDashboardActivity;
import com.learnon.app.ui.dashboard.DashboardActivity;
import com.learnon.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 1001;
    private static final String TAG = "LoginActivity";

    private EditText etEmail, etSenha;
    private Button btnEntrar, btnGoogle, btnInstructor;
    private TextView tvErro, tvCadastro;
    private GoogleSignInClient googleSignInClient;
    private SessionManager session;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        if (session.estaLogado()) {
            irParaDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail    = findViewById(R.id.etEmail);
        etSenha    = findViewById(R.id.etSenha);
        btnEntrar  = findViewById(R.id.btnEntrar);
        btnGoogle  = findViewById(R.id.btnGoogle);
        btnInstructor = findViewById(R.id.btnInstructor);
        tvErro     = findViewById(R.id.tvErro);
        tvCadastro = findViewById(R.id.tvCadastro);

        api = ApiClient.createService(ApiService.class);
        googleSignInClient = GoogleSignIn.getClient(
                this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                        .build()
        );

        btnEntrar.setOnClickListener(v -> fazerLogin());

        btnGoogle.setOnClickListener(v -> iniciarLoginGoogle());

        btnInstructor.setOnClickListener(v ->
                startActivity(new Intent(this, InstructorDashboardActivity.class))
        );

        tvCadastro.setOnClickListener(v ->
                startActivity(new Intent(this, CadastroActivity.class))
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RC_GOOGLE_SIGN_IN) return;

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            loginGoogleNoServidor(account.getIdToken());
        } catch (ApiException e) {
            btnGoogle.setEnabled(true);
            Log.e(TAG, "Erro no Google Sign-In: statusCode=" + e.getStatusCode(), e);
            mostrarErro("Nao foi possivel entrar com Google. Codigo: " + e.getStatusCode());
        }
    }

    private void fazerLogin() {
        String email = etEmail.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            mostrarErro("Preencha e-mail e senha.");
            return;
        }

        btnEntrar.setEnabled(false);
        btnEntrar.setText("Entrando...");
        tvErro.setVisibility(View.GONE);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", senha);

        api.login(body).enqueue(new Callback<Student>() {
            @Override
            public void onResponse(Call<Student> call, Response<Student> response) {
                btnEntrar.setEnabled(true);
                btnEntrar.setText("Entrar");

                if (response.isSuccessful() && response.body() != null) {
                    Student student = response.body();
                    session.salvarTokens(student.getToken(), student.getRefreshToken());
                    session.salvarNome(student.getName());
                    irParaDashboard();
                } else {
                    mostrarErro("E-mail ou senha incorretos.\n" + lerErro(response.errorBody()));
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                btnEntrar.setEnabled(true);
                btnEntrar.setText("Entrar");
                mostrarErro("Erro de conexao. Tente novamente.");
            }
        });
    }

    private void mostrarErro(String msg) {
        tvErro.setText(msg);
        tvErro.setVisibility(View.VISIBLE);
    }

    private void iniciarLoginGoogle() {
        btnGoogle.setEnabled(false);
        tvErro.setVisibility(View.GONE);
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN);
    }

    private void loginGoogleNoServidor(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            btnGoogle.setEnabled(true);
            mostrarErro("Google nao retornou um token valido.");
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("idToken", idToken);

        api.googleMobileLogin(body).enqueue(new Callback<Student>() {
            @Override
            public void onResponse(Call<Student> call, Response<Student> response) {
                btnGoogle.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Student student = response.body();
                    session.salvarTokens(student.getToken(), student.getRefreshToken());
                    session.salvarNome(student.getName());
                    irParaDashboard();
                } else {
                    mostrarErro("Nao foi possivel concluir o login Google.\n" + lerErro(response.errorBody()));
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                btnGoogle.setEnabled(true);
                mostrarErro("Erro de conexao ao entrar com Google.");
            }
        });
    }

    private String lerErro(ResponseBody errorBody) {
        if (errorBody == null) return "";

        try {
            return errorBody.string();
        } catch (Exception e) {
            return "";
        }
    }

    private void irParaDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}
