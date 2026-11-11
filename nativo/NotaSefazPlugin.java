package br.tec.feu.buywell;

import android.content.Intent;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Abre a página da nota (link do QR Code) numa janela interna do app e devolve o HTML ao BuyWell.
 * O usuário resolve qualquer verificação do site por lá; nós só lemos a página que ele está vendo.
 * JS: Capacitor.Plugins.NotaSefaz.abrir({ url }) -> { html, url } ou { cancelado: true }
 */
@CapacitorPlugin(name = "NotaSefaz")
public class NotaSefazPlugin extends Plugin {

    @PluginMethod
    public void abrir(PluginCall call) {
        String url = call.getString("url");
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            call.reject("endereço da nota inválido");
            return;
        }
        NotaSefazActivity.resultadoHtml = null;
        NotaSefazActivity.resultadoUrl = null;
        Intent i = new Intent(getContext(), NotaSefazActivity.class);
        i.putExtra("url", url);
        startActivityForResult(call, i, "aoVoltar");
    }

    @ActivityCallback
    private void aoVoltar(PluginCall call, ActivityResult result) {
        if (call == null) return;
        JSObject r = new JSObject();
        String html = NotaSefazActivity.resultadoHtml;
        if (html != null) {
            r.put("html", html);
            r.put("url", NotaSefazActivity.resultadoUrl);
        } else {
            r.put("cancelado", true);
        }
        NotaSefazActivity.resultadoHtml = null;
        call.resolve(r);
    }
}
