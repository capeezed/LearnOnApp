package com.learnon.app.ui.dashboard;

import android.content.Intent;
import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.Pedido;
import com.learnon.app.ui.agenda.AgendaActivity;
import com.learnon.app.ui.auth.LoginActivity;
import com.learnon.app.ui.chatbot.BotpressWebView;
import com.learnon.app.ui.cursos.MeusCursosActivity;
import com.learnon.app.ui.cursos.PaymentTestActivity;
import com.learnon.app.ui.pedidos.PedirCursoActivity;
import com.learnon.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvBoasVindas, tvSair, tvSemPedidos;
    private LinearLayout cardMeusCursos, cardAgenda, cardPedirCurso, cardComprarCurso, listaPedidos;
    private BotpressWebView webViewBotpressDashboard;
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
        cardComprarCurso = findViewById(R.id.cardComprarCurso);
        webViewBotpressDashboard = findViewById(R.id.webViewBotpressDashboard);
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

        cardComprarCurso.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentTestActivity.class))
        );

        configurarBotpressNoDashboard();

        carregarPedidos();
    }

    @Override
    public void onBackPressed() {
        if (webViewBotpressDashboard != null && webViewBotpressDashboard.isChatActive()) {
            webViewBotpressDashboard.evaluateJavascript(
                    "window.botpress && window.botpress.close && window.botpress.close();",
                    value -> webViewBotpressDashboard.setChatActive(false)
            );
            return;
        }
        super.onBackPressed();
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
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(ContextCompat.getDrawable(this, R.drawable.course_card_bg));
        row.setElevation(dp(2));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(params);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView marker = new TextView(this);
        marker.setText(">");
        marker.setGravity(Gravity.CENTER);
        marker.setTextSize(18);
        marker.setTextColor(0xFFECEEF9);
        marker.setTypeface(null, Typeface.BOLD);
        marker.setBackground(ContextCompat.getDrawable(this, R.drawable.course_chip_soft_bg));
        LinearLayout.LayoutParams markerParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        markerParams.setMargins(0, 0, dp(14), 0);
        marker.setLayoutParams(markerParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView titulo = new TextView(this);
        titulo.setText(textoOuPadrao(pedido.getTitle(), "Pedido sem titulo"));
        titulo.setTextSize(15);
        titulo.setTextColor(0xFFECEEF9);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setMaxLines(2);

        TextView tag = new TextView(this);
        tag.setText(textoOuPadrao(pedido.getTopicTag(), "Geral"));
        tag.setTextSize(12);
        tag.setTextColor(0xFFB4B4C3);
        tag.setPadding(0, dp(4), 0, 0);

        TextView status = new TextView(this);
        status.setText(traduzirStatus(pedido.getStatus()));
        status.setTextSize(12);
        status.setTextColor(statusCor(pedido.getStatus()));
        status.setTypeface(null, Typeface.BOLD);
        status.setPadding(0, dp(8), 0, 0);

        content.addView(titulo);
        content.addView(tag);
        content.addView(status);
        row.addView(marker);
        row.addView(content);

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
        if (status == null) return 0xFFB4B4C3;
        switch (status) {
            case "pending":
            case "aguardando_match":
            case "aguardando_instrutor":
                return 0xFF4937A6;
            case "matched":
            case "em_andamento":
            case "in_production":
                return 0xFF4937A6;
            case "delivered":
            case "concluido":
                return 0xFF6B5CFF;
            default:
                return 0xFFB4B4C3;
        }
    }

    private String textoOuPadrao(String texto, String padrao) {
        return texto == null || texto.trim().isEmpty() ? padrao : texto;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurarBotpressNoDashboard() {
        webViewBotpressDashboard.setBackgroundColor(0x00000000);
        webViewBotpressDashboard.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        WebSettings settings = webViewBotpressDashboard.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webViewBotpressDashboard.setWebViewClient(new WebViewClient());
        webViewBotpressDashboard.setWebChromeClient(new WebChromeClient());

        String html = "<!DOCTYPE html>"
                + "<html lang=\"pt-BR\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<style>"
                + "html, body { height: 100%; margin: 0; background: transparent; overflow: hidden; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<script src=\"https://cdn.botpress.cloud/webchat/v3.6/inject.js\" defer></script>"
                + "<script src=\"https://files.bpcontent.cloud/2026/05/10/11/20260510115921-COZ7U0ZS.js\" defer></script>"
                + "</body>"
                + "</html>";

        webViewBotpressDashboard.loadDataWithBaseURL(
                "https://cdn.botpress.cloud/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
