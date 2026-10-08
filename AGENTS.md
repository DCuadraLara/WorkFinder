# Flujo Git de WorkFinder

Estas instrucciones se aplican a los colaboradores y agentes que trabajen en este repositorio.
Remoto autorizado: `https://github.com/DCuadraLara/WorkFinder.git`; también se admite la URL SSH equivalente.

Al completar cada tarea de desarrollo, realiza commit y push sin solicitar confirmación adicional. Esta autorización no modifica los permisos ni las protecciones del entorno.

1. Comprueba la raíz con `git rev-parse --show-toplevel`, la rama con `git branch --show-current` y las URL de lectura y escritura de `origin`. Ambas deben corresponder al remoto autorizado. Si falta el remoto o HEAD está desacoplado, conserva el trabajo y comunica el problema; no cambies de rama ni publiques desde otro repositorio.
2. Revisa el estado, los diffs y los archivos nuevos. Ejecuta `mvn verify`; si Maven no está instalado, usa `./mvnw verify` o `.\mvnw.cmd verify`. Corrige los fallos y repite la verificación antes de publicar.
3. Añade únicamente los cambios de la tarea mediante rutas explícitas o selección de fragmentos. Conserva los cambios previos del usuario, incluidos los preparados en el índice. Excluye archivos generados, `target`, bases personales, configuración privada y credenciales. Revisa el diff preparado; no uses `git add .` ni `git add -A` sin acotar su alcance.
4. Crea un commit descriptivo en inglés siguiendo Conventional Commits, como `fix(ui): improve table readability`. Si no hay cambios, no crees un commit vacío.
5. Haz push de la rama actual a `origin` con una referencia explícita. Si falta seguimiento, configúralo para esa misma rama. No cambies de rama ni de remoto para publicar.
6. Ante conflictos, rechazo del push o falta de autenticación, conserva el trabajo y comunica el problema. No uses force push, incluido `--force-with-lease`, ni sobrescribas el historial remoto. No descartes cambios, configures acceso global ni desactives protecciones, hooks o comprobaciones de seguridad y certificados. Limita cualquier configuración necesaria al repositorio.
7. Resume los cambios, las comprobaciones y el resultado del commit y push. Incluye el identificador del commit y la rama; distingue un commit local de una publicación confirmada y explica lo que quede pendiente.

Localiza siempre el proyecto mediante Git; no dependas de una ruta personal ni del directorio de la conversación.
