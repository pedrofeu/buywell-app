package br.tec.feu.buywell;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

/**
 * Lê a página da nota (link do QR Code) e devolve o HTML ao BuyWell.
 * 1º tenta SEM interação: carrega a página num navegador invisível (fora da tela) e espera os itens.
 * Se em ~15 s os itens não aparecerem (ex.: o site pediu verificação manual), abre a janela visível
 * (NotaSefazActivity) para o usuário resolver; a leitura continua automática.
 * JS: Capacitor.Plugins.NotaSefaz.abrir({ url, oculto? }) -> { html, url, modo } ou { cancelado: true }
 */
@CapacitorPlugin(name = "NotaSefaz")
public class NotaSefazPlugin extends Plugin {

    private static final long LIMITE_OCULTO_MS = 15000;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @PluginMethod
    public void abrir(PluginCall call) {
        String url = call.getString("url");
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            call.reject("endereço da nota inválido");
            return;
        }
        if (Boolean.TRUE.equals(call.getBoolean("oculto", true))) {
            getActivity().runOnUiThread(() -> lerOculto(call, url));
        } else {
            abrirVisivel(call, url);
        }
    }

    private void abrirVisivel(PluginCall call, String url) {
        NotaSefazActivity.resultadoHtml = null;
        NotaSefazActivity.resultadoUrl = null;
        Intent i = new Intent(getContext(), NotaSefazActivity.class);
        i.putExtra("url", url);
        startActivityForResult(call, i, "aoVoltar");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void lerOculto(PluginCall call, String url) {
        final ViewGroup raiz = (ViewGroup) getActivity().getWindow().getDecorView();
        final WebView w = new WebView(getContext());
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        // tamanho de tela normal, mas fora da área visível e sem receber toques
        int largura = raiz.getWidth() > 0 ? raiz.getWidth() : 1080;
        int altura = raiz.getHeight() > 0 ? raiz.getHeight() : 1920;
        w.setLayoutParams(new FrameLayout.LayoutParams(largura, altura));
        w.setTranslationX(-largura * 3f);
        w.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        w.setFocusable(false);

        final boolean[] fim = {false};
        final int[] ultimoN = {-1};
        final Runnable[] verificar = new Runnable[1];

        Runnable encerrar = () -> {
            handler.removeCallbacksAndMessages(null);
            raiz.removeView(w);
            w.destroy();
        };

        verificar[0] = () -> {
            if (fim[0]) return;
            w.evaluateJavascript(NotaSefazActivity.LEITOR_JS, valor -> {
                if (fim[0]) return;
                try {
                    JSONObject o = new JSONObject(new JSONObject("{\"v\":" + valor + "}").getString("v"));
                    int n = o.optInt("n", 0);
                    if (n > 0 && n == ultimoN[0]) {
                        fim[0] = true;
                        JSObject r = new JSObject();
                        r.put("html", o.optString("html"));
                        r.put("url", w.getUrl());
                        r.put("modo", "automatico");
                        encerrar.run();
                        call.resolve(r);
                        return;
                    }
                    ultimoN[0] = n;
                } catch (Exception ignored) {
                    // ainda carregando
                }
                handler.postDelayed(verificar[0], 800);
            });
        };

        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String u) {
                handler.postDelayed(verificar[0], 400);
            }
        });

        // não apareceu em ~15 s: mostra a janela para o usuário (verificação manual do site)
        handler.postDelayed(() -> {
            if (fim[0]) return;
            fim[0] = true;
            encerrar.run();
            abrirVisivel(call, url);
        }, LIMITE_OCULTO_MS);

        raiz.addView(w);
        w.loadUrl(url);
        handler.postDelayed(verificar[0], 2000);
    }

    @ActivityCallback
    private void aoVoltar(PluginCall call, ActivityResult result) {
        if (call == null) return;
        JSObject r = new JSObject();
        String html = NotaSefazActivity.resultadoHtml;
        if (html != null) {
            r.put("html", html);
            r.put("url", NotaSefazActivity.resultadoUrl);
            r.put("modo", "janela");
        } else {
            r.put("cancelado", true);
        }
        NotaSefazActivity.resultadoHtml = null;
        call.resolve(r);
    }
}
