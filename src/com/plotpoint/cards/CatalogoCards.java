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
import android.view.MotionEvent;
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
import com.google.appinventor.components.runtime.EventDispatcher;

import com.google.appinventor.components.runtime.util.YailList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DesignerComponent(
        version = 1,
        description = "Componente nativo para criação de cartões com imagem, título e subtítulo HTML.",
        category = ComponentCategory.EXTENSION,
        nonVisible = false,
        iconName = "images/extension.png"
)
@SimpleObject(external = true)
@UsesPermissions(
        permissionNames = "android.permission.INTERNET"
)
public class CatalogoCards extends AndroidViewComponent {

    // ============================================================
    // CONTEXTO E THREADS
    // ============================================================

    private final Context context;

    private final Handler mainHandler;

    private final ExecutorService executor;

    // ============================================================
    // CACHE DE IMAGENS
    // ============================================================

    private final LruCache<String, Bitmap> imageCache;

    // ============================================================
    // VIEWS PRINCIPAIS
    // ============================================================

    private FrameLayout containerView;

    private LinearLayout listaCards;

    private View scrollView;

    // ============================================================
    // LISTA RECEBIDA DO KODULAR
    // ============================================================

    private YailList listaAtual;

    // ============================================================
    // CONFIGURAÇÕES DOS CARDS
    // ============================================================

    private int colunas = 3;

    private int espacamento = 10;

    private int larguraDoCard = 130;

    private int raioDosPosters = 8;

    // ============================================================
    // ORIENTAÇÃO
    // ============================================================

    private String orientacao = "VERTICAL";

    // ============================================================
    // TÍTULO
    // ============================================================

    private String modoTitulo = "RETICENCIAS";

    private int maxLinhasTitulo = 1;

    // ============================================================
    // SUBTÍTULO
    // ============================================================

    private String modoSubtitulo = "RETICENCIAS";

    private int maxLinhasSubtitulo = 1;

    // ============================================================
    // CONSTRUTOR
    // ============================================================

    public CatalogoCards(ComponentContainer container) {

        super(container);

        context = container.$context();

        mainHandler =
                new Handler(
                        Looper.getMainLooper()
                );

        executor =
                Executors.newFixedThreadPool(4);

        // --------------------------------------------------------
        // Define o tamanho máximo aproximado do cache.
        // --------------------------------------------------------

        final int maxMemory =
                (int)
                (
                    Runtime.getRuntime()
                            .maxMemory()
                    / 1024
                );

        final int cacheSize =
                maxMemory / 8;

        imageCache =
                new LruCache<String, Bitmap>(
                        cacheSize
                ) {

                    @Override
                    protected int sizeOf(
                            String key,
                            Bitmap bitmap
                    ) {

                        return bitmap.getByteCount()
                                / 1024;
                    }
                };

        // --------------------------------------------------------
        // Cria a interface nativa.
        // --------------------------------------------------------

        criarEstrutura();

        // --------------------------------------------------------
        // Registra o componente no container do Kodular.
        // --------------------------------------------------------

        container.$add(this);
    }

    // ============================================================
    // ESTRUTURA PRINCIPAL
    // ============================================================

    private void criarEstrutura() {

        containerView =
                new FrameLayout(context);

        containerView.setBackgroundColor(
                Color.BLACK
        );

        criarScroll();
    }

    // ============================================================
    // CRIA O SCROLL VERTICAL OU HORIZONTAL
    // ============================================================

