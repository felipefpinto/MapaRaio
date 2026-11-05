
package com.example.maparaio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import com.example.maparaio.ui.theme.MapaRaioTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.content.ContextCompat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions

import kotlin.math.*

private const val MAP_STYLE_LIGHT =
    "https://tiles.openfreemap.org/styles/liberty"

private const val MAP_STYLE_DARK =
    "https://tiles.openfreemap.org/styles/dark"

private const val CIRCLE_SOURCE = "raio-source"
private const val CIRCLE_FILL = "raio-fill"
private const val CIRCLE_LINE = "raio-line"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        setContent {
            MapaRaioTheme(
                dynamicColor = false
            ) {
                MapaScreen()
            }
        }
    }
}



@Composable



fun MapaScreen() {

    val context = LocalContext.current

    val modoEscuro = isSystemInDarkTheme()

    val teclado = LocalSoftwareKeyboardController.current
    val foco = LocalFocusManager.current

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var localizacao by remember {
        mutableStateOf<LatLng?>(null)
    }

    var mapa by remember {
        mutableStateOf<MapLibreMap?>(null)
    }

    var centroSelecionado by remember {
        mutableStateOf<LatLng?>(null)
    }

    var marcadorCentro by remember {
        mutableStateOf<Marker?>(null)
    }

    var raio by remember {
        mutableFloatStateOf(1000f)
    }

    var distanciaDigitada by remember {
        mutableStateOf("1000")
    }

    var unidadeDistancia by remember {
        mutableStateOf("m")
    }

    var erroDistancia by remember {
        mutableStateOf<String?>(null)
    }

    var painelExpandido by remember {
        mutableStateOf(false)
    }
    var textoPesquisa by remember {
        mutableStateOf("")
    }

    var pesquisando by remember {
        mutableStateOf(false)
    }

    var erroPesquisa by remember {
        mutableStateOf<String?>(null)
    }
    var enderecoEncontrado by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(raio) {
        val distancia = if (unidadeDistancia == "km") {
            raio / 1000f
        } else {
            raio
        }

        distanciaDigitada =
            if (distancia % 1f == 0f) {
                distancia.toInt().toString()
            } else {
                distancia.toString()
            }
    }

    fun executarPesquisa() {
        val endereco = textoPesquisa.trim()

        if (endereco.isEmpty() || pesquisando) return

        teclado?.hide()
        foco.clearFocus()

        pesquisando = true
        erroPesquisa = null
        enderecoEncontrado = null

        pesquisarEndereco(endereco) { posicao, erro, nome ->

            pesquisando = false

            if (posicao != null) {
                centroSelecionado = posicao
                enderecoEncontrado = nome ?: endereco
            } else {
                erroPesquisa = erro
            }
        }
    }


    fun enquadrarCirculo(
        mapa: MapLibreMap,
        centro: LatLng,
        raioMetros: Double
    ) {
        val geoJson = JSONObject(
            criarCirculoGeoJson(centro, raioMetros)
        )

        val coordenadas = geoJson
            .getJSONObject("geometry")
            .getJSONArray("coordinates")
            .getJSONArray(0)

        val limites = LatLngBounds.Builder()

        for (i in 0 until coordenadas.length()) {
            val ponto = coordenadas.getJSONArray(i)

            limites.include(
                LatLng(
                    ponto.getDouble(1),
                    ponto.getDouble(0)
                )
            )
        }

        val bounds = limites.build()

        mapa.easeCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                80,
                180,
                80,
                240
            ),
            500
        )
    }

    fun buscarLocalizacao() {

        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) return

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            if (location != null) {

                val posicao = LatLng(
                    location.latitude,
                    location.longitude
                )

                localizacao = posicao

                if (centroSelecionado == null) {
                    centroSelecionado = posicao
                }
            }
        }
    }

    val permissaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->

        if (permissoes.values.any { it }) {
            buscarLocalizacao()
        }
    }

    LaunchedEffect(Unit) {

        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            buscarLocalizacao()
        } else {
            permissaoLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    LaunchedEffect(centroSelecionado, mapa, raio) {

        val centro = centroSelecionado
        val mapaAtual = mapa

        if (centro != null && mapaAtual != null) {

            desenharCirculo(
                mapaAtual,
                centro,
                raio.toDouble()
            )
        }
    }


    LaunchedEffect(centroSelecionado, mapa) {

        val centro = centroSelecionado
        val mapaAtual = mapa

        if (centro != null && mapaAtual != null) {

            val marcadorAtual = marcadorCentro

            if (marcadorAtual == null) {

                marcadorCentro = mapaAtual.addMarker(
                    MarkerOptions()
                        .position(centro)
                        .title("Centro do raio")
                )

            } else {

                marcadorAtual.position = centro
            }
        }
    }


// Centraliza a câmera quando o centro muda.
// Alterações no raio não movimentam mais o mapa.

    LaunchedEffect(centroSelecionado, mapa, raio) {

        val centro = centroSelecionado
        val mapaAtual = mapa

        if (centro != null && mapaAtual != null) {
            enquadrarCirculo(
                mapaAtual,
                centro,
                raio.toDouble()
            )
        }
    }



    LaunchedEffect(modoEscuro, mapa) {

        val mapaAtual = mapa ?: return@LaunchedEffect

        val estiloDesejado =
            if (modoEscuro) {
                MAP_STYLE_DARK
            } else {
                MAP_STYLE_LIGHT
            }

        mapaAtual.setStyle(
            Style.Builder().fromUri(estiloDesejado)
        ) {
            val centro = centroSelecionado

            if (centro != null) {
                desenharCirculo(
                    mapaAtual,
                    centro,
                    raio.toDouble()
                )
            }
        }
    }


    // Centraliza a câmera quando o centro muda
    LaunchedEffect(centroSelecionado, mapa) {

        val centro = centroSelecionado
        val mapaAtual = mapa

        if (centro != null && mapaAtual != null) {

            mapaAtual.cameraPosition =
                CameraPosition.Builder()
                    .target(centro)
                    .zoom(calcularZoom(raio.toDouble()))
                    .build()
        }
    }

    // Controle automático do tema do mapa
    var temaAplicado by remember {
        mutableStateOf<Boolean?>(null)
    }

    LaunchedEffect(modoEscuro, mapa) {

        val mapaAtual = mapa ?: return@LaunchedEffect

        if (temaAplicado == modoEscuro) {
            return@LaunchedEffect
        }

        if (temaAplicado == null) {
            temaAplicado = modoEscuro
            return@LaunchedEffect
        }

        val estiloDesejado =
            if (modoEscuro) MAP_STYLE_DARK
            else MAP_STYLE_LIGHT

        mapaAtual.setStyle(
            Style.Builder().fromUri(estiloDesejado)
        ) {
            centroSelecionado?.let { centro ->
                desenharCirculo(
                    mapaAtual,
                    centro,
                    raio.toDouble()
                )
            }
        }

        temaAplicado = modoEscuro


        fun aplicarDistancia() {

            val numero = distanciaDigitada
                .trim()
                .replace(",", ".")
                .toDoubleOrNull()

            if (numero == null || !numero.isFinite()) {
                erroDistancia = "Digite uma distância válida"
                return
            }

            val metros = if (unidadeDistancia == "km") {
                numero * 1000.0
            } else {
                numero
            }

            if (metros < 100.0 || metros > 200000.0) {
                erroDistancia = "Informe um valor entre 100 m e 200 km"
                return
            }

            raio = metros.toFloat()
            erroDistancia = null
        }

    }

    fun aplicarDistancia() {

        val numero = distanciaDigitada
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()

        if (numero == null || !numero.isFinite()) {
            erroDistancia = "Digite uma distância válida"
            return
        }

        val metros = if (unidadeDistancia == "km") {
            numero * 1000.0
        } else {
            numero
        }

        if (metros < 100.0 || metros > 200000.0) {
            erroDistancia = "Informe um valor entre 100 m e 200 km"
            return
        }

        raio = metros.toFloat()
        erroDistancia = null
    }

    // Interface principal
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Aqui continua seu AndroidView,
        // barra de pesquisa, botão GPS e Card.
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { ctx ->

                MapView(ctx).apply {


                    getMapAsync { mapaCarregado ->

                        mapaCarregado.addOnMapClickListener { ponto ->

                            centroSelecionado = ponto

                            true
                        }

                        mapaCarregado.setStyle(
                            Style.Builder().fromUri(
                                if (modoEscuro) {
                                    MAP_STYLE_DARK
                                } else {
                                    MAP_STYLE_LIGHT
                                }
                            )
                        ) {
                            mapa = mapaCarregado
                        }

                        mapaCarregado.cameraPosition =
                            CameraPosition.Builder()
                                .target(LatLng(-23.5505, -46.6333))
                                .zoom(12.0)
                                .build()
                    }

                }
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 16.dp
                )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textoPesquisa,
                        onValueChange = {
                            textoPesquisa = it
                            erroPesquisa = null
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Buscar endereço...")
                        },

                        trailingIcon = {
                            if (textoPesquisa.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        textoPesquisa = ""
                                        erroPesquisa = null
                                        enderecoEncontrado = null
                                        foco.clearFocus()
                                        teclado?.hide()
                                    }
                                ) {
                                    Text(
                                        text = "×",
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        },

                        singleLine = true,
                        enabled = !pesquisando,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                executarPesquisa()
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            executarPesquisa()
                        },
                        enabled = !pesquisando &&
                                textoPesquisa.isNotBlank()
                    ) {
                        Text("Buscar")
                    }
                }
            }

            if (pesquisando) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }

            erroPesquisa?.let { mensagem ->
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = mensagem,
                        modifier = Modifier.padding(12.dp),
                        color =
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            enderecoEncontrado?.let { endereco ->

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Local encontrado",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = endereco,
                            fontSize = 13.sp,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

        }

        FloatingActionButton(
            onClick = {

                val posicao = localizacao

                if (posicao != null) {

                    centroSelecionado = posicao

                    // Se já estivermos no centro do GPS,
                    // ainda podemos recentralizar a câmera.
                    mapa?.let { mapaAtual ->
                        enquadrarCirculo(
                            mapaAtual,
                            posicao,
                            raio.toDouble()
                        )
                    }

                } else {

                    buscarLocalizacao()
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = "◎",
                fontSize = 28.sp
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .animateContentSize(
                    animationSpec = tween(durationMillis = 300)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column {

                // Cabeçalho do painel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            painelExpandido = !painelExpandido
                        }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MapaRaio",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Raio de ${formatarRaio(raio)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = if (painelExpandido) "⌄" else "⌃",
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Conteúdo expansível
                AnimatedVisibility(
                    visible = painelExpandido
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 20.dp
                            )
                    ) {
                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "RAIO DE ALCANCE",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formatarRaio(raio),
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.primary
                        )


                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Digite a distância desejada",
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedTextField(
                                value = distanciaDigitada,
                                onValueChange = {
                                    distanciaDigitada = it
                                    erroDistancia = null
                                },
                                modifier = Modifier.weight(1f),
                                label = {
                                    Text("Distância")
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )

                            Column {

                                FilterChip(
                                    selected = unidadeDistancia == "m",
                                    onClick = {
                                        unidadeDistancia = "m"
                                    },
                                    label = {
                                        Text("m")
                                    }
                                )

                                FilterChip(
                                    selected = unidadeDistancia == "km",
                                    onClick = {
                                        unidadeDistancia = "km"
                                    },
                                    label = {
                                        Text("km")
                                    }
                                )
                            }
                        }

                        erroDistancia?.let { mensagem ->
                            Text(
                                text = mensagem,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                aplicarDistancia()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aplicar distância")
                        }


                        Slider(
                            value = raio,
                            onValueChange = { valor ->
                                raio = (valor / 100f)
                                    .roundToInt()
                                    .coerceIn(1, 1000) * 100f
                            },
                            valueRange = 100f..100000f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("100 m")
                            Text("100 km")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                500f,
                                1000f,
                                5000f,
                                10000f
                            ).forEach { valor ->

                                val selecionado = raio == valor

                                FilledTonalButton(
                                    onClick = {
                                        raio = valor
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(
                                        horizontal = 2.dp,
                                        vertical = 4.dp
                                    ),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor =
                                            if (selecionado)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor =
                                            if (selecionado)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text(
                                        text = formatarRaio(valor),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    } // Fecha o Box

} // Fecha a função MapaScreen

fun formatarRaio(raio: Float): String {
    return if (raio >= 1000f) {
        val km = raio / 1000f
        if (km % 1f == 0f) {
            "${km.toInt()} km"
        } else {
            String.format(
                java.util.Locale("pt", "BR"),
                "%.1f km",
                km
            )
        }
    } else {
        "${raio.toInt()} m"
    }
}

fun calcularZoom(raio: Double): Double {
    return when {
        raio <= 500 -> 15.5
        raio <= 1000 -> 14.5
        raio <= 2000 -> 13.5
        raio <= 5000 -> 12.0
        raio <= 10000 -> 11.0
        else -> 10.0
    }
}

fun desenharCirculo(
    mapa: MapLibreMap,
    centro: LatLng,
    raioMetros: Double
) {
    val geoJson = criarCirculoGeoJson(
        centro,
        raioMetros
    )

    mapa.getStyle { style ->

        val sourceExistente =
            style.getSourceAs<GeoJsonSource>(CIRCLE_SOURCE)

        if (sourceExistente != null) {
            sourceExistente.setGeoJson(geoJson)
        } else {

            style.addSource(
                GeoJsonSource(
                    CIRCLE_SOURCE,
                    geoJson
                )
            )

            style.addLayer(
                FillLayer(
                    CIRCLE_FILL,
                    CIRCLE_SOURCE
                ).withProperties(
                    fillColor("#2563EB"),
                    fillOpacity(0.20f)
                )
            )

            style.addLayer(
                LineLayer(
                    CIRCLE_LINE,
                    CIRCLE_SOURCE
                ).withProperties(
                    lineColor("#2563EB"),
                    lineWidth(2.5f)
                )
            )
        }
    }
}

fun criarCirculoGeoJson(
    centro: LatLng,
    raioMetros: Double
): String {

    val pontos = JSONArray()

    val raioTerra = 6371000.0
    val distanciaAngular = raioMetros / raioTerra

    val latitude = Math.toRadians(centro.latitude)
    val longitude = Math.toRadians(centro.longitude)

    for (i in 0..120) {

        val angulo = 2.0 * PI * i / 120.0

        val latitudeDestino = asin(
            sin(latitude) * cos(distanciaAngular) +
                    cos(latitude) *
                    sin(distanciaAngular) *
                    cos(angulo)
        )

        val longitudeDestino = longitude + atan2(
            sin(angulo) *
                    sin(distanciaAngular) *
                    cos(latitude),
            cos(distanciaAngular) -
                    sin(latitude) *
                    sin(latitudeDestino)
        )

        val ponto = JSONArray()
        ponto.put(Math.toDegrees(longitudeDestino))
        ponto.put(Math.toDegrees(latitudeDestino))

        pontos.put(ponto)
    }

    val anel = JSONArray().put(pontos)

    val geometria = JSONObject()
        .put("type", "Polygon")
        .put("coordinates", anel)

    return JSONObject()
        .put("type", "Feature")
        .put("geometry", geometria)
        .put("properties", JSONObject())
        .toString()
}


private val executorPesquisa =
    Executors.newSingleThreadExecutor()


fun pesquisarEndereco(
    endereco: String,
    resultado: (LatLng?, String?, String?) -> Unit
) {
    executorPesquisa.execute {

        var conexao: HttpURLConnection? = null

        try {
            val enderecoCodificado = Uri.encode(endereco)

            val url = URL(
                "https://nominatim.openstreetmap.org/search" +
                        "?q=$enderecoCodificado" +
                        "&format=jsonv2" +
                        "&limit=1" +
                        "&countrycodes=br"
            )

            conexao = url.openConnection() as HttpURLConnection
            conexao.requestMethod = "GET"
            conexao.connectTimeout = 10000
            conexao.readTimeout = 10000

            // Mantenha aqui o identificador e contato reais do app.
            conexao.setRequestProperty(
                "User-Agent",
                "MapaRaio/1.0 (contato: seu-email@exemplo.com)"
            )

            conexao.setRequestProperty(
                "Accept-Language",
                "pt-BR,pt"
            )

            if (conexao.responseCode != 200) {
                throw Exception("Erro HTTP ${conexao.responseCode}")
            }

            val resposta = conexao.inputStream
                .bufferedReader()
                .use { it.readText() }

            val resultados = JSONArray(resposta)

            if (resultados.length() == 0) {
                Handler(Looper.getMainLooper()).post {
                    resultado(null, "Endereço não encontrado", null)
                }
                return@execute
            }

            val primeiro = resultados.getJSONObject(0)

            val posicao = LatLng(
                primeiro.getDouble("lat"),
                primeiro.getDouble("lon")
            )

            val nome = primeiro.optString("display_name", endereco)

            Handler(Looper.getMainLooper()).post {
                resultado(posicao, null, nome)
            }

        } catch (erro: Exception) {
            Handler(Looper.getMainLooper()).post {
                resultado(
                    null,
                    "Não foi possível pesquisar o endereço",
                    null
                )
            }
        } finally {
            conexao?.disconnect()
        }
    }
}

