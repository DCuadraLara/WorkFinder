# WorkFinder — fase 4 · funcionalidades completas

Aplicación de escritorio con Java 17, JavaFX, Maven y SQLite mediante JDBC para organizar candidaturas a prácticas y empleo. Cada fila representa **una candidatura**. Dos candidaturas a la misma empresa y al mismo puesto conservan identificadores diferentes.

Proyecto Maven independiente ubicado en `C:\Users\David\Documents\Proyectospersonales\WorkFinder`. El proyecto AgentAI se conserva en su ubicación original.

## Qué puedes hacer

- Añadir, listar, consultar mediante el formulario de edición, editar y eliminar candidaturas.
- Guardar empresa, puesto, categoría, estado, modalidad, ubicación, enlace, fecha de envío, CV, carta y notas/condiciones.
- Usar el formulario dentro de la ventana principal. Tiene desplazamiento vertical y sus botones permanecen visibles en el tamaño mínimo.
- Corregir errores marcados junto a cada campo sin perder los valores introducidos. Un fallo de SQLite mantiene el formulario y la versión anterior de la fila.
- Cancelar el formulario sin persistir cambios.
- Eliminar solo después de confirmar; Cancelar es la opción predeterminada del diálogo.
- Recargar la lista desde SQLite y recuperar la apertura tras corregir un problema del archivo.
- Cerrar y volver a abrir para recuperar los datos guardados.
- Cambiar el estado directamente en cada fila mediante un selector del enum, sin escritura libre. La selección se guarda de inmediato en SQLite; si falla, se restaura el valor y el color anteriores y se muestra un error.
- Buscar solo por empresa desde la lupa, con coincidencia parcial y sin distinguir mayúsculas ni tildes.
- Combinar filtros de estado, categoría y modalidad usando los enums. Limpiar todos los criterios u ocultar el panel conservándolos.
- Ver el total, los recuentos por estado y la gráfica horizontal por categoría, actualizados con cada cambio confirmado.

Se mantienen grafito, crema y cobre, los títulos en Georgia y la entrada de 940 ms. La entrada solo ocurre al abrir y se omite con Esc. Los controles de selección permanecen desactivados si no hay una candidatura seleccionada; durante una operación se impiden envíos duplicados. Si se intenta cerrar mientras hay una operación en curso, se pide esperar a que termine.

Las cuatro fases están implementadas. El resumen muestra cifras reales y representa **todas las candidaturas**, independientemente de los filtros de la tabla.

Los estados tienen texto y color: **Enviada** verde, **En revisión** amarillo, **Rechazada** rojo, **Entrevista** cobre, **Oferta** turquesa y **Retirada** gris. El color se limita al selector para conservar la paleta y la legibilidad de las filas.

## Búsqueda y filtros

La lupa, junto a Recargar, abre un panel compacto debajo de la tabla. También puedes usar **Ctrl+F**. El panel no reserva espacio mientras está oculto y los títulos del formulario solo aparecen al añadir o editar.

- Escribe el nombre de una empresa o una parte. La tabla se actualiza tras una pausa de 200 ms, o inmediatamente al pulsar Intro. Se ignoran mayúsculas y marcas diacríticas; por ejemplo, `orbita` encuentra `Órbita` y `nunez` encuentra `Núñez`.
- La búsqueda se limita al nombre de la empresa. No busca en puesto, ubicación ni notas. Comillas, `%` y `_` se interpretan literalmente.
- Estado, categoría y modalidad se combinan entre sí y con la empresa: una fila debe cumplir **todos** los criterios. La primera opción de cada selector elimina ese filtro.
- **Limpiar** restaura la lista completa. **×**, **Esc** o la lupa ocultan el panel sin quitar criterios; la lupa queda resaltada si hay filtros activos.
- Debajo de los botones aparece el ámbito, por ejemplo `3 de 12 · Búsqueda y filtros activos`. Este es el recuento de resultados de la tabla; el panel lateral conserva el ámbito global.
- Recargar, añadir, editar, eliminar y cambiar el estado mantienen los criterios de la sesión. Si una fila deja de cumplirlos después de guardar, desaparece del resultado. Una alta o edición guardada que queda fuera de la búsqueda muestra un aviso; pulsa Limpiar para verla.
- Si el filtrado deja fuera la fila seleccionada, se limpia la selección y se desactivan Editar y Eliminar. Un fallo al guardar conserva el modelo confirmado y restaura el estado anterior.