    private void criarScroll() {

        containerView.removeAllViews();

        // --------------------------------------------------------
        // MODO HORIZONTAL
        // --------------------------------------------------------

        if ("HORIZONTAL".equals(orientacao)) {

            HorizontalScrollView horizontal =
                    new HorizontalScrollView(
                            context
                    );

            horizontal.setHorizontalScrollBarEnabled(
                    false
            );

            horizontal.setVerticalScrollBarEnabled(
                    false
            );

            horizontal.setFillViewport(
                    false
            );

            listaCards =
                    new LinearLayout(
                            context
                    );

            listaCards.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            listaCards.setGravity(
                    Gravity.TOP
            );

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

        }

        // --------------------------------------------------------
        // MODO VERTICAL
        // --------------------------------------------------------

        else {

            ScrollView vertical =
                    new ScrollView(
                            context
                    );

            vertical.setVerticalScrollBarEnabled(
                    false
            );

            vertical.setHorizontalScrollBarEnabled(
                    false
            );

            vertical.setFillViewport(
                    true
            );

            listaCards =
                    new LinearLayout(
                            context
                    );

            listaCards.setOrientation(
                    LinearLayout.VERTICAL
            );

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

    // ============================================================
    // VIEW DO COMPONENTE
    // ============================================================

    @Override
    public View getView() {

        return containerView;
  }
    // ============================================================
    // DEFINIR LISTA
    // ============================================================

    @SimpleFunction(
            description = "Define a lista de cartões."
    )
    public void DefinirLista(YailList lista) {

        listaAtual = lista;

        atualizarListaVisual();
    }

    // ============================================================
    // LIMPAR
    // ============================================================

    @SimpleFunction(
            description = "Remove todos os cartões."
    )
    public void Limpar() {

        listaAtual = null;

        if (listaCards != null) {
            listaCards.removeAllViews();
        }
    }

    // ============================================================
    // ATUALIZAR
    // ============================================================

    @SimpleFunction(
            description = "Atualiza os cartões usando a lista atual."
    )
    public void Atualizar() {

        atualizarListaVisual();
    }

    // ============================================================
    // ATUALIZA A INTERFACE
    // ============================================================

    private void atualizarListaVisual() {

        if (listaCards == null) {
            return;
        }

        listaCards.removeAllViews();

        if (listaAtual == null) {
            return;
        }

        int quantidade =
                listaAtual.size();

        if (quantidade <= 0) {
            return;
        }

        // ========================================================
        // MODO HORIZONTAL
        // ========================================================

        if ("HORIZONTAL".equals(orientacao)) {

            for (int i = 1; i <= quantidade; i++) {

                Object objeto =
                        listaAtual.getObject(i);

                if (!(objeto instanceof YailList)) {
                    continue;
                }

                YailList item =
                        (YailList) objeto;

                View card =
                        criarCard(
                                item,
                                i
                        );

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

                listaCards.addView(
                        card,
                        params
                );
            }

            return;
        }

        // ========================================================
        // MODO VERTICAL
        // ========================================================

        LinearLayout linhaAtual = null;

        for (int i = 1; i <= quantidade; i++) {

            // ----------------------------------------------------
            // Cria uma nova linha a cada quantidade de colunas.
            // ----------------------------------------------------

            if ((i - 1) % colunas == 0) {

                linhaAtual =
                        new LinearLayout(
                                context
                        );

                linhaAtual.setOrientation(
                        LinearLayout.HORIZONTAL
                );

                linhaAtual.setGravity(
                        Gravity.TOP
                );

                listaCards.addView(
                        linhaAtual,
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                );
            }

            Object objeto =
                    listaAtual.getObject(i);

            if (!(objeto instanceof YailList)) {
                continue;
            }

            YailList item =
                    (YailList) objeto;

            View card =
                    criarCard(
                            item,
                            i
                    );

            // ----------------------------------------------------
            // Cada cartão ocupa uma fração igual da linha.
            // ----------------------------------------------------

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

            linhaAtual.addView(
                    card,
                    params
            );
        }
    }

    // ============================================================
    // CRIA UM CARD
    // ============================================================

    private View criarCard(
            YailList item,
            final int posicao
    ) {

        String imagem =
                obterTexto(
                        item,
                        1
                );

        String titulo =
                obterTexto(
                        item,
                        2
                );

        String subtitulo =
                obterTexto(
                        item,
                        3
                );

        // --------------------------------------------------------
        // Container principal do cartão.
        // --------------------------------------------------------

        LinearLayout card =
                new LinearLayout(
                        context
                );

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setGravity(
                Gravity.TOP
        );

        card.setClickable(
                true
        );

        card.setFocusable(
                true
        );

        // --------------------------------------------------------
        // Poster
        // --------------------------------------------------------

        AspectRatioImageView poster =
                new AspectRatioImageView(
                        context
                );

        poster.setRatio(
                2f / 3f
        );

        poster.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        poster.setBackground(
                criarFundoArredondado(
                        Color.rgb(
                                24,
                                24,
                                24
                        ),
                        raioDosPosters
                )
        );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            poster.setClipToOutline(
                    true
            );
        }

        LinearLayout.LayoutParams posterParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        card.addView(
                poster,
                posterParams
        );

        // --------------------------------------------------------
        // Título
        // --------------------------------------------------------

        TextView textoTitulo =
                criarTexto();

        aplicarHTML(
                textoTitulo,
                titulo
        );

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

        tituloParams.topMargin =
                dp(6);

        card.addView(
                textoTitulo,
                tituloParams
        );

        // --------------------------------------------------------
        // Subtítulo
        // --------------------------------------------------------

        TextView textoSubtitulo =
                criarTexto();

        textoSubtitulo.setTextSize(
                10
        );

        aplicarHTML(
                textoSubtitulo,
                subtitulo
        );

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

        subtituloParams.topMargin =
                dp(3);

        card.addView(
                textoSubtitulo,
                subtituloParams
        );

        // --------------------------------------------------------
        // Guarda os dados originais para os eventos.
        // --------------------------------------------------------

        final String imagemFinal =
                imagem;

        final String tituloFinal =
                titulo;

        final String subtituloFinal =
                subtitulo;

        // --------------------------------------------------------
        // Clique curto
        // --------------------------------------------------------

        card.setOnClickListener(
                v -> CartaoClicado(
                        posicao,
                        imagemFinal,
                        tituloFinal,
                        subtituloFinal
                )
        );

        // --------------------------------------------------------
        // Clique longo
        // --------------------------------------------------------

        card.setOnLongClickListener(
                v -> {

                    CartaoLongoClicado(
                            posicao,
                            imagemFinal,
                            tituloFinal,
                            subtituloFinal
                    );

                    return true;
                }
        );

        // --------------------------------------------------------
        // Carrega a imagem em segundo plano.
        // --------------------------------------------------------

        carregarImagem(
                poster,
                imagem,
                posicao
        );

        return card;
                        }
    // ============================================================
    // CRIA TEXTVIEW
    // ============================================================

    private TextView criarTexto() {

        TextView texto =
                new TextView(
                        context
                );

        texto.setTextColor(
                Color.WHITE
        );

        texto.setTextSize(
                13
        );

        texto.setGravity(
                Gravity.START
        );

        texto.setIncludeFontPadding(
                true
        );

        texto.setPadding(
                0,
                0,
                0,
                0
        );

        return texto;
    }

    // ============================================================
    // APLICA HTML AO TEXTO
    // ============================================================

    private void aplicarHTML(
            TextView texto,
            String conteudo
    ) {

        if (conteudo == null) {
            conteudo = "";
        }

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.N) {

            texto.setText(
                    Html.fromHtml(
                            conteudo,
                            Html.FROM_HTML_MODE_LEGACY
                    )
            );

        } else {

            texto.setText(
                    Html.fromHtml(
                            conteudo
                    )
            );
        }
    }

