package br.tec.feu.buywell;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(NotaSefazPlugin.class);  // leitor da nota no site da SEFAZ
        super.onCreate(savedInstanceState);
    }
}