El servicio filtra la lista completa cargada desde SQLite. No se ejecuta JDBC por cada pulsación ni se modifica la base para buscar. Las operaciones de persistencia siguen en segundo plano; la lista completa solo cambia tras confirmar su resultado. Para recoger cambios hechos desde otra instancia, pulsa Recargar. Los criterios no se guardan al cerrar la aplicación.

## Resumen y usabilidad

El panel lateral muestra **todas las candidaturas guardadas**. Una empresa con dos candidaturas cuenta como dos, incluso si el puesto coincide. Los filtros solo afectan a la tabla. El panel comienza directamente en el total, sin el título «Resumen» ni el texto introductorio. El ámbito se puede consultar al pasar el cursor por el total y también está disponible como texto accesible.

- Total real y distribución por estado en una gráfica de queso, con los mismos colores que sus selectores. La leyenda conserva los seis estados y sus recuentos; al pasar el cursor por un sector o su leyenda se muestran cantidad y porcentaje del total. Los sectores también reciben foco con Tab. Los estados con cero permanecen en la leyenda y no dibujan sectores. Sin candidaturas, durante la carga o ante un fallo inicial se muestra una indicación en lugar de sectores ficticios.
- Una barra horizontal por categoría profesional, con el nombre completo y la cantidad. Cada categoría conserva un color propio en su nombre y su barra, con tonos afines a la paleta actual. Todas comparten una escala de 0 al recuento de la categoría mayor; las barras representan cantidades, no porcentajes.
- La barra de la categoría con más candidaturas recibe un reflejo suave que la recorre en 3,2 segundos, con una pausa de 1,2 segundos entre pasadas. Si hay empate, se animan las barras empatadas. Conservan su color y longitud; la animación se adapta a los cambios confirmados, se detiene con la base vacía y al cerrar, y se pausa al ocultar la ventana. Se puede desactivar junto a la entrada con `-Dworkfinder.animations=false` como opción de la JVM.
- Las categorías y estados sin candidaturas muestran 0. Una base vacía muestra total 0, una indicación para añadir la primera candidatura y barras sin relleno.
- Se actualiza después de añadir, editar, eliminar, cambiar un estado o recargar. La edición de categoría mueve su recuento y ajusta la escala de las barras.
- Un fallo de persistencia conserva el resumen confirmado. Durante la carga inicial se muestran guiones; si falla, se explica que el resumen no está disponible, sin presentar ceros como datos cargados. Una recarga fallida conserva las cifras anteriores y lo indica.
- El panel se desplaza verticalmente cuando falta altura, también en el tamaño mínimo. Los nombres no se abrevían; cada fila de categoría tiene un tooltip con su recuento.

Atajos en Windows:

| Atajo | Acción |
| --- | --- |
| Ctrl+F | Abrir la búsqueda y enfocar la empresa. |
| Ctrl+N | Abrir una candidatura nueva. |
| Ctrl+S | Guardar el formulario abierto; los errores mantienen sus valores. |
| F5 | Recargar la lista y el resumen sin quitar filtros. |
| Esc | Ocultar el panel de búsqueda; al entrar, omitir la animación. |

Los atajos respetan el bloqueo durante las operaciones. Recargar no sustituye un formulario abierto. En sistemas que usan otra tecla de acceso directo, se utiliza la tecla correspondiente a `isShortcutDown` de JavaFX.

## Ejecutar y comprobar

Necesitas un **JDK 17** y conexión para la primera descarga de Maven, JavaFX, SQLite JDBC y las bibliotecas de pruebas. Maven Wrapper está incluido; no hace falta instalar Maven, el SDK de JavaFX ni un servidor SQLite.

Desde PowerShell:

```powershell
cd C:\Users\David\Documents\Proyectospersonales\WorkFinder
java -version
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd javafx:run
```

`verify` compila, ejecuta las pruebas y empaqueta `target/workfinder-0.4.0-SNAPSHOT.jar`. El JAR no incluye un instalador ni un entorno JavaFX autónomo; ejecuta con `javafx:run`.

En Linux/macOS usa `sh ./mvnw clean verify` y `sh ./mvnw javafx:run`.

### Ejecutar desde un IDE

