package br.tec.feu.buywell;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

/**
 * Navegador interno para a página da NFC-e. Quando os itens da nota aparecem na tela,
 * lê a página (inclusive frames do mesmo site) e devolve ao BuyWell automaticamente.
 */
public class NotaSefazActivity extends Activity {

    static volatile String resultadoHtml;   // HTML pode ser grande demais para um Intent
    static volatile String resultadoUrl;

    private static final int AZUL = Color.parseColor("#1565C0");
    static final String LEITOR_JS =
        "(function(){" +
        " function docs(d,a){a.push(d);var f=d.querySelectorAll('iframe,frame');" +
        "  for(var i=0;i<f.length;i++){try{var c=f[i].contentDocument;if(c)docs(c,a);}catch(e){}}return a;}" +
        " var ds=docs(document,[]),html='',txt='';" +
        " for(var i=0;i<ds.length;i++){html+=ds[i].documentElement.outerHTML+'\\n';" +
        "  txt+=(ds[i].body?ds[i].body.innerText:'')+'\\n';}" +
        " var n=(txt.match(/Vl\\.?\\s*Unit/gi)||[]).length;" +
        " return JSON.stringify({n:n,html:n>0?html:''});" +
        "})()";
    private static final String TUDO_JS =
        "(function(){var h=document.documentElement.outerHTML;" +
        " var f=document.querySelectorAll('iframe,frame');" +
        " for(var i=0;i<f.length;i++){try{h+='\\n'+f[i].contentDocument.documentElement.outerHTML;}catch(e){}}" +
        " return h;})()";

    private WebView web;
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean terminado = false;
    private int ultimoN = -1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("url");

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);

        LinearLayout barra = new LinearLayout(this);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(Gravity.CENTER_VERTICAL);
        barra.setBackgroundColor(AZUL);
        int p = dp(8);
        barra.setPadding(p, p, p, p);

        Button fechar = botao("✕");
        fechar.setOnClickListener(v -> finish());
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setPadding(p, 0, p, 0);
        status.setText("Abrindo a nota na SEFAZ…");
        Button importar = botao("Importar");
        importar.setOnClickListener(v -> importarManual());

        barra.addView(fechar);
        barra.addView(status, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        barra.addView(importar);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;  // tudo dentro desta janela (inclusive a verificação do site)
            }

            @Override
            public void onPageFinished(WebView view, String u) {
                verificarDepois(700);
            }
        });

        raiz.addView(barra, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        raiz.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(raiz);
        web.loadUrl(url);
        verificarDepois(2500);
    }

    private Button botao(String txt) {
        Button b = new Button(this);
        b.setText(txt);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void verificarDepois(long ms) {
        handler.postDelayed(this::verificar, ms);
    }

    /** Procura os itens na página; quando a contagem fica estável, entrega a nota. */
    private void verificar() {
        if (terminado || web == null) return;
        web.evaluateJavascript(LEITOR_JS, valor -> {
            if (terminado) return;
            try {
                JSONObject o = new JSONObject(new JSONObject("{\"v\":" + valor + "}").getString("v"));
                int n = o.optInt("n", 0);
                if (n > 0 && n == ultimoN) {
                    entregar(o.optString("html"), n + " itens encontrados — importando…");
                    return;
                }
                if (n > 0) status.setText(n + " itens encontrados…");
                else status.setText("Se o site pedir verificação, resolva aqui. Eu leio a nota sozinho.");
                ultimoN = n;
            } catch (Exception ignored) {
                // página ainda carregando
            }
            verificarDepois(1200);
        });
    }

    private void importarManual() {
        if (terminado || web == null) return;
        web.evaluateJavascript(TUDO_JS, valor -> {
            try {
                entregar(new JSONObject("{\"v\":" + valor + "}").getString("v"), "Enviando a página ao BuyWell…");
            } catch (Exception e) {
                status.setText("Não consegui ler esta página.");
            }
        });
    }

    private void entregar(String html, String msg) {
        if (terminado) return;
        terminado = true;
        status.setText(msg);
        resultadoHtml = html;
        resultadoUrl = web.getUrl();
        setResult(RESULT_OK);
        handler.postDelayed(this::finish, 500);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
