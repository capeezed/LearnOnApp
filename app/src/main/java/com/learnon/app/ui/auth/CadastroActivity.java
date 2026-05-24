package com.learnon.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.Student;
import com.learnon.app.ui.dashboard.DashboardActivity;
import com.learnon.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CadastroActivity extends AppCompatActivity {

    private EditText etNome, etEmail, etSenha;
    private Button btnCadastrar;
    private TextView tvErro, tvLogin;
    private SessionManager session;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        session     = new SessionManager(this);
        api         = ApiClient.createService(ApiService.class);

        etNome      = findViewById(R.id.etNome);
        etEmail     = findViewById(R.id.etEmail);
        etSenha     = findViewById(R.id.etSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        tvErro      = findViewById(R.id.tvErro);
        tvLogin     = findViewById(R.id.tvLogin);

        btnCadastrar.setOnClickListener(v -> fazerCadastro());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void fazerCadastro() {
        String nome  = etNome.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            mostrarErro("Preencha todos os campos.");
            return;
        }

        if (senha.length() < 8) {
            mostrarErro("A senha deve ter pelo menos 8 caracteres.");
            return;
        }

        btnCadastrar.setEnabled(false);
        btnCadastrar.setText("Criando conta...");
        tvErro.setVisibility(View.GONE);

        Map<String, String> body = new HashMap<>();
        body.put("name", nome);
        body.put("email", email);
        body.put("password", senha);

        api.register(body).enqueue(new Callback<Student>() {
            @Override
            public void onResponse(Call<Student> call, Response<Student> response) {
                btnCadastrar.setEnabled(true);
                btnCadastrar.setText("Criar conta");

                if (response.isSuccessful() && response.body() != null) {
                    Student student = response.body();
                    session.salvarTokens(student.getToken(), student.getRefreshToken());
                    session.salvarNome(student.getName());
                    startActivity(new Intent(CadastroActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    mostrarErro("Erro ao criar conta.\n" + lerErro(response.errorBody()));
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                btnCadastrar.setEnabled(true);
                btnCadastrar.setText("Criar conta");
                mostrarErro("Erro de conexao. Tente novamente.");
            }
        });
    }

    private void mostrarErro(String msg) {
        tvErro.setText(msg);
        tvErro.setVisibility(View.VISIBLE);
    }

    private String lerErro(ResponseBody errorBody) {
        if (errorBody == null) return "Tente novamente.";

        try {
            return errorBody.string();
        } catch (Exception e) {
            return "Nao foi possivel ler a resposta de erro.";
        }
    }
}
