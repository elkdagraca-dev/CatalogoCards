package com.plotpoint.cards;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.util.YailList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DesignerComponent(
        version = 1,
        description = "Sistema de cartões para catálogos com imagens, título e subtítulo HTML.",
        category = ComponentCategory.EXTENSION,
        nonVisible = false,
        iconName = "images/extension.png"
)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public class CatalogoCards extends AndroidViewComponent {

    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final LruCache<String, Bitmap> imageCache;

    private FrameLayout containerView;
    private View scrollView;
    private LinearLayout listaCards;

    private YailList listaAtual;

    private int colunas = 3;
    private int espacamento = 10;
    private int raioDosPosters = 8;
    private int larguraDoCard = 130;

    private String orientacao = "VERTICAL";
    private String modoTitulo = "RETICENCIAS";
    private String modoSubtitulo = "RETICENCIAS";

    private int maxLinhasTitulo = 1;
    private int maxLinhasSubtitulo = 1;

    public CatalogoCards(ComponentContainer container) {
        super(container);

        context = container.$context();
        mainHandler = new Handler(Looper.getMainLooper());

        executor = Executors.newFixedThreadPool(4);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        imageCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        criarEstrutura();
    }

    // ============================================================
    // ESTRUTURA PRINCIPAL
    // ============================================================

    private void criarEstrutura() {

        containerView = new FrameLayout(context);
        containerView.setBackgroundColor(Color.BLACK);

        criarScroll();

        container.$add(this);

        atualizarListaVisual();
    }

    private void criarScroll() {

        containerView.removeAllViews();

        if ("HORIZONTAL".equals(orientacao)) {

            HorizontalScrollView horizontal =
                    new HorizontalScrollView(context);

            horizontal.setHorizontalScrollBarEnabled(false);
            horizontal.setVerticalScrollBarEnabled(false);
            horizontal.setFillViewport(false);

            listaCards = new LinearLayout(context);
            listaCards.setOrientation(LinearLayout.HORIZONTAL);
            listaCards.setGravity(Gravity.TOP);
            listaCards.setPadding(
                    dp(espacamento),
                    dp(espacamento),
                    dp(espacamento),
                    dp(espacamento)
            );

            horizontal.addView(
                    listaCards,
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );

            containerView.addView(
                    horizontal,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );

            scrollView = horizontal;

        } else {

            ScrollView vertical = new ScrollView(context);

            vertical.setVerticalScrollBarEnabled(false);
            vertical.setHorizontalScrollBarEnabled(false);
            vertical.setFillViewport(true);

            listaCards = new LinearLayout(context);
            listaCards.setOrientation(LinearLayout.VERTICAL);

            listaCards.setPadding(
                    dp(espacamento),
                    dp(espacamento),
                    dp(espacamento),
                    dp(espacamento)
            );

            vertical.addView(
                    listaCards,
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            containerView.addView(
                    vertical,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );

            scrollView = vertical;
        }
    }

    @Override
    public View getView() {
        return containerView;
    }

    // ============================================================
    // LISTA
    // ============================================================

    @SimpleFunction(description = "Define a lista de cartões.")
    public void DefinirLista(YailList lista) {

        listaAtual = lista;

        atualizarListaVisual();
    }

    @SimpleFunction(description = "Remove todos os cartões.")
    public void Limpar() {

        listaAtual = null;

        if (listaCards != null) {
            listaCards.removeAllViews();
        }
    }

    @SimpleFunction(description = "Atualiza os cartões usando a lista atual.")
    public void Atualizar() {

        atualizarListaVisual();
    }

    private void atualizarListaVisual() {

        if (listaCards == null) {
            return;
        }

        listaCards.removeAllViews();

        if (listaAtual == null) {
            return;
        }

        int quantidade = listaAtual.size();

        if (quantidade == 0) {
            return;
        }

        if ("HORIZONTAL".equals(orientacao)) {

            for (int i = 1; i <= quantidade; i++) {

                Object objeto = listaAtual.getObject(i);

                if (!(objeto instanceof YailList)) {
                    continue;
                }

                YailList item = (YailList) objeto;

                View card = criarCard(item, i);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                dp(larguraDoCard),
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );

                params.setMargins(
                        dp(espacamento / 2),
                        0,
                        dp(espacamento / 2),
                        0
                );

                listaCards.addView(card, params);
            }

        } else {

            LinearLayout linhaAtual = null;

            for (int i = 1; i <= quantidade; i++) {

                if ((i - 1) % colunas == 0) {

                    linhaAtual = new LinearLayout(context);
                    linhaAtual.setOrientation(
                            LinearLayout.HORIZONTAL
                    );

                    linhaAtual.setGravity(Gravity.TOP);

                    listaCards.addView(
                            linhaAtual,
                            new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                    );
                }

                Object objeto = listaAtual.getObject(i);

                if (!(objeto instanceof YailList)) {
                    continue;
                }

                YailList item = (YailList) objeto;

                View card = criarCard(item, i);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1f
                        );

                params.setMargins(
                        dp(espacamento / 2),
                        dp(espacamento / 2),
                        dp(espacamento / 2),
                        dp(espacamento / 2)
                );

                linhaAtual.addView(card, params);
            }
        }
    }

    // ============================================================
    // CARD
    // ============================================================

    private View criarCard(YailList item, final int posicao) {

        String imagem = obterTexto(item, 1);
        String titulo = obterTexto(item, 2);
        String subtitulo = obterTexto(item, 3);

        LinearLayout card = new LinearLayout(context);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.TOP);
        card.setClickable(true);
        card.setFocusable(true);

        ImageView poster = new ImageView(context);

        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(
                criarFundoArredondado(
                        Color.rgb(24, 24, 24),
                        raioDosPosters
                )
        );

        poster.setClipToOutline(true);

        LinearLayout.LayoutParams posterParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                );

        card.addView(poster, posterParams);

        TextView textoTitulo = criarTexto();

        aplicarHTML(textoTitulo, titulo);

        configurarTexto(
                textoTitulo,
                modoTitulo,
                maxLinhasTitulo
        );

        LinearLayout.LayoutParams tituloParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        tituloParams.topMargin = dp(6);

        card.addView(textoTitulo, tituloParams);

        TextView textoSubtitulo = criarTexto();

        aplicarHTML(textoSubtitulo, subtitulo);

        configurarTexto(
                textoSubtitulo,
                modoSubtitulo,
                maxLinhasSubtitulo
        );

        LinearLayout.LayoutParams subtituloParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        subtituloParams.topMargin = dp(3);

        card.addView(textoSubtitulo, subtituloParams);

        final String imagemFinal = imagem;
        final String tituloFinal = titulo;
        final String subtituloFinal = subtitulo;

        card.setOnClickListener(v ->
                CartaoClicado(
                        posicao,
                        imagemFinal,
                        tituloFinal,
                        subtituloFinal
                )
        );

        card.setOnLongClickListener(v -> {

            CartaoLongoClicado(
                    posicao,
                    imagemFinal,
                    tituloFinal,
                    subtituloFinal
            );

            return true;
        });

        carregarImagem(
                poster,
                imagem,
                posicao
        );

        return card;
    }

    // ============================================================
    // TEXTO HTML
    // ============================================================

    private TextView criarTexto() {

        TextView texto = new TextView(context);

        texto.setTextColor(Color.WHITE);
        texto.setTextSize(13);
        texto.setGravity(Gravity.LEFT);
        texto.setIncludeFontPadding(true);

        return texto;
    }

    private void aplicarHTML(
            TextView texto,
            String conteudo
    ) {

        if (conteudo == null) {
            conteudo = "";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            texto.setText(
                    Html.fromHtml(
                            conteudo,
                            Html.FROM_HTML_MODE_LEGACY
                    )
            );

        } else {

            texto.setText(
                    Html.fromHtml(conteudo)
            );
        }
    }

    private void configurarTexto(
            TextView texto,
            String modo,
            int maxLinhas
    ) {

        if ("QUEBRAR_LINHA".equals(modo)) {

            texto.setSingleLine(false);
            texto.setMaxLines(
                    Math.max(1, maxLinhas)
            );

            texto.setEllipsize(null);

        } else {

            texto.setSingleLine(true);
            texto.setMaxLines(1);
            texto.setEllipsize(
                    TextUtils.TruncateAt.END
            );
        }
    }

    // ============================================================
    // IMAGENS
    // ============================================================

    private void carregarImagem(
            final ImageView imageView,
            final String urlOriginal,
            final int posicao
    ) {

        if (urlOriginal == null ||
                urlOriginal.trim().isEmpty()) {

            ImagemFalhou(
                    posicao,
                    ""
            );

            return;
        }

        final String url = urlOriginal.trim();

        Bitmap cached = imageCache.get(url);

        if (cached != null) {

            imageView.setImageBitmap(cached);

            ImagemCarregada(
                    posicao,
                    url
            );

            return;
        }

        executor.execute(() -> {

            Bitmap bitmap = baixarImagem(url);

            if (bitmap == null) {

                mainHandler.post(() ->
                        ImagemFalhou(
                                posicao,
                                url
                        )
                );

                return;
            }

            imageCache.put(url, bitmap);

            mainHandler.post(() -> {

                imageView.setImageBitmap(bitmap);

                ImagemCarregada(
                        posicao,
                        url
                );
            });
        });
    }

    private Bitmap baixarImagem(String urlString) {

        HttpURLConnection conexao = null;
        InputStream entrada = null;

        try {

            URL url = new URL(urlString);

            conexao = (HttpURLConnection) url.openConnection();

            conexao.setConnectTimeout(10000);
            conexao.setReadTimeout(15000);
            conexao.setInstanceFollowRedirects(true);

            conexao.connect();

            if (conexao.getResponseCode() !=
                    HttpURLConnection.HTTP_OK) {

                return null;
            }

            entrada = conexao.getInputStream();

            return BitmapFactory.decodeStream(entrada);

        } catch (Exception e) {

            return null;

        } finally {

            try {

                if (entrada != null) {
                    entrada.close();
                }

            } catch (Exception ignored) {
            }

            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }

    // ============================================================
    // PROPRIEDADES
    // ============================================================

    @SimpleProperty(
            category = PropertyCategory.BEHAVIOR,
            description = "Quantidade de colunas no modo vertical."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
            defaultValue = "3"
    )
    public void Colunas(int valor) {

        if (valor < 1) {
            valor = 1;
        }

        colunas = valor;

        atualizarListaVisual();
    }

    @SimpleProperty(
            category = PropertyCategory.BEHAVIOR,
            description = "Espaçamento entre os cartões em pixels."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
            defaultValue = "10"
    )
    public void Espacamento(int valor) {

        if (valor < 0) {
            valor = 0;
        }

        espacamento = valor;

        atualizarListaVisual();
    }

    @SimpleProperty(
            category = PropertyCategory.APPEARANCE,
            description = "Raio dos cantos dos posters."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
            defaultValue = "8"
    )
    public void RaioDosPosters(int valor) {

        if (valor < 0) {
            valor = 0;
        }

        raioDosPosters = valor;

        atualizarListaVisual();
    }

    @SimpleProperty(
            category = PropertyCategory.APPEARANCE,
            description = "Largura de cada cartão no modo horizontal."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
            defaultValue = "130"
    )
    public void LarguraDoCard(int valor) {

        if (valor < 40) {
            valor = 40;
        }

        larguraDoCard = valor;

        atualizarListaVisual();
    }

    @SimpleProperty(
            category = PropertyCategory.BEHAVIOR,
            description = "Define a orientação dos cartões: VERTICAL ou HORIZONTAL."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
            defaultValue = "VERTICAL"
    )
    public void Orientacao(String valor) {

        if (valor == null) {
            valor = "VERTICAL";
        }

        valor = valor.toUpperCase();

        if (!valor.equals("HORIZONTAL")) {
            valor = "VERTICAL";
        }

        orientacao = valor;

        criarScroll();
        atualizarListaVisual();
    }

    @SimpleProperty(
            category = PropertyCategory.APPEARANCE,
            description = "Define como o título será exibido."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
            defaultValue = "RETICENCIAS"
    )
    public void ModoDoTitulo(String valor) {

                if (valor == null) {
            valor = "RETICENCIAS";
        }

        valor = valor.toUpperCase();

        if (!valor.equals("QUEBRAR_LINHA")) {
            valor = "RETICENCIAS";
        }

        modoTitulo = valor;

        atualizarListaVisual();
    }

    @SimpleProperty(
            category = PropertyCategory.APPEARANCE,
            description = "Define como o subtítulo será exibido."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
            defaultValue = "RETICENCIAS"
    )
    public void ModoDoSubtitulo(String valor) {

        if (valor == null) {
            valor = "RETICENCIAS";
        }

        valor = valor.toUpperCase();

        if (!valor.equals("QUEBRAR_LINHA")) {
            valor = "RETICENCIAS";
        }

        modoSubtitulo = valor;

        atualizarListaVisual();
    }

    @SimpleProperty(
            category = PropertyCategory.APPEARANCE,
            description = "Número máximo de linhas do título."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
            defaultValue = "1"
    )
    public void MaxLinhasTitulo(int valor) {

        if (valor < 1) {
            valor = 1;
        }

        maxLinhasTitulo = valor;

        atualizarListaVisual();
    }

    @SimpleProperty(
            category = PropertyCategory.APPEARANCE,
            description = "Número máximo de linhas do subtítulo."
    )
    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
            defaultValue = "1"
    )
    public void MaxLinhasSubtitulo(int valor) {

        if (valor < 1) {
            valor = 1;
        }

        maxLinhasSubtitulo = valor;

        atualizarListaVisual();
    }

    // ============================================================
    // EVENTOS
    // ============================================================

    @SimpleEvent(
            description = "Disparado quando um cartão recebe um clique curto."
    )
    public void CartaoClicado(
            int posicao,
            String imagem,
            String titulo,
            String subtitulo
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "CartaoClicado",
                posicao,
                imagem,
                titulo,
                subtitulo
        );
    }

    @SimpleEvent(
            description = "Disparado quando um cartão recebe um clique longo."
    )
    public void CartaoLongoClicado(
            int posicao,
            String imagem,
            String titulo,
            String subtitulo
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "CartaoLongoClicado",
                posicao,
                imagem,
                titulo,
                subtitulo
        );
    }

    @SimpleEvent(
            description = "Disparado quando uma imagem é carregada."
    )
    public void ImagemCarregada(
            int posicao,
            String url
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "ImagemCarregada",
                posicao,
                url
        );
    }

    @SimpleEvent(
            description = "Disparado quando uma imagem falha ao carregar."
    )
    public void ImagemFalhou(
            int posicao,
            String url
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "ImagemFalhou",
                posicao,
                url
        );
    }

    // ============================================================
    // UTILITÁRIOS
    // ============================================================

    private String obterTexto(
            YailList item,
            int posicao
    ) {

        if (item == null ||
                item.size() < posicao) {

            return "";
        }

        Object valor = item.getObject(posicao);

        if (valor == null) {
            return "";
        }

        return String.valueOf(valor);
    }

    private GradientDrawable criarFundoArredondado(
            int cor,
            int raio
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(cor);
        drawable.setCornerRadius(
                dp(raio)
        );

        return drawable;
    }

    private int dp(int valor) {

        float densidade =
                context.getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                valor * densidade
        );
    }
        }
