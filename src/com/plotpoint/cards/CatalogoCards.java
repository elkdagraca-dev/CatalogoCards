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
