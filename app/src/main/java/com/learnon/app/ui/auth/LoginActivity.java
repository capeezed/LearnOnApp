package com.learnon.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etSenha;
    private Button btnEntrar, btnGoogle, btnInstructor;
    private TextView tvErro, tvCadastro;
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

        btnEntrar.setOnClickListener(v -> fazerLogin());

        btnGoogle.setOnClickListener(v -> {
            String url = BuildConfig.LEARNON_BASE_URL + "auth/google";
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(intent);
        });

        btnInstructor.setOnClickListener(v ->
                startActivity(new Intent(this, InstructorDashboardActivity.class))
        );

        tvCadastro.setOnClickListener(v ->
                startActivity(new Intent(this, CadastroActivity.class))
        );
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
                    session.salvarToken(student.getToken());
                    session.salvarNome(student.getName());
                    irParaDashboard();
                } else {
                    mostrarErro("E-mail ou senha incorretos.");
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

    private void irParaDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}
