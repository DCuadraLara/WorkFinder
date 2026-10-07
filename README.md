# WorkFinder

Aplicación de escritorio para organizar candidaturas a prácticas y empleo, desarrollada con **Java 17, JavaFX, Maven y SQLite**.

![Vista principal de WorkFinder con candidaturas y estadísticas](docs/screenshots/workfinder-main.png)

## Funcionalidades

- Añadir, consultar, editar y eliminar candidaturas con sus condiciones, enlace, fecha de envío, CV, carta de presentación y notas.
- Cambiar el estado directamente desde la tabla.
- Buscar por empresa y filtrar por estado, categoría profesional y modalidad.
- Consultar el total de candidaturas, la distribución por estado y las barras por categoría.

Cada fila representa una candidatura, aunque varias pertenezcan a la misma empresa. Las estadísticas incluyen todas las candidaturas; los filtros solo afectan a la tabla.

## Capturas

<details>
<summary>Formulario de edición</summary>

![Formulario de edición de una candidatura en WorkFinder](docs/screenshots/workfinder-form.png)

</details>

<details>
<summary>Búsqueda y filtros</summary>

![Tabla filtrada por categoría profesional en WorkFinder](docs/screenshots/workfinder-search.png)

</details>

## Ejecutar

Necesitas un **JDK 17** y conexión a Internet para descargar las dependencias la primera vez. El proyecto incluye Maven Wrapper; no requiere instalar Maven, el SDK de JavaFX ni un servidor de base de datos.

Desde la raíz del proyecto, en Windows:

```powershell
.\mvnw.cmd javafx:run
```

En Linux o macOS:

```sh
sh ./mvnw javafx:run
```

También puedes importar el proyecto Maven en tu IDE, seleccionar JDK 17 y ejecutar `com.davidcuadralara.workfinder.Main`.

## Pruebas

Para compilar, ejecutar las pruebas y generar el JAR:

```powershell
.\mvnw.cmd verify
```

En Linux o macOS, usa `sh ./mvnw verify`. Las pruebas de validación, filtros, estadísticas y persistencia utilizan bases temporales.

El JAR se genera en `target`; para abrir la aplicación con sus dependencias JavaFX, utiliza `javafx:run`.

## Datos locales

SQLite guarda las candidaturas en `${user.home}/.workfinder/workfinder.db` (en Windows, `%USERPROFILE%\.workfinder\workfinder.db`). La base se crea automáticamente al abrir y conserva los datos entre sesiones, fuera del proyecto y de `target`.

Para hacer una copia de seguridad, cierra WorkFinder y copia ese archivo.
