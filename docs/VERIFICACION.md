# Registro de verificación

## Base Android

La plantilla original del usuario compiló con `:app:assembleDebug`: BUILD SUCCESSFUL en 11 min 38 s. Ese APK acredita únicamente la plantilla inicial.

Se detectó posteriormente que Android Studio admite como máximo AGP 9.3.0. El proyecto se ajustó a AGP 9.3.0 y Gradle 9.5.0; [Android Developers](https://developer.android.com/build/releases/agp-9-3-0-release-notes) confirma esa pareja y el soporte de API 37.

Comprobación ejecutada del código actual:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Resultado: **BUILD SUCCESSFUL en 2 min 47 s**. APK generado y Lint aprobado. **15 tests unitarios aprobados, 0 errores y 0 fallos**: cinco del repositorio remoto, seis de validación/cancelación, tres del ViewModel y uno de la plantilla.

También se corrigieron los iconos adaptativos generados para minSdk 30: ahora están en `mipmap-anydpi-v26` y Android 6/7 utiliza los WebP existentes por densidad. Esto mantiene la compatibilidad minSdk 23 de la guía.

APK actual: `app/build/outputs/apk/debug/app-debug.apk`. La ejecución de la app fue confirmada por el usuario.

Pruebas instrumentadas ejecutadas con `:app:connectedDebugAndroidTest`: **BUILD SUCCESSFUL en 4 min 35 s**, sobre TECNO LE7 con Android 11. **5 pruebas aprobadas, 0 fallidas y 0 omitidas**: inicio, validación de búsqueda vacía, búsqueda demo con detalle y regreso, etiqueta de modo remoto y paquete de la aplicación.

## Backend local: aprobado

Comando: `node --test tests/search-products.test.ts`.

Resultado ejecutado: **14 pruebas aprobadas, 0 fallidas**. Se cubren métodos, CORS, JSON, tipo y longitud de consulta, secreto ausente, mapeo, URLs, máximo de diez ofertas, cero resultados, errores de proveedor/red, timeout y ventana del límite de solicitudes. Se usa una clave ficticia y un proveedor simulado.

## Supabase Cloud

Proyecto «App moviles», Reference ID `wydbjucuuhneoxbjfleh`, verificado saludable mediante Supabase CLI. La carpeta local se enlazó al proyecto y la función se desplegó con `functions deploy search-products --use-api`.

Endpoint: `https://wydbjucuuhneoxbjfleh.supabase.co/functions/v1/search-products`.

| Solicitud ejecutada | Resultado observado |
| --- | --- |
| POST con query de un carácter | HTTP 400, validación de longitud |
| GET | HTTP 405, método no permitido |
| POST con Nintendo Switch OLED, antes de guardar el secreto | HTTP 503, TINYFISH_NOT_CONFIGURED |
| POST con Nintendo Switch OLED, después de guardar el secreto | HTTP 200, ocho resultados reales |

La función existe y completa el recorrido Supabase → TinyFish → Supabase. La prueba real devolvió ocho resultados con enlaces a Nintendo, MercadoLibre Ecuador y tiendas ecuatorianas. El campo `searchedAt` de la respuesta fue `2026-10-23T01:42:27.204Z` (22 de octubre, 20:42 en Ecuador).

La clave privada fue guardada por el usuario en Supabase Secrets. No se leyó ni se copió su valor al proyecto. La respuesta conserva las fuentes y mantiene `Ver en fuente` para precios no normalizados.

## Dispositivo y entrega

El usuario confirmó el recorrido dentro de TinyPrice: buscar Nintendo Switch OLED, ver resultados encontrados en la web, abrir el detalle y pulsar Abrir fuente web. Esta evidencia corresponde a una prueba manual comunicada por el usuario, no a una captura o prueba instrumentada realizada por el agente.

Las comprobaciones manuales de tema, rotación y pérdida de red siguen pendientes. El repositorio de entrega es [JagoWoa/TinyPrice-Android-Supabase](https://github.com/JagoWoa/TinyPrice-Android-Supabase), con esta misma carpeta de Android Studio enlazada como origen de los cambios.

No se incluye ninguna API key privada en el repositorio ni en este informe.