    // ============================================================
    // CONFIGURAÇÃO DO TÍTULO/SUBTÍTULO
    // ============================================================

    private void configurarTexto(
            TextView texto,
            String modo,
            int maxLinhas
    ) {

        if (maxLinhas < 1) {
            maxLinhas = 1;
        }

        // ========================================================
        // RETICÊNCIAS
        // ========================================================

        if ("RETICENCIAS".equals(
                modo
        )) {

            texto.setSingleLine(
                    maxLinhas == 1
            );

            texto.setMaxLines(
                    maxLinhas
            );

            texto.setEllipsize(
                    TextUtils.TruncateAt.END
            );

        }

        // ========================================================
        // QUEBRAR LINHA
        // ========================================================

        else {

            texto.setSingleLine(
                    false
            );

            texto.setMaxLines(
                    maxLinhas
            );

            texto.setEllipsize(
                    null
            );
        }
    }

    // ============================================================
    // IMAGEVIEW COM PROPORÇÃO FIXA
    // ============================================================

    private static class AspectRatioImageView
            extends ImageView {

        private float ratio =
                2f / 3f;

        public AspectRatioImageView(
                Context context
        ) {

            super(context);
        }

        // --------------------------------------------------------
        // Define a proporção.
        // --------------------------------------------------------

        public void setRatio(
                float ratio
        ) {

            if (ratio > 0) {
                this.ratio = ratio;
            }

            requestLayout();
        }

        // --------------------------------------------------------
        // Mantém a proporção da imagem.
        // --------------------------------------------------------

        @Override
        protected void onMeasure(
                int widthMeasureSpec,
                int heightMeasureSpec
        ) {

            super.onMeasure(
                    widthMeasureSpec,
                    heightMeasureSpec
            );

            int largura =
                    getMeasuredWidth();

            if (largura > 0 && ratio > 0) {

                int altura =
                        Math.round(
                                largura / ratio
                        );

                setMeasuredDimension(
                        largura,
                        altura
                );
            }
        }
                    }
        
