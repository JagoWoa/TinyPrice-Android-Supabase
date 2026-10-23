# Seguimiento del laboratorio

La guía pública contiene 16 pasos. El paso 17 corresponde a la entrega en GitHub solicitada por el usuario. Este proyecto parte de la plantilla creada por el usuario en Android Studio y conserva su paquete.

| Paso | Actividad | Estado verificable |
| --- | --- | --- |
| 1 | Requisitos | Plantilla original compilada correctamente antes de modificarla. |
| 2 | Arquitectura | Compose → ViewModel → repositorio → Edge Function → TinyFish. |
| 3 | Cuenta TinyFish y clave | Cuenta creada y clave guardada por el usuario directamente en Supabase Secrets. |
| 4 | Prueba de Search | Proveedor real comprobado desde la Edge Function: consulta Nintendo Switch OLED, HTTP 200 y ocho resultados. No se recuperó la clave privada para repetir la prueba desde el cliente. |
| 5 | Entorno y skill Android | Android CLI 1.0.16625370 y skill oficial instalados. Ejecución de la app confirmada por el usuario. La actividad opcional con Gemini no se ejecutó. |
| 6 | Proyecto y dependencias | Adaptado al IDE instalado con AGP 9.3.0 y Gradle 9.5.0. Kotlin 2.2.10, Compose BOM 2026.02.01, SDK 37 y minSdk 23. |
| 7 | Modelos y repositorios | Modelo, demo, DTOs, Retrofit/Gson y repositorio remoto implementados. |
| 8 | Estado y ViewModel | StateFlow, eventos, validación, cancelación y consulta asociada a los resultados implementados. |
| 9 | Compose | Inicio, sugerencias, búsqueda, carga, lista, vacío/error y detalle implementados. |
| 10 | Cuenta Supabase | Cuenta existente y CLI autenticada por el usuario. |
| 11 | Proyecto remoto | Proyecto «App moviles», ref wydbjucuuhneoxbjfleh, saludable y enlazado mediante CLI. |
| 12 | Edge Function | search-products desplegada con Deno.serve, validación, timeout y adaptación de resultados. |
| 13 | Secreto y prueba remota | Secreto configurado. HTTP 200 y ocho resultados reales comprobados; anteriormente se verificaron 400, 405 y el 503 por secreto ausente. |
| 14 | Conectar Android | Recorrido Android → Supabase → TinyFish confirmado por el usuario: búsqueda, resultados web, detalle y apertura de fuente. |
| 15 | Calidad | APK generado, 15 tests unitarios Android y Lint aprobados. Además, 14 pruebas locales del backend y 5 pruebas instrumentadas en TECNO LE7 aprobadas. |
| 16 | Revisión y evidencias | Recorrido principal en dispositivo confirmado por el usuario. La clave no se incluye en las evidencias. Publicación en Play Store opcional. |
| 17 | Entrega GitHub | Esta misma carpeta de Android Studio enlazada al repositorio público JagoWoa/TinyPrice-Android-Supabase, rama main; incluye código Android, Edge Function, pruebas y documentación. |

## Adaptaciones respecto del texto del codelab

- La versión 9.4.1 de AGP excede el máximo admitido por el Android Studio instalado. Se utiliza AGP 9.3.0 con Gradle 9.5.0, conservando el flujo y SDK 37.
- El Android CLI actual exige `skills add android-cli --project=.`; omitir el nombre devuelve un error. La skill quedó instalada en `.agents/skills/android-cli`.
- Las pantallas y el tema mantienen el paquete del proyecto original: `com.example.tinyprice_android_supabase`.
- Supabase no requiere tablas para esta práctica. La clave publicable identifica el proyecto; la clave privada de TinyFish solo pertenece a Supabase Secrets.

## Comprobaciones manuales pendientes

El usuario confirmó búsqueda de Nintendo Switch OLED, resultados encontrados en la web, detalle y apertura de la fuente desde la app. Tema claro/oscuro, rotación y pérdida de red/reintento no se han confirmado manualmente. Las pruebas unitarias cubren entradas incorrectas y fallos de red. No se marca un caso como aprobado hasta ejecutarlo.
