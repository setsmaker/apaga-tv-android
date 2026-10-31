# Apaga TV Universal — Android

Aplicación Android para enviar señales infrarrojas de apagado desde un solo botón, sin Internet, publicidad ni registro.

**Marcas principales:** Samsung, LG, Sony, Xiaomi, Philips, TCL, Hisense y Panasonic. Pulsa **Otras marcas** para consultar el catálogo completo (más de 100 fabricantes en la compilación de octubre de 2026). La cantidad exacta figura en la app y en `ir-catalog-report.txt`.

## Descargar desde el móvil

1. Entra en **Actions** de este repositorio.
2. Abre la última ejecución verde de **Compilar APK Android**.
3. Abajo, en **Artifacts**, descarga **ApagaTV-apk**.
4. Descomprime el ZIP y abre `app-debug.apk`.
5. Android puede pedir autorización para instalar desde Chrome o desde el gestor de archivos.

## Cómo funciona

La app distingue dos categorías de señales:

- **OFF:** comandos etiquetados como apagado independiente. Se envían al pulsar «APAGAR TODAS». Esta etiqueta procede de la base o de una fuente documentada, pero no garantiza compatibilidad con todos los modelos.
- **POWER (alternativo):** el botón normal de encendido/apagado. Puede **encender una televisión que ya estuviera apagada**. Solo se transmite manualmente desde «Probar», nunca en la secuencia automática.

En cada marca se pueden recorrer los códigos y **guardar los OFF comprobados**. Cuando hay códigos OFF guardados para una marca, el botón universal los prioriza. Puedes guardar varios para distintos televisores.

Es necesario orientar el móvil hacia el receptor infrarrojo de la TV: la señal no atraviesa paredes. Se requiere un móvil Android que exponga el hardware mediante `ConsumerIrManager`.

## Bases de datos y fuentes

- [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) — colección de señales `.ir` bajo licencia **CC0 1.0**. Se importan códigos válidos de los protocolos soportados: NEC, NECext, Samsung32, Sony SIRC 12/15/20, Philips RC5 y parte de los RAW. Se excluyen los protocolos que esta versión aún no sabe transmitir correctamente.
- [Samsung: códigos OFF](https://community.home-assistant.io/t/ir-remote-transmitter-discrete-codes-for-samsung-lcd-on-off/215925) — `E0E019E6`.
- [LG: códigos OFF](https://gist.github.com/ledoge/ab78723efdaa00e6815e30d1792790fd) — `20DFA35C`.
- [TCL: códigos OFF](https://gist.github.com/DDRBoxman/c68b68e41a47338496ba8cbd1bb5d20e) — `57E318E7`.
- [Hisense: códigos OFF](https://hawkins.id.au/posts/discrete_ir_for_hisense_tv/) — `00FD8B74`.
- [Sony: SIRC OFF](https://github.com/Flameeyes/pysirc/blob/main/examples/circuitpython/sony-remote.py) — TV 1, comando 47.
- [RemoteCentral](https://files.remotecentral.com/library/3-1/index.html) — referencia para códigos discretos de otros modelos.

La fuente de código de Flipper se descarga al compilar con GitHub Actions. Otros repertorios como `probonopd/irdb` tienen condiciones de licencia adicionales y **no** se han incorporado automáticamente.

## Compilación

GitHub Actions ejecuta `tools/generate_catalog.py`, comprueba patrones IR con `tests/IrCodesCheck.java` y compila mediante Android SDK 35 / Java 17 / Gradle 8.9.

Este APK utiliza una firma de desarrollo de GitHub Actions; si ya instalaste una versión anterior con otra firma, **desinstala primero la anterior**. No se mantienen las preferencias al desinstalar.

## Limitaciones

Un código listado como OFF puede no funcionar en un modelo concreto. No hay confirmación de estado de la TV a través de infrarrojos. Algunos modelos no tienen código OFF independiente o solo responden por Bluetooth, Wi-Fi o con el mando original. Xiaomi y otras marcas pueden aparecer con POWER pero sin OFF.
