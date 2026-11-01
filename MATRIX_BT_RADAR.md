# MATRIX BT RADAR
Aplicación Android experimental con radar de dispositivos Bluetooth LE y clásicos, estética Matrix, contador actualizado cada 5 segundos y tendencias estadísticas de intensidad RSSI.

**Movimientos:** Se acercan (RSSI creciente), se alejan (RSSI decreciente), estables y sin datos suficientes. Son estimaciones: la intensidad varía también por reflejos, obstáculos y orientación, y no permite obtener dirección o distancia real. El ángulo de los puntos en pantalla es ilustrativo; la posición radial sí depende de la intensidad recibida.

Para compilar, abre **Actions -> Matrix Bluetooth Radar · APK** en esta rama y descarga el artefacto generado al completar la ejecución. Contiene **app-debug.apk**. Los fuentes están dentro de `matrixbt/source.zip` y puedes abrirlos en Android Studio.

No recopila ubicación GPS, no transmite datos fuera del teléfono y detiene el escaneo cuando la app pasa a segundo plano. Requiere permisos de Bluetooth.
