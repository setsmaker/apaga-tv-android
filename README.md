# Apaga TV — Android

App Android sin anuncios, sin conexión a Internet y sin registro. Una sola pulsación envía sucesivamente señales infrarrojas para Samsung, LG, Sony, Philips y Xiaomi. Puedes probar cada fabricante por separado y guardar qué marcas quieres incluir.

## Descargar APK desde el móvil

1. Abre la pestaña **Actions** de este repositorio.
2. Selecciona **Compilar APK Android** y abre la ejecución con ✓ verde.
3. Desplázate a **Artifacts** y pulsa **ApagaTV-apk**.
4. En el ZIP descargado, abre **app-debug.apk** para instalarla. Android puede pedir permiso para instalar apps desde Chrome o tu gestor de archivos.

Si no aparece ninguna ejecución, entra en **Actions → Compilar APK Android → Run workflow → Run workflow**.

## Uso

Abre Apaga TV, apunta el emisor infrarrojo del móvil hacia el sensor de la televisión y pulsa **APAGAR TODAS**. No hay que elegir un mando. Los botones **Probar** permiten comprobar cada fabricante.

Samsung, LG y Sony incluyen códigos de apagado específicos. Philips depende del modelo. Xiaomi utiliza un código POWER que **puede encender el aparato si ya estaba apagado**; por eso empieza desmarcada. Puedes activarla tras comprobar su comportamiento.

Los infrarrojos no atraviesan paredes, así que no puede apagar televisiones de habitaciones distintas con una sola emisión, salvo que todas reciban la señal directa.

## Compilación local

Abre el proyecto en Android Studio con Java 17, Android SDK 35, Gradle 8.9 y Android Gradle Plugin 8.7.3. Ejecuta `gradle assembleDebug`.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Avisos

Los códigos no están comprobados físicamente en tus modelos concretos. La app no puede saber si un televisor se ha apagado. Necesita un móvil con `ConsumerIrManager` que exponga el emisor IR a las aplicaciones Android.
