## Requisitos
Java 17 o superior.
Eclipse IDE con JUnit 5.

## Importar el proyecto
1. Abra Eclipse.
2. Seleccione File > Import.
3. Seleccione General > Existing Projects into Workspace.
4. Elija la carpeta de este proyecto (la que contiene `src`, `test` y `datos`).
5. Seleccione club_deportivo_seneca y pulse Finish.

## Ejecutar las consolas
Hay un programa (metodo `main`) por cada tipo de usuario, todos en `src/cds/consola`:

| Usuario | Clase | Usuario de prueba |
|---|---|---|
| Administrador | `ConsolaAdministrador` | admin / admin123 |
| Socio | `ConsolaSocio` | ana / ana123, carlos / carlos123 |
| Empleado (entrenador o fisioterapeuta) | `ConsolaEmpleado` | leo / leo123 (entrenador), paula / paula123 (fisioterapeuta) |

1. Abra la clase y seleccione Run As > Java Application.
2. El directorio de trabajo debe ser la carpeta del proyecto (es lo predeterminado en Eclipse),
   para que se encuentre `datos/club.json`.
3. Inicie sesion con su login y password.
4. Al elegir `0` en el menu principal, los cambios se guardan en el mismo `datos/club.json`.

## Estructura de la consola
- `ConsolaBase`: carga los datos, autentica, muestra el menu del rol y guarda al salir.
- `Entrada`: lee y valida todas las entradas (numeros, fechas, horas, s/n, listas).
- `Menu` y `Accion`: menu numerado reutilizable; para agregar una opcion basta con `menu.agregar("texto", accion)`.
- Un menu por area: `MenuAdminPersonal`, `MenuAdminDeportes`, `MenuAdminTienda`, `MenuEntrenador`,
  `MenuFisioterapeuta`, `MenuCompras` y `RegistroPartido`.

## Pruebas
Las pruebas JUnit 5 estan en la carpeta `test`.
`test/cds/historias` tiene una clase por historia de usuario del banco oficial; algunas tienen clases `@Nested`
que prueban cada criterio de aceptacion.
