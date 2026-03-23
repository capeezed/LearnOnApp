package com.learnon.app.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.Pedido;
import com.learnon.app.ui.agenda.AgendaActivity;
import com.learnon.app.ui.auth.LoginActivity;
import com.learnon.app.ui.cursos.MeusCursosActivity;
import com.learnon.app.ui.pedidos.PedirCursoActivity;
import com.learnon.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvBoasVindas, tvSair, tvSemPedidos;
    private LinearLayout cardMeusCursos, cardAgenda, cardPedirCurso, listaPedidos;
    private SessionManager session;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);
        api     = ApiClient.createService(ApiService.class);

        tvBoasVindas  = findViewById(R.id.tvBoasVindas);
        tvSair        = findViewById(R.id.tvSair);
        tvSemPedidos  = findViewById(R.id.tvSemPedidos);
        cardMeusCursos = findViewById(R.id.cardMeusCursos);
        cardAgenda     = findViewById(R.id.cardAgenda);
        cardPedirCurso = findViewById(R.id.cardPedirCurso);
        listaPedidos   = findViewById(R.id.listaPedidos);

        tvBoasVindas.setText("Ola, " + session.getNome() + "!");

        tvSair.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        cardMeusCursos.setOnClickListener(v ->
                startActivity(new Intent(this, MeusCursosActivity.class))
        );

        cardAgenda.setOnClickListener(v ->
                startActivity(new Intent(this, AgendaActivity.class))
        );

        cardPedirCurso.setOnClickListener(v ->
                startActivity(new Intent(this, PedirCursoActivity.class))
        );

        carregarPedidos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarPedidos();
    }

    private void carregarPedidos() {
        String token = "Bearer " + session.getToken();

        api.meusPedidos(token).enqueue(new Callback<List<Pedido>>() {
            @Override
            public void onResponse(Call<List<Pedido>> call, Response<List<Pedido>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pedido> pedidos = response.body();

                    listaPedidos.removeAllViews();

                    if (pedidos.isEmpty()) {
                        tvSemPedidos.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvSemPedidos.setVisibility(View.GONE);

                    int limite = Math.min(pedidos.size(), 5);
                    for (int i = 0; i < limite; i++) {
                        listaPedidos.addView(criarItemPedido(pedidos.get(i)));
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Pedido>> call, Throwable t) {
                tvSemPedidos.setVisibility(View.VISIBLE);
                tvSemPedidos.setText("Erro ao carregar pedidos.");
            }
        });
    }

    private View criarItemPedido(Pedido pedido) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(getDrawable(R.drawable.card_bg));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        row.setLayoutParams(params);
        row.setPadding(40, 32, 40, 32);

        TextView titulo = new TextView(this);
        titulo.setText(pedido.getTitle());
        titulo.setTextSize(15);
        titulo.setTextColor(0xFF1c2b3a);
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tag = new TextView(this);
        tag.setText(pedido.getTopicTag());
        tag.setTextSize(12);
        tag.setTextColor(0xFF7a7060);

        TextView status = new TextView(this);
        status.setText(traduzirStatus(pedido.getStatus()));
        status.setTextSize(12);
        status.setTextColor(statusCor(pedido.getStatus()));

        row.addView(titulo);
        row.addView(tag);
        row.addView(status);

        return row;
    }

    private String traduzirStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "pending":       return "Na fila";
            case "matched":       return "Instrutor encontrado";
            case "in_production": return "Em producao";
            case "delivered":     return "Entregue";
            default:              return status;
        }
    }

    private int statusCor(String status) {
        if (status == null) return 0xFF7a7060;
        switch (status) {
            case "pending":       return 0xFFc8963e;
            case "matched":       return 0xFF2a7a6e;
            case "in_production": return 0xFF1c2b3a;
            case "delivered":     return 0xFF2a7a6e;
            default:              return 0xFF7a7060;
        }
    }
}