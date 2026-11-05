# 📍 MapaRaio

**Aplicativo Android para visualizar áreas de alcance geográfico por meio de círculos de raio ajustável em um mapa interativo.**

O **MapaRaio** permite selecionar qualquer localização no mapa e visualizar uma área de cobertura ao seu redor, definindo distâncias entre **100 metros e 200 quilômetros**.

Desenvolvido em **Kotlin**, utilizando **Jetpack Compose**, **MapLibre** e mapas baseados em dados do **OpenStreetMap**.

## 📱 Sobre o projeto

O MapaRaio foi desenvolvido para facilitar a visualização de distâncias geográficas de maneira simples, rápida e intuitiva.

Com ele, é possível determinar um ponto central, configurar um raio de alcance e visualizar a região correspondente diretamente no mapa.

O aplicativo pode ser utilizado para analisar áreas de atendimento, cobertura de entregas, deslocamentos e regiões de interesse.

## ✨ Funcionalidades

- 🗺️ **Mapa interativo:** navegação e visualização de mapas.
- 📍 **Localização por GPS:** identificação da posição atual do dispositivo.
- 🔎 **Pesquisa de endereços:** localização de lugares por meio de busca textual.
- 👆 **Seleção manual:** escolha do centro do raio tocando em qualquer ponto do mapa.
- 🔵 **Área de cobertura:** exibição de um círculo azul representando a distância selecionada.
- 📌 **Marcador central:** identificação visual do ponto selecionado.
- 📏 **Raio ajustável:** configuração de distâncias entre 100 metros e 200 quilômetros.
- ✏️ **Distância personalizada:** entrada manual de valores em metros ou quilômetros.
- 🎚️ **Controle deslizante:** ajuste rápido do raio.
- ⚡ **Distâncias predefinidas:** botões para seleção rápida.
- 🔍 **Zoom automático:** enquadramento da área de cobertura na tela.
- 🌙 **Modo claro e escuro:** adaptação automática ao tema do dispositivo.
- 📱 **Interface expansível:** painel inferior para configurar as distâncias.

## 🛠️ Tecnologias utilizadas

| Tecnologia | Utilização |
|---|---|
| Kotlin | Linguagem de programação |
| Jetpack Compose | Construção da interface |
| Material Design 3 | Componentes visuais |
| MapLibre Android SDK | Renderização e interação com o mapa |
| OpenFreeMap | Estilos e tiles do mapa |
| OpenStreetMap | Dados cartográficos |
| Nominatim | Pesquisa e geocodificação de endereços |
| Google Play Services Location | Obtenção da localização GPS |
| Gradle | Gerenciamento de dependências e compilação |
| Android Studio | Ambiente de desenvolvimento |

## ⚙️ Como executar o projeto

### Pré-requisitos

- Android Studio instalado.
- Android SDK configurado.
- Dispositivo Android ou emulador.
- Conexão com a internet para carregar mapas e pesquisar endereços.

### 1. Clonar o repositório

```bash
git clone https://github.com/SEU_USUARIO/MapaRaio.git
```

### 2. Abrir no Android Studio

Abra o Android Studio, selecione **Open** e escolha a pasta do projeto.

### 3. Sincronizar o Gradle

Aguarde a sincronização das dependências do projeto.

### 4. Executar

Conecte um dispositivo Android ou inicie um emulador e clique em **Run**.

Ao iniciar, permita o acesso à localização para utilizar os recursos de GPS.

## 📐 Como funciona o cálculo do raio?

O aplicativo utiliza cálculos geográficos baseados na geometria esférica da Terra para determinar os pontos que formam o círculo de alcance.

A partir de uma coordenada central (latitude e longitude) e da distância informada, são calculados pontos distribuídos ao redor do centro.

Esses pontos são organizados em uma geometria **GeoJSON Polygon**, renderizada no mapa pelo MapLibre.

Quando o usuário altera o centro ou a distância, o aplicativo atualiza o círculo e ajusta automaticamente o enquadramento do mapa.

## 🧭 Como utilizar

1. Abra o aplicativo e aguarde o carregamento do mapa.
2. Utilize sua localização atual, pesquise um endereço ou toque em um ponto do mapa.
3. Observe o marcador que identifica o centro selecionado.
4. Abra o painel inferior para configurar o raio.
5. Ajuste a distância pelo controle deslizante, pelos botões rápidos ou digitando um valor.
6. Visualize a área de cobertura destacada no mapa.

## 📦 APK

O projeto também pode ser compilado como um arquivo APK para instalação em dispositivos Android.

Para gerar um APK de teste no Android Studio:

**Build → Generate App Bundles or APKs → Generate APKs**

O arquivo gerado normalmente estará em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

> O APK de debug é destinado a testes. Para distribuição oficial, recomenda-se gerar uma versão de release assinada.

## 🌐 Serviços de mapas

O MapaRaio utiliza tecnologias de mapas abertos, sem depender da API paga do Google Maps.

A pesquisa de endereços utiliza o serviço Nominatim, sujeito à sua política de utilização. Para distribuição em maior escala, é necessário avaliar os limites e requisitos do serviço.

## 🎯 Objetivo do projeto

Desenvolver uma ferramenta Android simples e acessível para visualizar distâncias e áreas de cobertura geográfica, explorando recursos de geolocalização, mapas interativos e desenvolvimento mobile nativo.

## 👨‍💻 Desenvolvimento

Projeto desenvolvido para estudo e aplicação prática de desenvolvimento Android com Kotlin, Jetpack Compose e integração com serviços de mapas.

---

**MapaRaio — Visualize distâncias, explore possibilidades.**