Abre **la carpeta WorkFinder** como proyecto Maven, importa o recarga las dependencias y selecciona **JDK 17**. Ejecuta `com.davidcuadralara.workfinder.Main` como aplicación Java con el classpath del módulo WorkFinder. Este es el punto de entrada para IntelliJ, Eclipse, NetBeans y otros IDE con soporte Maven; `WorkFinderApplication` mantiene únicamente el ciclo de vida de JavaFX.

`Main` no hereda de `Application` y llama a `Application.launch(WorkFinderApplication.class, args)`. Esto evita la comprobación especial del lanzador Java al arrancar directamente una subclase de JavaFX, según el patrón de [OpenJFX para un lanzador independiente](https://openjfx.io/openjfx-docs/#non-modular-application). Las dependencias JavaFX y sus bibliotecas nativas siguen siendo necesarias: las proporciona Maven para la plataforma actual.

Si ya tienes una configuración de ejecución que apunta a `WorkFinderApplication`, cambia su clase principal a `com.davidcuadralara.workfinder.Main`. Puedes seguir usando el objetivo Maven `javafx:run`, que también arranca `Main`.

Para abrir sin animación, añade la opción de JVM `-Dworkfinder.animations=false`. Con Maven puedes incluirla en `JAVA_TOOL_OPTIONS`, que también llega al proceso JavaFX. Esc solo omite la apertura actual.

## Base de datos

Ubicación predeterminada: **`${user.home}/.workfinder/workfinder.db`**. En este equipo:

```text
C:\Users\David\.workfinder\workfinder.db
```

La carpeta y la tabla se crean en segundo plano al abrir. El esquema usa `CREATE TABLE IF NOT EXISTS`: las aperturas posteriores no borran ni reinicializan las filas. Cada operación abre su conexión y la cierra junto con sus consultas y resultados mediante `try-with-resources`.

El identificador es `INTEGER PRIMARY KEY AUTOINCREMENT`; las fechas se almacenan como texto ISO `yyyy-mm-dd`, los enums por su nombre estable (`ENVIADA`, etc.) y los indicadores como 0/1. La lista se presenta por fecha de envío descendente y, en caso de empate, por identificador descendente.

La inserción es una transacción: se confirma tras recibir el identificador; si falla, se revierte. Edición y eliminación son sentencias atómicas por identificador. El cambio de estado actualiza únicamente esa columna, comprobando el estado anterior para evitar sobrescribir cambios de otra ventana. Si la fila ya no existe o su estado cambió, se muestra un error y se pide recargar. Se utilizan `PreparedStatement` y parámetros para los datos; comillas y textos con apariencia de SQL se guardan literalmente.

Un archivo bloqueado puede hacer esperar hasta cuatro segundos al hilo de base de datos. La ventana sigue respondiendo y muestra el resultado real de la operación. Los errores se presentan en pantalla y su causa técnica se registra en la consola.

La base está fuera de `target` y de los recursos empaquetados. Para copiarla como respaldo, cierra WorkFinder y copia el archivo. El esquema inicial no realiza migraciones de esquemas antiguos incompatibles.

Para una ejecución con otro archivo puede usarse la opción de JVM `-Dworkfinder.db.path=RUTA_ABSOLUTA`, también mediante `JAVA_TOOL_OPTIONS`. Las comprobaciones de desarrollo usan archivos aislados dentro de `target`, sin introducir candidaturas ficticias en la base personal.

## Modelo y validación

No había clases ni enums aportados; esta entrega define la primera versión. El modelo es inmutable, tiene campos privados y no importa JavaFX.

| Enum | Valores visibles |
| --- | --- |
| `CategoriaProfesional` | Desarrollo de software, Datos, Sistemas y redes, Diseño, Marketing y comunicación, Administración, Otras. |
| `EstadoCandidatura` | Enviada, En revisión, Entrevista, Oferta, Rechazada, Retirada. |
| `Modalidad` | Presencial, Híbrida, Remota. |

Los selectores usan estos enums sin escritura libre. Una candidatura nueva comienza en Enviada; categoría y modalidad se eligen expresamente.

«En revisión» conserva el nombre interno `EN_PROCESO` que usaba «En proceso». Solo cambia la etiqueta visible; los datos existentes no necesitan una migración ni se sustituyen los valores del enum.

Reglas del servicio:

- Obligatorios: empresa, puesto, categoría, estado, modalidad y fecha de envío.
- Ubicación obligatoria para presencial o híbrida; opcional en remoto.
- Fecha real con formato `dd/mm/aaaa`, sin ajuste automático de días inválidos, y no posterior a hoy.
- Enlace opcional; si se indica, URL HTTP/HTTPS completa con host, sin espacios ni credenciales incrustadas.
- Empresa, puesto y ubicación: máximo 200 caracteres. Enlace: 2000. Notas: 10000.
- Se recortan espacios externos en empresa, puesto, ubicación y enlace. Se conservan saltos de línea y formato de las notas.
- CV y carta son indicadores independientes; no se presupone que se hayan enviado.

El identificador 0 solo indica un modelo nuevo todavía sin persistir. SQLite asigna el identificador definitivo. Editar produce otro objeto con el mismo identificador y no modifica el objeto anterior antes de confirmar la persistencia.

## Archivos y conexiones

```text
src/main/java/com/davidcuadralara/workfinder/
├── Main.java
├── model/
│   ├── Candidatura.java
│   ├── CategoriaProfesional.java
│   ├── EstadoCandidatura.java
│   ├── FiltroCandidaturas.java
│   ├── Modalidad.java
│   └── ResumenCandidaturas.java
├── service/
│   ├── CandidaturaService.java
│   └── ValidacionException.java
├── repository/
│   ├── CandidaturaRepository.java
│   └── PersistenciaException.java
└── ui/
    ├── WorkFinderApplication.java
    ├── MainView.java
    ├── CandidaturaForm.java
    ├── CandidaturaSearch.java
    ├── CandidaturaSummary.java
    ├── EstadoTableCell.java
    └── PanelEntrance.java
```

| Archivo o grupo | Responsabilidad |
| --- | --- |
| `model` | Datos independientes de JavaFX y valores de los selectores. |
| `CandidaturaService` | Validación, normalización, interpretación de fechas, filtrado, cálculo del resumen y operaciones mediante el repositorio recibido por constructor. El reloj también puede recibirse por constructor para verificar fechas. |
| `CandidaturaRepository` | Esquema, conexiones y consultas SQLite. Recibe el archivo por constructor. |
| Excepciones | Errores por campo y fallos de almacenamiento con causa original, sin éxitos simulados. |
| `Main` | Punto de entrada independiente de `Application` para IDE y Maven; lanza `WorkFinderApplication`. |
| `WorkFinderApplication` | Ciclo de vida de JavaFX: construye repositorio, servicio, ejecutor y vista; conecta escena, CSS, apertura y cierre. |
| `MainView` | Eventos, lista completa confirmada y tabla visible. Delega criterios al servicio, mantiene la selección solo si sigue visible y ejecuta JDBC mediante tareas. |
| `CandidaturaForm` | Controles, carga de valores y representación de errores. No contiene SQL ni reglas de negocio. |
| `FiltroCandidaturas` | Criterios inmutables del dominio, independientes de JavaFX. Un enum null representa cualquier valor. |
| `ResumenCandidaturas` | Total, mapas inmutables de recuentos y máximo de la gráfica; incluye ceros para cada valor de los enums. |
| `CandidaturaSearch` | Controles del panel desplegable y espera de 200 ms al escribir; entrega criterios a la vista, sin filtrar datos ni ejecutar SQL. |
| `CandidaturaSummary` | Representación del total, queso por estado, barras con colores por categoría y mensajes de carga; ámbito mediante tooltip y texto accesible. Recibe el resumen calculado; no consulta SQLite ni cuenta candidaturas. |
| `EstadoTableCell` | Selector de estado en las filas, colores mediante pseudoclases CSS y protección frente a eventos del reciclaje de celdas. Delega la selección a `MainView`. |
| `PanelEntrance` | Entrada visual y limpieza del recorte al terminar, omitir o cerrar. |
| `src/main/resources/com/davidcuadralara/workfinder/css/workfinder.css` | Paleta, colores por estado, foco, selección, formulario, errores y diálogo de confirmación. |
| `src/test/java` | Pruebas de validación, filtros, resumen y persistencia real con bases temporales y reloj fijo. |
| `pom.xml`, wrapper, `.gitignore` | Dependencias y ejecución reproducible; exclusión de compilación, archivos locales de IDE y bases de datos. |

Flujo de guardado:

```text
JavaFX: recoger formulario
  → CandidaturaService.interpretarFecha (sin JDBC)
  → Task en ejecutor workfinder-sqlite
      → CandidaturaService.anadir/editar
          → validar y normalizar
          → CandidaturaRepository → SQLite
  → callback del Task en JavaFX
      → éxito: mostrar la fila confirmada y cerrar formulario
      → error: mantener formulario y fila anterior, mostrar explicación
```

La base no se consulta desde eventos o controles. Hay un único ejecutor de trabajo; cada tarea recoge valores inmutables antes de enviarse. El modelo no usa propiedades JavaFX; los adaptadores de columnas están en la vista.

Búsqueda: `CandidaturaSearch` → `FiltroCandidaturas` → `MainView` → `CandidaturaService.filtrar` → tabla. Es un cálculo en memoria, sin JDBC. El servicio combina los criterios y normaliza el nombre para la comparación; la vista representa el resultado y conserva por ID una selección que siga visible. Ni los filtros ni el resumen cambian el esquema de la base.

Resumen: lista completa confirmada → `CandidaturaService.calcularResumen` → `ResumenCandidaturas` → `CandidaturaSummary`. Los recuentos y el máximo de categoría se calculan en el servicio, sin JDBC. La vista solo dibuja y calcula la longitud geométrica de cada barra a partir de los valores recibidos.

El selector sigue el mismo circuito: `EstadoTableCell` → `MainView` → tarea de fondo → `CandidaturaService.cambiarEstado` → `CandidaturaRepository.cambiarEstado`. Solo tras confirmar SQLite se reemplaza el modelo inmutable de la fila. Un fallo restaura el selector desde el modelo anterior; las acciones quedan bloqueadas durante el guardado para evitar envíos duplicados.

## Recorrido manual

1. Abre la aplicación. Si la base está vacía, debe aparecer el mensaje inicial y Añadir habilitado; Editar y Eliminar deben estar desactivados.
2. Pulsa Añadir y guarda con empresa/puesto vacíos: deben aparecer marcas y explicaciones, manteniendo el formulario.
3. Prueba una fecha como `31/02/2026`, una fecha futura y un enlace sin `https://`. Corrige lo indicado.
4. Rellena los campos, selecciona categoría, estado y modalidad, marca CV/carta según proceda y añade notas. Guarda y comprueba la fila.
5. Selecciona la fila y pulsa Editar para consultar todos sus datos, incluidos enlace, documentos y condiciones. Cambia cualquier campo y guarda; debe conservar el identificador.
6. Abre otra edición, cambia valores y pulsa Cancelar: la fila debe conservar sus datos anteriores.
7. Cierra y vuelve a abrir: las candidaturas guardadas deben seguir ahí.
8. Registra otra candidatura para la misma empresa y puesto: deben verse dos filas independientes.
9. Pulsa Eliminar y cancela: la fila debe seguir. Repite confirmando: debe desaparecer tras guardar la eliminación.
10. Redimensiona al mínimo y comprueba el desplazamiento del formulario, los botones siempre visibles y la selección/foco de la tabla. Pasa el cursor sobre textos recortados para leerlos completos.
11. Abre el selector de estado de una fila y elige En revisión: debe mostrarse en amarillo. Elige Rechazada: debe mostrarse en rojo. Recarga o vuelve a abrir y comprueba que el estado persiste. Abrir y cerrar el menú sin elegir otro estado no guarda nada.
12. Pulsa la lupa o Ctrl+F. Busca parte de una empresa sin tildes y en otro uso de mayúsculas. Comprueba que no aparecen candidaturas solo por tener ese texto en el puesto.
13. Combina los tres filtros, comprueba el recuento de resultados y prueba una combinación sin coincidencias.
14. Oculta el panel con × o Esc y vuelve a abrir: debe conservar los criterios. Pulsa Limpiar: deben volver todas las filas.
15. Con un filtro de Enviada, cambia una fila a Rechazada: debe salir del resultado después de guardarse. Limpia y comprueba que sigue en la base con su estado nuevo.
16. Añade o edita una candidatura que quede fuera de los criterios: debe informar del guardado y permitir verla al limpiar. Recarga manteniendo la búsqueda y comprueba la conservación de criterios.
17. Comprueba el total, la gráfica de queso y la leyenda por estado. Pasa el cursor por los sectores para ver cantidad y porcentaje, y prueba su foco con Tab. Filtra la tabla y verifica que el resumen mantiene todas las candidaturas.
18. Cambia la categoría y el estado de una candidatura: deben actualizarse sus recuentos y la gráfica. Añade y elimina filas, comprobando el total después de confirmar.
19. Desplaza el resumen para leer todas las categorías en el tamaño mínimo. Comprueba nombres completos, cantidades y barras de cero.
20. Prueba Ctrl+N, Ctrl+S y F5. Un guardado inválido con Ctrl+S debe conservar los datos y mostrar las ayudas.
21. Con una base de pruebas vacía, comprueba total 0 y categorías/estados en cero. Si falla la apertura, deben mostrarse guiones y un mensaje de error; Recargar permite reintentar.

## Verificaciones de esta entrega

El 7 de octubre de 2026, en Windows con JDK 17.0.17:

- `mvnw verify`: **54 pruebas, 0 fallos, 0 errores**, y empaquetado correcto.
- Persistencia con SQLite real: esquema idempotente, reapertura, todos los campos, identificadores independientes, edición/eliminación por ID, orden de fechas, textos con comillas, fallos SQL, rutas inválidas y datos almacenados incompatibles.
- JavaFX con base de QA: alta, validación y conservación de valores, edición, fallo SQL conservando formulario/fila, cancelar y confirmar eliminar, eliminación fallida, reapertura, cancelación de formulario y reintento tras fallo inicial.
- Ejecución del trabajo JDBC en segundo plano y resultados en JavaFX, con bloqueo de envíos duplicados.
- Revisión visual del formulario en tamaño inicial y mínimo, errores, filas y confirmación; se conserva la paleta.
- Cambio de estado con SQLite real: conservación de otras columnas y otras filas, reapertura, fallo SQL y reintento, validación previa y rechazo de cambios desde una fila desactualizada.
- JavaFX con base aislada: seis colores y opciones del enum, abrir/cancelar el menú sin escrituras, guardado en segundo plano, conservación de la selección, fallo SQL con restauración del valor/color, reintento, recarga, reciclaje de celdas y coherencia con el formulario de edición.
- Lanzador independiente: arranque del `Main` real con classpath normal y base aislada, comprobación del formulario y cierre normal; Maven apunta al mismo punto de entrada. No se han probado manualmente todos los IDE.
- Distribución revisada en tamaño inicial y mínimo: tabla sin encabezado ni espacio reservado, lupa junto a Recargar, formulario con título solo mientras está abierto y recuperación de la tabla al cancelar.
- Filtros: 10 pruebas de servicio para empresa parcial, mayúsculas/tildes, caracteres literales, combinación de criterios, resultados vacíos, orden e inmutabilidad, limpieza sin pérdida de datos y estados confirmados en SQLite.
- JavaFX de fase 3 con base aislada: búsqueda y filtros sin nuevas consultas JDBC, Ctrl+F/Esc, ocultar/limpiar, selección que sale del resultado, fallo y reintento de estado con filtros, alta/edición fuera del resultado con aviso, cancelar, recarga, eliminación confirmada/cancelada y revisión en tamaño mínimo.
- Resumen: 8 pruebas de servicio para lista vacía, todos los estados/categorías, candidaturas duplicadas por empresa, sumas y escala, inmutabilidad, independencia del filtrado, cambios confirmados y fallos SQL.
- JavaFX de fase 4 con bases aisladas: total/recuentos/barras tras alta, edición, estado y eliminación; ámbito global con filtros; fallo SQL conservando cifras; proporciones de barras y ceros; tamaño inicial/mínimo y desplazamiento; vacío, apertura fallida y reintento; recarga fallida conservando datos; Ctrl+N/Ctrl+S/F5.
- Gráfica de queso por estado: comprobación JavaFX de los seis colores, cantidades y porcentajes, foco y tooltip, un único estado al 100 %, leyenda con ceros, actualización, base vacía, carga fallida y recarga conservando datos. Revisada integrada en la ventana inicial y mínima.
- Colores por categoría y panel sin encabezado: compilación y empaquetado correctos; comprobación JavaFX de los siete colores en etiquetas y barras, total situado arriba sin huecos y revisión inicial/mínima. Repetido el recorrido integrado de filtros, cambios confirmados, datos vacíos y recuperación ante errores con bases aisladas.
- Flujo de la barra con más candidaturas: compilación y empaquetado correctos; comprobación JavaFX del avance del reflejo, escala estable, cambio del máximo, empates, base vacía, pausa al ocultar, reanudación, cierre y desactivación de animaciones. Repetido el recorrido integrado con bases de QA.
- Las capturas y la comprobación temporal de UI se guardan en `target`, excluido de Git. Las pruebas de servicio/repositorio sí están en `src/test/java` y se ejecutan con `verify`.

Queda por revisar manualmente el recorrido completo con teclado y tecnologías de asistencia. Búsqueda, filtros y resumen están comprobados. No se han generado instaladores ni probado manualmente todos los IDE.

En el entorno de desarrollo se usó `-Dmaven.repo.local=C:\Users\David\.m2\repository` para seleccionar la caché del usuario; en una terminal normal bastan los comandos anteriores.

## Estado de las fases

Ajuste de diseño aplicado: eliminado el encabezado «Candidaturas» sobre la tabla, sin reservar su espacio. La tabla comienza más arriba y la lupa está en la barra inferior de acciones, junto a Recargar. Los títulos del formulario solo ocupan espacio al añadir o editar. Se conserva la paleta y la animación de entrada.

| Fase | Estado |
| --- | --- |
| 1 | Completa: estructura Maven, ventana, CSS y animación de entrada. |
| 2 | Completa: modelo, validación y CRUD con SQLite/JDBC. |
| 3 | Completa: selector de estado, colores, búsqueda por empresa y filtros. |
| 4 | Completa: total, estados, gráfica por categoría, actualización y usabilidad. |

## Cambios visuales aplicados

Se han quitado del panel lateral el título «Resumen» y el texto «Todas las candidaturas. Los filtros solo afectan a la tabla.». El total y las gráficas comienzan más arriba, sin reservar el espacio anterior. Las estadísticas siguen calculándose sobre todas las candidaturas; el ámbito se puede consultar en el tooltip del total y como texto accesible.

Cada categoría profesional tiene un color estable en su etiqueta y su barra. Son tonos suaves, afines a grafito, crema y cobre. Las etiquetas y cantidades siguen visibles, incluso para categorías en cero. Los colores por estado y su gráfica de queso se mantienen independientes.

| Categoría | Tono | Color |
| --- | --- | --- |
| Desarrollo de software | Cobre | `#E9B17C` |
| Datos | Verde agua | `#93C7BD` |
| Sistemas y redes | Azul grisáceo | `#94B4C6` |
| Diseño | Lavanda | `#B5A5C7` |
| Marketing y comunicación | Rosa apagado | `#D19FAD` |
| Administración | Oliva claro | `#B6BE91` |
| Otras | Arena | `#C7BAA4` |

## Retomar desde este punto

Abre esta carpeta como proyecto Maven en el editor o como proyecto local en Codex y consulta este README. Están terminadas las cuatro fases: CRUD, estados con colores y recuperación ante errores, búsqueda por empresa y filtros, total/recuentos y gráfica por categoría. El resumen representa todas las candidaturas y se actualiza después de los cambios confirmados. Implementa solo el siguiente paso que solicite David. Ya se ha quitado el encabezado «Candidaturas», subido la tabla y reubicado la lupa junto a Recargar. También se han quitado el título y la introducción del panel lateral y aplicado colores propios a las categorías. El punto de entrada para IDE y Maven es `com.davidcuadralara.workfinder.Main`.

Se mantienen Java 17, JavaFX, Maven, SQLite/JDBC y el paquete `com.davidcuadralara.workfinder`. La interfaz llama al servicio y este al repositorio; el modelo es independiente de JavaFX y JDBC se ejecuta fuera del hilo de la interfaz. Conserva la paleta grafito, crema y cobre y la animación de entrada.

El traslado de la carpeta no cambia la ruta de la base de datos personal (`C:\Users\David\.workfinder\workfinder.db`). Las bases y capturas de QA que están en `target` son temporales y no contienen los datos personales.

Referencias de configuración: [OpenJFX con Maven](https://openjfx.io/openjfx-docs/#maven), [SQLite JDBC de Xerial](https://github.com/xerial/sqlite-jdbc) y [JUnit 5](https://junit.org/junit5/docs/5.10.5/user-guide/).