    // ============================================================
    // CARREGA IMAGEM
    // ============================================================

    private void carregarImagem(
            final ImageView imageView,
            final String url,
            final int posicao
    ) {

        // --------------------------------------------------------
        // URL vazia
        // --------------------------------------------------------

        if (url == null ||
                url.trim().isEmpty()) {

            ImagemFalhou(
                    posicao,
                    url == null ? "" : url
            );

            return;
        }

        final String urlFinal =
                url.trim();

        // --------------------------------------------------------
        // Verifica primeiro o cache.
        // --------------------------------------------------------

        Bitmap imagemCache =
                imageCache.get(
                        urlFinal
                );

        if (imagemCache != null) {

            imageView.setImageBitmap(
                    imagemCache
            );

            ImagemCarregada(
                    posicao,
                    urlFinal
            );

            return;
        }

        // --------------------------------------------------------
        // Mantém o fundo enquanto a imagem carrega.
        // --------------------------------------------------------

        imageView.setImageDrawable(
                null
        );

        // --------------------------------------------------------
        // Download em segundo plano.
        // --------------------------------------------------------

        executor.execute(
                new Runnable() {

                    @Override
                    public void run() {

                        Bitmap bitmap =
                                baixarImagem(
                                        urlFinal
                                );

                        if (bitmap != null) {

                            imageCache.put(
                                    urlFinal,
                                    bitmap
                            );

                            mainHandler.post(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            imageView.setImageBitmap(
                                                    bitmap
                                            );

                                            ImagemCarregada(
                                                    posicao,
                                                    urlFinal
                                            );
                                        }
                                    }
                            );

                        } else {

                            mainHandler.post(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            ImagemFalhou(
                                                    posicao,
                                                    urlFinal
                                            );
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    // ============================================================
    // BAIXA A IMAGEM
    // ============================================================

    private Bitmap baixarImagem(
            String urlString
    ) {

        HttpURLConnection conexao =
                null;

        InputStream input =
                null;

        try {

            URL url =
                    new URL(
                            urlString
                    );

            conexao =
                    (HttpURLConnection)
                            url.openConnection();

            conexao.setConnectTimeout(
                    10000
            );

            conexao.setReadTimeout(
                    15000
            );

            conexao.setInstanceFollowRedirects(
                    true
            );

            conexao.setRequestMethod(
                    "GET"
            );

            conexao.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0"
            );

            conexao.connect();

            int codigo =
                    conexao.getResponseCode();

            if (codigo < 200 ||
                    codigo >= 300) {

                return null;
            }

            input =
                    conexao.getInputStream();

            return BitmapFactory.decodeStream(
                    input
            );

        } catch (Exception e) {

            return null;

        } finally {

            if (input != null) {

                try {
                    input.close();
                } catch (Exception ignored) {
                }
            }

            if (conexao != null) {
                conexao.disconnect();
            }
        }
                                                    }
