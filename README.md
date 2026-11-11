# BuyWell — app Android

App que abre o **BuyWell Web** (`https://buywell.feu.tec.br`, servidor no DESKTOP-3LB7RCN) e acrescenta o que uma página web não pode fazer: **ler a nota fiscal no site da SEFAZ**.

## Como funciona

- O app é uma "casca" ([Capacitor](https://capacitorjs.com)) que carrega o BuyWell Web. As atualizações do BuyWell chegam ao app sem gerar outro APK.
- Ao ler o QR Code do cupom, o BuyWell chama o plugin nativo `NotaSefaz`:
  1. ele abre a página da nota numa janela interna (`NotaSefazActivity`);
  2. o usuário resolve por lá qualquer verificação do site;
  3. quando os itens aparecem, o app lê a página e a devolve ao BuyWell, que importa a nota (`POST /api/nfce` com `html`).
- O botão **Importar** na barra azul envia a página manualmente, se a leitura automática não disparar.

## Gerar o APK

A cada `push` na branch `main`, o GitHub Actions compila o APK (veja `.github/workflows/apk.yml`). Para baixar: aba **Actions** › última execução › **Artifacts › BuyWell-APK**. Vem um `.zip` com o `.apk` dentro.

O APK é assinado com a chave `debug.keystore`, que é fixa para que uma versão nova instale por cima da anterior. **Não serve para a Play Store**, que exige chave própria de publicação.

## Instalar no Android

1. Envie o `.apk` (por WhatsApp, e-mail ou Drive) e abra-o no celular.
2. Autorize "instalar apps desconhecidos" para o app usado para abri-lo.
3. Na primeira abertura, faça o login do Cloudflare Access: o e-mail precisa estar na policy do BuyWell.
